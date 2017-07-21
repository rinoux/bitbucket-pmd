package com.fr.cra.mergecheck

import com.atlassian.bitbucket.i18n.I18nService
import com.atlassian.bitbucket.repository.RepositoryService
import com.atlassian.bitbucket.scm.pull.{MergeRequest, MergeRequestCheck}
import com.fr.cra.Logging
import com.fr.cra.config.repo.{AoRepoConfig, RepoConfigDao}
import com.fr.cra.statistics.CodeReviewStatisticsDao

/**
  * 合并代码前检查是否符合合并条件，如不符合则设置merge为禁用
  * Created by rinoux on 16/9/1.
  */
class CodeAnalysisMergeCheck(codeReviewResultsDao : CodeReviewStatisticsDao, 
                             i18nService : I18nService,
                             repositoryService : RepositoryService, 
                             repoConfigDao : RepoConfigDao) extends AnyRef with MergeRequestCheck with Logging {

  override def check(mergeRequest : MergeRequest) : Unit = {
    val pullRequest = mergeRequest.getPullRequest
    val repository = pullRequest.getToRef.getRepository
    if (repoConfigDao.isStatisticsActive(repository)) {
      val repoConfig : AoRepoConfig = repoConfigDao.find(repository).getOrElse(log.error("FINE-CRA: No repository config found for " + repository.getName)).asInstanceOf[AoRepoConfig]

      codeReviewResultsDao.find(pullRequest).foreach(crs => {
        val settings = repoConfig.getStaticAnalyzerSettings
        /**
          * 判断分析器是否启用
          * @param name
          * @return
          */
        def isAnalyzerEnabled(name : String) : Boolean = {
          settings.exists(sas => sas.getName.equals(name) && sas.isEnabled)
        }
        crs.getStaticAnalyzerResults.filter(result => isAnalyzerEnabled(result.getName)).foreach(sar => {
          //分析器名称和设置的名称要匹配,且MaxMergeErrors有值
          settings.find(s => s.getName == sar.getName && Option(s.getMaxMergeErrors).isDefined).foreach(sas => {
            val actualViolations = sar.getErrorCount + sar.getFatalCount
            if (actualViolations > sas.getMaxMergeErrors) {
              //超过最大merge错误容许值,就要veto了
              vetoMerge(mergeRequest, sas.getMaxMergeErrors, actualViolations, sar.getName)
            }
          })
        })
      })
    }
  }

  def vetoMerge(request: MergeRequest, maxErrorsAllowed: Integer, actualErrors: Integer, analyzerName : String) = {
    val summary = i18nService.getMessage("cra.mergecheck.veto.title", analyzerName)
    val detailedMessage = i18nService.getMessage("cra.mergecheck.veto.msg", analyzerName, maxErrorsAllowed, actualErrors)
    request.veto(summary, detailedMessage)
  }
}
