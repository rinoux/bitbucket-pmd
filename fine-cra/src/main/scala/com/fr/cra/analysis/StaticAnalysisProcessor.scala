package com.fr.cra.analysis

import com.fr.cra.config.repo.{AoRepoConfig, AoStaticAnalyzerSettings}
import org.apache.commons.lang3.StringUtils

/**
  * 静态分析处理器模型，所有分析器包括checkstyle和PMD都是继承此类<br>
  *   定义了分析器的名称、版本、logo文件路径、内置配置文件路径属性；<br>
  *    　定义了抽象方法getFileExtensionsToProcess在子类中指定被处理文件的格式后缀名<br>
  *
  * Created by rinoux on 16/8/8.
  */
abstract class StaticAnalysisProcessor extends AnyRef{

  /**
    * 核心处理方法，子类实现
    * @param review 待分析Review对象
    * @return
    */
  def process(review : Review) : AnalyzerResult

  /**
    * 分析器名称
    * @return
    */
  def getName : String

  /**
    * 版本
    * @return
    */
  def getVersion : Option[String]

  /**
    * logo路径
    * @return
    */
  def getLogoPath : String

  /**
    * 内置配置文件路径（检查规则文件）
    * @return
    */
  def getBuiltinConfigPath : Option[String]
  /**
    *   指定待分析文件格式后缀名
    * @return
    */
  protected def getFileExtensionsToProcess : Set[String]

  /**
    *  根据后缀名找出匹配的文件集
    * @param review
    * @return
    */
  protected def getFilesToProcess(review : Review) : Seq[ReviewFile] = {
    review.reviewFiles.filter(rf => {
      getFileExtensionsToProcess.contains(StringUtils.substringAfterLast(rf.filePath.toString, "."))
    })
  }

  /**
    *   创建pr的评论，子类实现
    * @param violation
    * @param formatter
    * @param repoConfig
    * @return
    */
  def createPullRequestComment(violation : Violation, formatter : ViolationCommentFormatter, repoConfig : AoRepoConfig) : String

  /**
    * 获取分析器的设置,找到相同名称的分析器
    * @param repoConfig repo的配置
    * @return
    */
  protected def getAnalyzerSettings(repoConfig : AoRepoConfig) : AoStaticAnalyzerSettings = {
    repoConfig.getStaticAnalyzerSettings.find(sas => sas.getName.equals(this.getName)).getOrElse(throw new IllegalArgumentException("Must not run when no settings for analyzer"))
  }
}
