package com.fr.cra.statistics

import java.lang.Boolean

import net.java.ao.schema.{Indexed, Table}
import net.java.ao.{Accessor, Entity, Mutator, OneToMany}
import net.java.ao.schema.NotNull
/**
  * 代码检查数据的ao<br>
  *   包括pr id, repo id, 各分析器的结果, 是否最新
  * Created by rinoux on 16/8/16.
  */
@Table("CodeReviewStats")
trait AoCodeReviewStatistics extends Object with Entity {
  @Accessor(value = "PULL_REQUEST_ID")
  @Indexed
  @NotNull
  def getPullRequestId : Long
  @Mutator(value = "PULL_REQUEST_ID")
  def setPullRequestId(id : Long) : Unit
  @Accessor(value = "REPO_ID")
  @Indexed
  @NotNull
  def getRepositoryId : Integer
  @Mutator(value = "REPO_ID")
  def setRepositoryId(id : Integer) : Unit
  /**
    * 获取不同分析器的结果
    * @return
    */
  @OneToMany(reverse = "getCodeReviewStatistics")
  def getStaticAnalyzerResults : Array[AoStaticAnalyzerResults]
  @Accessor(value = "IS_UP_TO_DATE")
  def isUpToDate : java.lang.Boolean
  @Mutator(value = "IS_UP_TO_DATE")
  def setUpToDate(id : java.lang.Boolean) : Unit
}
