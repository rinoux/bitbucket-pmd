package com.fr.cra.analysis.pmd

import com.fr.cra.Logging
import com.fr.cra.analysis._
import net.sourceforge.pmd.renderers._
import net.sourceforge.pmd.util.datasource.DataSource
import net.sourceforge.pmd.{Report, RulePriority}
import com.fr.cra.CODE_REVIEW_ASSISTANT_NAME

import scala.collection.JavaConverters

/**
  * Created by rinoux on 16/8/25.
  */
class PMDCollectorRenderer() extends AbstractRenderer("CRA", CODE_REVIEW_ASSISTANT_NAME) with Logging {

  val reviewResult : AnalyzerResult = new AnalyzerResult(PMDProcessor.Name)
  override def defaultFileExtension() : Null = null
  override def start() : Unit = log.debug("FINE-CRA: PMD audit started")
  override def end() : Unit = log.debug("FINE-CRA: PMD audit ended")
  override def startFileAnalysis(ds : DataSource) : Unit = log.debug("FINE-CRA: PMD startFileAnalysis")
  override def renderFileReport(report : Report) : Unit = {
    savePMDViolations(report)
    reportPMDErrors(report)
  }

  def savePMDViolations(report: Report): Unit = {
    JavaConverters.iterableAsScalaIterableConverter(report).asScala.withFilter(_ != null).foreach(rv => {
      val violation = Violation(rv.getFilename, rv.getBeginLine, rv.getDescription, toCraSeverity(rv.getRule.getPriority), rv.getRule.getDescription, JavaConverters.asScalaBufferConverter(rv.getRule.getExamples).asScala.mkString("\n"))
      log.debug("FINE-CRA: PMD violation detected:" + violation)
      reviewResult.addViolation(violation)
    })

  }
  def reportPMDErrors(report: Report): Unit = {
    JavaConverters.asScalaIteratorConverter(report.errors()).asScala.foreach(pe => {
      reviewResult.addViolation(Violation(pe.getFile, 0, pe.getMsg, FATAL, "", ""))
      log.error("FINE-CRA: PMD processing error detected: " +  pe.getMsg)
    })
    JavaConverters.asScalaIteratorConverter(report.configErrors()).asScala.foreach(rce => {
      reviewResult.addViolation(Violation(rce.rule().getName, 0, rce.issue(), FATAL, "", ""))
      log.error("FINE-CRA: PMD Configuration error detected: " + rce.issue())
    })
  }

  def toCraSeverity(rulePriority: RulePriority): Severity = {
    rulePriority match {
      case RulePriority.HIGH =>
        ERROR
      case RulePriority.MEDIUM_HIGH =>
        WARNING
      case RulePriority.MEDIUM =>
        INFO
      case RulePriority.MEDIUM_LOW =>
        INFO
      case RulePriority.LOW =>
        INFO
      case _ =>
        throw new RuntimeException(new StringBuilder().append("RulePriority ").append(rulePriority).append("is not supported").toString())
    }
  }
}
