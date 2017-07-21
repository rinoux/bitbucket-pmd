package com.fr.cra.pullrequest

import java.util.concurrent.ExecutorService
import javax.annotation.{PostConstruct, PreDestroy}

import com.atlassian.bitbucket.event.pull.PullRequestOpenedEvent
import com.atlassian.event.api.{EventListener, EventPublisher}
import com.fr.cra.Logging
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.stereotype.Component

import scala.concurrent.{ExecutionContext, Future}


/**
  * pr 打开时的事件处理，另一线程开始cra检查
  * Created by rinoux on 16/8/8.
  */
@Component
class PullRequestOpenedListener @Autowired()(codeReviewAnalysisRunner : CodeReviewAnalysisRunner,
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
  /**
    * PR打开事件处理，在bitbucket 4.7以下版本，需要对EventListener进行手动的注册和注销，
    * 使用@PostConstruct， 和@PreDestroy对注册和注销方法进行标注，4.7以上版本则不需要
    * @param event
    * @return
    */
  @EventListener
  def onPullRequestOpened(event : PullRequestOpenedEvent) : Unit = {
    println("=================pullrequest opened!================")

    Future{
      codeReviewAnalysisRunner.runAnalysisOn(event.getPullRequest)
    }(executionContext)
  }
}
