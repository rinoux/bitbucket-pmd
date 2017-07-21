package com.fr.cra.pullrequest.diff

import scala.collection.mutable.ListBuffer

/**
  * Created by rinoux on 16/8/5.
  */
class Hunk(val srcLine : Int, val srcSpan : Int, val dstLine : Int, dstSpan : Int) {
  private var truncated : Boolean = false
  var segments: ListBuffer[Segment] = new ListBuffer[Segment]
  def +=(boolean: Boolean) = {
    truncated = boolean
  }
}
