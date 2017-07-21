package com.fr.cra.analysis.pmd.processor

import java.io.{BufferedInputStream, IOException}

import com.fr.cra.Logging
import net.sourceforge.pmd.Report.ProcessingError
import net.sourceforge.pmd._
import net.sourceforge.pmd.processor.AbstractPMDProcessor
import net.sourceforge.pmd.renderers.Renderer
import net.sourceforge.pmd.util.datasource.DataSource

import scala.collection.JavaConverters

/**
  * Created by rinoux on 2016/11/1.
  */
class FrMonoThreadProcessor(config: PMDConfiguration) extends AbstractPMDProcessor(config) with Logging{

  def processFiles(ruleSets: RuleSets, files : Seq[DataSource], ctx: RuleContext, renderers: Seq[Renderer]): Unit = {
    val processor : SourceCodeProcessor = new SourceCodeProcessor(config)
    files.foreach(d => {
      val niceFileName = filenameFrom(d)
      val report = PMD.setupReport(ruleSets, ctx, niceFileName)
      log.info("PDM IS Processing " + ctx.getSourceCodeFilename)
      ruleSets.start(ctx)
      renderers.foreach(r => r.startFileAnalysis(d))

      try {
        val bis = new BufferedInputStream(d.getInputStream)
        ctx.setLanguageVersion(null)
        processor.processSourceCode(bis, ruleSets, ctx)
      } catch {
        case e1 : PMDException =>
          log.error("Error while processing file: " + niceFileName, e1)
          report.addError(new ProcessingError(e1.getMessage, niceFileName))
        case e2 : IOException =>
          log.error(" Unable to read source file" + niceFileName, e2.getCause)
          report.addError(new ProcessingError(e2.getMessage, niceFileName))
        case e3 : RuntimeException =>
          log.error("RuntimeException while processing file" + niceFileName, e3.getCause)
          report.addError(new ProcessingError(e3.getMessage, niceFileName))
      }

      ruleSets.end(ctx)
      renderReports(JavaConverters.seqAsJavaListConverter(renderers).asJava, ctx.getReport)
    })
  }

}
