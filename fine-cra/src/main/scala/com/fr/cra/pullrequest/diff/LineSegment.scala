package com.fr.cra.pullrequest.diff

import com.atlassian.bitbucket.content.ConflictMarker

/**
  * Created by rinoux on 16/8/5.
  */
class LineSegment(val line : String, val maker : ConflictMarker, var truncated : Boolean) {

  def += (b: Boolean): Unit = {
    truncated = b
  }
}
