package com.fr.cra.reposync

import com.atlassian.bitbucket.pull.{PullRequest, PullRequestSearchRequest, PullRequestService, PullRequestState}
import com.atlassian.bitbucket.repository.Repository
import com.fr.cra.Utils

import scala.collection.immutable.HashSet
/**
  * Created by rinoux on 16/8/9.
  */
class BranchesInOpenPRsCollector(pullRequestService: PullRequestService) extends AnyRef {

  def collectOpenPullRequests(sourceBranches : List[String], repo : Repository) : HashSet[PullRequest] = {

    val openPullRequests: HashSet[PullRequest] = new HashSet[PullRequest]
    sourceBranches.foreach(sourceBranch => {
      searchOpenPRsWithThisSourceBranch(repo, sourceBranch).foreach(openPullRequests.+)
    })
    openPullRequests
  }
  def searchOpenPRsWithThisSourceBranch(repository: Repository, sourceBranch: String) : Iterable[PullRequest] = {
    val builder : PullRequestSearchRequest.Builder = new PullRequestSearchRequest.Builder
    val searchRequest : PullRequestSearchRequest = builder.state(PullRequestState.OPEN).fromRepositoryId(repository.getId).fromRefId(sourceBranch).build()

    Utils.page(100, Utils.pageDefault)(pageRequest => pullRequestService.search(searchRequest, pageRequest))
  }
}
