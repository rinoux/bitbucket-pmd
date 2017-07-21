package com.fr.cra.reposync

import java.io.File

import com.atlassian.bitbucket.repository.Repository
import com.atlassian.bitbucket.scm.Command
import com.atlassian.bitbucket.scm.git.command.clone.GitClone
import com.atlassian.bitbucket.scm.git.command.{GitCommandBuilderFactory, GitScmCommandBuilder}
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.fr.cra.Logging
import com.fr.cra.GIT_CMD_TIMEOUT_IN_SEC
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired

/**
  *克隆已缓存的repo
  * Created by rinoux on 16/8/9.
  */
class CachedRepoCloner @Autowired()(@ComponentImport gitCommandFactory: GitCommandBuilderFactory,
                                    repoInformation: CachedRepoInformation) extends AnyRef with Logging{

  /**
    * 文件不存在时clone一份
    * @param repo
    */
  def cloneIfNotExisting(repo : Repository) : Unit = {
    createRepoBaseDir(repo, createCachedReposDir)
    val builder : GitScmCommandBuilder = gitCommandFactory.builder()

    val gitCloner : GitClone = builder.getClass.getMethod("clone").invoke(builder).asInstanceOf[GitClone]
    val bareDir : File = repoInformation.getBareRepoDir(repo)

    if (!bareDir.exists()) {
      val cmd = gitCloner.bare().origin(repoInformation.getRealRepoDir(repo)).directory(bareDir).build()
      cmd.setExecutionTimeout(GIT_CMD_TIMEOUT_IN_SEC)
      cmd.call()
    }
  }

  def createCachedReposDir: File ={
    val cachedReposDir = repoInformation.cachedReposDir
    cachedReposDir.mkdirs()
    cachedReposDir

  }

  /**
    * 创建repo的根目录
    * @param repository
    * @param cloneFolder
    * @return
    */
  def createRepoBaseDir(repository: Repository, cloneFolder : File) : Boolean = {
    repoInformation.getRepoBaseDir(repository).mkdir()
  }
}
