package com.fr.cra.analysis

import java.net.URI

import com.atlassian.bitbucket.server.ApplicationPropertiesService

/**
  * 对不合规范的代码评论的格式<br>
  *   使用markdown规范<br>
  * Created by rinoux on 16/8/12.
  */
class ViolationCommentFormatter(violation : Violation, applicationPropertiesService : ApplicationPropertiesService) extends AnyRef {
  /**
    * markdown表样式
    * @param text 内容
    * @return
    */
  def formatAsTable(text : String) : String = {

    val img: String = createImageSeverityMarkdown
    val msg: String = img.replace("\n", "")

    "| --------|---------|\n| " + img + " | " + msg + " |"
  }

  /**
    * 多行样式
    * @param text 内容
    * @param showSeverityIcon 类型图标
    * @param examples 例子
    * @return
    */
  def formatAsMultilineText(text : String, showSeverityIcon : Boolean, examples : String) : String = {
    var msg : String = null
    showSeverityIcon match {
      case true =>
        msg = msgWithSeverityIcon(text, examples)
      case false =>
        msg = msgWithSeverityText(text, examples)
    }
    if (examples.isEmpty) msg else {
      msg + " Example:\n\n" + intendLinesByTab(examples) + "\n\n"
    }
  }

  /**
    * 创建markdown图标
    * @return
    */
  def createImageSeverityMarkdown : String = {
    val imgFile = violation.severity.icon
    val baseUrl = applicationPropertiesService.getBaseUrl
    //图标文件url
    val imgPath = baseUrl + "/plugins/servlet/cra/severity-image?file=" + imgFile
    val altText = violation.severity.alternativeText

    "![" + altText + "](" + imgPath + " \"Severity Level: " +altText + "\")"
  }

  /**
    *
    * @param examples
    * @return
    */
  def intendLinesByTab(examples : String) : String = {

    val sb: StringBuilder = new StringBuilder
    examples.split("\n").map(sb.+("\t").+(_))

    sb.toString().mkString("\n")
  }

  /**
    * 带图标的消息
    * @param text 消息
    * @param examples 例子
    * @return
    */
  def msgWithSeverityIcon(text: String, examples: String): String = {

    createImageSeverityMarkdown + "\n第" + violation.line + "行\n" + text.replace("\n", "")
  }

  /**
    * 带错误类型文字的消息(markdown)
    * @param text 消息
    * @param examples 例子
    * @return
    */
  def msgWithSeverityText(text: String, examples: String): String = {
    "**" + this.violation.severity.alternativeText + "(第" + violation.line + "行):** " +  text.replace("\n", "")
  }
}
