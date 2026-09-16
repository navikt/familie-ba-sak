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
    /**
     * Validerer en automatisk behandling av søknad etter behandlingsresultatsteget.
     *
     * @throws [no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil] hvis ett av følgende krav ikke er oppfylt:
     * - Alle vilkår er oppfylt
     * - Behandlingsresultatet er innvilget eller delvis innvilget
     * - Kun personer det er fremstilt krav for har endring i andeler
     */
    fun validerAtBehandlingKanVedtasAutomatisk(behandling: Behandling) {
        AutovedtakSøknadValidering.validerAtVilkårsvurderingErOppfylt(
            vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandlingThrows(behandling.id),
        )

        AutovedtakSøknadValidering.validerAtBehandlingsresultatErInnvilgetEllerDelvisInnvilget(
            behandlingsresultat = behandling.resultat,
        )

        val forrigeVedtatteBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)

        AutovedtakSøknadValidering.validerAtKunPersonerFremstiltKravForHarEndringIAndeler(
            behandlingId = behandling.id,
            andelerDenneBehandlingen = beregningService.hentAndelerTilkjentYtelseForBehandling(behandling.id),
            andelerForrigeBehandling = forrigeVedtatteBehandling?.let { beregningService.hentAndelerTilkjentYtelseForBehandling(it.id) } ?: emptyList(),
            personerFremstiltKravFor = søknadGrunnlagService.finnPersonerFremstiltKravFor(behandling = behandling, forrigeBehandling = forrigeVedtatteBehandling).toSet(),
        )
    }

    /**
     * @throws [no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil] hvis simuleringen ikke gir utbetaling eller gir feilutbetaling
     */
    fun validerAtSimuleringGirUtbetalingUtenFeilutbetaling(simulering: List<ØkonomiSimuleringMottaker>) {
        AutovedtakSøknadValidering.validerAtSimuleringGirUtbetaling(simulering)
        AutovedtakSøknadValidering.validerAtDetIkkeErFeilutbetaling(simulering)
    }
}
