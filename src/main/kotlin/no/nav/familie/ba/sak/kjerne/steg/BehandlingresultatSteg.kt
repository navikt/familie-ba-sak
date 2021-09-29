package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandlingresultatSteg(
        private val behandlingService: BehandlingService,
        private val simuleringService: SimuleringService,
        private val vedtakService: VedtakService,
        private val vedtaksperiodeService: VedtaksperiodeService,
        private val behandlingsresultatService: BehandlingsresultatService,
) : BehandlingSteg<String> {

    @Transactional
    override fun utførStegOgAngiNeste(behandling: Behandling, data: String): StegType {

        val behandlingMedResultat = if (behandling.erMigrering() && behandling.skalBehandlesAutomatisk) {
            settBehandlingResultat(behandling, BehandlingResultat.INNVILGET)
        } else {
            val resultat = behandlingsresultatService.utledBehandlingsresultat(behandlingId = behandling.id)
            behandlingService.oppdaterResultatPåBehandling(behandlingId = behandling.id,
                                                           resultat = resultat)
        }

        if (behandlingMedResultat.opprettetÅrsak != BehandlingÅrsak.SATSENDRING) {
            vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(vedtak = vedtakService.hentAktivForBehandlingThrows(
                    behandlingId = behandling.id))
        }

        if (behandlingMedResultat.skalBehandlesAutomatisk && behandlingMedResultat.resultat != BehandlingResultat.AVSLÅTT) {
            behandlingService.oppdaterStatusPåBehandling(behandlingMedResultat.id, BehandlingStatus.IVERKSETTER_VEDTAK)
        } else {
            simuleringService.oppdaterSimuleringPåBehandling(behandlingMedResultat)
        }

        return hentNesteStegForNormalFlyt(behandlingMedResultat)
    }

    override fun stegType(): StegType {
        return StegType.BEHANDLINGSRESULTAT
    }

    private fun settBehandlingResultat(behandling: Behandling, resultat: BehandlingResultat): Behandling {
        behandling.resultat = resultat
        return behandlingService.lagreEllerOppdater(behandling)
    }
}