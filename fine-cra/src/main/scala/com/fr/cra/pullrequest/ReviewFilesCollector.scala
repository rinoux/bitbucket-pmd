package com.fr.cra.pullrequest

import java.io.File

import com.atlassian.bitbucket.commit.{AbstractCommitCallback, Commit}
import com.atlassian.bitbucket.content.{AbstractChangeCallback, Change, ChangeType}
import com.atlassian.bitbucket.pull.PullRequestDiffRequest.Builder
import com.atlassian.bitbucket.pull.{PullRequest, PullRequestChangesRequest, PullRequestDiffRequest, PullRequestService}
import com.fr.cra.Logging
import com.fr.cra.analysis.ReviewFile
import com.fr.cra.pullrequest.diff.{Diff, FileChanges}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import scala.collection.immutable.HashSet
import scala.runtime.ObjectRef

/**
  * 待检查文件收集器
  * Created by rinoux on 16/8/15.
  */
@Component
class ReviewFilesCollector @Autowired()(pullRequestService : PullRequestService) extends AnyRef with Logging {
  /**
    * 收集到的文件有变化
    * @param pullRequest pr
    * @return
    */
  def collectFilesChanges(pullRequest : PullRequest) : Seq[ReviewFile] = {
    val reviewFiles = ObjectRef.create(new HashSet[ReviewFile])

    pullRequest.getFromRef
    val changeRequest = new PullRequestChangesRequest.Builder(pullRequest).build()
    pullRequestService.streamChanges(changeRequest, new AbstractChangeCallback{
      //主要改的地方
      override def onChange(change: Change): Boolean = {
        /*
        if (change.getType.equals(ChangeType.DELETE)) {
          true
        } else {
          val diffs = getDiffsOf(change, pullRequest)
          val singleFileChanges = new FileChanges(change.getPath)
          diffs.foreach(d => {
            singleFileChanges.addChange(d)
          })
          reviewFiles.elem = reviewFiles.elem.+(ReviewFile(change.getPath, singleFileChanges))
          true
        }
        */

        change.getType match {
          case ChangeType.DELETE =>
            true
          case ChangeType.ADD =>
            //println("==添加文件==>" + change.getPath)
            val diffs = getDiffsOf(change, pullRequest)
            val singleFileChanges = new FileChanges(change.getPath)
            diffs.foreach(d => {
              singleFileChanges.addChange(d)
            })
            reviewFiles.elem = reviewFiles.elem.+(ReviewFile(change.getPath, singleFileChanges))
            true
          case _ =>
            //println("==变更文件==>" + change.getPath)
            val diffs = getDiffsOf(change, pullRequest)
            val singleFileChanges = new FileChanges(change.getPath)
            diffs.foreach(d => {
              singleFileChanges.addChange(d)
            })
            reviewFiles.elem = reviewFiles.elem.+(ReviewFile(change.getPath, singleFileChanges))
            true
        }
      }
    })
    reviewFiles.elem.toSeq
  }

  /**
    * 取得pull request的区别之处
    * @param change 变化
    * @param pullRequest pr
    * @return
    */
  def getDiffsOf(change : Change, pullRequest: PullRequest) : List[Diff] = {
    //将change变成Diffs
    val srcPath : String = Option(change.getSrcPath).flatMap(f => Option(f.toString)).orNull
    val diffRequest : PullRequestDiffRequest = new Builder(pullRequest, change.getPath.toString).srcPath(srcPath).build()
    val diffContentCallback : CraDiffContentCallback = new CraDiffContentCallback
    pullRequestService.streamDiff(diffRequest, diffContentCallback)
    diffContentCallback.getResult
  }
}
