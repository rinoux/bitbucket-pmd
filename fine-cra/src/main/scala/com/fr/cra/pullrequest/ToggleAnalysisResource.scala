package com.fr.cra.pullrequest

import javax.ws.rs.core.Response
import javax.ws.rs.{POST, Path, Produces}

import com.atlassian.bitbucket.pull.{PullRequest, PullRequestService}
import com.atlassian.bitbucket.repository.{Repository, RepositoryService}
import com.atlassian.bitbucket.user.{ApplicationUser, SecurityService, UserService}
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.atlassian.sal.api.transaction.{TransactionCallback, TransactionTemplate}
import com.fr.cra.{CODE_REVIEW_ASSISTANT_NAME, Logging}
import com.fr.cra.analysis.StaticAnalyzerRegistry
import com.fr.cra.config.repo.RepoConfigDao
import com.fr.cra.config.serviceuser.ServiceUserConfigDao
import com.fr.cra.pullrequest.ops.PullRequestDeleteCommentsOp
import org.springframework.beans.factory.annotation.Autowired

import scala.runtime.NonLocalReturnControl

/**
  * 切换分析资源,入口，供js文件连结使用<br>
  *   此处就是两个功能，开始cra检查和移除cra评论
  * Created by rinoux on 16/8/15.
  */
@Path("toggle-analysis")
@Produces(Array("application/json"))
class ToggleAnalysisResource @Autowired()(codeReviewAnalysisRunner : CodeReviewAnalysisRunner,
                                          repoService : RepositoryService,
                                          repoConfigDao : RepoConfigDao,
                                          @ComponentImport transactionTemplate : TransactionTemplate,
                                          securityService : SecurityService,
                                          serviceUserConfigDao :ServiceUserConfigDao,
                                          userService : UserService,
                                          staticAnalyzerRegistry : StaticAnalyzerRegistry,
                                          pullRequestService : PullRequestService) extends AnyRef with Logging {
  /**
    * 此处是代码分析启动的地方，点击run cra analysis。还有一处是PullRequestOpenedListner.onPullRequestOpened
    * @param input
    * @return
    */
  @POST
  @Path("/run-analysis")
  def runAnalysis(input : TriggerRunInput) : Response = {
    val pullRequest : PullRequest = Option(pullRequestService.getById(input.repoId, input.pullRequestId)).getOrElse(throw new NonLocalReturnControl(new Object, badRequest("No pull request found with ID " + input.pullRequestId)))
    codeReviewAnalysisRunner.runAnalysisOn(pullRequest)
    Response.status(Response.Status.OK).build
  }

  /**
    * 移除评论请求
    * @param input
    * @return
    */
  @POST
  @Path("/remove-comments")
  def removeComments(input : TriggerRunInput) : Response = {
    try {
      val repository = Option(repoService.getById(input.repoId)).getOrElse(throw new NonLocalReturnControl[Response](new AnyRef, badRequest("No repository found with ID " + input.repoId)))
      val pullRequest = Option(pullRequestService.getById(input.repoId, input.pullRequestId)).getOrElse(throw new NonLocalReturnControl[Response](new AnyRef, badRequest("No pull request found with ID " + input.pullRequestId)))

      val repoConfig = repoConfigDao.find(repository).getOrElse(throw new NonLocalReturnControl[Response](new AnyRef, badRequest("No config found for repository " + repository.getName)))

      val analyzers = staticAnalyzerRegistry.onlyEnabledAnalyzers(repoConfig)

      //删除操作
      transactionTemplate.execute(new TransactionCallback[Unit](){
        override def doInTransaction(): Unit = {
          //删评论DeleteCommentsOp
          analyzers.foreach(processor => deleteExistingComments(pullRequest, repository, getServiceUser(processor.getName)))
          deleteExistingComments(pullRequest, repository, getServiceUser(CODE_REVIEW_ASSISTANT_NAME))
        }
      })
      Response.status(Response.Status.OK).build()
    } catch {
      case e: NonLocalReturnControl[Response] =>
        e.value
    }
  }

  def getServiceUser(checkName : String) : ApplicationUser = {
    val serviceUserConfig = serviceUserConfigDao.find(checkName).getOrElse(throw new IllegalStateException("Service users should exist at this time"))
    Option(userService.getUserById(serviceUserConfig.getServiceUserId)).getOrElse(throw new IllegalArgumentException("No service user " + checkName + " for registered"))
  }

  /**
    * 删除已经存在的评论
    * @param pullRequest
    * @param repo
    * @param applicationUser
    */
  def deleteExistingComments(pullRequest : PullRequest, repo : Repository, applicationUser: ApplicationUser): Unit = {
    val deleteCommentsOp = new PullRequestDeleteCommentsOp(applicationUser, pullRequest, pullRequestService, repo)
    val reason = "Delete old comments of " + applicationUser.getDisplayName
    securityService.impersonating(applicationUser, reason).call(deleteCommentsOp)
  }

  def badRequest(msg : String): Response = {
    Response.status(Response.Status.BAD_REQUEST).entity(msg).build()
  }
}
