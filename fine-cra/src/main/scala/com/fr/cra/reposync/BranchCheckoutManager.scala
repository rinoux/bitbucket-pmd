package com.fr.cra.reposync

import java.io.File

import com.atlassian.bitbucket.pull.PullRequest
import com.atlassian.bitbucket.repository.{Repository, RepositoryService}
import com.atlassian.bitbucket.scm.Command
import com.atlassian.bitbucket.scm.git.command.GitCommandBuilderFactory
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.fr.cra.Logging
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import com.fr.cra.GIT_CMD_TIMEOUT_IN_SEC

/**
  * 分支checkout工具
  * Created by rinoux on 16/8/8.
  */
class BranchCheckoutManager @Autowired()(@ComponentImport gitCommandFactory : GitCommandBuilderFactory,
                                         repositoryService : RepositoryService,
                                         cachedRepoInfo : CachedRepoInformation) extends AnyRef with Logging {

  /**
    * 将pullrequest checkout到缓存中
    * @param pullRequest
    * @return
    */
  def checkoutToCachedDir(pullRequest : PullRequest) : File = {

    val repository: Repository = pullRequest.getFromRef.getRepository
    val checkoutDir : File = cachedRepoInfo.getCachedPRCheckoutDir(pullRequest)
    checkoutDir.mkdirs()
    if(checkoutDir.exists()) {
      //将pullrequest的所在分支代码缓存到prs中？
      val cmd : Command[Unit] = new GitCheckoutBuilder(gitCommandFactory.builder()).workingDirectory(cachedRepoInfo.getBareRepoDir(repository)).workTree(checkoutDir.getAbsolutePath).branch(getFromBranchName(pullRequest)).build()
      cmd.setExecutionTimeout(GIT_CMD_TIMEOUT_IN_SEC)
      cmd.call()
    } else throw new IllegalStateException("Was not able to create directory to checkout source branch of pull request: " + checkoutDir.getAbsolutePath)
    checkoutDir
  }

  /**
    * pull request的来源(某个branch或者某个用户create)
    * @param pullRequest
    * @return
    */
  def getFromBranchName(pullRequest: PullRequest) : String = {
    pullRequest.getFromRef.getDisplayId
  }
}
