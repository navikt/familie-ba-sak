package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class AndelerTilkjentYtelseOgEndreteUtbetalingerService(
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository
) {
    @Transactional
    fun finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId: Long): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        return andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
            .map { AndelTilkjentYtelseMedEndreteUtbetalinger(it) }
    }

    @Transactional
    fun finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandlingId: Long): List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> {
        return endretUtbetalingAndelRepository.findByBehandlingId(behandlingId)
            .map { EndretUtbetalingAndelMedAndelerTilkjentYtelse(it) }
    }

    fun finnEndreteUtbetalingerMedAndelerIHenholdTilVilkårsvurdering(behandlingId: Long) =
        finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandlingId)
}

data class AndelTilkjentYtelseMedEndreteUtbetalinger internal constructor(
    private val andelTilkjentYtelse: AndelTilkjentYtelse
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
    fun overlapperPeriode(månedPeriode: MånedPeriode) = andelTilkjentYtelse.overlapperPeriode(månedPeriode)
    fun medTom(tom: YearMonth): AndelTilkjentYtelseMedEndreteUtbetalinger {
        return AndelTilkjentYtelseMedEndreteUtbetalinger(
            andelTilkjentYtelse.copy(stønadTom = tom)
        )
    }

    val stønadFom get() = andelTilkjentYtelse.stønadFom
    val stønadTom get() = andelTilkjentYtelse.stønadTom
    val prosent get() = andelTilkjentYtelse.prosent
    val andel get() = andelTilkjentYtelse
    val endreteUtbetalinger
        get() = andel.endretUtbetalingAndeler

    companion object {
        fun utenEndringer(andelTilkjentYtelse: AndelTilkjentYtelse) =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                andelTilkjentYtelse
            )
    }
}

data class EndretUtbetalingAndelMedAndelerTilkjentYtelse(
    val endretUtbetalingAndel: EndretUtbetalingAndel
) {
    fun overlapperMed(månedPeriode: MånedPeriode) = endretUtbetalingAndel.overlapperMed(månedPeriode)
    fun årsakErDeltBosted() = endretUtbetalingAndel.årsakErDeltBosted()

    val periode get() = endretUtbetalingAndel.periode
    val person get() = endretUtbetalingAndel.person
    val begrunnelse get() = endretUtbetalingAndel.begrunnelse
    val søknadstidspunkt get() = endretUtbetalingAndel.søknadstidspunkt
    val avtaletidspunktDeltBosted get() = endretUtbetalingAndel.avtaletidspunktDeltBosted
    val prosent get() = endretUtbetalingAndel.prosent
    val aktivtFødselsnummer get() = endretUtbetalingAndel.person?.aktør?.aktivFødselsnummer()
    val årsak get() = endretUtbetalingAndel.årsak
    val id get() = endretUtbetalingAndel.id
    val fom get() = endretUtbetalingAndel.fom
    val tom get() = endretUtbetalingAndel.tom
    val andelerTilkjentYtelse
        get() = endretUtbetalingAndel.andelTilkjentYtelser
}

/**
 * Frempek på en endring som kommer. Men gjør ingenting nå
 */
fun AndelTilkjentYtelse.medEndring(
    endretUtbetalingAndelMedAndelerTilkjentYtelse: EndretUtbetalingAndelMedAndelerTilkjentYtelse
) = AndelTilkjentYtelseMedEndreteUtbetalinger(
    this
)
