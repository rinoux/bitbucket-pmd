package com.fr.cra.pullrequest.ops

import com.atlassian.bitbucket.comment.{AddCommentRequest, CommentService}
import com.atlassian.bitbucket.util.Operation

/**
  * 添加差异评论
  * Created by rinoux on 16/8/5.
  */
class PullRequestAddDiffCommentOp(commentRequests : Seq[AddCommentRequest], commentService: CommentService) extends Object with Operation[Unit, Exception]{
  /**
    * 在bitbucket 4.9以上版本AddDiffCommentRequest已经废弃,官方推荐使用AddPullRequestLineCommentRequest 或者 AddCommitLineCommentRequest
    */
  override def perform(){
    commentRequests.foreach(cr => commentService.addComment(cr))
  }

}
