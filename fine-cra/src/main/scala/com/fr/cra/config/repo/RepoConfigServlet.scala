package com.fr.cra.config.repo

import java.sql.SQLException
import javax.servlet
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.atlassian.bitbucket.AuthorisationException
import com.atlassian.bitbucket.i18n.I18nService
import com.atlassian.bitbucket.permission.{Permission, PermissionValidationService}
import com.atlassian.bitbucket.repository.{Repository, RepositoryService}
import com.atlassian.bitbucket.scm.git.command.GitCommandBuilderFactory
import com.atlassian.bitbucket.user.UserAdminService
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.atlassian.soy.renderer.SoyTemplateRenderer
import com.atlassian.webresource.api.assembler.PageBuilderService
import com.fr.cra.Logging
import com.fr.cra.config.serviceuser.{ServiceUserConfigDao, ServiceUserCreator}
import com.fr.cra.reposync.{CachedRepoCloner, CachedRepoInformation}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import scala.collection.JavaConverters

/**
  * 用于repository cra设置的页面servlet
  * Created by rinoux on 16/8/24.
  */
@Component
class RepoConfigServlet @Autowired()(permissionValidationService : PermissionValidationService,
                        serviceUserCreator : ServiceUserCreator,
                        repositoryService : RepositoryService,
                        pageBuilderService : PageBuilderService, 
                        repoConfigDao : RepoConfigDao, 
                        @ComponentImport gitCommandFactory : GitCommandBuilderFactory,
                        serviceUserConfigDao : ServiceUserConfigDao,
                        userAdminService : UserAdminService,
                        @ComponentImport soyTemplateRenderer : SoyTemplateRenderer,
                        repoConfigValidator : RepoConfigValidator,
                        i18nService : I18nService, 
                        cachedRepoInfo : CachedRepoInformation, 
                        repoConfigData : RepoConfigSoyData) extends HttpServlet with Logging {

  override def doGet(req : HttpServletRequest, res : HttpServletResponse) : Unit = {

    getRepo(req) match {
      case Some(repo) =>
        try {
          permissionValidationService.validateForRepository(repo, Permission.REPO_ADMIN)
        } catch {
          case e : AuthorisationException =>
            notAuthorized(req, res, repo)
            return
        }
        try {

          val t = repoConfigValidator.validate(repoConfigDao.getOrCreate(repo), None)
          if (t != null) {
            val repoConfig = t._1
            val validationErrors = t._2
            var results : Map[String, AnyRef] = null
            //将config转为soy资源
            if (validationErrors.nonEmpty) {
              results = repoConfigData.soyResults(repoConfig, repo, Some(Right(validationErrors)))
            } else {
              results = repoConfigData.soyResults(repoConfig, repo, None)
            }
            //打开设置时的渲染repo config的界面， repo-config.js
            render(res, RepoConfigServlet.PluginModuleKey, RepoConfigServlet.SoyTemplateName, results)
          }
        } catch {
          case e : SQLException =>
            throw new servlet.ServletException(e)
        }
      case _ =>
        res.sendError(404)
    }
  }
  override def doPost(request : HttpServletRequest, response : HttpServletResponse) : Unit = {
    getRepository(request) match {
      case Some(repo) =>
        try {
          permissionValidationService.validateForRepository(repo, Permission.REPO_ADMIN)
        } catch {
          case e : AuthorisationException =>
            log.error("FINE-CRA: Not allowed to change repository settings", e)
            notAuthorized(request, response, repo)
            return
        }
        try {
          val (repoConfig, validationErrors) = repoConfigValidator.validate(repoConfigDao.getOrCreate(repo), Some(request))
          //没有错误时
          if (validationErrors.isEmpty) {
            cachedCloneIfNecessary(repoConfig, repo)
            //创建必要的服务端用户
            serviceUserCreator.createNecessaryServiceUsers(repo, repoConfig)
            repoConfigDao.save(repoConfig)
            val successMsg = Some(Left(i18nService.getMessage("cra.repo.settings.save.success.msg")))
            val soyResult = repoConfigData.soyResults(repoConfig, repo, successMsg)

            //提交设置的渲染
            render(response, RepoConfigServlet.PluginModuleKey, RepoConfigServlet.SoyTemplateName, soyResult)
          } else {
            //有错误时
            val soyResult2 = repoConfigData.soyResults(repoConfig, repo, Some(Right(validationErrors)))
            render(response, RepoConfigServlet.PluginModuleKey, RepoConfigServlet.SoyTemplateName, soyResult2)
          }
        } catch {
          case e : Exception =>
            log.error("FINE-CRA: Error while saving repository settings", e)
            response.sendError(500)
        }
      case _ =>
        response.sendError(404)
    }
  }

  /**
    * 具体的页面渲染工作
    * @param response
    * @param resourceKey
    * @param templateName
    * @param params
    */
  def render(response: HttpServletResponse, resourceKey : String, templateName : String, params : Map[String, AnyRef]): Unit = {
    response.setContentType("text/html;charset=UTF-8")
    pageBuilderService.assembler().resources().requireContext("plugin.page.cra")
    soyTemplateRenderer.render(response.getWriter, resourceKey, templateName, JavaConverters.mapAsJavaMapConverter(params).asJava)
  }

  def notAuthorized(request: HttpServletRequest, response: HttpServletResponse, repository : Repository): Unit ={
    log.error("FINE-CRA: User " + request.getRemoteUser + " tried to access the admin page for " + repository.getSlug)
    response.sendError(401, i18nService.getMessage("cra.repo.settings.not.authorized.msg"))
  }

  def cachedCloneIfNecessary(aoRepoConfig: AoRepoConfig, repository: Repository): Unit ={
    if (isOneStaticAnalyzerActive(aoRepoConfig)) {
      val cloner = new CachedRepoCloner(gitCommandFactory, cachedRepoInfo)
      cloner.cloneIfNotExisting(repository)
    }
  }

  def isOneStaticAnalyzerActive(repoConfig: AoRepoConfig) : Boolean ={
    repoConfig.getStaticAnalyzerSettings.exists(_.isEnabled)
  }

  /**
    * 根据请求获取repository
    * @param request
    * @return
    */
  def getRepository(request: HttpServletRequest) : Option[Repository] = {
    val pathInfo = request.getPathInfo
    //格式：/PROJECT_1/rep_1， 就是repo-admin后面的部分，
    if (Option(pathInfo).isDefined) {
      val pathParts= pathInfo.split("/")
      if (pathParts.length == 3) {
        //pathParts(1)是project key，pathParts(2)是repo slug
        Option(repositoryService.getBySlug(pathParts(1), pathParts(2)))
      } else None
    } else None
  }
  def getRepo(request: HttpServletRequest) : Option[Repository] = {
    var pathInfo = request.getPathInfo
    if (Option(pathInfo).isEmpty) {
      None
    } else {
      if (pathInfo.startsWith("/")) {
        pathInfo = pathInfo.substring(0)
        val pathParts = pathInfo.split("/")
        if (pathParts.length != 3) {
          None
        } else {
          Option(repositoryService.getBySlug(pathParts(1), pathParts(2)))
        }
      } else None
    }
  }
}
object RepoConfigServlet extends AnyRef with Serializable {
  val SoyTemplateName = "plugin.page.cra.repositoryConfigurationPanel"
  //用户端设置界面资源，atlassian-plugin.xml中
  val PluginModuleKey = "com.fr.cra:finecra-configuration-resources"
}
