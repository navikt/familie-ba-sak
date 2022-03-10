package no.nav.familie.ba.sak.integrasjoner.infotrygd

import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class InfotrygdFeedService(
    val opprettTaskService: OpprettTaskService
) {

    @Transactional
    fun sendTilInfotrygdFeed(barnsIdenter: List<String>) {
        opprettTaskService.opprettSendFeedTilInfotrygdTask(barnsIdenter)
    }

    @Transactional
    fun sendStartBehandlingTilInfotrygdFeed(aktørStoenadsmottaker: Aktør) {
        opprettTaskService.opprettSendStartBehandlingTilInfotrygdTask(aktørStoenadsmottaker)
    }
}
