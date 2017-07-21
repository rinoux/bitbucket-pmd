package com.fr.cra.analysis

/**
  * 某个repo的冲突数据分布模型
  * Created by rinoux on 16/8/4.
  */
/**
  *
  * @param numFatals 严重的数量
  * @param numErrors 错误的数量
  * @param numWarnings 警告的数量
  * @param numInfos 信息的数量
  */
case class ViolationCounts(
                            numFatals : Int,
                            numErrors : Int,
                            numWarnings : Int,
                            numInfos : Int)
  extends AnyRef with Product with Serializable {
}
