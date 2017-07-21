package com.fr.cra.branch

/**
  * BranchOptions的三个下拉选项
  * Created by rinoux on 16/8/16.
  */
object BranchOptions extends Enumeration {
  type Type = BranchOptions.Value
  val WHITE_LIST, BLACK_LIST, ALL = BranchOptions.Value
  def from(s : String) : BranchOptions.Type = {
    values.find(v => {
      v.toString.equals(s)
    }) match {
      case Some(value) =>
        value
      case None =>
        BranchOptions.ALL
    }
  }
}
