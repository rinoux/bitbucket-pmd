package com.fr.cra.pullrequest.ops

import com.atlassian.bitbucket.comment.AddDiffCommentRequest
import com.atlassian.bitbucket.pull.{PullRequest, PullRequestService}
import com.atlassian.bitbucket.repository.Repository
import com.atlassian.bitbucket.util.Operation

/**
  * 添加差异评论
  * Created by rinoux on 16/8/5.
  */
class PullRequestAddDiffCommentOp(commentRequests : Seq[AddDiffCommentRequest], repository: Repository, pullRequestService: PullRequestService, pullRequest: PullRequest) extends Object with Operation[Unit, Exception]{
  /**
    * 在bitbucket 4.9以上版本AddDiffCommentRequest已经废弃,官方推荐使用AddPullRequestLineCommentRequest 或者 AddCommitLineCommentRequest
    */
  override def perform(){
    val repoId : Int = pullRequest.getToRef.getRepository.getId
    commentRequests.foreach(cr => pullRequestService.addDiffComment(repoId, pullRequest.getId, cr))//do not exec
  }

}
