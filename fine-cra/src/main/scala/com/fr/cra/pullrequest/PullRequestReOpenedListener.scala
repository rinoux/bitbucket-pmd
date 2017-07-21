package com.fr.cra.pullrequest

import java.util.concurrent.ExecutorService
import javax.annotation.{PostConstruct, PreDestroy}

import com.atlassian.bitbucket.event.pull.PullRequestReopenedEvent
import com.atlassian.event.api.{EventListener, EventPublisher}
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.fr.cra.Logging
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.stereotype.Component

import scala.concurrent.{ExecutionContext, Future}
import scala.runtime.AbstractPartialFunction
import scala.util.{Success, Try}

/**
  * decline后re-open的事件监听处理
  * Created by rinoux on 16/8/8.
  */
@Component
class PullRequestReOpenedListener @Autowired()(codeReviewAnalysisRunner : CodeReviewAnalysisRunner,
                                               executorService : ExecutorService,
                                               eventPublisher: EventPublisher) extends AnyRef with Logging {
  implicit val executionContext = ExecutionContext.fromExecutorService(executorService)
  @PostConstruct
  def init() : Unit = {
    eventPublisher.register(this)
  }

  @PreDestroy
  def destroy() : Unit = {
    eventPublisher.unregister(this)
  }
  @EventListener
  def onPullRequestReOpened(event : PullRequestReopenedEvent) : Unit = {
    println("=================pullrequest reopened!================")
    Future{
      codeReviewAnalysisRunner.runAnalysisOn(event.getPullRequest)
    }(executionContext)
  }
}
