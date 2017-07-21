package com.fr.cra.statistics

import com.atlassian.bitbucket.event.repository.RepositoryDeletedEvent
import com.atlassian.event.api.EventListener
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService
import com.fr.cra.Logging
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
  * 当repo删除时的回调，此时删除此repo的codeReviewStatistics
  * Created by rinoux on 16/8/16.
  */
@Component
class OnRepositoryDeletedCleaner @Autowired()(codeReviewStatisticsDao : CodeReviewStatisticsDao) extends AnyRef with Logging {
  @EventListener
  def onRepositoryDeleted(event : RepositoryDeletedEvent) : Unit = {
    val repo = event.getRepository
    log.warn("FINE-CRA: Detected repository deletion, will now delete code review statistics for " + repo.getName)
    codeReviewStatisticsDao.deleteForRepo(repo.getId)
  }




}
