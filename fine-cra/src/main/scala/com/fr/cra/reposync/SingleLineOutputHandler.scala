package com.fr.cra.reposync

import com.atlassian.bitbucket.scm.CommandOutputHandler
import com.atlassian.utils.process.StringOutputHandler
import org.apache.commons.lang3.StringUtils

import scala.reflect.ScalaSignature

/**
  * Created by rinoux on 16/8/9.
  */
class SingleLineOutputHandler extends StringOutputHandler with CommandOutputHandler[String]{
  override def getOutput: String = StringUtils.chomp(super.getOutput)
}
