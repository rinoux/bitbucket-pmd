package com.fr.cra.statistics

import net.java.ao.{Accessor, Entity, Mutator}
import net.java.ao.schema.{Indexed, Table, NotNull}

/**
  * 单个分析器的结果, 各类sevrity的数量
  * Created by rinoux on 16/8/16.
  */
@Table(value = "STATIC_LYZR_RESULTS")
trait AoStaticAnalyzerResults extends Object with Entity {
  @Accessor(value = "CODE_REVIEW_STATISTICS")
  def getCodeReviewStatistics : AoCodeReviewStatistics
  @Mutator(value = "CODE_REVIEW_STATISTICS")
  def setCodeReviewStatistics(statistics : AoCodeReviewStatistics) : Unit
  @Accessor(value = "NAME")
  @Indexed
  @NotNull
  def getName : String
  @Accessor(value = "FATAL_COUNT")
  def getFatalCount : Integer
  @Mutator(value = "FATAL_COUNT")
  def setFatalCount(count : Integer) : Unit
  @Accessor(value = "ERROR_COUNT")
  def getErrorCount : Integer
  @Mutator(value = "ERROR_COUNT")
  def setErrorCount(count : Integer) : Unit
  @Accessor(value = "WARNING_COUNT")
  def getWarningCount : Integer
  @Mutator(value = "WARNING_COUNT")
  def setWarningCount(value : Integer) : Unit
  @Accessor(value = "INFO_COUNT")
  def getInfoCount : Integer
  @Mutator(value = "INFO_COUNT")
  def setInfoCount(value : Integer) : Unit
}
