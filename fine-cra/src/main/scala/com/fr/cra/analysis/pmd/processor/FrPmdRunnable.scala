package com.fr.cra.analysis.pmd.processor

import java.io.{BufferedInputStream, IOException, InputStreamReader}
import java.util.concurrent.{Callable, ExecutorService}

import com.fr.cra.Logging
import net.sourceforge.pmd.Report.ProcessingError
import net.sourceforge.pmd._
import net.sourceforge.pmd.renderers.Renderer
import net.sourceforge.pmd.util.datasource.DataSource

import scala.collection.JavaConverters

/**
  * Created by rinoux on 2016/11/1.
  */
class FrPmdRunnable(executorService: ExecutorService, ruleSets: RuleSets, pmdConfiguration: PMDConfiguration, dataSource: DataSource, fileName : String, renderers : java.util.List[Renderer]) extends PMD(pmdConfiguration) with Callable[Report] with Logging{

  override def call(): Report = {
    val thread = Thread.currentThread().asInstanceOf[FrPmdRunnable.PmdThread]
    val ctx = thread.getRuleContext
    val report = PMD.setupReport(ruleSets, ctx, fileName)
    log.info("PMD is Processing " + ctx.getSourceCodeFilename)
    //JavaConverters.asScalaIteratorConverter(renderers.iterator()).asScala.foreach(r => r.startFileAnalysis(dataSource))
    for (i <- 0 until renderers.size()) {
      renderers.get(i).startFileAnalysis(dataSource)
    }

    try {
      ctx.setLanguageVersion(null)
      super.getSourceCodeProcessor.processSourceCode(new InputStreamReader(dataSource.getInputStream), ruleSets, ctx)

    } catch {
      case e1 : PMDException =>
        log.error("Error while processing file: " + this.fileName, e1)
        FrPmdRunnable.addError(report, e1, fileName)
      case e2 : IOException =>
        addErrorAndShutdown(report, e2, "IOException during processing of " + this.fileName)
      case e3 : RuntimeException =>
        addErrorAndShutdown(report, e3, "RuntimeException during processing of " + this.fileName)
    }
    report
  }

  def addErrorAndShutdown(report: Report, exception: Exception, errorMessage: String): Unit = {
    log.error(errorMessage, exception)
    FrPmdRunnable.addError(report, exception, fileName)
    executorService.shutdown()
  }
}
object FrPmdRunnable{
  def addError(report: Report, exception: Exception, fileName: String): Unit ={
    report.addError(new ProcessingError(exception.getMessage, fileName))
  }

  def createThread(id: Int, target: Runnable, ruleSets: RuleSets,  ctx: RuleContext) : Thread = {
    new PmdThread(id, target, ruleSets, ctx)
  }

  private class PmdThread(id: Int, target: Runnable, ruleSets: RuleSets, ctx: RuleContext) extends Thread(target, "PmdThread " + id){
    def getRuleContext: RuleContext = {
      ctx
    }
    override def toString: String = "PmdThread-" + this.id
  }
}
