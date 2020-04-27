package no.nav.familie.ba.sak.oppgave.domene

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FraBehandlingTilOppgaveScheduler(private val behandlingRepository: BehandlingRepository,
                                       private val oppgaveRepository: OppgaveRepository) {

    @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE)
    fun migrerBehandleSakFraBehandlingTilOppgave() {
        LOGGER.info("Migrerer oppgaver fra Behandling til Oppgave")
        val behandlinger = behandlingRepository.findByOppgaveNotNull()
        val oppgaver = behandlinger.map { behandling ->
            if (behandling.status == BehandlingStatus.OPPRETTET) {
                Oppgave(behandling = behandling,
                        gsakId = behandling.oppgaveId!!,
                        type = Oppgavetype.BehandleSak,
                        erFerdigstilt = false,
                        opprettetTidspunkt = behandling.endretTidspunkt
                )
            } else {
                Oppgave(behandling = behandling,
                        gsakId = behandling.oppgaveId!!,
                        type = Oppgavetype.BehandleSak,
                        erFerdigstilt = true,
                        opprettetTidspunkt = behandling.endretTidspunkt
                )
            }
        }.filter { oppgaveRepository.findByOppgavetypeAndBehandling(it.behandling, it.type) == null }

        oppgaveRepository.saveAll(oppgaver)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(FraBehandlingTilOppgaveScheduler::class.java)
    }
}