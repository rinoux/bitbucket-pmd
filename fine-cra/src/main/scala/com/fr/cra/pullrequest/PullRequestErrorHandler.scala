package com.fr.cra.pullrequest

import com.atlassian.bitbucket.pull.{PullRequest, PullRequestService}
import com.atlassian.bitbucket.server.ApplicationPropertiesService
import com.atlassian.bitbucket.user.{ApplicationUser, SecurityService, UserService}
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService
import com.fr.cra.analysis.{FATAL, Violation, ViolationCommentFormatter}
import com.fr.cra.config.repo.AoRepoConfig
import com.fr.cra.config.serviceuser.{AoServiceUserConfig, ServiceUserConfigDao}
import com.fr.cra.pullrequest.ops.PullRequestAddMsgCommentOp
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.fr.cra.CODE_REVIEW_ASSISTANT_NAME

/**
  * Created by rinoux on 16/8/15.
  */
@Component
class PullRequestErrorHandler @Autowired()(applicationPropertiesService : ApplicationPropertiesService,
                                           pullRequestService : PullRequestService,
                                           securityService : SecurityService,
                                           serviceUserConfigDao : ServiceUserConfigDao,
                                           userService : UserService) extends AnyRef {
  /**
    * 创建错误评论到pullReq
    * @param pullRequest pr
    * @param repoConfig repo配置
    * @param message 内容
    */
  def createErrorComment(pullRequest : PullRequest, repoConfig : AoRepoConfig, message : String) : Unit = {
    val craUser : ApplicationUser = getServiceUser(CODE_REVIEW_ASSISTANT_NAME)
    val violation = Violation("", 0, message, FATAL, "", "")
    val prCommentMsg = formatPullRequestComment(message, violation, repoConfig)
    val op = new PullRequestAddMsgCommentOp(message, pullRequestService, pullRequest)
    val reason = "Code Review Assistant detected a problem running on pull request"
    securityService.impersonating(craUser, reason).call(op)
  }

  /**
    * 根据用户名从配置获取用户
    * @param userName 用户名
    * @return
    */
  def getServiceUser(userName: String): ApplicationUser = {
    val serviceUserConfig : AoServiceUserConfig= serviceUserConfigDao.find(userName).getOrElse(throw new IllegalStateException("Code Review Assistant service user config should exist at this time"))
    Option(userService.getUserById(serviceUserConfig.getServiceUserId)).getOrElse(throw new IllegalArgumentException("Code Review Assistant service user should exist at this time"))
  }

  /**
    * 组织评论内容
    * @param message
    * @param violation
    * @param repoConfig
    * @return
    */
  def formatPullRequestComment(message : String, violation: Violation, repoConfig: AoRepoConfig) : String = {
    val formatter : ViolationCommentFormatter = new ViolationCommentFormatter(violation, applicationPropertiesService)
    formatter.formatAsMultilineText(message, repoConfig.isSeverityIconShown, "")
  }
}
