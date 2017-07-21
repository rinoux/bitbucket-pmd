package com.fr.cra.analysis

import java.util.concurrent.ExecutorService

import com.fr.cra.analysis.checkstyle.CheckstyleProcessor
import com.fr.cra.analysis.pmd.PMDProcessor
import com.fr.cra.config.repo.AoRepoConfig
import com.fr.cra.reposync.CachedRepoInformation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
  *静态分析器记录,原代码包括checkstyle,pmd,jhint什么的,这里只有checkstyle，和PMD
  * Created by rinoux on 16/8/8.
  */
@Component
class StaticAnalyzerRegistry @Autowired()(cachedRepoInfo : CachedRepoInformation,
                              executorService : ExecutorService) extends AnyRef {
  // checkstyle&PMD ，所有分析器必须在此注册
  val builtInAnalyzers = List(new CheckstyleProcessor(cachedRepoInfo), new PMDProcessor(cachedRepoInfo))

  def allBuiltInAnalyzers : List[StaticAnalysisProcessor] = {
    builtInAnalyzers
  }

  /**
    * 获取内置分析器
    * @param name
    * @return
    */
  def getBuiltInAnalyzer(name : String) : Option[StaticAnalysisProcessor] = {
    //根据静态分析处理器找到内置分析器
    builtInAnalyzers.find(processor => processor.getName.equals(name))
  }
  def onlyEnabledAnalyzers(aoRepoConfig : AoRepoConfig) : Seq[StaticAnalysisProcessor] = {
    //无自定义,直接使用内置
    onlyEnabledBuiltinAnalyzers(aoRepoConfig)
  }
  def onlyEnabledBuiltinAnalyzers(aoRepoConfig: AoRepoConfig) : Seq[StaticAnalysisProcessor] = {
    //满足aoRepoConfig存在某个processor的配置，且该processor启用
    builtInAnalyzers.filter(processor => {
      val settings = aoRepoConfig.getStaticAnalyzerSettings
      settings.exists(setting => {
        setting.getName.equals(processor.getName) && setting.isEnabled
      })
    })
  }
}