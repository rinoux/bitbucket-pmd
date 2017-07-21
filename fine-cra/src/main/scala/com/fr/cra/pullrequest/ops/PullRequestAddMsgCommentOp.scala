package com.fr.cra.pullrequest.ops

import com.atlassian.bitbucket.pull.{PullRequest, PullRequestService}
import com.atlassian.bitbucket.util.Operation

/**
  * 添加非cra评论时回调
  * Created by rinoux on 16/8/5.
  */
class PullRequestAddMsgCommentOp(commitMessage : String, pullRequestService: PullRequestService, pullRequest: PullRequest) extends AnyRef with Operation[Unit, Exception] {
  override def perform() {
    pullRequestService.addComment(pullRequest.getToRef.getRepository.getId, pullRequest.getId, commitMessage)
  }
}
