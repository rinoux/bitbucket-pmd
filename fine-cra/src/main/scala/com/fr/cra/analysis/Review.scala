package com.fr.cra.analysis

import com.atlassian.bitbucket.pull.PullRequest
import java.util.concurrent.ConcurrentHashMap

import com.fr.cra.config.repo.AoRepoConfig

import scala.collection.JavaConverters
import scala.collection.concurrent.Map


/**
  * 代码检查的结果的中间对象,保存和获取检查结果/错误的容器<br>
  * Created by rinoux on 16/8/12.
  */
class Review(val reviewFiles : Seq[ReviewFile], val pullRequest : PullRequest, val repoConfig : AoRepoConfig) extends AnyRef {
  //分析结果集
  var analyzerResults : Map[String, AnalyzerResult] = JavaConverters.mapAsScalaConcurrentMapConverter(new ConcurrentHashMap[String, AnalyzerResult]()).asScala
  //错误集
  var reviewErrors : Map[String, Seq[String]] = JavaConverters.mapAsScalaConcurrentMapConverter(new ConcurrentHashMap[String, Seq[String]]()).asScala

  /**
    * 添加分析结果
    * @param reviewResult
    */
  def addReviewResult(reviewResult : AnalyzerResult) : Unit = {
    analyzerResults.+=(reviewResult.staticAnalyzerName -> reviewResult)
  }

  /**
    * 添加分析错误结果
    * @param reviewError
    */
  def addReviewError(reviewError : ReviewError) : Unit = {
    val currentError : Seq[String] = reviewErrors.getOrElse(reviewError.analyzerName, Vector())
    reviewErrors.+=(reviewError.analyzerName -> currentError.:+(reviewError.errorMsg))
  }

  /**
    * 获取某个分析器(比如checkstyle或者PMD)分析结果中的所有错误
    * @param staticAnalyzerName
    * @return
    */
  def getReviewErrors(staticAnalyzerName : String) : Seq[String] = {
    reviewErrors.getOrElse(staticAnalyzerName, Vector())
  }

  /**
    * 获取所有错误
    * @return
    */
  def getAllReviewErrors :scala.collection.immutable.Map[String, Seq[String]] = {
    reviewErrors.toMap
  }

  /**
    * 获取某个分析器(比如checkstyle或者PMD)的分析结果
    * @param staticAnalyzerName
    * @return
    */
  def getReviewResult(staticAnalyzerName : String) : Option[AnalyzerResult] = {

    analyzerResults.get(staticAnalyzerName)
  }
  //重写用于打印检查结果
  override def toString() : String = {
    val sb: StringBuilder = new StringBuilder
    analyzerResults.map(s => {
      sb.+(s._1).+(":")+(s._2)
    })
    sb.mkString("\n")
  }
}
