package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
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
    fun validerAtBehandlingKanVedtasAutomatisk(behandling: Behandling) {
        AutovedtakSøknadValidering.validerAtVilkårsvurderingErOppfylt(
            vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandlingThrows(behandling.id),
        )

        AutovedtakSøknadValidering.validerAtBehandlingsresultatErInnvilgetEllerDelvisInnvilget(
            behandlingsresultat = behandling.resultat,
        )

        val forrigeVedtatteBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
        val andelerDenneBehandlingen = beregningService.hentAndelerTilkjentYtelseForBehandling(behandling.id)

        AutovedtakSøknadValidering.validerAtKunPersonerFremstiltKravForHarEndringIAndeler(
            behandlingId = behandling.id,
            andelerDenneBehandlingen = andelerDenneBehandlingen,
            andelerForrigeBehandling = forrigeVedtatteBehandling?.let { beregningService.hentAndelerTilkjentYtelseForBehandling(it.id) } ?: emptyList(),
            personerFremstiltKravFor = søknadGrunnlagService.finnPersonerFremstiltKravFor(behandling = behandling, forrigeBehandling = forrigeVedtatteBehandling).toSet(),
        )

        AutovedtakSøknadValidering.validerAtInnvilgedePerioderIkkeOverlapperTidligereUtbetalingsperioder(
            behandlingId = behandling.id,
            andelerDenneBehandlingen = andelerDenneBehandlingen,
            andelerFraTidligereIverksatteBehandlinger = hentAndelerFraTidligereIverksatteBehandlinger(behandling),
        )
    }

    private fun hentAndelerFraTidligereIverksatteBehandlinger(behandling: Behandling): List<AndelTilkjentYtelse> {
        val tidligereIverksatteBehandlingIder =
            behandlingHentOgPersisterService
                .hentIverksatteBehandlinger(fagsakId = behandling.fagsak.id)
                .map { it.id }
                .filter { it != behandling.id }

        if (tidligereIverksatteBehandlingIder.isEmpty()) return emptyList()

        return beregningService.hentAndelerTilkjentYtelseForBehandlinger(tidligereIverksatteBehandlingIder)
    }

    fun validerAtSimuleringGirUtbetalingUtenFeilutbetaling(simulering: List<ØkonomiSimuleringMottaker>) {
        AutovedtakSøknadValidering.validerAtSimuleringGirUtbetaling(simulering)
        AutovedtakSøknadValidering.validerAtDetIkkeErFeilutbetaling(simulering)
    }
}
