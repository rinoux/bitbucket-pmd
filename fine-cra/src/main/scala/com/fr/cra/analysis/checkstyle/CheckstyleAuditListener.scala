package com.fr.cra.analysis.checkstyle

import com.fr.cra.Logging
import com.fr.cra.analysis._
import com.puppycrawl.tools.checkstyle.api.{AuditEvent, AuditListener, SeverityLevel}

/**
  * checkstyle 行为监听观察
  * Created by rinoux on 16/8/12.
  */
class CheckstyleAuditListener() extends Object with AuditListener with Logging{
  var reviewResult : AnalyzerResult = new AnalyzerResult(CheckstyleProcessor.Name)

  override def auditStarted(auditEvent : AuditEvent) : Unit = {
    log.debug("FINE-CRA: Checkstyle audit started")
  }
  override def auditFinished(auditEvent : AuditEvent) : Unit = {
    log.debug("FINE-CRA: Checkstyle audit finished")
  }
  override def fileStarted(auditEvent : AuditEvent) : Unit = {
    log.debug("FINE-CRA: Checkstyle audit started for file " +  auditEvent.getFileName)
  }
  override def fileFinished(auditEvent : AuditEvent) : Unit = {
    log.debug("FINE-CRA: Checkstyle audit finished for file " +  auditEvent.getFileName)
  }

  /**
    * 根据等级添加错误信息
    * @param auditEvent
    */
  override def addError(auditEvent : AuditEvent) : Unit = {
    val severityLevel = auditEvent.getSeverityLevel
    if (!severityLevel.equals(SeverityLevel.IGNORE)) {
      val v = Violation(auditEvent.getFileName, auditEvent.getLine, auditEvent.getMessage, this.toCraSeverity(auditEvent.getSeverityLevel), "", "")
      log.info("FINE-CRA: Checkstyle violation found: " +  v)
      reviewResult.addViolation(v)
    }
  }
  override def addException(auditEvent : AuditEvent, exception : Throwable) : Unit = {
    log.error("FINE-CRA: Checkstyle audit exception thrown for file " + auditEvent, exception)
    reviewResult.addViolation(Violation(auditEvent.getFileName, 0, exception.getMessage, FATAL, "", ""))
  }

  /**
    * 将SeverityLevel转为CRA的Severity
    * @param severityLevel
    * @return
    */
  def toCraSeverity(severityLevel: SeverityLevel) : Severity = {
    severityLevel match {
      case SeverityLevel.INFO =>
        INFO
      case SeverityLevel.WARNING =>
        WARNING
      case SeverityLevel.ERROR =>
        ERROR
      case _ =>
        throw new RuntimeException("SeverityLevel " + severityLevel + " is not supported")
    }
  }
}
