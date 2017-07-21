package com.fr.cra.analysis

/**
  * Created by rinoux on 16/8/5.
  */
case object WARNING extends AnyRef with Severity with Serializable with Product{
  override def icon: String = "warning.png"

  override def alternativeText: String = "Warning"
}

