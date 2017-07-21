package com.fr.cra

import java.io.File
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.fr.cra.analysis.StaticAnalyzerRegistry
import org.apache.commons.io.IOUtils

/**
  * 内置分析器配置文件的servlet
  * Created by rinoux on 16/9/18.
  */
class BuiltinConfigListingServlet(staticAnalyzerRegistry : StaticAnalyzerRegistry) extends HttpServlet with Logging {
  override def doGet(req : HttpServletRequest, res : HttpServletResponse) : Unit = {
    //根据name参数获取
    staticAnalyzerRegistry.getBuiltInAnalyzer(req.getParameter("name")) match {
      case Some(processor) =>
        processor.getBuiltinConfigPath.foreach(configFilePath => {
          res.setHeader("Content-Disposition", StringBuilder.newBuilder.append("attachment; filename=").append(new File(configFilePath).getName).toString())
          res.setStatus(200)
          using(this.getClass.getResourceAsStream(configFilePath))(is => {
            using(res.getOutputStream)(os => {
              IOUtils.copy(is, os)
              os.flush()
            })
          })
        })
      case None =>
        res.setStatus(600)
    }
  }
}
