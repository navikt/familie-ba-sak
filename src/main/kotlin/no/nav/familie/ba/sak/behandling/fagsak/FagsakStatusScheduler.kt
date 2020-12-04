package no.nav.familie.ba.sak.behandling.fagsak

import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FagsakStatusScheduler(val fagsakService: FagsakService) {

    /*
     * Siden barnetrygd er en månedsytelse vil en fagsak alltid løpe ut en måned
     * Det er derfor nok å finne alle fagsaker som ikke lenger har noen løpende utbetalinger den 1 hver måned.
     */

    @Scheduled(cron = "0 0 7 1 * *")
    fun oppdaterFagsakStatuser() {
        when (LeaderClient.isLeader()) {
            true -> {
                val antallOppdaterte = fagsakService.oppdaterLøpendeStatusPåFagsaker()
                LOG.info("Oppdatert status på $antallOppdaterte fagsaker til ${FagsakStatus.AVSLUTTET.name}")
            }
            false -> {
                LOG.info("Oppdatering av fagsakstatuser ikke kjørt på denne poden.")
            }
        }
    }

    companion object {

        val LOG = LoggerFactory.getLogger(FagsakStatusScheduler::class.java)
    }
}