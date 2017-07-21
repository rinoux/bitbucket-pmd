package com.fr.cra.pullrequest

import com.atlassian.bitbucket.pull.PullRequest
import com.atlassian.bitbucket.repository.{Repository, RepositoryService}
import com.atlassian.bitbucket.scm.git.command.GitCommandBuilderFactory
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.fr.cra.Logging
import com.fr.cra.analysis.Review
import com.fr.cra.branch.ShouldAnalyzeBranchChecker
import com.fr.cra.config.repo.{AoRepoConfig, RepoConfigDao}
import com.fr.cra.reposync.{CachedRepoCloner, CachedRepoFetcher, CachedRepoInformation}
import com.fr.cra.statistics.CodeReviewStatisticsDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
  * 代码检查分析器启动入口
  * Created by rinoux on 16/8/8.
  */
@Component
class CodeReviewAnalysisRunner @Autowired()(repositoryService : RepositoryService,
                                            repoConfigDao : RepoConfigDao,
                                            codeReviewStatisticsDao : CodeReviewStatisticsDao,
                                            pullRequestReviewer : PullRequestReviewer,
                                            @ComponentImport gitCommandFactory : GitCommandBuilderFactory,
                                            shouldAnalyzeBranchChecker : ShouldAnalyzeBranchChecker,
                                            cachedRepoInfo : CachedRepoInformation,
                                            reviewFilesCollector : ReviewFilesCollector,
                                            pullRequestErrorHandler : PullRequestErrorHandler) extends AnyRef with Logging {
  /**
    * 启动分析
    * @param pullRequest pr
    */
  def runAnalysisOn(pullRequest : PullRequest) : Unit = {
    if(shouldAnalyzeBranchChecker.shouldAnalyze(pullRequest)) {
      //repoTo用于获取目标仓库的cra配置
      val repoTo = pullRequest.getToRef.getRepository
     //repoFrom用于缓存合并来源仓库的代码，实现对新增文件的检查
      val repoConfig = getConfigFor(repoTo)
      analyzePullRequest(pullRequest, repoConfig)
      //此处已放弃证书检查
    }
  }

  def analyzePullRequest(pullRequest: PullRequest, repoConfig: AoRepoConfig): Unit = {
    val repoFrom = pullRequest.getFromRef.getRepository
    //将pr的变更文件取出放到一个seq
    val filesToReview = reviewFilesCollector.collectFilesChanges(pullRequest)
    //组装成一个Review对象
    val review = new Review(filesToReview, pullRequest, getConfigFor(pullRequest.getToRef.getRepository))
    try {
      //将改pr的检查数据dao状态设置为未同步到最新
      codeReviewStatisticsDao.setStatus(review.pullRequest, uptoDate = false)
      //克隆repo
      cloneInCacheIfNotExisting(repoFrom)
      //抓取
      fetchCachedRefs(pullRequest, repoFrom)
      //由pullRequestReviewer来执行review
      pullRequestReviewer.execute(review)
    } catch {
      case e: Exception =>
        log.error("FINE-CRA: Failed to execute code review analysis", e)
        val pullRequestMsg = "Failed to execute code review analysis. Reason: " + e.getMessage
        pullRequestErrorHandler.createErrorComment(pullRequest, repoConfig, pullRequestMsg)
    } finally {
      //设置这个pr已经检查并同步到最新
      codeReviewStatisticsDao.setStatus(review.pullRequest, uptoDate = true)
    }

  }
  def getConfigFor(repo : Repository) : AoRepoConfig = {
    repoConfigDao.find(repo).getOrElse(throw new IllegalStateException("Repository must have a configuration"))
  }

  def fetchCachedRefs(pullRequest: PullRequest, repository: Repository): Unit = {
    val fetcher = new CachedRepoFetcher(this.gitCommandFactory, repository, cachedRepoInfo)
    fetcher.fetchAll(List(pullRequest.getFromRef.getId))
  }
  def cloneInCacheIfNotExisting(repository: Repository): Unit = {
    val cachedRepoCloner = new CachedRepoCloner(gitCommandFactory, cachedRepoInfo)
    cachedRepoCloner.cloneIfNotExisting(repository)
  }
}
