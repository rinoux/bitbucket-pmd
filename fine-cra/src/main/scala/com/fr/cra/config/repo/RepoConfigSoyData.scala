package com.fr.cra.config.repo

import java.util

import com.atlassian.bitbucket.nav.NavBuilder
import com.atlassian.bitbucket.repository.Repository
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.fr.cra.Logging
import com.fr.cra.analysis._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import scala.collection.JavaConverters
import scala.collection.immutable.StringOps
import scala.collection.mutable.ListBuffer
import scala.runtime.BoxesRunTime

/**
  * 将repo 配置文件内容的soydata 经navbuilder生成url
  * Created by rinoux on 16/8/15.
  */
@Component
class RepoConfigSoyData @Autowired()(@ComponentImport navBuilder : NavBuilder,
                                      staticAnalyzerRegistry : StaticAnalyzerRegistry) extends AnyRef with Logging {

  def soyResults(repoConfig : AoRepoConfig, repo : Repository, successMsgOrErrors : Option[Either[String, SoyContext.FieldErrors]]) : Map[String, AnyRef] = {
    /**
      * severityIcon,successOrFieldErrors,repositoryDetails,severityTypes,builtinConfigServletPath,
      * branchOptions, staticAnalyzerSettings
      */
    val soyResult = severityIcon.|+|(successOrFieldErrors(successMsgOrErrors).|+|(repositoryDetails(repo).|+|(builtinConfigServletPath).|+|(severityTypes.|+|(branchOptions.|+|(staticAnalyzerSettings)))))
    soyResult.run.apply(repoConfig).toMap
  }
  def severityIcon : SoyContext = {
    new SoyContext(rc => Some((RepoConfigSoyData.ShowSeverityIcon, bool2Boolean(rc.isSeverityIconShown))))
  }
  def bool2Boolean(boolean: Boolean) : java.lang.Boolean = {
    if (boolean) true else false
  }
  def successOrFieldErrors(successMsgOrErrors : Option[Either[String, SoyContext.FieldErrors]]) : SoyContext = {
    new SoyContext(rc => {
      successMsgOrErrors.flatMap{
        case Left(s) =>
          Some(("success", s))
        case Right(validationErrors) =>
          val t = validationErrors.map(kv => (kv._1, JavaConverters.seqAsJavaListConverter(kv._2).asJava))
          Some(("errors", JavaConverters.mapAsJavaMapConverter(validationErrors.map(kv => (kv._1, JavaConverters.seqAsJavaListConverter(kv._2).asJava))).asJava))
      }
    })
  }
  def repositoryDetails(repo : Repository) : SoyContext = {
    new SoyContext(rc => Some(("repository", repo)))
  }

  def builtinConfigServletPath : SoyContext = {
    new SoyContext(rc => Some(("builtinConfigServletPath", navBuilder.pluginServlets().path("cra", "builtin-config").buildAbsolute())))
  }

  def severityTypes : SoyContext = {
    new SoyContext(rc => Some(("severityTypes", buildSeverityTypesList)))
  }
  def buildSeverityTypesList : java.util.List[java.util.Map[String, String]] = {
    val data : ListBuffer[java.util.Map[String, String]] = new ListBuffer[util.Map[String, String]]
    data.+=(JavaConverters.mapAsJavaMapConverter(Map("text" -> FATAL.alternativeText, "value" -> FATAL.toString)).asJava)
    data.+=(JavaConverters.mapAsJavaMapConverter(Map("text" -> ERROR.alternativeText, "value" -> ERROR.toString)).asJava)
    data.+=(JavaConverters.mapAsJavaMapConverter(Map("text" -> WARNING.alternativeText, "value" -> WARNING.toString)).asJava)
    data.+=(JavaConverters.mapAsJavaMapConverter(Map("text" -> INFO.alternativeText, "value" -> INFO.toString)).asJava)
    JavaConverters.bufferAsJavaListConverter(data).asJava
  }
  def branchOptions : SoyContext = {
    new SoyContext(rc => {
      val iterable = Some((RepoConfigSoyData.BranchOptions, rc.getBranchOptions))
      Option(rc.getBranchOptions) match {
        case Some(x) =>
          if (x.nonEmpty) {
            iterable.++(Some(RepoConfigSoyData.BranchOptionsBranches, rc.getBranchOptionsBranches))
          } else iterable.++(Some(RepoConfigSoyData.BranchOptionsBranches, ""))
        case _ =>
          iterable.++(Some(RepoConfigSoyData.BranchOptionsBranches, ""))
      }
    })
  }
  def staticAnalyzerSettings : SoyContext = {
    new SoyContext(rc => {
      staticAnalyzerRegistry.allBuiltInAnalyzers.map(sap => {
        //思路是取出每个sap的配置放到Iterable，最后做一个reduceleft合并
        Some((sap.getName + "Settings", rc.getStaticAnalyzerSettings.find(sas => sas.getName.equals(sap.getName)).getOrElse("?"))).++(Some((sap.getName + "Version", sap.getVersion.getOrElse("?"))))
      }).reduceLeft((a, b) => a.++(b))
    })
  }
}
object RepoConfigSoyData extends AnyRef {
  val ErrorDetails : String = "details"
  val ShowSeverityIcon : String = "showSeverityIcon"
  val BranchOptions : String = "branchOptions"
  val BranchOptionsBranches : String = "branchOptionsBranches"

  val Enabled : String = "Enabled"
  val ConfigFrom : String = "ConfigFrom"
  val ConfigRepoPath : String = "ConfigRepoPath"
  val DefaultSeverity : String = "DefaultSeverity"
  val ConfigUrl : String = "ConfigUrl"
  val MaxMergeErrors : String = "MaxMergeErrors"
  val ViolationExamplesEnabled : String = "ViolationExamplesEnabled"
}
