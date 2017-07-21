package com.fr.cra.config.serviceuser

import com.atlassian.activeobjects.tx.Transactional
import com.atlassian.bitbucket.user.ServiceUser

/**
  * Created by rinoux on 16/8/15.
  */
@Transactional
trait ServiceUserConfigDao extends AnyRef {
  def all() : List[AoServiceUserConfig]
  def find(toolName : String) : Option[AoServiceUserConfig]
  def save(serviceUser : ServiceUser, toolName : String) : Unit
}
