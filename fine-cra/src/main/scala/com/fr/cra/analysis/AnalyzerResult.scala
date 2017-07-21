package com.fr.cra.analysis

import scala.collection.immutable.Vector

/**
  * 单个分析器分析结果<br>
  *   violations的getter和setter,以及获取ViolationCounts<br>
  * Created by rinoux on 16/8/4.
  */

class AnalyzerResult(val staticAnalyzerName : String) {
  var violations : scala.collection.immutable.Vector[Violation] = Vector[Violation]()
  def addViolation(violation : Violation) : Unit = {
    violations = violations.:+(violation)
  }
  def getViolations : Vector[Violation] = {
    violations.distinct
  }

  def getSummary : ViolationCounts = {
    ViolationCounts(
      violations.count(v => v.severity.equals(FATAL)),
      violations.count(v => v.severity.equals(ERROR)),
      violations.count(v => v.severity.equals(WARNING)),
      violations.count(v => v.severity.equals(INFO)))
  }
  override def toString: String = {
    getSummary.toString
  }
}
