package no.nav.familie.ba.sak.behandling.steg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.logg.LoggService
import org.springframework.stereotype.Service

@Service
class FerdigstillBehandlingSteg(
        private val fagsakService: FagsakService,
        private val behandlingService: BehandlingService,
        private val loggService: LoggService
): BehandlingSteg<String> {
    private val antallBehandlingerFerdigstilt: Map<BehandlingType, Counter> = BehandlingType.values().map {
        it to Metrics.counter("behandling.ferdigstilt", "type",
                              it.name,
                              "beskrivelse",
                              it.visningsnavn)
    }.toMap()

    override fun utførSteg(behandling: Behandling, data: String): Behandling {
        val fagsak = behandling.fagsak

        if (behandling.status !== BehandlingStatus.IVERKSATT) {
            error("Prøver å ferdigstille behandling ${behandling.id}, men status er ${behandling.status}")
        }

        if (behandling.brev == BrevType.INNVILGET && fagsak.status != FagsakStatus.LØPENDE) {
            fagsakService.oppdaterStatus(fagsak, FagsakStatus.LØPENDE)
        } else {
            fagsakService.oppdaterStatus(fagsak, FagsakStatus.STANSET)
        }

        antallBehandlingerFerdigstilt[behandling.type]?.increment()
        loggService.opprettFerdigstillBehandling(behandling)
        return behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.FERDIGSTILT)
    }

    override fun stegType(): StegType {
        return StegType.FERDIGSTILLE_BEHANDLING
    }
}