package com.fr.cra.reposync

import java.io.{IOException, InputStream}

import com.atlassian.bitbucket.scm.{Command, CommandOutputHandler}
import com.atlassian.utils.process.{BaseOutputHandler, ProcessException}
import com.google.common.io.ByteStreams
import org.slf4j.{Logger, LoggerFactory}

/**
  * Created by rinoux on 16/8/8.
  */
class LoggingCommandOutputHandler extends BaseOutputHandler with CommandOutputHandler[Unit] {
  var log: Logger = _
  override def getOutput: Unit ={}
  //判断logging Command是否为空
  override def process(output: InputStream): Unit = {
    try {
      Option(ByteStreams.toByteArray(output)) match {
        case Some(bytes) =>
          if (this.log.isDebugEnabled) {
            log.debug("FINE-SRA: Git command completed with the following output:\n", new String(bytes, "UTF-8"))
          }
        case None =>
          log.error("Got no output")
      }
    } catch {
      case e : IOException =>
        throw new ProcessException(e)
    }

  }

  def setCommand(command: Command[Unit]): Unit ={
    log = LoggerFactory.getLogger(command.getClass)
  }
}
