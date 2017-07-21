package com.fr.cra.pullrequest

import javax.xml.bind.annotation.{XmlAccessType, XmlAccessorType, XmlElement, XmlRootElement}

import org.codehaus.jackson.annotate.JsonProperty

import scala.beans.BeanProperty

/**
  * 启动时输入的参数（repoid 和 pr id）
  * Created by rinoux on 16/8/15.
  */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
case class TriggerRunInput(@BeanProperty @XmlElement @JsonProperty("repoId") var repoId : Integer,
                           @BeanProperty @XmlElement @JsonProperty("pullRequestId") var pullRequestId : Long) extends AnyRef with Product with Serializable {

}
