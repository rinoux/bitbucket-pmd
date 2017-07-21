package com.fr.cra.config.repo

import net.java.ao.{Accessor, Entity, Mutator}
import net.java.ao.schema.{Default, Indexed, Table}
import net.java.ao.schema.NotNull

/**
  * 静态分析器的设置信息模型<br>
  *   包括分析器的名称,repo config, enable, configForm, configUrl, tool Commander等等<br>
  * Created by rinoux on 16/8/10.
  */
@Table(value = "StatAnlzrSettings")
trait AoStaticAnalyzerSettings extends Object with Entity {
  //所有分析器的通用配置，repoId, SeverityIcon, branchOption
  @Accessor(value = "REPO_CONFIG")
  def getRepoConfig : AoRepoConfig
  @Mutator(value = "REPO_CONFIG")
  def setRepoConfig(repoConfig : AoRepoConfig) : Unit
  //分析器名称
  @Accessor(value = "NAME")
  @Indexed
  @NotNull
  def getName : String
  @Mutator(value = "NAME")
  def setName(name : String) : Unit
  //分析器是否启用
  @Accessor(value = "ENABLED")
  @Default(value = "false")
  @NotNull
  def isEnabled : Boolean
  @Mutator(value = "ENABLED")
  def setEnabled(enabled : Boolean) : Unit
  //配置文件的来源，默认builtin
  @Accessor(value = "CONFIG_FROM")
  @Default(value = "BUILTIN")
  def getConfigFrom : String
  @Mutator(value = "CONFIG_FROM")
  def setConfigFrom(configType : String) : Unit
  //配置文件的repo path
  @Accessor(value = "CONFIG_REPO_PATH")
  def getConfigRepoPath : String
  @Mutator(value = "CONFIG_REPO_PATH")
  def setConfigRepoPath(rulePath : String) : Unit
  //配置文件的url
  @Accessor(value = "CONFIG_URL")
  def getConfigUrl : String
  @Mutator(value = "CONFIG_URL")
  def setConfigUrl(url : String) : Unit
  //最大允许冲突量
  @Accessor(value = "MAX_MERGE_ERRORS")
  def getMaxMergeErrors : Integer
  @Mutator(value = "MAX_MERGE_ERRORS")
  def setMaxMergeErrors(maxNumErrors : Integer) : Unit

  @Accessor(value = "VIOLATION_EXAMPLES")
  @Default(value = "false")
  @NotNull
  def isViolationExamplesEnabled : Boolean
  @Mutator(value = "VIOLATION_EXAMPLES")
  def setViolationExamplesEnabled(enabled : Boolean) : Unit
}

