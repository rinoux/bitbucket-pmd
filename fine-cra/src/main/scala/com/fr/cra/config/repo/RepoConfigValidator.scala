package com.fr.cra.config.repo

import java.util.regex.{Pattern, PatternSyntaxException}
import javax.servlet.ServletRequest

import com.atlassian.bitbucket.i18n.I18nService
import com.atlassian.bitbucket.nav.NavBuilder
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.fr.cra.analysis.StaticAnalyzerRegistry
import com.fr.cra.{Logging, Utils}
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import scala.collection.immutable.HashMap

/**
  * 读取ServletRequest的Param参数配置,设置到repoconfig中<br>
  * Created by rinoux on 16/8/15.
  */
@Component
class RepoConfigValidator @Autowired()(i18NService : I18nService,
                                       staticAnalyzerRegistry : StaticAnalyzerRegistry,
                                       @ComponentImport navBuilder : NavBuilder) extends AnyRef with Logging {

  /**
    * 合法性检查<br>
    *   阉割了ctags/bug predct/code narc
    * @param repoConfig
    * @param req
    * @return
    */
  def validate(repoConfig : AoRepoConfig, req : Option[ServletRequest]) : (AoRepoConfig, SoyContext.FieldErrors) = {
    //这个error会接受具体处理的错误,貌似没什么错误要接受,放在这里
    val errors = new HashMap[String, List[String]]()
    handleAnalyzerOptions(repoConfig, req)
    handleBuiltinStaticAnalyzers(req, repoConfig, errors)
    //返回一个tuple
    (repoConfig, errors)
  }

  /**
    * 处理分析器选项<br>
    *   把通用选项从req中取出设置到repoconfig
    * @param repoConfig
    * @param req
    */
  def handleAnalyzerOptions(repoConfig : AoRepoConfig, req : Option[ServletRequest]): Unit = {
    req match {
      case Some(request) =>
        repoConfig.setSeverityIconShown(Option(request.getParameter(RepoConfigSoyData.ShowSeverityIcon)).isDefined)
        repoConfig.setBranchOptions(request.getParameter(RepoConfigSoyData.BranchOptions))
        repoConfig.setBranchOptionsBranches(request.getParameter(RepoConfigSoyData.BranchOptionsBranches))
      case None =>
        repoConfig.setSeverityIconShown(repoConfig.isSeverityIconShown)
        repoConfig.setBranchOptions(repoConfig.getBranchOptions)
        repoConfig.setBranchOptionsBranches(repoConfig.getBranchOptionsBranches)
    }
  }

  def isRegexValid(regex : String) : Boolean = {
    try {
      Pattern.compile(regex)
      true
    } catch {
      case e :PatternSyntaxException =>
        false
    }
  }

  /**
    * 处理内置分析器<br>
    *   把设置选项从req中读取并设置到repoconfig的analyzerSettings
    * @param req
    * @param repoConfig
    * @param errors
    */
  def handleBuiltinStaticAnalyzers(req : Option[ServletRequest], repoConfig : AoRepoConfig, errors : HashMap[String, List[String]]): Unit = {
    staticAnalyzerRegistry.allBuiltInAnalyzers.foreach(processor => {
      val analyzerSettings = repoConfig.getStaticAnalyzerSettings.find(s => s.getName.equals(processor.getName)).getOrElse(throw new IllegalStateException("No settings found for " + processor.getName))
      req.foreach(request => {
        //定义一个从request获取相应配置的方法
        def fromHttpRequest(param : String) : String = {
          request.getParameter(new StringBuilder().append(processor.getName).append(param).toString())
        }
        analyzerSettings.setEnabled(getBoolean(fromHttpRequest(RepoConfigSoyData.Enabled)))
        analyzerSettings.setConfigFrom(fromHttpRequest(RepoConfigSoyData.ConfigFrom))
        analyzerSettings.setConfigRepoPath(fromHttpRequest(RepoConfigSoyData.ConfigRepoPath))
        analyzerSettings.setConfigUrl(fromHttpRequest(RepoConfigSoyData.ConfigUrl))
        analyzerSettings.setMaxMergeErrors(getInteger(fromHttpRequest(RepoConfigSoyData.MaxMergeErrors)))
      })
      if (analyzerSettings.isEnabled) {
        errors.++(validateStaticAnalyzerSettings(analyzerSettings))
      }
    })
  }

  def getInteger(value : String) : Integer = {
    var i: Integer = null
    try {
      i = Integer.parseInt(value)
    } catch {
      case e : Exception =>
        i = null
    }
    i
  }

  def getBoolean(value : String) : Boolean = {
    Option(value).getOrElse("").nonEmpty
  }

  /**
    * 检查StaticAnalyzerSettings中的url,path等内容是否合法
    * @param settings
    * @return
    */
  def validateStaticAnalyzerSettings(settings : AoStaticAnalyzerSettings): Map[String, List[String]] = {
    var errors : scala.collection.mutable.HashMap[String, List[String]] = new scala.collection.mutable.HashMap[String, List[String]]
    val from = ConfigType.from(settings.getConfigFrom)
    if (from.equals(ConfigType.FROM_URL)) {
      if (Option(settings.getConfigRepoPath).getOrElse("").isEmpty) {
        errors.+=(new StringBuilder().append(settings.getName).append(RepoConfigSoyData.ConfigUrl).toString() -> List(i18NService.getMessage("cra.repo.settings.validation.urlnotdefined.msg")))
        rememberFirstTabWithErrors(errors, settings.getName)
      } else if (!Utils.isUrl(settings.getConfigUrl) && Utils.isFileReadable(settings.getConfigUrl)) {
        errors.+=(new StringBuilder().append(settings.getName).append(RepoConfigSoyData.ConfigUrl).toString() -> List(i18NService.getMessage("cra.repo.settings.validation.pathinvalid.msg")))
        rememberFirstTabWithErrors(errors, settings.getName)
      } else if (Utils.isUrl(settings.getConfigUrl) && Utils.isReachable(settings.getConfigUrl)) {
        errors.+=(new StringBuilder().append(settings.getName).append(RepoConfigSoyData.ConfigUrl).toString() -> List(i18NService.getMessage("cra.repo.settings.validation.urlinvalid.msg")))
        rememberFirstTabWithErrors(errors, settings.getName)
      }
    }
    if (Option(settings.getMaxMergeErrors).isDefined) {
      val i = Option(settings.getMaxMergeErrors).get
      if (i < 0) {
        errors.+=(new StringBuilder().append(settings.getName).append(RepoConfigSoyData.ConfigUrl).toString() -> List(i18NService.getMessage("cra.repo.settings.validation.notanumber.msg")))
        rememberFirstTabWithErrors(errors, settings.getName)
      }
    }
    errors.toMap //返回
  }

  /**
    * 记住第一个带错误的Tab
    * @param errors
    * @param analyzerName
    * @return
    */
  def rememberFirstTabWithErrors(errors : scala.collection.mutable.HashMap[String, List[String]], analyzerName : String) : Object = {
    if (!errors.contains("errorTab")) {
      errors.+=("errorTab" -> List(analyzerName))
    } else {
      Unit
    }
  }
}
