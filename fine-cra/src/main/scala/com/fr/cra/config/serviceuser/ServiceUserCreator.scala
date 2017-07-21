package com.fr.cra.config.serviceuser

import java.io.InputStream

import com.atlassian.bitbucket.permission.{Permission, PermissionAdminService, SetPermissionRequest}
import com.atlassian.bitbucket.repository.Repository
import com.atlassian.bitbucket.user._
import com.atlassian.bitbucket.util.{Operation, UncheckedOperation}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import com.atlassian.bitbucket.user.ServiceUserCreateRequest.Builder
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService
import com.fr.cra.Logging
import com.fr.cra.analysis.StaticAnalyzerRegistry
import com.fr.cra.config.repo.AoRepoConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.fr.cra.CODE_REVIEW_ASSISTANT_NAME

/**
  * 创建服务端用户
  * Created by rinoux on 16/8/15.
  */
@Component
class ServiceUserCreator @Autowired()(serviceUserConfigDao : ServiceUserConfigDao,
                                      userAdminService : UserAdminService,
                                      securityService : SecurityService,
                                      staticAnalyzerRegistry : StaticAnalyzerRegistry,
                                      permissionAdminService : PermissionAdminService,
                                      userService : UserService) extends AnyRef with Logging {
  /**
    * 创建必要的服务端用户
    * @param repo
    * @param repoConfig
    */
  def createNecessaryServiceUsers(repo : Repository, repoConfig : AoRepoConfig) : Unit = {

    staticAnalyzerRegistry.onlyEnabledAnalyzers(repoConfig).foreach(processor => {
      val serviceUser = createServiceUser(repo, processor.getName)
      updateAvatar(serviceUser, processor.getLogoPath)
    })
    val craUser : ServiceUser = createServiceUser(repo, CODE_REVIEW_ASSISTANT_NAME)
    updateAvatar(craUser, "/img/cra-plugin-logo.png")
  }

  /**
    * 创建服务端用户(具体创建)
    * @param repository
    * @param serviceName
    * @return
    */
  def createServiceUser(repository: Repository, serviceName: String) : ServiceUser = {
    serviceUserConfigDao.find(serviceName) match {
      case Some(userConfig) =>
        //把ApplicationUser变成ServerUser
        val user : ServiceUser = userService.getUserById(userConfig.getServiceUserId).asInstanceOf[ServiceUser]
        setRepositoryReadPermission(repository, user)
        user
      case None =>
        val request : ServiceUserCreateRequest = new Builder().name(serviceName).build()
        val newUser : ServiceUser = userAdminService.createServiceUser(request)
        serviceUserConfigDao.save(newUser, serviceName)
        setRepositoryReadPermission(repository, newUser)
        newUser
    }
  }

  /**
    * 设置repo的读权限
    * @param repository
    * @param serviceUser
    */
  def setRepositoryReadPermission(repository: Repository, serviceUser: ServiceUser): Unit = {
    val spr: SetPermissionRequest = new SetPermissionRequest.Builder().repositoryPermission(Permission.REPO_READ, repository).user(serviceUser).build()
    permissionAdminService.setPermission(spr)
  }

  /**
    * 更改头像
    * @param serviceUser 服务用户
    * @param imgPath 图片路径
    */
  def updateAvatar(serviceUser: ApplicationUser, imgPath : String): Unit = {
    securityService.withPermission(Permission.ADMIN, "Updating avatar image needs admin permissions").call(new UncheckedOperation[Unit]() {
      override def perform(): Unit = {
        com.fr.cra.using(getClass.getResourceAsStream(imgPath))(is => {
          val data = new String(Base64.encodeBase64(IOUtils.toByteArray(is)))
          val contentType = "image/png"
          userService.updateAvatar(serviceUser, "data:" + contentType + ";base64," + data)
        })
      }
    })
  }
}
