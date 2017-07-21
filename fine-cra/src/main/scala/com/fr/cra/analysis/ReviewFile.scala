package com.fr.cra.analysis

import com.atlassian.bitbucket.content.Path
import com.fr.cra.pullrequest.diff.FileChanges

/**
  * 被检查的文件(包括文件路径和文件变动两个属性)<br>
  * Created by rinoux on 16/8/12.
  */
case class ReviewFile(filePath : Path,
                      singleFileChanges : FileChanges)
  extends AnyRef with Product with Serializable {
  /**
    * filepath的路径应该是'/'开头,属于相对路径
    */

}

