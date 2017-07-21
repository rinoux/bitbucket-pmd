package com.fr.cra.analysis.pmd

import java.io.File
import java.util

import com.atlassian.bitbucket.pull.PullRequest
import com.fr.cra.analysis._
import com.fr.cra.analysis.pmd.wrapper.{FrPMD, FrRuleSetsWrapper}
import com.fr.cra.config.repo.{AoRepoConfig, ConfigType}
import com.fr.cra.reposync.CachedRepoInformation
import com.fr.cra.{Logging, Utils}
import net.sourceforge.pmd._
import net.sourceforge.pmd.lang.{Language, LanguageFilenameFilter}
import net.sourceforge.pmd.renderers.Renderer
import net.sourceforge.pmd.util.datasource.{DataSource, FileDataSource}
import org.apache.commons.io.FileUtils

import scala.collection.mutable.ListBuffer
import scala.collection.{JavaConverters, mutable}
import scala.xml.{Elem, NodeBuffer, XML}

/**
  * PMD分析器，5.1.3以上版本不兼容
  * Created by rinoux on 16/8/25.
  */
class PMDProcessor(cachedRepoInfo : CachedRepoInformation) extends StaticAnalysisProcessor with Logging {
  var currentPullRequest: PullRequest = null
  override def process(review : Review) : AnalyzerResult = {
    //toReviewResult(doPMD(review, createPMDConfig(review, isUrlOrPath = true)))
    currentPullRequest = review.pullRequest
    toReviewResult(doPMD(review))
  }
  def toReviewResult(renderer: Renderer) : AnalyzerResult = {
    renderer.asInstanceOf[PMDCollectorRenderer].reviewResult
  }


  override def getName : String = PMDProcessor.Name
  override def getVersion : Option[String] = Option(PMDProcessor.Version)
  override def getLogoPath : String = PMDProcessor.LogoPath
  override def getBuiltinConfigPath : Option[String] = Option(PMDProcessor.BuiltinConfigPath)
  protected override def getFileExtensionsToProcess : Set[String] = Set("java")

  /**
    * 根据冲突创建评论标签
    * @param violation
    * @param formatter
    * @param repoConfig
    * @return
    */
  override def createPullRequestComment(violation : Violation, formatter : ViolationCommentFormatter, repoConfig : AoRepoConfig) : String = {
    val message = violation.message
    val description = violation.detailedMessage
    var text : String = null
    //判断message和description是否接近，不相同的话就把description当作输出文本
    if (description.nonEmpty && Utils.getLevenshteinDistance(message.trim, description.trim) > 10) {
      text = StringBuilder.newBuilder.append("*").append(message).append("*: ").append(description).toString()
    } else {
      text = message
    }
    if (pmdExamplesActivated(repoConfig)) {
      formatter.formatAsMultilineText(text, repoConfig.isSeverityIconShown, violation.examples)
    } else {
      formatter.formatAsMultilineText(text, repoConfig.isSeverityIconShown, "")
    }
  }

  /**
    * 创建pmd配置PMDConfiguration
    * @param review
    * @return
    */
  def createPMDConfig(review: Review, isUrlOrPath : Boolean) : PMDConfiguration = {
    val pmdConfig = new PMDConfiguration
    pmdConfig.setReportFormat(classOf[PMDCollectorRenderer].getCanonicalName)
    val pullRequestDir = cachedRepoInfo.getCachedPRCheckoutDir(review.pullRequest)
    if (isUrlOrPath) {
      val pmdConfigPath = getPmdConfigPath(pullRequestDir, review.repoConfig)
      pmdConfig.setRuleSets(pmdConfigPath)
    }
    pmdConfig
  }

  def doPMD(review: Review) : Renderer = {
    val analyzerSetting = review.repoConfig.getStaticAnalyzerSettings.find(setting => setting.getName.equals(PMDProcessor.Name)).get
    val configUrl = analyzerSetting.getConfigUrl
    Option(configUrl) match {
      case Some(url) =>
        if (analyzerSetting.getConfigFrom.equals(ConfigType.FROM_URL.toString) && configUrl.startsWith("jdbc:mysql:")) {
          val pmdConfig = createPMDConfig(review, isUrlOrPath = false)
          var repoName : String = null
          if (currentPullRequest != null) {
            //根据目标仓库的名称确定使用的rule
            repoName = cachedRepoInfo.getRepository(currentPullRequest).getName
          }
          val ruleSets = new FrRuleSetsWrapper(repoName, url).loadPmdRuleSetsFromMysql
          //val ruleSets = new FrRuleSetsWrapper(null, cachedRepoInfo).loadPmdRuleSetsFromXML(cachedRepoInfo.getCraHomeDir + "/pmd-basic.xml")

          val filesToAnalyze = getFilesToAnalyze(review, getApplicableLanguages(pmdConfig, ruleSets))
          val files = new util.LinkedList[DataSource]()
          filesToAnalyze.foreach(d => files.add(d))

          val ctx = new RuleContext()
          val renderer = pmdConfig.createRenderer()
          renderer.start()

          val renderers = new util.LinkedList[Renderer]()
          renderers.add(renderer)

          FrPMD.processFiles(pmdConfig, ruleSets, files, ctx, renderers)
          renderer.end()
          renderer
        } else doPMD(review, createPMDConfig(review, isUrlOrPath = true))
      case None =>
        log.error("FINE-CRA: no suitable url found")
        null
    }
  }
  /**
    * 执行PMD检查
    * @param review
    * @param pmdConfig
    * @return
    */
  def doPMD(review: Review, pmdConfig: PMDConfiguration): Renderer = {
    val ruleSetFactory = RulesetsFactoryUtils.getRulesetFactory(pmdConfig)
    val ruleSets = RulesetsFactoryUtils.getRuleSets(pmdConfig.getRuleSets, ruleSetFactory)
    if (Option(ruleSets).isEmpty) {
      throw new IllegalStateException("No rulesets found to run PMD")
    } else {
      val filesToAnalyze = getFilesToAnalyze(review, getApplicableLanguages(pmdConfig, ruleSets))
      val ctx = new RuleContext()
      val renderer = pmdConfig.createRenderer()
      renderer.start()
      //PMD处理
      PMD.processFiles(pmdConfig, ruleSetFactory, JavaConverters.bufferAsJavaListConverter(filesToAnalyze).asJava, ctx, JavaConverters.seqAsJavaListConverter(Seq(renderer)).asJava)
      renderer.end()
      renderer
    }
  }

  /**
    * 获得待检查的文件
    * @param review
    * @param languages
    * @return
    */
  def getFilesToAnalyze(review: Review, languages : Set[Language]) : ListBuffer[DataSource] = {
    val pullRequestDir = cachedRepoInfo.getCachedPRCheckoutDir(review.pullRequest)
    val changedFiles = review.reviewFiles.map(f => new File(pullRequestDir, f.filePath.toString).getAbsolutePath)
    val langFilter = new LanguageFilenameFilter(JavaConverters.setAsJavaSetConverter(languages).asJava)
    val langFiles = changedFiles.filter(f => langFilter.accept(null, f))
    val filesToAnalyze = new ListBuffer[DataSource]()
    langFiles.foreach(f => filesToAnalyze.+=(new FileDataSource(new File(f))))
    filesToAnalyze
  }


  /**
    * 获得可用的语言
    * @param configuration
    * @param ruleSets
    * @return
    */
  def getApplicableLanguages(configuration : PMDConfiguration, ruleSets: RuleSets) : Set[Language] = {

    val languages = new mutable.HashSet[Language]()
    val discoverer = configuration.getLanguageVersionDiscoverer
    JavaConverters.asScalaSetConverter(ruleSets.getAllRules).asScala.withFilter(_ != null).foreach(rule => {
      val language = rule.getLanguage
      if (!languages.contains(language)) {
        if (RuleSet.applies(rule, discoverer.getDefaultLanguageVersion(language))){
          languages.add(language)
        }
      }
    })
    languages.toSet
  }

  /**
    * 从不同的方式获得pmd的配置文件路径
    * @param pullRequestDir
    * @param repoConfig
    * @return
    */
  def getPmdConfigPath(pullRequestDir : File, repoConfig: AoRepoConfig) : String = {
    val pmdSettings = this.getAnalyzerSettings(repoConfig)
    ConfigType.from(pmdSettings.getConfigFrom) match {
      case ConfigType.BUILTIN =>
        val pmdBasic = new File(this.cachedRepoInfo.getCraHomeDir, "pmd-basic.xml")
        //将内置配置文件的配置复制到pmd-basic.xml
        FileUtils.copyInputStreamToFile(this.getClass.getResourceAsStream(PMDProcessor.BuiltinConfigPath), pmdBasic)
        pmdBasic.getAbsolutePath
      case ConfigType.FROM_REPO =>
        //获得配置文件的路径
        val pmdConfig = new File(pullRequestDir, pmdSettings.getConfigRepoPath)
        if (!pmdConfig.exists()) {
          throw new IllegalStateException("PMD config file " + pmdSettings.getConfigRepoPath + " not found")
        }
        pmdConfig.getAbsolutePath
      case ConfigType.FROM_URL =>
        if (!pmdSettings.getConfigUrl.startsWith("jdbc:mysql:") && isLegalUrlorPathOnFilesystem(pmdSettings.getConfigUrl) && Utils.isUrl(pmdSettings.getConfigUrl)) {
          if (Utils.isUrl(pmdSettings.getConfigUrl)) {
            //直接根据rulesets的url构建pmd的配置文件
            val xml : Elem =
              <ruleset name="Custom ruleset" xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 http://pmd.sourceforge.net/ruleset_2_0_0.xsd">
                <description>this rulesets is construct from url or path</description>
                <rule ref={pmdSettings.getConfigUrl}/>
              </ruleset>
            //来自路径url的文件都会生成为crarepos/pmd-ruleset.xml
            val rulesetPath : String = new File(cachedRepoInfo.getCraHomeDir, "pmd-ruleset.xml").getAbsolutePath
            XML.save(rulesetPath, xml, "UTF-8", xmlDecl = true, null)
            rulesetPath
          } else pmdSettings.getConfigUrl
        } else throw new IllegalStateException("PMD config file " +pmdSettings.getConfigUrl + " not found")
    }
  }
  def pmdExamplesActivated(repoConfig: AoRepoConfig): Boolean = {
    this.getAnalyzerSettings(repoConfig).isViolationExamplesEnabled
  }

  def isLegalUrlorPathOnFilesystem(pmdConfigPath : String) : Boolean = {
    Utils.isUrl(pmdConfigPath) && Utils.isReachable(pmdConfigPath) ||
      !Utils.isUrl(pmdConfigPath) && Utils.isFileReadable(pmdConfigPath)
  }

}
object PMDProcessor extends AnyRef {
  val Name : String = "PMD"
  val Version : String = "5.1.3"
  val LogoPath : String = "/img/tools/pmd-logo.png"
  val BuiltinConfigPath : String = "/rules/pmd-basic.xml"
}
