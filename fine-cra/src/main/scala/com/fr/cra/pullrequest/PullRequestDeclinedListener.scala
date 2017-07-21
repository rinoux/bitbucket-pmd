package com.fr.cra.pullrequest

import javax.annotation.{PostConstruct, PreDestroy}

import com.atlassian.bitbucket.event.pull.PullRequestDeclinedEvent
import com.atlassian.event.api.{EventListener, EventPublisher}
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService
import com.fr.cra.Logging
import com.fr.cra.branch.ShouldAnalyzeBranchChecker
import com.fr.cra.reposync.CachedRepoInformation
import com.fr.cra.statistics.CodeReviewStatisticsDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
  * pr被拒监听事件处理
  * Created by rinoux on 16/8/8.
  */
@Component
class PullRequestDeclinedListener @Autowired()(shouldAnalyzeBranchChecker : ShouldAnalyzeBranchChecker,
                                               codeReviewStatisticsDao : CodeReviewStatisticsDao,
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
  def onPullRequestDeclined(event : PullRequestDeclinedEvent) : Unit = {
    println("=================pullrequest declined!================")
    val pr = event.getPullRequest
    if (shouldAnalyzeBranchChecker.shouldAnalyze(pr)) {
      cachedRepoInformation.deleteCachedPRDirs(pr)
      codeReviewStatisticsDao.setStatus(pr, uptoDate = true)
    }
  }
}
