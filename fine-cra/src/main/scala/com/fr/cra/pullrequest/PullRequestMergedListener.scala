package com.fr.cra.pullrequest

import javax.annotation.{PostConstruct, PreDestroy}

import com.atlassian.bitbucket.event.pull.PullRequestMergedEvent
import com.atlassian.bitbucket.pull.PullRequest
import com.atlassian.event.api.{EventListener, EventPublisher}
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService
import com.fr.cra.Logging
import com.fr.cra.branch.ShouldAnalyzeBranchChecker
import com.fr.cra.reposync.CachedRepoInformation
import com.fr.cra.statistics.CodeReviewStatisticsDao
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
  * PR merge合并监听事件处理
  * Created by rinoux on 16/8/8.
  */
@Component
class PullRequestMergedListener @Autowired()(codeReviewStatisticsDao : CodeReviewStatisticsDao,
                                             shouldAnalyzeBranchChecker : ShouldAnalyzeBranchChecker,
                                             cachedRepoInformation : CachedRepoInformation,
                                             eventPublisher: EventPublisher) extends AnyRef with Logging {

  @PostConstruct
  def init() : Unit = {
    eventPublisher.register(this)
  }

  @PreDestroy
  def destroy() : Unit = {
    eventPublisher.unregister(this)
  }
  @EventListener
  def onPullRequestMerged(event : PullRequestMergedEvent) : Unit = {
    println("=================pullrequest merged!================")
    val pullRequest : PullRequest = event.getPullRequest
    if (shouldAnalyzeBranchChecker.shouldAnalyze(pullRequest)) {
      cachedRepoInformation.deleteCachedPRDirs(pullRequest)
      codeReviewStatisticsDao.setStatus(pullRequest, uptoDate = true)
    }
  }
}