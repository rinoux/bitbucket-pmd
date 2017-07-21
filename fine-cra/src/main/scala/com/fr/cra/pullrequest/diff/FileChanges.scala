package com.fr.cra.pullrequest.diff

import com.atlassian.bitbucket.content.Path
import com.fr.cra.pullrequest.diff.ChangeType.ChangeType

import scala.Predef.ArrowAssoc
import scala.collection.immutable.Map
import scala.runtime.IntRef



/**
  * 文件变化的具体细节:变化类型、变化的语句块部分
  * Created by rinoux on 16/8/5.
  */
class FileChanges(filename : Path) {
  //ChangeType是ChangeTyp.Value的别名
  var changes : Map[Any, ChangeType] = Map()
  def +=(map: Map[Any, ChangeType]):Unit = {
    changes = map
  }

  //Diff -> Hunk -> Segment -> LineSegment
  /**
    * 添加变更
    * @param diff
    */
  def addChange(diff: Diff) : Unit = {

    diff.hunks.foreach((hunk : Hunk) => {
      val lineNR : IntRef = IntRef.create(hunk.dstLine)
      hunk.segments.foreach((segment : Segment) => {
        segment.lines.foreach((line : LineSegment) => {
          val changeType : ChangeType = ChangeType.withName(segment.diffType.toString)
          changes.+(lineNR -> changeType)
        })
      })
    })
  }
  def getResult : Map[Any, ChangeType] = {
    this.changes
  }
}
