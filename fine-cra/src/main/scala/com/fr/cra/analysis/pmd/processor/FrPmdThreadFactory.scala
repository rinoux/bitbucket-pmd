package com.fr.cra.analysis.pmd.processor

import java.util
import java.util.Collections
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

import net.sourceforge.pmd.{RuleContext, RuleSets}

/**
  * Created by rinoux on 2016/11/1.
  */
class FrPmdThreadFactory(ruleSets: RuleSets, ctx: RuleContext) extends AnyRef with ThreadFactory{
  val counter = new AtomicInteger
  val threadList: java.util.List[Runnable] = Collections.synchronizedList(new util.LinkedList[Runnable]())

  override def newThread(r: Runnable): Thread = {
    val t = FrPmdRunnable.createThread(counter.incrementAndGet(), r,ruleSets, ctx)
    threadList.add(t)
    t
  }
}
