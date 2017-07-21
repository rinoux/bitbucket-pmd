package com.fr

/**
  * 全局变量,全局方法
  * Created by rinoux on 16/8/9.
  */
package object cra extends AnyRef{

  /**
    * 对stream使用完毕后自动关闭<br>
    *   using接受的类型必须包含一个close方法<br>
    * @param resource 资源对象
    * @param block 调用资源对象
    * @tparam T 包含close方法的类
    */
  def using[T <: AnyRef {
    def close() : Unit
  }](resource : T)(block : T => Unit) : Unit = {
    try {
      block(resource)
    } finally {
      if (resource != null) resource.close()
    }
  }

  val MAX_TEXT_LENGTH : Int = 255
  val GIT_CMD_TIMEOUT_IN_SEC : Int = 900
  val CRA_EXECUTE_TIMEOUT_IN_SEC : Int = 120
  val CODE_REVIEW_ASSISTANT_NAME : String = "FINE CRA"
}