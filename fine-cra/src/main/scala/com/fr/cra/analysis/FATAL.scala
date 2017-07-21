package com.fr.cra.analysis

/**
  * Created by rinoux on 16/8/5.
  */
case object FATAL extends AnyRef with Severity with Serializable with Product {
  override def icon: String = "fatal.png"

  override def alternativeText: String = "Fatal error"
}

