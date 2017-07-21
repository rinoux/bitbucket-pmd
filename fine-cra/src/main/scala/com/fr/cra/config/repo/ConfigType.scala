package com.fr.cra.config.repo

/**
  * Created by rinoux on 16/8/15.
  */
object ConfigType extends Enumeration{
  //type Type = ConfigType.type
  val BUILTIN, FROM_REPO, FROM_URL, FROM_FR_DB= Value

  //config的来源：built in，from url， from git repo
  def from(s : String) : ConfigType.Value= {
    this.values.find(v => v.toString.equals(s)).getOrElse(ConfigType.FROM_REPO)
  }

}
