package com.fr.cra.analysis

import com.fr.cra.Logging
import org.slf4j.Logger

import scala.runtime.ScalaRunTime

/**
  * 对冲突的描述模型
  * Created by rinoux on 16/8/4.
  */
/**
  *
  * @param filePath 冲突所在文件的路径
  * @param line 冲突所在行数
  * @param message 冲突信息
  * @param severity 严重等级
  * @param detailedMessage 详细情况
  * @param examples 例子
  */
case class Violation (filePath : String,
                      line : Int,
                      message : String,
                      severity : Severity,
                      detailedMessage : String,
                      examples : String)
  extends AnyRef with Product with Serializable{

}
