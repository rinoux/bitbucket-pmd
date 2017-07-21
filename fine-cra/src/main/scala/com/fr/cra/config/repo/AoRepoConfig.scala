package com.fr.cra.config.repo

import net.java.ao.schema.{Default, NotNull, Table, Unique}
import net.java.ao.{Accessor, Entity, Mutator, OneToMany}

/**
  * repo的配置信息模型<br>
  *   主要包括repoId, SeverityIcon, branchOption,(ctags, bugprediction)的getter和setter,
  *   以及获取所有数据分析器的设置<br>
  * Created by rinoux on 16/8/10.
  */
@Table(value = "RepoConfig")
trait AoRepoConfig extends Object with Entity {
  @Accessor(value = "REPO_ID")
  @Unique
  @NotNull
  def getRepoId : Integer
  def setRepoId(id : Integer) : Unit
  @Accessor(value = "SEVERITY_ICON_SHOWN")
  @Default(value = "false")
  @NotNull
  def isSeverityIconShown : Boolean
  @Mutator(value = "SEVERITY_ICON_SHOWN")
  def setSeverityIconShown(showIcon : Boolean) : Unit
  @Accessor(value = "BRANCH_OPTIONS")
  def getBranchOptions : String
  @Mutator(value = "BRANCH_OPTIONS")
  def setBranchOptions(branchOptions : String) : Unit
  @Accessor(value = "BRANCH_OPTIONS_BRANCHES")
  def getBranchOptionsBranches : String
  @Mutator(value = "BRANCH_OPTIONS_BRANCHES")
  def setBranchOptionsBranches(branches : String) : Unit
  @OneToMany(reverse = "getRepoConfig")
  def getStaticAnalyzerSettings : Array[AoStaticAnalyzerSettings]
}
