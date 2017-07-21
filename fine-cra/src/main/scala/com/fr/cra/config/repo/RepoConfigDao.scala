package com.fr.cra.config.repo

import com.atlassian.activeobjects.tx.Transactional
import com.atlassian.bitbucket.repository.Repository
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService
import org.springframework.stereotype.Component
/**
  * Created by rinoux on 16/8/9.
  */
@Transactional
//@Component
trait RepoConfigDao extends AnyRef{

  def all() : List[AoRepoConfig]
  def getOrCreate(repo : Repository) : AoRepoConfig
  def find(repo : Repository) : Option[AoRepoConfig]
  def save(repoConfig : AoRepoConfig) : Unit
  def isCraActive(repo : Repository) : Boolean
  def isStatisticsActive(repo : Repository) : Boolean
}
