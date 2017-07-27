package com.fr.cra.pullrequest.ops

import com.atlassian.bitbucket.comment.{Comment, CommentDeletionException, CommentSearchRequest, CommentService}
import com.atlassian.bitbucket.content.{AbstractChangeCallback, Change}
import com.atlassian.bitbucket.pull.{PullRequest, PullRequestChangesRequest, PullRequestService}
import com.atlassian.bitbucket.repository.Repository
import com.atlassian.bitbucket.user.ApplicationUser
import com.atlassian.bitbucket.util.{Operation, PageRequestImpl}
import com.fr.cra.Logging

import scala.collection.JavaConverters
import scala.collection.mutable.ListBuffer
/**
  * 删除评论时回调
  * Created by rinoux on 16/8/5.
  */
class PullRequestDeleteCommentsOp(staticAnalysisUser : ApplicationUser,
                                  pullRequest: PullRequest,
                                  pullRequestService: PullRequestService,
                                  repository: Repository,
                                  commentService: CommentService) extends Object with Operation[Unit, Exception] with Logging{
  override def perform(): Unit = {
    val changesRequest = new PullRequestChangesRequest.Builder(pullRequest).withComments(true).build()
    val commentsToDelete: ListBuffer[CommentDeletion] = null
    pullRequestService.streamChanges(changesRequest, new AbstractChangeCallback {
      override def onChange(change: Change): Boolean = {
        val path = change.getPath.toString
        val searchRequest: CommentSearchRequest = new CommentSearchRequest.Builder(pullRequest).path(path).build()
        JavaConverters.iterableAsScalaIterableConverter(commentService.searchThreads(searchRequest, new PageRequestImpl(0, 9999)).getValues).asScala.foreach(anchor => {
          val comment: Comment = anchor.getRootComment
          if (isCraCommentWithNoReplies(comment)) {
            commentsToDelete += new CommentDeletion(repository.getId, pullRequest.getId, comment.getId, comment.getVersion)
          }
        })
        true
      }
    })

    commentsToDelete.foreach(c => commentService.deleteComment(c.commentId, c.commentVersion))
  }


  def isCraCommentWithNoReplies(comment: Comment): Boolean = {
    comment.getAuthor == this.staticAnalysisUser && comment.getComments.isEmpty
  }


  class CommentDeletion(val repoId: Int, val pullRequestId: Long, val commentId: Long, val commentVersion: Int) extends AnyRef with Product{
    override def productElement(n: Int): Any = {
      n match {
        case 0 => repoId
        case 1 => pullRequestId
        case 2 => commentId
        case 3 => commentVersion
        case _ => throw new IndexOutOfBoundsException(n.toString)
      }
    }

    override def productArity: Int = 4

    override def canEqual(that: Any): Boolean = that.isInstanceOf[CommentDeletion]

    override def productIterator: Iterator[Any] = super.productIterator

    override def productPrefix: String = "CommentDeletion"
  }
}
