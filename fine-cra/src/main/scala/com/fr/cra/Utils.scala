package com.fr.cra

import java.io.File
import java.net.{HttpURLConnection, MalformedURLException, URL}
import java.util
import java.util.Optional

import com.atlassian.bitbucket.util._
import org.apache.commons.lang3.StringUtils

import scala.Option
import scala.collection.JavaConverters


/**
  * 一些工具
  * Created by rinoux on 16/8/9.
  */
object Utils extends AnyRef {

  /**
    * 返回两个字符串之间的Levenshtein距离(字符转换的最少次数)
    * @param text1 字符
    * @param text2 字符
    * @return
    */
  def getLevenshteinDistance(text1 : String, text2 : String) : Int = {
    StringUtils.getLevenshteinDistance(text1, text2)
  }

  /**
    * 缩短路径名
    * @param path 原路径
    * @param maximumLength 最大长度限制
    * @return
    */
  def shortenPath(path : String, maximumLength : Int) : String = {
    if(path.length <= maximumLength)
      {
        return path
      }
    val lastPathFragment : String = getlastPathFragment(path, maximumLength)

    if (lastPathFragment.length == maximumLength) {
      lastPathFragment
    } else {
      path.substring(Math.max(0, maximumLength - "...".length - lastPathFragment.length)) + "..." + lastPathFragment
    }

  }

  /**
    * 文件是否可读
    * @param absolutePath 文件的绝对路径
    * @return
    */
  def isFileReadable(absolutePath : String) : Boolean = {
    val f: File = new File(absolutePath)
    f.exists() && f.isFile && f.canRead
  }

  /**
    * 获得相对路径
    * @param baseDir 根路径
    * @param filePath 文件路径
    * @return
    */
  def getRelativePath(baseDir : File, filePath : String) : String = {
    if (new File(filePath).isAbsolute) {
      baseDir.toURI.relativize(new File(filePath).toURI).getPath
    } else filePath
  }

  /**
    * 是否为合法url路径
    * @param path 路径
    * @return
    */
  def isUrl(path : String) : Boolean = {
    try {
      new URL(path)
      true
    } catch {
      case me : MalformedURLException =>
        false
    }
  }

  /**
    * 是否可见
    * @param url 路径
    * @return
    */
  def isReachable(url : String) : Boolean = {
    try {
      val u: URL = new URL(url)
      val httpUrlConnection: HttpURLConnection = u.openConnection().asInstanceOf[HttpURLConnection]
      httpUrlConnection.setRequestMethod("GET")
      httpUrlConnection.connect()
      true
    } catch {
      case e :Exception => false
    }
  }

  /**
    * 一个curring,分页
    * @param limit
    * @param start
    * @param f
    * @tparam A
    * @return
    */
  def page[A](limit : Int, start : => Int)(f : (PageRequest) => Page[A]) : Iterable[A] = {
    val i: Iterable[A] = JavaConverters.iterableAsScalaIterableConverter(new PagedIterable(new PageProvider[A] {
      override def get(pageRequest: PageRequest): Page[A] = {
        f(pageRequest)
      }
    }, PageUtils.newRequest(limit, start))).asScala
    i
  }
  def pageDefault : Int = 0


  /**
    * 获得路径中的最后一个有效部分,去掉最后的反斜杠,前端的盘符(如C:\\)
    * @param path 原路径
    * @param maxLength 最大长度限制
    * @return
    */
  def getlastPathFragment(path: String, maxLength: Int) : String = {
    var result : String = ""
    if (path.endsWith("/")) {
      result = path.substring(0, path.length - 1)
    }
    var index: Int = path.lastIndexOf('/')
    if (index == -1) {
      //若不存在"/", 则找到最后一个"\"
      index = path.lastIndexOf("\\")
    }
    if (index != -1) {
      result = path.substring(index)
    }

    if (result.length > maxLength - "...".length) {
      new StringBuilder().+("...").+(result.substring(Math.max(0, result.length - maxLength + "...".length))).toString
    } else {
      result
    }
  }

  /**
    * java Optional -> scala Option
    *
    * @param optional
    * @tparam T
    */
  implicit final class RichJOption[T](val optional: Optional[T]) {

    def asScala: Option[T] = optional match {
      case null => null
      case _ => if (optional.isPresent) Option(optional.get()) else None
    }

  }

  /**
    * scala Option -> java Optional
    *
    * @param option
    * @tparam T
    */
  implicit final class RichOption[T](val option: Option[T]) {

    def asJava: Optional[T] = option match {
      case null => null
      case _ => if (option.isDefined) Optional.of(option.get) else Optional.empty()
    }

  }
}



