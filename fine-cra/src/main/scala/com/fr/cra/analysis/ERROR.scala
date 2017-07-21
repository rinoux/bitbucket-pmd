package com.fr.cra.analysis

/**
  * Created by rinoux on 16/8/5.
  */
case object ERROR extends AnyRef with Severity with Serializable with Product {
  override def icon: String = "error.png"
  override def alternativeText: String = "Error"
}

