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
        private val behandlingResultatService: BehandlingResultatService,
        private val loggService: LoggService
): BehandlingSteg<String> {
    private val antallBehandlingerFerdigstilt: Map<BehandlingType, Counter> = BehandlingType.values().map {
        it to Metrics.counter("behandling.ferdigstilt", "type",
                              it.name,
                              "beskrivelse",
                              it.visningsnavn)
    }.toMap()

    override fun utførStegOgAngiNeste(behandling: Behandling, data: String): StegType {
        val fagsak = behandling.fagsak
        val behandlingResultatType = behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandling.id)

        if (behandling.status !== BehandlingStatus.IVERKSATT) {
            error("Prøver å ferdigstille behandling ${behandling.id}, men status er ${behandling.status}")
        }

        if (behandlingResultatType == BehandlingResultatType.INNVILGET && fagsak.status != FagsakStatus.LØPENDE) {
            fagsakService.oppdaterStatus(fagsak, FagsakStatus.LØPENDE)
        } else {
            fagsakService.oppdaterStatus(fagsak, FagsakStatus.STANSET)
        }

        antallBehandlingerFerdigstilt[behandling.type]?.increment()
        loggService.opprettFerdigstillBehandling(behandling)
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.FERDIGSTILT)
        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.FERDIGSTILLE_BEHANDLING
    }
}