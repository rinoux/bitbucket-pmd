package com.fr.cra.pullrequest

import com.atlassian.bitbucket.content.{AbstractDiffContentCallback, ConflictMarker, DiffSegmentType, Path}
import com.fr.cra.Logging
import com.fr.cra.pullrequest.diff.{Diff, Hunk, LineSegment, Segment}

import scala.collection.mutable.ListBuffer

/**
  * 差异内容位置触发的回调
  * Created by rinoux on 16/8/15.
  */
class CraDiffContentCallback() extends AbstractDiffContentCallback with Logging {
  val diffs = ListBuffer[Diff]()
  override def onSegmentStart(diffSegmentType : DiffSegmentType) : Unit = {
    diffs.last.hunks.last.segments.+=(new Segment(diffSegmentType))
  }
  override def onSegmentLine(line : String, marker : ConflictMarker, truncated : Boolean) : Unit = {
    Option(marker) match {
      case Some(m) =>
        log.info("FINE-CRA: Not collecting change on line " + line + " because of conflict marker: " + marker)
      case None =>
        diffs.last.hunks.last.segments.last.lines.+=(new LineSegment(line, marker, truncated))
    }
  }
  override def onSegmentEnd(truncated : Boolean) : Unit = {
    diffs.last.hunks.last.segments.last.+=(truncated)
  }
  override def onDiffStart(src : Path, dst : Path) : Unit = {
    diffs.+=(new Diff(src, dst))
  }
  override def onDiffEnd(truncated : Boolean) : Unit = {
    diffs.last.+=(truncated)
  }
  override def onHunkStart(srcLine : Int, srcSpan : Int, dstLine : Int, dstSpan : Int) : Unit = {
    diffs.last.hunks.+=(new Hunk(srcLine, srcSpan, dstLine, dstSpan))
  }
  override def onHunkEnd(truncated : Boolean) : Unit = {
    diffs.last.hunks.last.+=(truncated)
  }
  def getResult : List[Diff] = {
    diffs.toList
  }
}
