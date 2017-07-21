package com.fr.cra.analysis.checkstyle

import java.io.{File, InputStream}
import java.net.URL

import com.fr.cra.analysis._
import com.fr.cra.config.repo.{AoRepoConfig, AoStaticAnalyzerSettings, ConfigType}
import com.fr.cra.reposync.CachedRepoInformation
import com.fr.cra.{Logging, Utils}
import com.puppycrawl.tools.checkstyle.api.{CheckstyleException, Configuration}
import com.puppycrawl.tools.checkstyle.{Checker, ConfigurationLoader, PropertiesExpander}
import org.xml.sax
import org.xml.sax.InputSource

import scala.collection.JavaConverters
import scala.runtime.ObjectRef

/**
  * checkstyle 静态分析处理单元
  * Created by rinoux on 16/8/4.
  */
class CheckstyleProcessor(cachedRepoInfo : CachedRepoInformation) extends StaticAnalysisProcessor with Logging{


  /**
    * 处理
    * @param review 检查工具模型
    * @return
    */
  override def process(review : Review) : AnalyzerResult = {

    val initialClassLoader : ClassLoader = Thread.currentThread().getContextClassLoader
    Thread.currentThread().setContextClassLoader(getClass.getClassLoader)
    try {
      //com.puppycrawl.tools.checkstyle.Checker
      val checker : Checker = new Checker
      //为checker设置类加载器
      checker.setClassLoader(getClass.getClassLoader)
      checker.setModuleClassLoader(getClass.getClassLoader)

      val pullRequestDir : File = cachedRepoInfo.getCachedPRCheckoutDir(review.pullRequest)
      val config1 : Configuration = handleSuppressionFilters(createCheckstyleConfig(pullRequestDir, review.repoConfig), pullRequestDir)
      val config : Configuration = createCheckstyleConfig(pullRequestDir, review.repoConfig)
      //添加配置
      checker.configure(config)

      val auditListener : CheckstyleAuditListener = new CheckstyleAuditListener
      //添加监听者
      checker.addListener(auditListener)
      //检查处理java文件
      checker.process(JavaConverters.seqAsJavaListConverter(getFilesToProcess(review, pullRequestDir)).asJava)
      checker.destroy()
      //返回结果
      auditListener.reviewResult
    } catch {
      case e : CheckstyleException => throw new RuntimeException(e.getMessage, e)
    } finally {
      Thread.currentThread().setContextClassLoader(initialClassLoader)
    }
  }
  override def getName : String = {
    CheckstyleProcessor.Name
  }

  /**
    * 创建评论
    * @param violation
    * @param formatter
    * @param repoConfig
    * @return
    */
  override def createPullRequestComment(violation : Violation, formatter : ViolationCommentFormatter, repoConfig : AoRepoConfig) : String = {
    formatter.formatAsMultilineText(violation.message, repoConfig.isSeverityIconShown, "")
  }
  override def getVersion : Option[String] = {
    Option(CheckstyleProcessor.Version)
  }
  override def getLogoPath : String = {
    CheckstyleProcessor.LogoPath
  }
  override def getBuiltinConfigPath : Option[String] = {
    Option(CheckstyleProcessor.BuiltinConfigPath)
  }
  protected override def getFileExtensionsToProcess : Set[String] = {
    Set("java")
  }

  /**
    * 此处存疑,没有存在的必要性
    * @param configuration
    * @param pullrequestDir
    * @return
    */
  def handleSuppressionFilters(configuration: Configuration, pullrequestDir: File): Configuration = {
    val handler : SuppressionFilePathTransformer = new SuppressionFilePathTransformer
    handler.transformRelativeToAbsolutePath(configuration, pullrequestDir)
  }

  /**
    * 根据repoConfig创建对应checkstyle配置
    * @param pullRequestDir
    * @param repoConfig
    * @return
    */
  def createCheckstyleConfig(pullRequestDir: File, repoConfig: AoRepoConfig) :Configuration = {
    val checkstyleSettings : AoStaticAnalyzerSettings = getAnalyzerSettings(repoConfig)
    val expander = new PropertiesExpander(System.getProperties)
    val aOmitIgnoredModules = true
    ConfigType.from(checkstyleSettings.getConfigFrom) match {
      case ConfigType.BUILTIN =>
        var checkstyleConfig : Configuration = null
        com.fr.cra.using(this.getClass.getResourceAsStream(CheckstyleProcessor.BuiltinConfigPath))(is => {
          checkstyleConfig = ConfigurationLoader.loadConfiguration(new InputSource(is), expander, aOmitIgnoredModules)
        })
        checkstyleConfig
      case ConfigType.FROM_REPO =>
        var checkstyleConfig : Configuration = null
        val configPathInRepo = checkstyleSettings.getConfigRepoPath
        val checkstyleConfigPath = new File(pullRequestDir, configPathInRepo).getAbsolutePath
        if (Utils.isFileReadable(checkstyleConfigPath)) {
         checkstyleConfig =  ConfigurationLoader.loadConfiguration(checkstyleConfigPath, expander, aOmitIgnoredModules)
        } else {
          throw new IllegalStateException("Checkstyle config file " + configPathInRepo + " not found")
        }
        checkstyleConfig
      case ConfigType.FROM_URL =>
        val checkstyleConfigUrl = checkstyleSettings.getConfigUrl
        var checkstyleConfig : Configuration = null
        if (isLegalUrlOrPathOnFilesystem(checkstyleConfigUrl)) {
          if (Utils.isUrl(checkstyleConfigUrl)) {
            com.fr.cra.using(new URL(checkstyleConfigUrl).openStream())(in => {
              checkstyleConfig = ConfigurationLoader.loadConfiguration(new InputSource(in), expander, aOmitIgnoredModules)
            })
          } else {
            checkstyleConfig = ConfigurationLoader.loadConfiguration(checkstyleConfigUrl, expander, aOmitIgnoredModules)
          }
        } else {
          throw new IllegalStateException("Checkstyle config file " + checkstyleConfigUrl +  " not found")
        }
        checkstyleConfig
    }
  }

  /**
    * 获取待处理文件
    * @param review
    * @param pullRequestDir
    * @return
    */
  protected def getFilesToProcess(review: Review, pullRequestDir: File): Seq[File] = {
    //直接返回
    review.reviewFiles.map(f => new File(pullRequestDir, f.filePath.toString))

  }

  /**
    * 判断是来自文件路径还是url
    * @param checkstyleConfigUrl
    * @return
    */
  def isLegalUrlOrPathOnFilesystem(checkstyleConfigUrl: String): Boolean ={
    (!Utils.isUrl(checkstyleConfigUrl) && Utils.isFileReadable(checkstyleConfigUrl)) || (Utils.isUrl(checkstyleConfigUrl) && Utils.isReachable(checkstyleConfigUrl))
  }
}

/**
  * 定义checkstyle的一些信息:名称、版本、错误详情、logo、检查规则文件
  */
object CheckstyleProcessor extends AnyRef {
  val Name : String = "Checkstyle"
  val Version : String = "7.1"
  val ErrorDetailsLink : String = "http://users.csc.calpoly.edu/~jdalbey/SWE/Tools/CheckstyleErrorsExplained.html"
  val LogoPath : String = "/img/tools/checkstyle-logo.png"
  val BuiltinConfigPath : String = "/rules/checkstyle-google-checks.xml"
}
