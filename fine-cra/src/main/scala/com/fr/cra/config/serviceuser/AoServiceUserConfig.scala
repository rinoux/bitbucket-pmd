package com.fr.cra.config.serviceuser

import net.java.ao.schema.{Table, Unique}
import net.java.ao.{Accessor, Entity, Mutator, Preload}

/**
  * Created by rinoux on 16/8/15.
  */
@Preload
@Table(value = "ServiceUser001")
trait AoServiceUserConfig extends Object with Entity {
  @Accessor(value = "SERVICE_USER_ID")
  @Unique
  def getServiceUserId : Integer
  @Mutator(value = "SERVICE_USER_ID")
  def setServiceUserId(id : Integer) : Unit
  @Accessor(value = "TOOL_NAME")
  def getToolName : String
  @Mutator(value = "TOOL_NAME")
  def setToolName(toolName : String) : Unit
}
