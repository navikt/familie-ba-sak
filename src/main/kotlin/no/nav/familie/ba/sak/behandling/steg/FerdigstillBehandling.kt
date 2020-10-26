package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingMetrikker
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.logg.LoggService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate.now

@Service
class FerdigstillBehandling(
        private val fagsakService: FagsakService,
        private val beregningService: BeregningService,
        private val behandlingService: BehandlingService,
        private val behandlingMetrikker: BehandlingMetrikker,
        private val loggService: LoggService
) : BehandlingSteg<String> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: String): StegType {
        LOG.info("Forsøker å ferdigstille behandling ${behandling.id}")

        if (behandling.status !== BehandlingStatus.IVERKSETTER_VEDTAK &&
            behandling.status !== BehandlingStatus.HENLAGT) {
            error("Prøver å ferdigstille behandling ${behandling.id}, men status er ${behandling.status}")
        }

        loggService.opprettFerdigstillBehandling(behandling)
        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandling.id, status = BehandlingStatus.AVSLUTTET)

        oppdaterFagsakStatus(behandling = behandling)

        behandlingMetrikker.oppdaterBehandlingMetrikker(behandling)

        return hentNesteStegForNormalFlyt(behandling)
    }

    private fun oppdaterFagsakStatus(behandling: Behandling) {
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id)
        val erLøpende = tilkjentYtelse.andelerTilkjentYtelse.any { it.stønadTom >= now() }
        if (erLøpende) {
            fagsakService.oppdaterStatus(behandling.fagsak, FagsakStatus.LØPENDE)
        } else {
            fagsakService.oppdaterStatus(behandling.fagsak, FagsakStatus.AVSLUTTET)
        }
    }

    override fun stegType(): StegType {
        return StegType.FERDIGSTILLE_BEHANDLING
    }

    companion object {

        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}