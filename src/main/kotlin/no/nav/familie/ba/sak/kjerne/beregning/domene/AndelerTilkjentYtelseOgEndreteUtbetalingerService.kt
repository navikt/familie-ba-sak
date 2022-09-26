package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerPeriodeInnenforTilkjentytelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerÅrsak
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class AndelerTilkjentYtelseOgEndreteUtbetalingerService(
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val featureToggleService: FeatureToggleService
) {
    @Transactional
    fun finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId: Long): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        knyttEventueltSammenAndelerOgEndringer(behandlingId)
        return lagKombinator(behandlingId).lagAndelerMedEndringer()
    }

    @Transactional
    fun finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandlingId: Long): List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> {
        knyttEventueltSammenAndelerOgEndringer(behandlingId)
        return lagKombinator(behandlingId).lagEndreteUtbetalingMedAndeler()
    }

    /**
     * Spesialvariant som brukes mot frontend og i behandlingsresultatsteget
     * Sjekker at endringsperiode gir mening ift vilkårsvurderingen
     * Hvis ikke returnes en tom liste med andel tilkjent ytelse
     * Det signaliserer at "noe" er feil. I frontend brukes dette for å gi "gul trekant"
     * I behandlingsresultatsteget brukes det tilsvarende for å validere om det er mulig å gå videre
     */
    fun finnEndreteUtbetalingerMedAndelerIHenholdTilVilkårsvurdering(behandlingId: Long) =
        finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandlingId)
            .map {
                it.utenAndelerVedValideringsfeil {
                    validerÅrsak(
                        it.årsak,
                        it.endretUtbetalingAndel,
                        vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId)
                    )
                }
            }

    private fun lagKombinator(behandlingId: Long) =
        AndelTilkjentYtelseOgEndreteUtbetalingerKombinator(
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId),
            endretUtbetalingAndelRepository.findByBehandlingId(behandlingId),
            featureToggleService.isEnabled(FeatureToggleConfig.BRUK_FRIKOBLEDE_ANDELER_OG_ENDRINGER)
        )

    /**
     * Oppretter kobling i DB mellom andeler og endringer som et ekstra sikkerhetsnett
     * hvis BRUK_FRIKOBLEDE_ANDELER_OG_ENDRINGER-toggle skrus av igjen
     * Lener seg på at koblingen andel->endring fører til tilsvarende kobling endring->andel
     * og tilsvarende at fjerning av kobling propageres
     */
    private fun knyttEventueltSammenAndelerOgEndringer(behandlingId: Long) {
        if (featureToggleService.isEnabled(FeatureToggleConfig.BRUK_FRIKOBLEDE_ANDELER_OG_ENDRINGER) &&
            !featureToggleService.isEnabled(FeatureToggleConfig.BRUK_FRIKOBLEDE_ANDELER_OG_ENDRINGER_UTEN_SIKKERHETSNETT)
        ) {
            val andelerTilkjentYtelse = lagKombinator(behandlingId).lagAndelerMedEndringer().map {
                it.andel.endretUtbetalingAndeler.clear()
                it.andel.endretUtbetalingAndeler.addAll(it.endreteUtbetalinger)

                it.andel
            }
            andelTilkjentYtelseRepository.saveAllAndFlush(andelerTilkjentYtelse)
        }
    }
}

private class AndelTilkjentYtelseOgEndreteUtbetalingerKombinator(
    private val andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    private val endretUtbetalingAndeler: Collection<EndretUtbetalingAndel>,
    private val brukFrikobleteAndelerOgEndringer: Boolean
) {
    fun lagAndelerMedEndringer(): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        return andelerTilkjentYtelse.map { lagAndelMedEndringer(it) }
    }

    fun lagEndreteUtbetalingMedAndeler(): List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> {
        return endretUtbetalingAndeler.map { lagEndringMedAndeler(it) }
    }

    private fun lagAndelMedEndringer(andelTilkjentYtelse: AndelTilkjentYtelse): AndelTilkjentYtelseMedEndreteUtbetalinger {
        val endreteUtbetalinger = endretUtbetalingAndeler
            .filter { overlapper(andelTilkjentYtelse, it) }

        return AndelTilkjentYtelseMedEndreteUtbetalinger(
            andelTilkjentYtelse,
            endreteUtbetalinger,
            brukFrikobleteAndelerOgEndringer
        )
    }

    private fun lagEndringMedAndeler(endretUtbetalingAndel: EndretUtbetalingAndel): EndretUtbetalingAndelMedAndelerTilkjentYtelse {
        val andeler = andelerTilkjentYtelse
            .filter { overlapper(it, endretUtbetalingAndel) }

        return EndretUtbetalingAndelMedAndelerTilkjentYtelse(
            endretUtbetalingAndel,
            andeler,
            brukFrikobleteAndelerOgEndringer
            // Sjekker at endringen ikke "stikker utenfor" hele den tilkjente ytelsen.
            // I motsatt fall er ikke endringen gyldig og skal ikke være koblet til andelen
            // Et lite hack som utnytter kode som finnes.
            // Bør skrives om til å gjøre en "ekte" sjekk på om endringen overlapper andeler uten hull
        ).utenAndelerVedValideringsfeil {
            validerPeriodeInnenforTilkjentytelse(
                endretUtbetalingAndel,
                andelerTilkjentYtelse
            )
        }
    }

    private fun overlapper(
        andelTilkjentYtelse: AndelTilkjentYtelse,
        endretUtbetalingAndel: EndretUtbetalingAndel
    ): Boolean {
        return andelTilkjentYtelse.aktør == endretUtbetalingAndel.person?.aktør &&
            endretUtbetalingAndel.fom != null && endretUtbetalingAndel.tom != null &&
            endretUtbetalingAndel.periode.overlapperHeltEllerDelvisMed(andelTilkjentYtelse.periode)
    }
}

data class AndelTilkjentYtelseMedEndreteUtbetalinger internal constructor(
    private val andelTilkjentYtelse: AndelTilkjentYtelse,
    private val endreteUtbetalingerAndeler: Collection<EndretUtbetalingAndel>,
    private val brukFrikobleteAndelerOgEndringer: Boolean
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
            andelTilkjentYtelse.copy(stønadTom = tom),
            endreteUtbetalinger,
            brukFrikobleteAndelerOgEndringer
        )
    }

    val stønadFom get() = andelTilkjentYtelse.stønadFom
    val stønadTom get() = andelTilkjentYtelse.stønadTom
    val prosent get() = andelTilkjentYtelse.prosent
    val andel get() = andelTilkjentYtelse
    val endreteUtbetalinger
        get() = if (brukFrikobleteAndelerOgEndringer) {
            endreteUtbetalingerAndeler
        } else {
            andel.endretUtbetalingAndeler
        }

    companion object {
        fun utenEndringer(andelTilkjentYtelse: AndelTilkjentYtelse): AndelTilkjentYtelseMedEndreteUtbetalinger {
            if (andelTilkjentYtelse.endretUtbetalingAndeler.size > 0) {
                throw IllegalArgumentException(
                    "Skal opprette AndelTilkjentYtelseMedEndreteUtbetalinger uten endringer, men underliggende andel HAR endringer"
                )
            }

            return AndelTilkjentYtelseMedEndreteUtbetalinger(
                andelTilkjentYtelse,
                emptyList(),
                true // Likegyldig hvilken verdi denne har. I begge tilfeller returneres tom liste.
            )
        }
    }
}

data class EndretUtbetalingAndelMedAndelerTilkjentYtelse(
    val endretUtbetalingAndel: EndretUtbetalingAndel,
    private val andeler: List<AndelTilkjentYtelse>,
    internal val brukFrikobleteAndelerOgEndringer: Boolean
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
        get() = if (brukFrikobleteAndelerOgEndringer) {
            andeler
        } else {
            endretUtbetalingAndel.andelTilkjentYtelser
        }
}

/**
 * Fjerner andelene hvis det funksjonen som sendes inn kaster en exception
 * Brukes som en wrapper rundt en del valideringsfunksjoner som kaster exception når ting ikke validerer
 * Manglende andeler brukes et par steder som et signal om at noe er feil
 */
private fun EndretUtbetalingAndelMedAndelerTilkjentYtelse.utenAndelerVedValideringsfeil(
    validator: () -> Unit
) = if (brukFrikobleteAndelerOgEndringer == true) {
    try {
        validator()
        this
    } catch (e: Throwable) {
        this.copy(andeler = emptyList())
    }
} else {
    this
}

/**
 * Hjelpefunksjon som oppretter AndelTilkjentYtelseMedEndreteUtbetalinger fra AndelTilkjentYtelse og legger til en endring.
 * Utnytter at <endretUtbetalingAndelMedAndelerTilkjentYtelse> vet om funksjonsbryteren <brukFrikobleteAndelerOgEndringer> er satt
 * og viderefører den til den opprettede AndelTilkjentYtelseMedEndreteUtbetalinger
 */
fun AndelTilkjentYtelse.medEndring(
    endretUtbetalingAndelMedAndelerTilkjentYtelse: EndretUtbetalingAndelMedAndelerTilkjentYtelse
) = AndelTilkjentYtelseMedEndreteUtbetalinger(
    this,
    listOf(endretUtbetalingAndelMedAndelerTilkjentYtelse.endretUtbetalingAndel),
    brukFrikobleteAndelerOgEndringer = endretUtbetalingAndelMedAndelerTilkjentYtelse.brukFrikobleteAndelerOgEndringer
)
