package com.fr.cra.analysis.checkstyle

import java.io.File
import java.util
import java.util.HashMap

import com.fr.cra.Logging
import com.fr.cra.analysis.checkstyle.SuppressionFilePathTransformer.MyConfiguration
import com.google.common.collect.{ImmutableMap, Lists, Maps}
import com.puppycrawl.tools.checkstyle.api.{CheckstyleException, Configuration}
import org.slf4j.Logger

import scala.collection.{JavaConverters, mutable}

/**
  * 转换文件的路径(相对->绝对)<br>
  * Created by rinoux on 16/8/12.
  */
class SuppressionFilePathTransformer() extends AnyRef with Logging {

  /**
    * 转换路径主方法
    * @param config 配置
    * @param pullRequestDir pullRequest的根路径
    * @return
    */
  def transformRelativeToAbsolutePath(config : Configuration, pullRequestDir : File) : Configuration = {
    val myConfig = copyConfig(config)
    val suppressionElements = myConfig.getChildren.filter(c => {
      c.getName.equals(SuppressionFilePathTransformer.SuppressionFilterElement)
    })
    if (suppressionElements.length != 0) {
      suppressionElements.foreach(se => {
        val fileName = se.getAttribute(SuppressionFilePathTransformer.FileAttribute)
        if (Option(fileName).isDefined && !new File(fileName).exists() && se.isInstanceOf[SuppressionFilePathTransformer.MyConfiguration]) {
          val suppressionFile = new File(pullRequestDir, fileName)
          se.asInstanceOf[SuppressionFilePathTransformer.MyConfiguration].addAttribute(SuppressionFilePathTransformer.FileAttribute, suppressionFile.getAbsolutePath)
        }
      })
    }
    myConfig
  }
  def copyConfig(config: Configuration): MyConfiguration = {
    val myConfig: MyConfiguration = new MyConfiguration(config.getName)
    //把message、attr,child都复制到myConfig
    JavaConverters.mapAsScalaMapConverter(config.getMessages).asScala.foreach(kv => {
      val (k, v) = kv
      myConfig.addMessage(k, v)
    })
    config.getAttributeNames.foreach(n => myConfig.addAttribute(n, config.getAttribute(n)))
    config.getChildren.foreach(child =>{
      myConfig.addChild(copyConfig(child))
    })
    myConfig
  }
}

/**
  *待查文件路径转换
  */
object SuppressionFilePathTransformer extends AnyRef {
  val SuppressionFilterElement: String = "SuppressionFilter"
  val FileAttribute: String = "file"
  val children : java.util.ArrayList[Configuration] = Lists.newArrayList()
  val attributeMap : util.HashMap[String, String] = Maps.newHashMap()
  val messages : util.HashMap[String, String] = Maps.newHashMap()

  /**
    * 实现Configuration接口, 见checkstyle api的DefaultConfiguration
    * @param name
    */
  class MyConfiguration(name: String) extends Object with Configuration {
    override def getName: String = {
      name
    }
    override def getAttribute(name: String): String = {
      if (attributeMap.containsKey(name)) {
        attributeMap.get(name).toString
      } else {
        throw new CheckstyleException(mutable.StringBuilder.newBuilder.append("missing key '").append(name).append("' in ").append(getName()).toString())
      }
    }

    override def getMessages: ImmutableMap[String, String] = {
      ImmutableMap.copyOf(messages)
    }

    override def getAttributeNames: Array[String] = {
      val keySet : util.Set[String] = attributeMap.keySet()
      keySet.toArray(new Array[String](keySet.size()))
    }

    override def getChildren: Array[Configuration] = {
      children.toArray(new Array[Configuration](children.size()))
    }

    def addAttribute(name: String, value: String): Unit = {
      attributeMap.put(name, value)
    }

    def removeChild(configuration: Configuration): Unit = {
      children.remove(configuration)
    }

    def addChild(configuration: Configuration): Unit = {
      children.add(configuration)
    }

    def addMessage(key: String, value: String): Unit = {
      messages.put(key, value)
    }
  }

}
