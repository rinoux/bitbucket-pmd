package com.fr.cra.pullrequest

import java.util
import java.util.Map

import com.atlassian.bitbucket.pull.PullRequest
import com.atlassian.plugin.web.Condition
import com.fr.cra.branch.ShouldAnalyzeBranchChecker
import org.springframework.stereotype.Component

/**
  * 判断cra是否为branch 启动
  * Created by rinoux on 16/8/15.
  */
class CraEnabledForBranchCondition(shouldAnalyzeBranchChecker : ShouldAnalyzeBranchChecker) extends AnyRef with Condition {
  override def init(context : util.Map[String, String]) : Unit = {

  }
  override def shouldDisplay(context : util.Map[String, AnyRef]) : Boolean = {
    val value = context.get("pullRequest")
    value.isInstanceOf[PullRequest] && shouldAnalyzeBranchChecker.shouldAnalyze(value.asInstanceOf[PullRequest])
  }
}
