package com.fr.cra.config.repo

/**
  * Created by rinoux on 16/8/15.
  */

case class SoyContext(run : AoRepoConfig => Iterable[(String, AnyRef)]) extends AnyRef with Product with Serializable {
  /**
    * 把两个soycontext的运行内容合并
    * @param p
    * @return
    */
  def |+|(p : => SoyContext) : SoyContext = {
    new SoyContext(r => {
      this.run.apply(r).++(p.run.apply(r))
    })
  }
}
object SoyContext extends AnyRef with Serializable {
  val zero = new SoyContext((ar: AoRepoConfig) => {
    Option.option2Iterable(None)
  })
  type Params = (String) => Option[String]
  type FieldErrors = Map[String, List[String]]
}
