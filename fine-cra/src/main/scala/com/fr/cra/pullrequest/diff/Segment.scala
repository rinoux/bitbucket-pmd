package com.fr.cra.pullrequest.diff

import com.atlassian.bitbucket.content.DiffSegmentType

import scala.collection.mutable.ListBuffer

/**
  * Created by rinoux on 16/8/5.
  */
class Segment(val diffType : DiffSegmentType) {
  //每个segement有两个属性:一个是否truncated,一个是line行容器

  private var truncated : Boolean = _
  val lines : ListBuffer[LineSegment] = new ListBuffer[LineSegment]
  //getter
  def +=(b: Boolean) = {
    this.truncated = b
  }
}
