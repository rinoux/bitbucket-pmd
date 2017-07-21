package com.fr.cra.statistics

import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.{CacheControl, Response}
import javax.ws.rs.{GET, Path, Produces, QueryParam}

import com.atlassian.bitbucket.i18n.I18nService
import com.atlassian.bitbucket.pull.PullRequestService
import com.atlassian.bitbucket.repository.RepositoryService
import com.fr.cra.Logging
import com.fr.cra.analysis._
import com.fr.cra.branch.ShouldAnalyzeBranchChecker
import com.fr.cra.config.repo.{AoRepoConfig, RepoConfigDao}
import com.fr.cra.reposync.CachedRepoInformation
import com.google.gson.Gson

import scala.collection.JavaConverters
import scala.collection.immutable.{HashMap, Map}
import scala.runtime.NonLocalReturnControl

/**
  * 获取检查结果数据的服务<br>
  * jersey doget<br>
  * Created by rinoux on 16/8/16.
  */
@Path("reviewstatistics")
@Produces(Array("application/json"))
class CodeReviewStatisticsResource(codeReviewResultsDao : CodeReviewStatisticsDao,
                                   repoConfigDao : RepoConfigDao,
                                   cachedRepoInfo : CachedRepoInformation,
                                   shouldAnalyzeBranchChecker : ShouldAnalyzeBranchChecker,
                                   repoService : RepositoryService,
                                   i18nService : I18nService,
                                   staticAnalyzerRegistry : StaticAnalyzerRegistry,
                                   pullRequestService : PullRequestService)
  extends AnyRef with Logging {
  /**
    * 带参GET请求
    * 获得检查数据结果<br>
    * @param repoId repoid
    * @param pullRequestId pull request id
    * @return 带各个级别危险的个数的json和放回状态码
    */
  @GET
  def getCodeReviewResults(@QueryParam("repoId") repoId : Int, @QueryParam("pullRequestId") pullRequestId : Long) : Response = {
    Option(repoService.getById(repoId)).getOrElse(throw new NonLocalReturnControl(new Object, badRequest("No repository found with ID " + repoId)))
    val pullRequest = pullRequestService.getById(repoId, pullRequestId)
    Option(pullRequest).getOrElse(throw new NonLocalReturnControl(new Object, badRequest("No pull request found by ID " + pullRequestId)))
    if (!repoConfigDao.isStatisticsActive(repoService.getById(repoId)) || !shouldAnalyzeBranchChecker.shouldAnalyze(pullRequest)) {
      Response.status(Response.Status.NO_CONTENT).build
    }
    repoConfigDao.find(repoService.getById(repoId)) match {
        //如果此repo有代码检查的配置
      case Some(rc) =>
        //根据pr取出结果
        codeReviewResultsDao.find(pullRequest) match {
            //找到的code review result应当是包含static analyzer result
          case Some(crr) =>
            var status : Status = null
            val json = toReviewResults(rc, crr) //that's the problem
            if (json.isEmpty) {
              Option(crr.isUpToDate) match {
                case Some(b) =>
                  if (b) status = Status.OK else status = Status.ACCEPTED  //200或202
                case _ => status = Status.CREATED //201
              }
            } else {
              if (isStatisticUpToDate(crr)) {
                status = Status.OK
              } else status = Status.ACCEPTED //202
            }
            val js = new Gson().toJson(json)
            println("检查结果Json：" + js)
            //使用gson时必须是java.util的map，否则出现数据为定义的情况，scala的map和java的map组织方式不一样
            Response.status(status).entity(new Gson().toJson(json)).cacheControl(noCache).build()
          case None =>
            Response.status(Response.Status.CREATED).cacheControl(noCache).build() //201
        }
      case None =>
        badRequest("还未对ID为" + repoId + "的仓库进行代码检查配置")
    }
  }

  /**
    * 数据是否更新
    * @param codeReviewStatistics
    * @return
    */
  def isStatisticUpToDate(codeReviewStatistics: AoCodeReviewStatistics) : Boolean = {
    codeReviewStatistics.isUpToDate
  }

  /**
    * 设置返回不缓存cache
    * @return
    */
  private def noCache : CacheControl = {
    val c = new CacheControl
    c.setNoCache(true)
    c
  }

  /**
    * 将结果变为map供gson使用
    * @param repoConfig 配置
    * @param codeReviewResults 结果
    * @return
    */
  private def toReviewResults(repoConfig: AoRepoConfig, codeReviewResults: AoCodeReviewStatistics) : java.util.Map[String, java.util.Map[String, Integer]] = {
    val codeReviewJson = new scala.collection.mutable.HashMap[String, java.util.Map[String, Integer]]()
    staticAnalyzerRegistry.onlyEnabledBuiltinAnalyzers(repoConfig).foreach(sa => {
      codeReviewResults.getStaticAnalyzerResults.find(sar => {
        sar.getName.equals(sa.getName) && Option(sar.getInfoCount).isDefined
      }) match {
        case Some(res) =>
          codeReviewJson.+=((sa.getName, toMap(res)))
        case None =>
          codeReviewJson.+=((sa.getName, new java.util.HashMap()))
      }
    })
    JavaConverters.mutableMapAsJavaMapConverter(codeReviewJson).asJava
  }

  def badRequest(msg : String): Response = {
    Response.status(Response.Status.BAD_REQUEST).entity(msg).cacheControl(noCache).build() //400
  }

  def toMap(results: AoStaticAnalyzerResults) : java.util.Map[String, Integer] = {
    val m = Map(FATAL.toString -> results.getFatalCount, ERROR.toString -> results.getErrorCount, WARNING.toString -> results.getWarningCount, INFO.toString -> results.getInfoCount)
    JavaConverters.mapAsJavaMapConverter(m).asJava
  }

}
