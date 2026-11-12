package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.autovedtak.søknad.AutovedtakSøknadMåBehandlesManueltÅrsak.OVERLAPP_ANDELER_FOR_BARN
import no.nav.familie.ba.sak.kjerne.autovedtak.søknad.AutovedtakSøknadMåBehandlesManueltÅrsak.UTBETALING_OVER_100_PROSENT_BARN
import no.nav.familie.ba.sak.kjerne.autovedtak.søknad.AutovedtakSøknadMåBehandlesManueltÅrsak.UTBETALING_OVER_100_PROSENT_SØKER
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.beregning.tilPeriode
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.tidslinje.harOverlappMed
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import org.springframework.stereotype.Service

@Service
class AutovedtakSøknadValideringService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val beregningService: BeregningService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val infotrygdService: InfotrygdService,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
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

    fun validerAtInnvilgedePerioderIkkeOverlapperMedTidligereUtbetalinger(behandling: Behandling) {
        val forrigeVedtatteBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
        val infotrygdstønaderTilSøker = infotrygdService.hentInfotrygdstønaderForSøker(ident = behandling.fagsak.aktør.aktivFødselsnummer(), historikk = true).bruker

        AutovedtakSøknadValidering.validerAtInnvilgedePerioderIkkeOverlapperMedTidligereUtbetalinger(
            andelerDenneBehandlingen = beregningService.hentAndelerTilkjentYtelseForBehandling(behandling.id),
            andelerForrigeBehandling = forrigeVedtatteBehandling?.let { beregningService.hentAndelerTilkjentYtelseForBehandling(it.id) } ?: emptyList(),
            infotrygdstønaderTilSøker = infotrygdstønaderTilSøker,
        )
    }

    fun validerAtIngenUtbetalingerOverstiger100Prosent(behandling: Behandling) {
        try {
            tilkjentYtelseValideringService.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(behandling)
        } catch (feil: UtbetalingsikkerhetFeil) {
            secureLogger.info("Automatisk behandling av søknad i behandling ${behandling.id} gir mer enn 100% utbetaling for barn: ${feil.message}")
            throw AutovedtakSøknadMåBehandlesManueltFeil(
                beskrivelse = "Automatisk behandling av søknad gir utbetaling som overstiger 100% for ett eller flere barn.\n$MÅ_HÅNDTERES_MANUELT",
                årsak = UTBETALING_OVER_100_PROSENT_BARN,
            )
        }

        try {
            tilkjentYtelseValideringService.validerAtSøkerIkkeFårFlereUtvidetAndelerSammePeriode(behandling)
        } catch (feil: UtbetalingsikkerhetFeil) {
            secureLogger.info("Automatisk behandling av søknad i behandling ${behandling.id} gir mer enn 100% utvidet barnetrygd for søker: ${feil.message}")
            throw AutovedtakSøknadMåBehandlesManueltFeil(
                beskrivelse = "Automatisk behandling av søknad gir utbetaling av utvidet barnetrygd som overstiger 100% for søker.\n$MÅ_HÅNDTERES_MANUELT",
                årsak = UTBETALING_OVER_100_PROSENT_SØKER,
            )
        }
    }

    fun validerAtSimuleringGirUtbetalingUtenFeilutbetaling(simulering: List<ØkonomiSimuleringMottaker>) {
        AutovedtakSøknadValidering.validerAtSimuleringGirUtbetaling(simulering)
        AutovedtakSøknadValidering.validerAtDetIkkeErFeilutbetaling(simulering)
    }

    fun validerAtBehandlingIkkeHarOverlappendeAndeler(behandling: Behandling) {
        val personerFremstiltKravFor = søknadGrunnlagService.finnPersonerFremstiltKravFor(behandling = behandling, forrigeBehandling = null).toSet()
        val andelerPerBarnSøktFor =
            beregningService
                .hentAndelerTilkjentYtelseForBehandling(behandling.id)
                .filter { it.aktør in personerFremstiltKravFor && !it.erSøkersAndel() }
                .groupBy { it.aktør }

        val harOverlapp =
            andelerPerBarnSøktFor.any { (barn, nyeAndeler) ->
                val harTidligereAndelerTidslinje =
                    beregningService
                        .hentAndelerFraIverksatteBehandlingerForAktør(barn)
                        .filter { it.behandlingId != behandling.id && !it.erSøkersAndel() }
                        .map { it.tilPeriode().tilTidslinje() }
                        .kombiner { true }

                val harNyeAndelerTidslinje =
                    nyeAndeler
                        .map { it.tilPeriode().tilTidslinje() }
                        .kombiner { true }

                harNyeAndelerTidslinje.harOverlappMed(harTidligereAndelerTidslinje)
            }

        if (harOverlapp) {
            throw AutovedtakSøknadMåBehandlesManueltFeil(
                beskrivelse = "Automatisk behandling av søknad gir andeler som overlapper med andeler i en tidligere iverksatt behandling for samme barn.\n$MÅ_HÅNDTERES_MANUELT",
                årsak = OVERLAPP_ANDELER_FOR_BARN,
            )
        }
    }

    companion object {
        private const val MÅ_HÅNDTERES_MANUELT = "Behandling av søknad må håndteres manuelt."
    }
}
