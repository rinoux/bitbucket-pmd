package com.fr.cra.reposync

import java.io.File

import com.atlassian.bitbucket.scm.Command
import com.atlassian.bitbucket.scm.git.command.GitScmCommandBuilder
/**
  * Created by rinoux on 16/8/8.
  */
class GitCheckoutBuilder(builder: GitScmCommandBuilder) extends AnyRef {

  var myBranch: String  = _
  var myWorkTree : String = _

  def branch(value : String) : GitCheckoutBuilder = {
    myBranch = value
    this
  }
  def workingDirectory(value : File) : GitCheckoutBuilder = {
    builder.workingDirectory(value)
    this
  }
  def workTree(value : String) : GitCheckoutBuilder = {
    myWorkTree = value
    this
  }
  def build() : Command[Unit] = {
    val handler: LoggingCommandOutputHandler = new LoggingCommandOutputHandler()
    builder.clearArguments()
    applyArguments()
    val command: Command[Unit] = builder.build(handler)
    handler.setCommand(command.asInstanceOf[Command[Unit]])
    command
  }

  def applyArguments(): Unit ={
    builder.argument(StringBuilder.newBuilder.append("--work-tree=").append(myWorkTree).toString())
    builder.argument("checkout")
    builder.argument("-f").argument(myBranch)
  }
}
