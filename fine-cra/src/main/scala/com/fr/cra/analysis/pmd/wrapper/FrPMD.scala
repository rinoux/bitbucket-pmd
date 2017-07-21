package com.fr.cra.analysis.pmd.wrapper

import java.util
import java.util.{Collections, Comparator}

import com.fr.cra.analysis.pmd.processor.{FrMonoThreadProcessor, FrMultiThreadProcessor}
import net.sourceforge.pmd.renderers.Renderer
import net.sourceforge.pmd.util.SystemUtils
import net.sourceforge.pmd.util.datasource.DataSource
import net.sourceforge.pmd.{PMD, PMDConfiguration, RuleContext, RuleSets}

import scala.collection.JavaConverters

/**
  * Created by rinoux on 2016/11/1.
  */
class FrPMD extends PMD {

}
object FrPMD{

  def processFiles(pmdConfig : PMDConfiguration, ruleSets : RuleSets, files : java.util.List[DataSource], ctx: RuleContext, renderers: java.util.List[Renderer]): Unit = {

    sortFiles(pmdConfig, files)
    if (SystemUtils.MT_SUPPORTED && pmdConfig.getThreads > 0) {
      new FrMultiThreadProcessor(pmdConfig).processFiles(ruleSets, files, ctx, renderers)
    } else new FrMonoThreadProcessor(pmdConfig)
  }

  private def sortFiles(configuration: PMDConfiguration, files: java.util.List[DataSource]) {
    if (configuration.isStressTest) Collections.shuffle(files)
    else {
      val useShortNames: Boolean = configuration.isReportShortNames
      val inputPaths: String = configuration.getInputPaths
      Collections.sort(files, new Comparator[DataSource]() {
        def compare(left: DataSource, right: DataSource): Int = {
          val leftString: String = left.getNiceFileName(useShortNames, inputPaths)
          val rightString: String = right.getNiceFileName(useShortNames, inputPaths)
          leftString.compareTo(rightString)
        }
      })
    }
  }
}
