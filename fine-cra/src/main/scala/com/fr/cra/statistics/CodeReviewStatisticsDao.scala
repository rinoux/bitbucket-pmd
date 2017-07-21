package com.fr.cra.statistics

import com.atlassian.activeobjects.tx.Transactional
import com.atlassian.bitbucket.pull.PullRequest
import com.fr.cra.analysis.Review
import org.springframework.stereotype.Component

/**
  * 存储review容器的dao
  * Created by rinoux on 16/8/16.
  */
@Transactional
trait CodeReviewStatisticsDao extends AnyRef {
  def all() : List[AoCodeReviewStatistics]
  def find(pullRequest : PullRequest) : Option[AoCodeReviewStatistics]
  def save(review : Review) : Unit
  def setStatus(pullRequest : PullRequest, uptoDate : Boolean) : Unit
  def deleteForRepo(repoId : Int) : Unit
}
