package com.fr.cra.analysis

/**
  * Created by rinoux on 16/8/4.
  */
trait Severity{
  def icon : String
  def alternativeText : String
}
object Severity {
  def byName(name : String) : Severity = {
    name match {
      case "FATAL" =>
         FATAL
      case "ERROR" =>
         ERROR
      case "INFO" =>
         INFO
      case "WARNING" =>
         WARNING
      case _ => throw new IllegalArgumentException("Unknown severity " + name)
    }
  }
}

