package com.fr.cra.pullrequest

import java.io.File
import java.util.concurrent.ExecutorService

import com.atlassian.bitbucket.comment._
import com.atlassian.bitbucket.content.{DiffFileType, DiffSegmentType}
import com.atlassian.bitbucket.i18n.I18nService
import com.atlassian.bitbucket.permission.Permission
import com.atlassian.bitbucket.pull.{PullRequest, PullRequestService}
import com.atlassian.bitbucket.repository.{Repository, RepositoryService}
import com.atlassian.bitbucket.scm.git.command.GitCommandBuilderFactory
import com.atlassian.bitbucket.server.ApplicationPropertiesService
import com.atlassian.bitbucket.user.{ApplicationUser, SecurityService, UserService}
import com.atlassian.bitbucket.util.{PageRequestImpl, UncheckedOperation}
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.atlassian.sal.api.transaction.{TransactionCallback, TransactionTemplate}
import com.fr.cra.analysis._
import com.fr.cra.config.repo.{AoRepoConfig, RepoConfigDao}
import com.fr.cra.config.serviceuser.ServiceUserConfigDao
import com.fr.cra.pullrequest.diff.ChangeType
import com.fr.cra.pullrequest.ops.{PullRequestAddDiffCommentOp, PullRequestDeleteCommentsOp}
import com.fr.cra.reposync.{BranchCheckoutManager, CachedRepoInformation}
import com.fr.cra.statistics.CodeReviewStatisticsDao
import com.fr.cra.{Logging, Utils}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.fr.cra.CRA_EXECUTE_TIMEOUT_IN_SEC

import scala.collection.JavaConverters
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * 整个插件的核心类
  * Pull Request 代码检查<br>
  *   从staticAnalyzerRegistry取出已启用的分析器执行代码检查<br>
  * Created by rinoux on 16/8/8.
  */
@Component
class PullRequestReviewer @Autowired()(pullRequestService : PullRequestService,
                                       applicationPropertiesService : ApplicationPropertiesService,
                                       repositoryService : RepositoryService,
                                       @ComponentImport gitCommandFactory : GitCommandBuilderFactory,
                                       serviceUserConfigDao : ServiceUserConfigDao,
                                       repoConfigDao : RepoConfigDao,
                                       @ComponentImport transactionTemplate : TransactionTemplate,
                                       securityService : SecurityService,
                                       codeReviewResultsDao : CodeReviewStatisticsDao,
                                       executorService : ExecutorService,
                                       staticAnalyzerRegistry : StaticAnalyzerRegistry,
                                       cachedRepoInfo : CachedRepoInformation,
                                       i18nService : I18nService,
                                       pullRequestErrorHandler : PullRequestErrorHandler,
                                       userService : UserService,
                                       commentService: CommentService) extends AnyRef with Logging {
  implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

  /**
    * 代码检查操作入口
    * @param review
    */
  def execute(review : Review) : Unit = {
    val repoConfig = getRepoConfig(review)
    //检出pr到缓存
    checkout(review.pullRequest)
    runAnalyzers(review, repoConfig)
  }

  /**
    * 获取repo配置
    * @param review
    * @return
    */
  def getRepoConfig(review: Review) : AoRepoConfig ={
    repoConfigDao.find(review.pullRequest.getToRef.getRepository).getOrElse(throw new IllegalStateException("Can not find repository configuration information"))
  }

  /**
    * checkout文件到缓存路径，之后的文件分析都在缓存文件路径进行
    * @param pullRequest
    * @return
    */
  def checkout(pullRequest : PullRequest) : File = {
    new BranchCheckoutManager(gitCommandFactory, repositoryService, cachedRepoInfo).checkoutToCachedDir(pullRequest)
  }

  /**
    * 运行分析器入口
    * @param review 存放结果
    * @param repoConfig 读取配置
    */
  def runAnalyzers(review: Review, repoConfig: AoRepoConfig): Unit ={
    val repo = repositoryService.getById(repoConfig.getRepoId)
    removeExistingComments(review, repoConfig, repo)
    //在此运行多个分析器对同一个review对象进行检查
    runAnalyzersConcurrently(review, repoConfig)
    codeReviewResultsDao.save(review)
    log.debug("FINE-CRA: Review finished with results:\n" + review)
  }

  /**
    * 并发运行多个分析器,主要是要把执行processor.process(review)的任务交给其他线程
    * @param review 结果集
    * @param repoConfig repo配置
    */
  def runAnalyzersConcurrently(review: Review, repoConfig: AoRepoConfig): Unit ={
    //取出能用的
    val enabledAnalyzers = staticAnalyzerRegistry.onlyEnabledAnalyzers(repoConfig)
    //任务
    val reviewResultFutures = enabledAnalyzers.map(processor => {
      Future{
        try {
          log.debug("FINE-CRA: Running static analyzer " + processor.getName)
          Option(runStaticAnalyzer(processor, review, repoConfig))
        } catch {
          case e : Exception =>
            log.error("FINE-CRA" + processor.getName + " failed", e)
            review.addReviewError(ReviewError(processor.getName, e.getMessage))
            None
        }
      }(executionContext)
    })

    if (reviewResultFutures.nonEmpty) {
      awaitSuccess(reviewResultFutures, Seq()) match {
        case Left(a) =>
          throw new RuntimeException("Failed to analyze files", a)
        case Right(reviewResults) =>
          log.debug("FINE-CRA: Finished running all analyzers.")
          reviewResults.flatten.foreach(ar => review.addReviewResult(ar))
          review.getAllReviewErrors.foreach(f => {
            if (f != null) {
              val (analyzerName, errors) = f
              errors.foreach(errorMsg => {
                val msg = i18nService.getMessage("cra.review.analyzer.failed.msg", (analyzerName, errorMsg))
                pullRequestErrorHandler.createErrorComment(review.pullRequest, repoConfig, msg)
              })
            }
          })
      }

    }
  }

  /**
    * 等待线程中的AnalyzerResult结果生成
    * @param fs 任务队列
    * @param done 执行结果存放
    * @return
    */
  def awaitSuccess(fs: Seq[Future[Option[AnalyzerResult]]], done : Seq[Option[AnalyzerResult]]) : Either[Throwable, Seq[Option[AnalyzerResult]]] = {
    while (true) {
      val first = Future.firstCompletedOf(fs)(executionContext)
      val  value = Await.ready(first, new DurationInt(CRA_EXECUTE_TIMEOUT_IN_SEC).seconds).value
      value match {
        case None =>

        case Some(t) =>
          t match {
            case f : Failure[Option[AnalyzerResult]] =>
              return Left(f.exception)
            case s : Success[Option[AnalyzerResult]] =>
              //运行成功
              val (complete, running) = fs.partition(f => f.isCompleted)
              val answer = complete.flatMap(f => Option.option2Iterable(f.value))
              answer.find(_.isFailure) match {
                case Some(x) =>
                  x match {
                    case fa : Failure[Option[AnalyzerResult]] =>
                      return Left(fa.exception)
                    case _ =>
                  }
                case _ =>
              }
              if (running.isEmpty) {
                return Right(done.++:(answer.map(f => f.get)))
              }
              done.++:(answer.map(f => f.get))
          }
      }
    }
    null
  }

  /**
    * 实际干活的
    * @param processor 分析器
    * @param review review容器对象
    * @param repoConfig repo的分析配置
    * @return
    */
  def runStaticAnalyzer(processor :  StaticAnalysisProcessor, review: Review, repoConfig: AoRepoConfig) : AnalyzerResult = {
    val thisPullRequestDir = cachedRepoInfo.getCachedPRCheckoutDir(review.pullRequest)
    val repository = review.pullRequest.getToRef.getRepository
    //diffcomments
    var diffComments = new ListBuffer[AddFileCommentRequest]()
    //linecomment
    //var lineComments = new ListBuffer[AddPullRequestLineCommentRequest]()
    //是否在变动diff行上,不需要了
    @deprecated
    def isOnChangedLine(violation: Violation) : Boolean = {
      getChangeType(violation, Utils.getRelativePath(thisPullRequestDir, violation.filePath),review).contains(ChangeType.ADDED)
    }
    //添加评论后的结果
    val reviewResult = new AnalyzerResult(processor.getName)
    //分析器的直接结果
    val analyzerResult = processor.process(review)
    log.debug("FINE-CRA: " + processor.getName + "finished with " + analyzerResult.getSummary)
    //get valid violations and create related comments, meanwhile transmit analyzer results to review results
    //原插件只取在diff line上的violation，这里取全部
    analyzerResult.getViolations.foreach(vio => {
      val filePathInRepo = Utils.getRelativePath(thisPullRequestDir, vio.filePath)
      val violationMsg = createCommentText(processor, vio, repoConfig)

      if (commentDoesNotExistYet(review, repository, filePathInRepo, violationMsg, vio.line)) {
        //diffComments.+=(createDiffComment(vio, filePathInRepo, violationMsg))
        //lineComments.+=(createLineComment(review.pullRequest, vio, filePathInRepo, violationMsg))
        diffComments.+=(createDiffComment(vio, filePathInRepo, violationMsg, review.pullRequest))
      }
      reviewResult.addViolation(vio)
    })
    val reason = "Code Review Assistant plug-in detected a source code violation"
    //根据冲突调用生成评论的回调
    /*
    val lineCommentOp = new PullRequestAddLineCommentOp(lineComments.toList, pullRequestService)
    transactionTemplate.execute(new TransactionCallback[Unit] {
      override def doInTransaction(): Unit = {
        securityService.impersonating(getServiceUser(processor.getName), reason).call(lineCommentOp)
      }
    })
    */

    val op = new PullRequestAddDiffCommentOp(diffComments.toList, commentService)
    transactionTemplate.execute(new TransactionCallback[Unit] {
      override def doInTransaction(): Unit = {
        securityService.impersonating(getServiceUser(processor.getName), reason).call(op)
      }
    })

    reviewResult
  }

  /**
    * 获取冲突的变化类型add remove none context
    * @param violation 冲突
    * @param repoRelativeFilePath repo的相对文件路径
    * @param review review容器
    * @return 变化类型
    */
  def getChangeType(violation: Violation, repoRelativeFilePath : String, review: Review) : Option[ChangeType.Value] = {
    review.reviewFiles.find(f => {
      f.filePath.toString.equals(repoRelativeFilePath)
    }).flatMap(f => f.singleFileChanges.getResult.get(violation.line))
  }

  /**
    * 为violations创建checkstyle等的评论
    * @param processor 分析器
    * @param violation 冲突名称
    * @param repoConfig repo cra配置
    * @return
    */
  def createCommentText(processor: StaticAnalysisProcessor, violation: Violation, repoConfig: AoRepoConfig) : String = {
    val formatter = new ViolationCommentFormatter(violation, applicationPropertiesService)
    processor.createPullRequestComment(violation, formatter, repoConfig)
  }

  def commentDoesNotExistYet(review: Review, repository: Repository, filePath : String, text : String, line : Int) : Boolean = {
    val searchRequest: CommentSearchRequest = new CommentSearchRequest.Builder(review.pullRequest.asInstanceOf[Commentable]).path(filePath).build()
    val threads: Iterable[CommentThread] = this.securityService.withPermission(Permission.REPO_READ, "Read repo outside of auth context").call(
      new UncheckedOperation[Iterable[CommentThread]]() {
      override def perform(): Iterable[CommentThread] = {
        JavaConverters.iterableAsScalaIterableConverter(commentService.searchThreads(searchRequest, new PageRequestImpl(0, 9999)).getValues()).asScala
      }
    })

    val matchingComments: Iterable[CommentThread] = threads.flatMap(thread => {
      Option.option2Iterable[CommentThread](Utils.RichJOption(thread.getAnchor)
        .asScala
        .withFilter(anchor => thread.getRootComment.getText == text && anchor.getLine == line)
        .map(_ => thread))
    })

    matchingComments.isEmpty
  }

  def createDiffComment(violation : Violation, filePathInRepo: String, violationMsg: String, pullRequest: PullRequest) : AddFileCommentRequest = {
    if (violation.line > 0) {
      new AddLineCommentRequest.Builder(pullRequest, violationMsg, CommentThreadDiffAnchorType.EFFECTIVE, filePathInRepo).fileType(DiffFileType.TO)
        .lineType(DiffSegmentType.ADDED).line(violation.line).build()
    } else new AddFileCommentRequest.Builder(pullRequest.asInstanceOf[Commentable], violationMsg, CommentThreadDiffAnchorType.EFFECTIVE, filePathInRepo).build()
  }

  /**
    * 删除已有评论
    * @param review review容器
    * @param repoConfig repo分析配置
    * @param repo repo
    */
  def removeExistingComments(review: Review, repoConfig: AoRepoConfig, repo: Repository): Unit = {
    transactionTemplate.execute(new TransactionCallback[Unit] {
      override def doInTransaction(): Unit = {
        //只删除已启用的分析器检查结果中的评论
        staticAnalyzerRegistry.onlyEnabledAnalyzers(repoConfig).foreach(f => {
          //调用PullRequestDeleteCommentsOp.perform
          val user = getServiceUser(f.getName)
          val deleteCommentsOp = new PullRequestDeleteCommentsOp(user, review.pullRequest, pullRequestService, repo, commentService)
          val reason = "Delete old comments of " + user.getDisplayName
          securityService.impersonating(user, reason).call(deleteCommentsOp)
        })
      }
    })
  }

  /**
    * 根据分析名称获得用户名称
    * @param staticAnalysisName 静态分析名称，作为评论的用户名
    * @return 用户
    */
  def getServiceUser(staticAnalysisName : String) : ApplicationUser = {
    val serviceUserConfig = serviceUserConfigDao.find(staticAnalysisName).getOrElse(throw new IllegalStateException("Service users should exist at this time"))
    Option(userService.getUserById(serviceUserConfig.getServiceUserId)).getOrElse(throw new IllegalArgumentException("No service user " + staticAnalysisName + " for registered"))

  }
}
