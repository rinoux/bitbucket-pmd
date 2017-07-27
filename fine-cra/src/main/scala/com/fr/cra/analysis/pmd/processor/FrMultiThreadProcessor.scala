package com.fr.cra.analysis.pmd.processor

import java.util
import java.util.concurrent.{ExecutionException, Executors, Future}

import net.sourceforge.pmd.{PMDConfiguration, Report, RuleContext, RuleSets}
import net.sourceforge.pmd.processor.AbstractPMDProcessor
import net.sourceforge.pmd.renderers.Renderer
import net.sourceforge.pmd.util.datasource.DataSource

import scala.collection.JavaConverters

/**
  * Created by rinoux on 2016/11/1.
  */
class FrMultiThreadProcessor(config: PMDConfiguration) extends AbstractPMDProcessor(config){

  def processFiles(ruleSets: RuleSets, files : java.util.List[DataSource], ctx: RuleContext, renderers: java.util.List[Renderer]): Unit = {
    ruleSets.start(ctx)
    val factory = new FrPmdThreadFactory(ruleSets, ctx)
    //问题就是这里，多个线程竞争导致AbstractRuleChainVisitor 的ConcurrentModificationException异常
    //暂时只搞一个线程
    val executor = Executors.newFixedThreadPool(1, factory)

    val tasks = new util.LinkedList[Future[Report]]()

    for (i <- 0 until files.size()) {
      val dataSource = files.get(i)
      val r = new FrPmdRunnable(executor, ruleSets, config, dataSource, filenameFrom(dataSource), renderers)
      val future = executor.submit(r)
      tasks.add(future)
    }
    executor.shutdown()
    processReports(renderers, tasks)
    ruleSets.end(ctx)
    super.renderReports(renderers, ctx.getReport)
  }

  def processReports(renderers: java.util.List[Renderer], tasks: util.LinkedList[Future[Report]]): Unit = {
    while (!tasks.isEmpty) {
      val future = tasks.remove(0)
      var report: Report = null
      try {
        report = future.get()
      } catch {
        case e1: InterruptedException =>
          Thread.currentThread().interrupt()
          future.cancel(true)
        case e2: ExecutionException =>
          e2.getCause match {
            case r1: RuntimeException =>
              throw r1
            case r2: Error =>
              throw r2
          }
          throw new IllegalStateException("PmdRunnable exception", e2.getCause)
      }
      renderReports(renderers, report)
    }
  }
}
