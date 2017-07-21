package com.fr.cra.config.serviceuser

import com.atlassian.activeobjects.external.ActiveObjects
import com.atlassian.bitbucket.user.ServiceUser
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import net.java.ao.{DBParam, Query}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
  * 服务端用户配置的DAO
  * Created by rinoux on 16/8/15.
  */
@Component
class ServiceUserConfigDaoImpl @Autowired()(@ComponentImport ao : ActiveObjects) extends AnyRef with ServiceUserConfigDao {
  /**
    * 获取所有服务端用户配置
    * @return
    */
  override def all() : List[AoServiceUserConfig] = {
    ao.find[AoServiceUserConfig, Integer](classOf[AoServiceUserConfig]).toList
  }

  /**
    * 获取指定名称的服务端用户配置
    * @param toolName
    * @return
    */
  override def find(toolName : String) : Option[AoServiceUserConfig] = {
    val where: Query = Query.select().where("TOOL_NAME = ?", toolName)
    ao.find[AoServiceUserConfig, Integer](classOf[AoServiceUserConfig], where).headOption
  }

  /**
    * 保存服务端用户
    * @param serviceUser
    * @param toolName
    */
  override def save(serviceUser : ServiceUser, toolName : String) : Unit = {
    ao.create[AoServiceUserConfig, Integer](classOf[AoServiceUserConfig], new DBParam("SERVICE_USER_ID", serviceUser.getId), new DBParam("TOOL_NAME", toolName))
  }
}
