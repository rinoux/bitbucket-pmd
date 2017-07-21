package com.fr.cra.pullrequest.ops

import com.atlassian.bitbucket.comment.{Comment, CommentDeletionException, DiffCommentAnchor}
import com.atlassian.bitbucket.content.{AbstractChangeCallback, Change}
import com.atlassian.bitbucket.pull.{PullRequest, PullRequestChangesRequest, PullRequestService}
import com.atlassian.bitbucket.repository.Repository
import com.atlassian.bitbucket.user.ApplicationUser
import com.atlassian.bitbucket.util.Operation
import com.fr.cra.Logging

import scala.collection.{Iterator, JavaConverters}
import scala.collection.mutable.ListBuffer
import scala.runtime.ScalaRunTime
/**
  * 删除评论时回调
  * Created by rinoux on 16/8/5.
  */
class PullRequestDeleteCommentsOp(staticAnalysisUser : ApplicationUser, pullRequest: PullRequest, pullRequestService: PullRequestService, repository: Repository) extends Object with Operation[Unit, Exception] with Logging{

  override def perform() {

    val changesRequest = new PullRequestChangesRequest.Builder(pullRequest).withComments(true).build()
    //变化时
    pullRequestService.streamChanges(changesRequest, new AbstractChangeCallback{
      //pullRequest有删除操作时回调,将删除的内容放入listBuffer
      override def onChange(change: Change): Boolean = {

        val path = change.getPath.toString
        pullRequestService.findCommentAnchors(repository.getId, pullRequest.getId, path)
        JavaConverters.iterableAsScalaIterableConverter(pullRequestService.findCommentAnchors(repository.getId, pullRequest.getId, path)).asScala.foreach(anchor => {
          val comment = anchor.getComment
          //删除评论,在原插件代码中，要判断评论是否有回复，评论是否来自于cra，符合了才能删除，实际上，
          //pullRequestService本身不允许已经有回复的评论被删除，否则会com.atlassian.bitbucket.comment.CommentDeletionException: This comment has replies which must be deleted first。
          try {
            if (comment.getAuthor.equals(staticAnalysisUser)) {
              pullRequestService.deleteComment(repository.getId, pullRequest.getId, comment.getId, comment.getVersion)
            }
          } catch {
            case e: CommentDeletionException =>
              log.error("FINE-CRA:要删除评论首先需要删除评论的所有回复！")
          }
        })
        true
      }
    })
  }
}
