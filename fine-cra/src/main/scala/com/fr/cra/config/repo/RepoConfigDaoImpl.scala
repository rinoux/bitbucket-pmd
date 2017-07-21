package com.fr.cra.config.repo

import com.atlassian.activeobjects.external.ActiveObjects
import com.atlassian.bitbucket.repository.Repository
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.fr.cra.analysis.StaticAnalyzerRegistry
import net.java.ao.{DBParam, Query}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
/**
  * (AO是Atlassian的ORM(对象关系模型)层,用于插件的存储)<br>
  *
  * Created by rinoux on 16/8/10.
  */
@Component
class RepoConfigDaoImpl @Autowired()(@ComponentImport ao: ActiveObjects,
                                     staticAnalyzerRegistry: StaticAnalyzerRegistry) extends AnyRef with RepoConfigDao{
  /**
    * 获取所有
    * @return
    */
  override def all(): List[AoRepoConfig] = {
    ao.find[AoRepoConfig, Integer](classOf[AoRepoConfig]).toList
  }

  /**
    * 重写getOrCreate Repo Config
    * @param repo
    * @return
    */
  override def getOrCreate(repo: Repository): AoRepoConfig = {
    find(repo) match {
      case Some(existRepoConfig) =>
        existRepoConfig
      case None =>
        val newRepoConfig: AoRepoConfig = ao.create[AoRepoConfig, Integer](classOf[AoRepoConfig], new DBParam("REPO_ID", repo.getId))
        staticAnalyzerRegistry.allBuiltInAnalyzers.foreach(processor => {
          val staticAnalyzerSettings : AoStaticAnalyzerSettings = ao.create[AoStaticAnalyzerSettings, Integer](classOf[AoStaticAnalyzerSettings], new DBParam("NAME", processor.getName))
          staticAnalyzerSettings.setRepoConfig(newRepoConfig)
          staticAnalyzerSettings.save()
        })
        newRepoConfig
    }
  }

  /**
    * 根据repo查找相应的config
    * @param repo
    * @return
    */
  override def find(repo: Repository): Option[AoRepoConfig] = {
    val where : Query = Query.select().where("REPO_ID = ?", repo.getId.asInstanceOf[Integer])
    ao.find[AoRepoConfig, Integer](classOf[AoRepoConfig], where).headOption
  }

  /**
    * 保存
    * @param repoConfig
    */
  override def save(repoConfig: AoRepoConfig): Unit = {
    //保存通用配置和每单个分析器的配置
    repoConfig.getStaticAnalyzerSettings.foreach(sas => sas.save())
    repoConfig.save()
  }

  /**
    * 是否是CRA创建的repo config
    * @param repo
    * @return
    */
  override def isCraActive(repo: Repository): Boolean = {
    isStatisticsActive(repo)
  }

  /**
    * 判断是否建立cra配置
    * @param repo
    * @return
    */
  override def isStatisticsActive(repo: Repository): Boolean = {
    find(repo) match {
      case Some(config) =>
        config.getStaticAnalyzerSettings.exists(_.isEnabled)
      case _ =>
        false
    }
  }
}
