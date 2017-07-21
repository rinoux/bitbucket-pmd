package com.fr.cra.reposync

import java.util.concurrent.ExecutorService

import com.atlassian.bitbucket.event.repository.RepositoryPushEvent
import com.atlassian.bitbucket.i18n.I18nService
import com.atlassian.bitbucket.pull.{PullRequest, PullRequestService}
import com.atlassian.bitbucket.repository.Repository
import com.atlassian.bitbucket.scm.git.command.GitCommandBuilderFactory
import com.atlassian.event.api.EventListener
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.atlassian.scheduler.SchedulerService
import com.fr.cra.Logging
import com.fr.cra.analysis.Review
import com.fr.cra.branch.ShouldAnalyzeBranchChecker
import com.fr.cra.config.repo.RepoConfigDao
import com.fr.cra.pullrequest.{PullRequestReviewer, ReviewFilesCollector}
import com.fr.cra.statistics.CodeReviewStatisticsDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import scala.collection.JavaConverters
import scala.collection.immutable.HashSet
import scala.concurrent.{ExecutionContext, Future}


/**
  * Created by rinoux on 16/8/9.
  */
@Component
class PushListener @Autowired()(repoConfigDao : RepoConfigDao,
                                cachedRepoInfo : CachedRepoInformation,
                                schedulerService : SchedulerService,
                                pullRequestReviewer : PullRequestReviewer,
                                shouldAnalyzeBranchChecker : ShouldAnalyzeBranchChecker,
                                codeReviewResultsDao : CodeReviewStatisticsDao,
                                pullRequestService : PullRequestService,
                                reviewFilesCollector : ReviewFilesCollector,
                                @ComponentImport gitCommandFactory : GitCommandBuilderFactory,
                                executorService : ExecutorService,
                                i18nService : I18nService) extends AnyRef with Logging {
  implicit val executionContext = ExecutionContext.fromExecutorService(executorService)
  @EventListener
  def onRepositoryPushed(event : RepositoryPushEvent) : Unit = {
    val repository = event.getRepository
    //获取所有需要分析的分支
    val branchesToAnalyze = JavaConverters.collectionAsScalaIterableConverter(event.getRefChanges).asScala.toList.filter(rc => {
      //判断
      shouldAnalyzeBranchChecker.shouldAnalyze(repository, rc.getRef.getId)
    })
    if (branchesToAnalyze.nonEmpty) {
      Future{
        log.debug("FINE-CRA: Push received while being active on this repository")
        cachedCloneIfNecessary(repository)
        fetchAllRefsForCache(repository, branchesToAnalyze.map(_.getRef.getId))
        runOnChangesetsInOpenPRs(repository, branchesToAnalyze.map(_.getRef.getId))
      }(executionContext)
    }

  }
  def cachedCloneIfNecessary(repo: Repository): Unit = {
    new CachedRepoCloner(gitCommandFactory, cachedRepoInfo).cloneIfNotExisting(repo)
  }
  def fetchAllRefsForCache(repo: Repository, sourceBranchIds : List[String]): Unit = {
    new CachedRepoFetcher(gitCommandFactory, repo, cachedRepoInfo).fetchAll(sourceBranchIds)
  }
  def runOnChangesetsInOpenPRs(repo: Repository, sourceBranchIds : List[String]): Unit = {
    searchOpenPRsWithThoseSourceBranches(repo, sourceBranchIds).foreach(pr => {
      try {
        codeReviewResultsDao.setStatus(pr, uptoDate = false)
        val filesToReview = reviewFilesCollector.collectFilesChanges(pr)
        val review = new Review(filesToReview, pr, repoConfigDao.getOrCreate(repo))
        pullRequestReviewer.execute(review)
      } catch {
        case e: Exception =>
          log.error("FINE-CRA: Failed to execute code review analysis on pull request " + pr.getId, e)
      } finally {
        codeReviewResultsDao.setStatus(pr, uptoDate = true)
      }
    })
  }
  def searchOpenPRsWithThoseSourceBranches(repo: Repository, sourceBranchIds : List[String]): HashSet[PullRequest] = {
    new BranchesInOpenPRsCollector(pullRequestService).collectOpenPullRequests(sourceBranchIds, repo)
  }




}
