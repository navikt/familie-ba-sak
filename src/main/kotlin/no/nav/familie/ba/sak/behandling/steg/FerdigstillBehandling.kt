package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingMetrikker
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.logg.LoggService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FerdigstillBehandling(
        private val fagsakService: FagsakService,
        private val beregningService: BeregningService,
        private val behandlingService: BehandlingService,
        private val behandlingMetrikker: BehandlingMetrikker,
        private val behandlingResultatService: BehandlingResultatService,
        private val loggService: LoggService
) : BehandlingSteg<String> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: String): StegType {
        LOG.info("Forsøker å ferdigstille behandling ${behandling.id}")

        val behandlingResultat = behandlingResultatService.hentAktivForBehandling(behandlingId = behandling.id)

        if (behandling.status !== BehandlingStatus.IVERKSETTER_VEDTAK && behandlingResultat?.erHenlagt() == false) {
            error("Prøver å ferdigstille behandling ${behandling.id}, men status er ${behandling.status}")
        }

        if (behandlingResultat?.erHenlagt() == false) {
            loggService.opprettFerdigstillBehandling(behandling)
        }

        behandlingMetrikker.oppdaterBehandlingMetrikker(behandling)
        if (behandling.status == BehandlingStatus.IVERKSETTER_VEDTAK) {
            oppdaterFagsakStatus(behandling = behandling)
        } else { // Dette betyr henleggelse.
            if (behandlingService.hentBehandlinger(behandling.fagsak.id).size == 1) {
                fagsakService.oppdaterStatus(behandling.fagsak, FagsakStatus.AVSLUTTET)
            }
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

        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}