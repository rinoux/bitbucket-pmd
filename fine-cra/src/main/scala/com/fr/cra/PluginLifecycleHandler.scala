package com.fr.cra

import com.atlassian.event.api.{EventListener, EventPublisher}
import com.atlassian.plugin.event.events.PluginEnabledEvent
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService
import com.atlassian.scheduler.SchedulerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.{DisposableBean, InitializingBean}
import org.springframework.stereotype.Component

/**
  * 插件的生命周期管理
  * Created by rinoux on 16/8/17.
  */
@Component
class PluginLifecycleHandler @Autowired()(schedulerService : SchedulerService, eventPublisher : EventPublisher) extends Object with InitializingBean with DisposableBean with Logging {
  override def afterPropertiesSet() : Unit = {

  }
  //监听插件启用
  @EventListener
  def onPluginEnabledEvent(event : PluginEnabledEvent) : Unit = {
    if (isThisPlugin(event)) {
      preparePluginStart()
    }
  }
  override def destroy() : Unit = {
    unregisterListener()
    unregisterJobRunner()
  }

  def preparePluginStart(): Unit ={
    log.warn("FINE-CRA: Got the plug-in start event... Time to get started!")
    registerListener()
    try {
      launch()
    } catch {
      case e: Exception => log.error("FINE-CRA: Unexpected error during launch", e)
    }
  }
  def launch(): Unit ={
    registerJobRunner()
  }
  def registerJobRunner(): Unit ={
  }
  def unregisterJobRunner(): Unit ={
  }
  def unregisterListener(): Unit ={
    eventPublisher.unregister(this)
  }
  def registerListener(): Unit ={
    eventPublisher.register(this)
  }
  //判断是否是当前插件
  def isThisPlugin(event: PluginEnabledEvent): Boolean ={
    event.getPlugin.getKey.equals("com.fr.cra")
  }
}
