package com.fr.cra.reposync

import java.io.{File, IOException}

import com.atlassian.bitbucket.pull.PullRequest
import com.atlassian.bitbucket.repository.{Repository, RepositoryService}
import com.atlassian.bitbucket.scm.git.GitScmConfig
import com.atlassian.bitbucket.server.ApplicationPropertiesService
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.fr.cra.Logging
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component


/**
  * 缓存repo的信息的容器
  * Created by rinoux on 16/8/8.
  */
@Component
class CachedRepoInformation @Autowired()(repositoryService: RepositoryService,
                                         applicationPropertiesService: ApplicationPropertiesService,
                                         @ComponentImport gitScmConfig: GitScmConfig) extends AnyRef with Logging{

  //缓存repo的到home/cached/crarepos路径中
  var cachedReposDir = new File(applicationPropertiesService.getCacheDir, "crarepos")

  def getRepository(pullRequest: PullRequest) : Repository = {
    pullRequest.getToRef.getRepository
  }

  def getPullRequestsBaseDir(repository: Repository) : File = {
    new File(getRepoBaseDir(repository), "prs")
  }
  def getCachedPRCheckoutDir(pullRequest : PullRequest) : File = {
    val repo : Repository = getRepository(pullRequest)
    new File(getPullRequestsBaseDir(repo), pullRequest.getId.toString)
  }
  def deleteCachedPRDirs(pullRequest : PullRequest) : Unit = {
    safelyDeleteDirectory(getCachedPRCheckoutDir(pullRequest))
  }
  def safelyDeleteDirectory(filePath: File): Unit = {
    try {
      FileUtils.deleteDirectory(filePath.getAbsoluteFile)
      log.info("FINE-CRA: Deleted cache directory " + filePath.getAbsolutePath + " for pull request")
    } catch {
      case e: IOException =>
        log.error("FINE-CRA: Failed to delete cache directory " + filePath.getAbsolutePath + " for pull request")
    }

  }
  def getRepoBaseDir(repo : Repository) : File = {
    new File(cachedReposDir, repo.getId.toString)
  }
  def getBareRepoDir(repo : Repository) : File = {
    new File(getRepoBaseDir(repo), repo.getId.toString)
  }
  def getRealRepoDir(repo : Repository) : File = {
    this.gitScmConfig.getRepositoryDir(repo)
  }
  def getCraHomeDir : File = cachedReposDir

}
