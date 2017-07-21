package com.fr.cra.analysis

/**
  * 检查出错(包括分析器名称和错误信息两个属性)<br>
  * Created by rinoux on 16/8/12.
  */
case class ReviewError(analyzerName : String,
                       errorMsg : String)
  extends AnyRef with Product with Serializable {

}
