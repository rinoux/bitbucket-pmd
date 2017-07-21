package com.fr.cra.analysis

/**
  * Created by rinoux on 16/8/5.
  */
case object INFO extends AnyRef with Severity with Serializable with Product {
  override def icon: String = "info.png"

  override def alternativeText: String = "Info"
}
