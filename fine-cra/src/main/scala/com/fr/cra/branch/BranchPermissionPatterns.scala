package com.fr.cra.branch

import org.springframework.util.AntPathMatcher

/**
  * 分支是否需要检查
  * Created by rinoux on 16/8/16.
  */
class BranchPermissionPatterns() extends AnyRef {
  val pathMatcher : AntPathMatcher = new AntPathMatcher()

  def matches(refId : String, branchOptions : BranchOptions.Type, branchOptionsBranches : String) : Boolean = {
    branchOptions match {
      case BranchOptions.ALL =>
        true
      case BranchOptions.BLACK_LIST =>
        val forbiddenPatterns = branchOptionsBranches.split("\\s+")
        !forbiddenPatterns.exists(s => matches(refId, s))
      case BranchOptions.WHITE_LIST =>
        val allowedPatterns = branchOptionsBranches.split("\\s+")
        allowedPatterns.exists(s => matches(refId, s))
    }
  }

  def matches(refId : String, branchPattern : String) : Boolean = {

    if (Option(branchPattern).getOrElse("").toString.trim.isEmpty) {
      false
    } else {
      var value = branchPattern
      if (!branchPattern.startsWith("**") && !branchPattern.startsWith("refs/")) {
        value = "**/" + value
      }
      if (branchPattern.endsWith("/") || branchPattern.endsWith("\\")) {
        value = value + "**"
      }
      pathMatcher.`match`(value, refId)
    }
  }
}
