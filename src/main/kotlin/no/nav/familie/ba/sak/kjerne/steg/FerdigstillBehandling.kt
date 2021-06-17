package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingMetrikker
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

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
        logger.info("Forsøker å ferdigstille behandling ${behandling.id}")

        val erHenlagt = behandlingService.hent(behandling.id).erHenlagt()

        if (behandling.status !== BehandlingStatus.IVERKSETTER_VEDTAK && !erHenlagt) {
            error("Prøver å ferdigstille behandling ${behandling.id}, men status er ${behandling.status}")
        }

        if (!erHenlagt) {
            loggService.opprettFerdigstillBehandling(behandling)
        }

        behandlingMetrikker.oppdaterBehandlingMetrikker(behandling)
        if (behandling.status == BehandlingStatus.IVERKSETTER_VEDTAK) {
            oppdaterFagsakStatus(behandling = behandling)
        } else { // Dette betyr henleggelse.
            if (behandlingService.hentBehandlinger(behandling.fagsak.id).size == 1) {
                fagsakService.oppdaterStatus(behandling.fagsak, FagsakStatus.AVSLUTTET)
            }
            behandlingService.hentAktivForFagsak(behandling.fagsak.id)?.aktiv = false
            behandlingService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id)?.apply {
                aktiv = true
                behandlingService.lagreEllerOppdater(this)
            }
        }

        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandling.id, status = BehandlingStatus.AVSLUTTET)
        return hentNesteStegForNormalFlyt(behandling)
    }

    private fun oppdaterFagsakStatus(behandling: Behandling) {
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id)
        val erLøpende = tilkjentYtelse.andelerTilkjentYtelse.any { it.stønadTom >= inneværendeMåned() }
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

        private val logger = LoggerFactory.getLogger(FerdigstillBehandling::class.java)
    }
}