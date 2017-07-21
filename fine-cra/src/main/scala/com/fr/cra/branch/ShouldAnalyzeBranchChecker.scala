package com.fr.cra.branch

import com.atlassian.bitbucket.pull.PullRequest
import com.atlassian.bitbucket.repository.Repository
import com.fr.cra.Logging
import com.fr.cra.config.repo.RepoConfigDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
  * 判断分支是否需要代码检查
  * Created by rinoux on 16/8/16.
  */
@Component
class ShouldAnalyzeBranchChecker @Autowired()(repoConfigDao : RepoConfigDao) extends AnyRef with Logging {
  val branchPermissionPatterns : BranchPermissionPatterns = new BranchPermissionPatterns

  def shouldAnalyze(pullRequest : PullRequest) : Boolean = {
    shouldAnalyze(pullRequest.getToRef.getRepository, pullRequest.getFromRef.getDisplayId)
  }

  /**
    * 是否需要代码检查，判断条件是配置cra启动并且符合branchoptions选项的规定范围
    * @param targetRepository
    * @param branchId
    * @return
    */
  def shouldAnalyze(targetRepository : Repository, branchId : String) : Boolean = {
    var matches = false
    if (repoConfigDao.isCraActive(targetRepository)) {
      if (targetRepository.getScmId.equals("git")) {
        matches = branchMatches(branchId, targetRepository)
      }
    }
    matches
  }
  def branchMatches(refId: String, repository: Repository): Boolean = {
    var matches: Boolean = false
    repoConfigDao.find(repository) match {
      case Some(repoConfig) =>
        val branchOptions = BranchOptions.from(repoConfig.getBranchOptions)
        val branches = repoConfig.getBranchOptionsBranches
        matches = branchPermissionPatterns.matches(refId, branchOptions, branches)
        if (!matches) {
          log.info("FINE-CRA: Will not consider " + refId + " because of branch options " + branchOptions)
        }
      case None =>
        log.error("FINE-CRA: No configuration found for repository " + repository.getName)
    }
    matches
  }
}
