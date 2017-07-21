package com.fr.cra.pullrequest.diff

import com.atlassian.bitbucket.content.Path

import scala.collection.mutable.ListBuffer

/**
  * Created by rinoux on 16/8/5.
  */
class Diff(val src : Path, val dst : Path) {

  val hunks : ListBuffer[Hunk] = new ListBuffer[Hunk]
  //truncated表示是删节的
  private var truncated : Boolean = false

  /**
    * 设置truncated的值
    * @param boolean
    */
  def += (boolean: Boolean) = {
    truncated = boolean
  }
}
