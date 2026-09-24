package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.springframework.stereotype.Service

@Service
class AutovedtakSøknadValideringService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val beregningService: BeregningService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) {
    fun validerAtVilkårsvurderingErOppfylt(behandling: Behandling) {
        AutovedtakSøknadValidering.validerAtVilkårsvurderingErOppfylt(
            vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandlingThrows(behandling.id),
        )
    }

    fun validerAtBehandlingsresultatErInnvilgetEllerDelvisInnvilget(behandling: Behandling) {
        AutovedtakSøknadValidering.validerAtBehandlingsresultatErInnvilgetEllerDelvisInnvilget(
            behandlingsresultat = behandling.resultat,
        )
    }

    fun validerAtKunPersonerFremstiltKravForHarEndringIAndeler(behandling: Behandling) {
        val forrigeVedtatteBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)

        AutovedtakSøknadValidering.validerAtKunPersonerFremstiltKravForHarEndringIAndeler(
            behandlingId = behandling.id,
            andelerDenneBehandlingen = beregningService.hentAndelerTilkjentYtelseForBehandling(behandling.id),
            andelerForrigeBehandling = forrigeVedtatteBehandling?.let { beregningService.hentAndelerTilkjentYtelseForBehandling(it.id) } ?: emptyList(),
            personerFremstiltKravFor = søknadGrunnlagService.finnPersonerFremstiltKravFor(behandling = behandling, forrigeBehandling = forrigeVedtatteBehandling).toSet(),
        )
    }

    fun validerAtSimuleringGirUtbetalingUtenFeilutbetaling(simulering: List<ØkonomiSimuleringMottaker>) {
        AutovedtakSøknadValidering.validerAtSimuleringGirUtbetaling(simulering)
        AutovedtakSøknadValidering.validerAtDetIkkeErFeilutbetaling(simulering)
    }
}
