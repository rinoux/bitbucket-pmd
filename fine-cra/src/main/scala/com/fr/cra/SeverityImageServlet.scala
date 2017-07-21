package com.fr.cra

import java.io.{File, FileInputStream, IOException}
import java.util.concurrent.TimeUnit
import javax.servlet.ServletOutputStream
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import org.apache.commons.io.IOUtils
import org.slf4j.Logger

/**
  * 获取危险级别图标的servlet
  * Created by rinoux on 16/8/17.
  */
class SeverityImageServlet() extends HttpServlet with Logging {

  override def doGet(req : HttpServletRequest, res : HttpServletResponse) : Unit = {
    res.setHeader("Cache-Control", StringBuilder.newBuilder.append("max-age=").append(TimeUnit.DAYS.toSeconds(28L)).toString())
    //根据请求参数获取对应图标文件
    Option(req.getParameter("file")) match {
      case Some(filename) =>
        //currying
        using(this.getClass.getResourceAsStream("/img/severity/" + filename))(in => {
          val out : ServletOutputStream = res.getOutputStream
          try {
            //这个好用(-:
            IOUtils.copy(in, out)
          } catch {
            case e : IOException => log.error("FINE-CRA: Failed to retrieve image" + filename)
          }
        })
      case None =>
        res.setStatus(400)
    }
  }
}
