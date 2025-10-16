package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerPeriodeInnenforTilkjentytelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerÅrsak
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AndelerTilkjentYtelseOgEndreteUtbetalingerService(
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) {
    @Transactional
    fun finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId: Long): List<AndelTilkjentYtelseMedEndreteUtbetalinger> = lagKombinator(behandlingId).lagAndelerMedEndringer()

    @Transactional
    fun finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandlingId: Long): List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId)
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val endreteUtbetalingerMedAndeler = lagKombinator(behandlingId).lagEndreteUtbetalingMedAndeler()

        return if (!behandling.erSatsendringMånedligValutajusteringFinnmarkstilleggEllerSvalbardtillegg()) {
            // Hvis noen valideringer feiler, så signalerer vi det til frontend ved å fjerne tilknyttede andeler
            // SB vil få en feilmelding og løsningen blir å slette eller oppdatere endringen
            // Da vil forhåpentligvis valideringen være ok, koblingene til andelene være beholdt
            endreteUtbetalingerMedAndeler.filterBortAndelerMedValideringsfeil(vilkårsvurdering)
        } else {
            endreteUtbetalingerMedAndeler
        }
    }

    private fun lagKombinator(behandlingId: Long) =
        AndelTilkjentYtelseOgEndreteUtbetalingerKombinator(
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId),
            endretUtbetalingAndelRepository.findByBehandlingId(behandlingId),
        )
}

fun List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>.filterBortAndelerMedValideringsfeil(vilkårsvurdering: Vilkårsvurdering?): List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> =
    this.map {
        it
            .utenAndelerVedValideringsfeil {
                validerPeriodeInnenforTilkjentytelse(
                    it.endretUtbetalingAndel,
                    it.andelerTilkjentYtelse,
                )
            }.utenAndelerVedValideringsfeil {
                validerÅrsak(
                    it.endretUtbetalingAndel,
                    vilkårsvurdering,
                )
            }
    }

private class AndelTilkjentYtelseOgEndreteUtbetalingerKombinator(
    private val andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    private val endretUtbetalingAndeler: Collection<EndretUtbetalingAndel>,
) {
    fun lagAndelerMedEndringer(): List<AndelTilkjentYtelseMedEndreteUtbetalinger> = andelerTilkjentYtelse.map { lagAndelMedEndringer(it) }

    fun lagEndreteUtbetalingMedAndeler(): List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> = endretUtbetalingAndeler.map { lagEndringMedAndeler(it) }

    private fun lagAndelMedEndringer(andelTilkjentYtelse: AndelTilkjentYtelse): AndelTilkjentYtelseMedEndreteUtbetalinger {
        val endreteUtbetalinger =
            endretUtbetalingAndeler
                .filter { overlapper(andelTilkjentYtelse, it) }

        return AndelTilkjentYtelseMedEndreteUtbetalinger(
            andelTilkjentYtelse,
            endreteUtbetalinger,
        )
    }

    private fun lagEndringMedAndeler(endretUtbetalingAndel: EndretUtbetalingAndel): EndretUtbetalingAndelMedAndelerTilkjentYtelse {
        val andeler =
            andelerTilkjentYtelse
                .filter { overlapper(it, endretUtbetalingAndel) }

        return EndretUtbetalingAndelMedAndelerTilkjentYtelse(
            endretUtbetalingAndel,
            andeler,
        )
    }

    private fun overlapper(
        andelTilkjentYtelse: AndelTilkjentYtelse,
        endretUtbetalingAndel: EndretUtbetalingAndel,
    ): Boolean =
        endretUtbetalingAndel.personer.any { it.aktør == andelTilkjentYtelse.aktør } &&
            endretUtbetalingAndel.fom != null &&
            endretUtbetalingAndel.tom != null &&
            endretUtbetalingAndel.periode.overlapperHeltEllerDelvisMed(andelTilkjentYtelse.periode)
}

data class AndelTilkjentYtelseMedEndreteUtbetalinger(
    private val andelTilkjentYtelse: AndelTilkjentYtelse,
    private val endreteUtbetalingerAndeler: Collection<EndretUtbetalingAndel>,
) {
    val periodeOffset get() = andelTilkjentYtelse.periodeOffset
    val sats get() = andelTilkjentYtelse.sats
    val type get() = andelTilkjentYtelse.type
    val kalkulertUtbetalingsbeløp get() = andelTilkjentYtelse.kalkulertUtbetalingsbeløp
    val aktør get() = andelTilkjentYtelse.aktør

    fun erSøkersAndel() = andelTilkjentYtelse.erSøkersAndel()

    fun erSmåbarnstillegg() = andelTilkjentYtelse.erSmåbarnstillegg()

    fun erUtvidet(): Boolean = andelTilkjentYtelse.erUtvidet()

    fun erAndelSomSkalSendesTilOppdrag() = andelTilkjentYtelse.erAndelSomSkalSendesTilOppdrag()

    fun erAndelSomharNullutbetalingPgaDifferanseberegning() = andelTilkjentYtelse.erAndelSomharNullutbetalingPgaDifferanseberegning()

    val stønadFom get() = andelTilkjentYtelse.stønadFom
    val stønadTom get() = andelTilkjentYtelse.stønadTom
    val prosent get() = andelTilkjentYtelse.prosent
    val andel get() = andelTilkjentYtelse
    val endreteUtbetalinger = endreteUtbetalingerAndeler

    companion object {
        fun utenEndringer(andelTilkjentYtelse: AndelTilkjentYtelse): AndelTilkjentYtelseMedEndreteUtbetalinger =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                andelTilkjentYtelse = andelTilkjentYtelse,
                endreteUtbetalingerAndeler = emptyList(),
            )
    }
}

data class EndretUtbetalingAndelMedAndelerTilkjentYtelse(
    val endretUtbetalingAndel: EndretUtbetalingAndel,
    private val andeler: List<AndelTilkjentYtelse>,
) {
    fun overlapperMed(månedPeriode: MånedPeriode) = endretUtbetalingAndel.overlapperMed(månedPeriode)

    val periode get() = endretUtbetalingAndel.periode
    val personer get() = endretUtbetalingAndel.personer
    val begrunnelse get() = endretUtbetalingAndel.begrunnelse
    val søknadstidspunkt get() = endretUtbetalingAndel.søknadstidspunkt
    val avtaletidspunktDeltBosted get() = endretUtbetalingAndel.avtaletidspunktDeltBosted
    val prosent get() = endretUtbetalingAndel.prosent
    val personIdenter get() = endretUtbetalingAndel.personer.map { it.aktør.aktivFødselsnummer() }
    val årsak get() = endretUtbetalingAndel.årsak
    val id get() = endretUtbetalingAndel.id
    val fom get() = endretUtbetalingAndel.fom
    val tom get() = endretUtbetalingAndel.tom
    val andelerTilkjentYtelse = andeler
}

/**
 * Fjerner andelene hvis det funksjonen som sendes inn kaster en exception
 * Brukes som en wrapper rundt en del valideringsfunksjoner som kaster exception når ting ikke validerer
 * Manglende andeler brukes et par steder som et signal om at noe er feil
 */
private fun EndretUtbetalingAndelMedAndelerTilkjentYtelse.utenAndelerVedValideringsfeil(
    validator: () -> Unit,
) = try {
    validator()
    this
} catch (e: FunksjonellFeil) {
    this.copy(andeler = emptyList())
}

/**
 * Hjelpefunksjon som oppretter AndelTilkjentYtelseMedEndreteUtbetalinger fra AndelTilkjentYtelse og legger til en endring.
 * Utnytter at <endretUtbetalingAndelMedAndelerTilkjentYtelse> vet om funksjonsbryteren <brukFrikobleteAndelerOgEndringer> er satt
 * og viderefører den til den opprettede AndelTilkjentYtelseMedEndreteUtbetalinger
 */
fun AndelTilkjentYtelse.medEndring(
    endretUtbetalingAndelMedAndelerTilkjentYtelse: EndretUtbetalingAndelMedAndelerTilkjentYtelse,
) = AndelTilkjentYtelseMedEndreteUtbetalinger(
    andelTilkjentYtelse = this,
    endreteUtbetalingerAndeler = listOf(endretUtbetalingAndelMedAndelerTilkjentYtelse.endretUtbetalingAndel),
)

fun Collection<AndelTilkjentYtelse>.tilAndelerTilkjentYtelseMedEndreteUtbetalinger(endretUtbetalingAndeler: Collection<EndretUtbetalingAndel>) = AndelTilkjentYtelseOgEndreteUtbetalingerKombinator(this, endretUtbetalingAndeler).lagAndelerMedEndringer()

fun Collection<EndretUtbetalingAndel>.tilEndretUtbetalingAndelMedAndelerTilkjentYtelse(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>) = AndelTilkjentYtelseOgEndreteUtbetalingerKombinator(andelerTilkjentYtelse, this).lagEndreteUtbetalingMedAndeler()
