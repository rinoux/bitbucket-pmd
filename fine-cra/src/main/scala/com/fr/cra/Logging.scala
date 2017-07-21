package com.fr.cra

import org.apache.log4j.Logger
/**
  * 统一的日志接口
  * Created by rinoux on 16/8/8.
  */
trait Logging {
  protected val log = Logger.getLogger(this.getClass.getName)
  /**
    * 日志级别为DEBUG的时候，使用log.debug(msg)debug日志会予以显示
    * 日志级别设置在bitbucket/home/shared/bitbucket.properties中，建议不要开低级别，量太多
    * @param msg
    * @tparam T
    */
  def debug[T](msg: => T): Unit = {
    if (log.isDebugEnabled) {
      log.debug(msg.toString)
    }
  }
}
