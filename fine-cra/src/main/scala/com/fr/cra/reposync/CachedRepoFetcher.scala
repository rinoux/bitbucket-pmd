package com.fr.cra.reposync

import com.atlassian.bitbucket.repository.Repository
import com.atlassian.bitbucket.scm.git.command.GitCommandBuilderFactory
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.fr.cra.Logging
import org.springframework.beans.factory.annotation.Autowired
import com.fr.cra.GIT_CMD_TIMEOUT_IN_SEC


/**
  * Created by rinoux on 16/8/9.
  */
class CachedRepoFetcher @Autowired()(@ComponentImport gitCommandFactory : GitCommandBuilderFactory,
                                     repo : Repository,
                                     cachedRepoInfo : CachedRepoInformation) extends AnyRef with Logging {
  def fetchAll(refIds : List[String]) : Unit = {
    refIds.foreach(refId => {
      val bareRepoDir = cachedRepoInfo.getBareRepoDir(repo)
      val realRepoDir = cachedRepoInfo.getRealRepoDir(repo)
      val refspec = StringBuilder.newBuilder.append(refId).append(":").append(refId).toString
      val cmd = gitCommandFactory.builder(repo).workingDirectory(bareRepoDir).fetch().force(true).repository(realRepoDir).refspec(refspec).build(new SingleLineOutputHandler)
      cmd.setExecutionTimeout(GIT_CMD_TIMEOUT_IN_SEC)
      cmd.call()
    })
  }
}
