package com.fr.cra.statistics

import com.atlassian.activeobjects.external.ActiveObjects
import com.atlassian.bitbucket.pull.PullRequest
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.fr.cra.analysis.{AnalyzerResult, Review, StaticAnalysisProcessor, StaticAnalyzerRegistry}
import net.java.ao.{DBParam, Query}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
  * Created by rinoux on 16/8/16.
  */
@Component
class CodeReviewStatisticsDaoImpl @Autowired()(@ComponentImport ao : ActiveObjects,
                                               staticAnalyzerRegistry : StaticAnalyzerRegistry) extends AnyRef with CodeReviewStatisticsDao {

  override def all() : List[AoCodeReviewStatistics] = {
    ao.find[AoCodeReviewStatistics, Integer](classOf[AoCodeReviewStatistics]).toList
  }
  override def find(pullRequest : PullRequest) : Option[AoCodeReviewStatistics] = {
    val q = Query.select().where("REPO_ID = ? AND PULL_REQUEST_ID = ?", pullRequest.getToRef.getRepository.getId.asInstanceOf[java.lang.Integer], pullRequest.getId.asInstanceOf[java.lang.Long])
    ao.find[AoCodeReviewStatistics, Integer](classOf[AoCodeReviewStatistics], q).headOption
  }
  override def setStatus(pullRequest : PullRequest, uptoDate : Boolean) : Unit = {
    val csr = find(pullRequest).getOrElse(ao.create[AoCodeReviewStatistics, Integer](classOf[AoCodeReviewStatistics], new DBParam("REPO_ID", pullRequest.getToRef.getRepository.getId), new DBParam("PULL_REQUEST_ID", pullRequest.getId)))
    csr.setUpToDate(uptoDate)
    csr.save()
  }
  override def save(review : Review) : Unit = {
    val pullRequest = review.pullRequest
    //获得AoCodeReviewStatistics,没有的话根据repo的信息创建新的
    val csr = find(pullRequest).getOrElse(ao.create[AoCodeReviewStatistics, Integer](classOf[AoCodeReviewStatistics], new DBParam("REPO_ID", pullRequest.getToRef.getRepository.getId), new DBParam("PULL_REQUEST_ID", pullRequest.getId)))
    staticAnalyzerRegistry.onlyEnabledAnalyzers(review.repoConfig).foreach(processor => {
      //检查后结果数据处理
      review.getReviewResult(processor.getName) match {
        case Some(rr) =>
          val sar = csr.getStaticAnalyzerResults.find(staticAnalyzerResult => {
            staticAnalyzerResult.getName.equals(rr.staticAnalyzerName)
          }).getOrElse(createNewReviewResults(processor))
          sar.setCodeReviewStatistics(csr)
          setResults(sar, rr)
          sar.save()
        case None =>
          csr.getStaticAnalyzerResults.find(staticAnalyzerResult => {
            // 删除未启用但是有结果的分析器的分析结果
            staticAnalyzerResult.getName.equals(processor.getName)
          }).foreach(ao.delete(_))
      }
    })
  }
  override def deleteForRepo(repoId : Int) : Unit = {
    val q = Query.select.where("REPO_ID = ?", repoId.asInstanceOf[java.lang.Integer])
    val statistics = ao.find[AoCodeReviewStatistics, Integer](classOf[AoCodeReviewStatistics], q)
    statistics.foreach(crs => {
      crs.getStaticAnalyzerResults.foreach(sar => {
        ao.delete(sar)
      })
      ao.delete(crs)
    })
  }
  def setResults(entity: AoStaticAnalyzerResults, reviewResult : AnalyzerResult): Unit ={
    val summary = reviewResult.getSummary
    entity.setErrorCount(summary.numErrors)
    entity.setFatalCount(summary.numFatals)
    entity.setInfoCount(summary.numInfos)
    entity.setWarningCount(summary.numWarnings)
  }

  def createNewReviewResults(sap: StaticAnalysisProcessor) : AoStaticAnalyzerResults = {
    ao.create[AoStaticAnalyzerResults, Integer](classOf[AoStaticAnalyzerResults], new DBParam("NAME", sap.getName))
  }
}
