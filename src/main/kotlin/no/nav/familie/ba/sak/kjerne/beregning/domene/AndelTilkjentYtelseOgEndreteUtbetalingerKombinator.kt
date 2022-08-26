package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MIN_MÅNED
import org.springframework.stereotype.Service
import java.time.YearMonth

class AndelTilkjentYtelseOgEndreteUtbetalingerKombinator(
    private val andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    private val endretUtbetalingAndeler: Collection<EndretUtbetalingAndel>,
    private val brukNy: Boolean?
) {

    private fun lagAndelMedEndringer(andelTilkjentYtelse: AndelTilkjentYtelse): AndelTilkjentYtelseMedEndreteUtbetalinger {
        val endreteUtbetalinger = endretUtbetalingAndeler
            .filter { overlapper(andelTilkjentYtelse, it) }

        return AndelTilkjentYtelseMedEndreteUtbetalinger(andelTilkjentYtelse, endreteUtbetalinger, brukNy)
    }

    private fun lagEndringMedAndeler(endretUtbetalingAndel: EndretUtbetalingAndel): EndretUtbetalingAndelMedAndelerTilkjentYtelse {
        val andeler = andelerTilkjentYtelse
            .filter { overlapper(it, endretUtbetalingAndel) }

        return EndretUtbetalingAndelMedAndelerTilkjentYtelse(endretUtbetalingAndel, andeler, brukNy)
    }

    private fun overlapper(aty: AndelTilkjentYtelse, eua: EndretUtbetalingAndel): Boolean {
        val euaPeriode = MånedPeriode(eua.fom ?: MIN_MÅNED, eua.tom ?: MAX_MÅNED)
        val atyPeriode = MånedPeriode(aty.stønadFom, aty.stønadTom)

        return aty.aktør == eua.person?.aktør &&
            euaPeriode.overlapperHeltEllerDelvisMed(atyPeriode)
    }

    fun lagAndelerMedEndringer(): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        return andelerTilkjentYtelse.map { lagAndelMedEndringer(it) }
    }

    fun lagEndreteUtbetalingMedAndeler(): List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> {
        return endretUtbetalingAndeler.map { lagEndringMedAndeler(it) }
    }
}

data class AndelTilkjentYtelseMedEndreteUtbetalinger internal constructor(
    private val andelTilkjentYtelse: AndelTilkjentYtelse,
    private val endreteUtbetalingerAndeler: Collection<EndretUtbetalingAndel>,
    private val brukNy: Boolean?
) {
    constructor(
        andelTilkjentYtelse: AndelTilkjentYtelse,
        endretUtbetalingAndelMedAndelerTilkjentYtelse: EndretUtbetalingAndelMedAndelerTilkjentYtelse
    ) : this(
        andelTilkjentYtelse,
        listOf(endretUtbetalingAndelMedAndelerTilkjentYtelse.endretUtbetalingAndel),
        endretUtbetalingAndelMedAndelerTilkjentYtelse.brukNy
    )

    constructor(
        andelTilkjentYtelse: AndelTilkjentYtelse,
        endreteUtbetalingerAndeler: Collection<EndretUtbetalingAndel>
    ) : this(
        andelTilkjentYtelse,
        endreteUtbetalingerAndeler,
        true
    )

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
            andelTilkjentYtelse.copy(stønadTom = tom),
            endreteUtbetalinger,
            brukNy
        )
    }

    val stønadFom get() = andelTilkjentYtelse.stønadFom
    val stønadTom get() = andelTilkjentYtelse.stønadTom
    val prosent get() = andelTilkjentYtelse.prosent
    val andel get() = andelTilkjentYtelse
    val endreteUtbetalinger
        get() = if (brukNy == null) {
            emptyList()
        } else if (brukNy) {
            endreteUtbetalingerAndeler
        } else {
            andel.endretUtbetalingAndeler
        }

    companion object {
        fun utenEndringer(andelTilkjentYtelse: AndelTilkjentYtelse) =
            AndelTilkjentYtelseMedEndreteUtbetalinger(
                andelTilkjentYtelse,
                emptyList(),
                null
            )
    }
}

data class EndretUtbetalingAndelMedAndelerTilkjentYtelse(
    val endretUtbetalingAndel: EndretUtbetalingAndel,
    private val andeler: List<AndelTilkjentYtelse>,
    val brukNy: Boolean?
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
        get() = if (brukNy == null) {
            emptyList()
        } else if (brukNy) {
            andeler
        } else {
            endretUtbetalingAndel.andelTilkjentYtelser
        }
}

@Service
class AndelerTilkjentYtelseOgEndreteUtbetalingerService(
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val featureToggleService: FeatureToggleService
) {
    fun finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId: Long) =
        AndelTilkjentYtelseOgEndreteUtbetalingerKombinator(
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId),
            endretUtbetalingAndelRepository.findByBehandlingId(behandlingId),
            featureToggleService.isEnabled(FeatureToggleConfig.BRUK_FRIKOBLEDE_ANDELER_OG_ENDRINGER)
        ).lagAndelerMedEndringer()

    fun finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandlingId: Long) =
        AndelTilkjentYtelseOgEndreteUtbetalingerKombinator(
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId),
            endretUtbetalingAndelRepository.findByBehandlingId(behandlingId),
            featureToggleService.isEnabled(FeatureToggleConfig.BRUK_FRIKOBLEDE_ANDELER_OG_ENDRINGER)
        ).lagEndreteUtbetalingMedAndeler()
}
