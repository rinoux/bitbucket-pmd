package com.fr.cra.analysis.pmd.wrapper

import java.sql.DriverManager

import com.fr.cra.Logging
import net.sourceforge.pmd._
import net.sourceforge.pmd.lang.Language
import net.sourceforge.pmd.lang.rule.XPathRule

import scala.xml.XML

/**
  * Created by rinoux on 2016/10/31.
  */
class FrRuleSetsWrapper(ruleMatch: String, url: String) extends AnyRef with Logging {

  def isMysqlUrlLegal(urlStr : String): Boolean = {
    // TODO: judge whether url legal
    true
  }
  def loadPmdRuleSetsFromXML(xml: String) : RuleSets = {
    val ruleSetFile = XML.loadFile(xml)
    val xmlRules = ruleSetFile.\\("rule").filter(_.\("properties").\("property").\@("name") == "xpath")

    val ruleSet = new RuleSet
    ruleSet.setFileName("From-xml")
    ruleSet.setName(ruleSetFile.\@("name"))
    ruleSet.setDescription(ruleSetFile.\("description").head match {
      case <description>{desc}</description> =>
        desc.text.toString
    })
    xmlRules.foreach(n => {
      val xPathRule = new XPathRule()
      xPathRule.setName(n.\@("name"))
      xPathRule.setLanguage(Language.findByTerseName(n.\@("language")))
      xPathRule.setDescription(n.\("description").head match {
        case <description>{desc}</description> =>
          desc.text.toString
      })

      xPathRule.setMessage(n.\@("message"))
      xPathRule.setRuleClass(n.\@("class"))
      xPathRule.setSince(n.\@("since"))
      xPathRule.setPriority(RulePriority.valueOf(n.\("priority").head match {
        case <priority>{prio}</priority> => new Integer(prio.text)
      }))
      xPathRule.setXPath(n.\\("value").head match {
        case <value>{va}</value> => va.text
      })
      xPathRule.addExample(n.\("example").head match {
        case <example>{exmp}</example> => exmp.toString()
      })
      if (n.\@("externalInfoUrl").nonEmpty) {
        xPathRule.setExternalInfoUrl(n.\@("externalInfoUrl"))
      }
      xPathRule.setUsesTypeResolution()

      ruleSet.addRule(xPathRule)
    })

    new RuleSets(ruleSet)
  }

  /**
    * 初始化ruleSet
    * @return
    */
  def initRuleSets : RuleSet = {
    val ruleSet = new RuleSet
    ruleSet.setFileName("MysqlRule")
    ruleSet.setName("FineReport")
    ruleSet.setDescription("this rulesets is construct from url or path")
    ruleSet
  }

  /**
    * 构造查询语句
    * @return
    */
  def sqlWrapper: String = {
    val table = "fr_develop_pmd_rule"
    val sql = StringBuilder.newBuilder
      .append("SELECT * FROM ")
      .append(table)
      .append(" WHERE state=1 ")
      .append("AND FIND_IN_SET('all',target) OR ")
      .append("FIND_IN_SET('")
      .append(ruleMatch)
      .append("', target)")
      .append(" AND state=1")
    sql.toString()
  }

  def loadPmdRuleSetsFromMysql : RuleSets = {
    val ruleSet = this.initRuleSets

    if (isMysqlUrlLegal(url)) {
      Class.forName("com.mysql.jdbc.Driver")
      val conn = DriverManager.getConnection(url)
      val statement = conn.createStatement()
      //state means if enable, 1 represent enable, 0 for contrast
      val sql = this.sqlWrapper
      try {
        val rs = statement.executeQuery(sql)
        while (rs.next()) {
          Option(rs.getString("xpath")) match {
            case Some(s) =>
              //如果是xpath直接使用XPathRule
              val rule : XPathRule = new XPathRule()

              rule.setName(rs.getString("name"))
              rule.setLanguage(Language.findByTerseName(rs.getString("language")))
              rule.setSince(rs.getString("since"))
              rule.setMessage(rs.getString("message"))
              rule.setRuleClass(rs.getString("class"))
              rule.setDescription(rs.getString("description"))
              rule.setPriority(RulePriority.valueOf(rs.getInt("priority")))
              rule.addExample(rs.getString("example"))
              rule.setXPath(rs.getString("xpath").replace("\\n", "").replace("\n", ""))

              rule.setRuleSetName(ruleSet.getName)

              //RuleSetsFactory是默认设置为true
              rule.setUsesTypeResolution()

              ruleSet.addRule(rule)
            case None =>
              //如果是普通的rule（非xpath），反射获得rule对象然后设置
              Option(rs.getString("class")) match {
                case Some(classStr) =>
                  val rule = Class.forName(rs.getString("class")).newInstance().asInstanceOf[Rule]
                  rule.setName(rs.getString("name"))
                  rule.setLanguage(Language.findByTerseName(rs.getString("language")))
                  rule.setSince(rs.getString("since"))
                  rule.setMessage(rs.getString("message"))
                  rule.setDescription(rs.getString("description"))
                  rule.setPriority(RulePriority.valueOf(rs.getInt("priority")))
                  rule.addExample(rs.getString("example"))
                  ruleSet.addRule(rule)
                case None =>
                  throw new NullPointerException("'class' column must not be null!")
              }
          }
        }
      } catch {
        case e : Exception =>
          log.error("FINE-CRA: Error while visiting mysql or illegal rule item", e)
      } finally {
        statement.close()
        conn.close()
      }
    }
    new RuleSets(ruleSet)
  }
}


