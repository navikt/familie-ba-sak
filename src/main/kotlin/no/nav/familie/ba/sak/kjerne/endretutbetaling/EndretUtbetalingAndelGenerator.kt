package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.sisteDagIForrigeMåned
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.beregning.hentGyldigEtterbetaling3MndFom
import no.nav.familie.ba.sak.kjerne.beregning.hentGyldigEtterbetaling3ÅrFom
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak.ETTERBETALING_3MND
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak.ETTERBETALING_3ÅR
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærTilOgMed
import no.nav.familie.tidslinje.utvidelser.outerJoin
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.math.BigDecimal
import java.time.LocalDate

private val DATO_FOR_OVERGANG_TIL_ETTERBETALING_3MND = LocalDate.of(2024, 10, 1)

fun genererEndretUtbetalingAndelerMedÅrsakEtterbetaling3ÅrEller3Mnd(
    behandling: Behandling,
    søknadMottattDato: LocalDate,
    nåværendeAndeler: List<AndelTilkjentYtelse>,
    forrigeAndeler: List<AndelTilkjentYtelse>,
    personerPåBehandling: List<Person>,
): List<EndretUtbetalingAndel> {
    val (datoForGyldigEtterbetaling, årsak) =
        if (søknadMottattDato.isBefore(DATO_FOR_OVERGANG_TIL_ETTERBETALING_3MND)) {
            hentGyldigEtterbetaling3ÅrFom(søknadMottattDato) to ETTERBETALING_3ÅR
        } else {
            hentGyldigEtterbetaling3MndFom(søknadMottattDato) to ETTERBETALING_3MND
        }

    val sisteDatoForEndretUtbetalingAndel = datoForGyldigEtterbetaling.toLocalDate().sisteDagIForrigeMåned()

    val nåværendeAndelerTidslinjer = nåværendeAndeler.tilTidslinjerPerAktørOgType().beskjærTilOgMed(sisteDatoForEndretUtbetalingAndel)
    val forrigeAndelerTidslinjer = forrigeAndeler.tilTidslinjerPerAktørOgType().beskjærTilOgMed(sisteDatoForEndretUtbetalingAndel)

    val perioderMedUgyldigEtterbetalingForAktører =
        nåværendeAndelerTidslinjer
            .outerJoin(forrigeAndelerTidslinjer) { nåværendeAndel, forrigeAndel ->
                val aktør = nåværendeAndel?.aktør ?: forrigeAndel?.aktør
                val nåværendeBeløp = nåværendeAndel?.kalkulertUtbetalingsbeløp ?: 0
                val forrigeBeløp = forrigeAndel?.kalkulertUtbetalingsbeløp ?: 0
                aktør.takeIf { nåværendeBeløp > forrigeBeløp }
            }.values
            .kombiner()
            .tilPerioderIkkeNull()

    return perioderMedUgyldigEtterbetalingForAktører
        .map { periodeMedUgyldigEtterbetalingForAktører ->
            EndretUtbetalingAndel(
                id = 0,
                behandlingId = behandling.id,
                personer = personerPåBehandling.filter { periodeMedUgyldigEtterbetalingForAktører.verdi.contains(it.aktør) }.toMutableSet(),
                prosent = BigDecimal.ZERO,
                fom = periodeMedUgyldigEtterbetalingForAktører.fom?.toYearMonth(),
                tom = periodeMedUgyldigEtterbetalingForAktører.tom?.toYearMonth(),
                årsak = årsak,
                avtaletidspunktDeltBosted = null,
                søknadstidspunkt = søknadMottattDato,
                begrunnelse = "Fylt ut automatisk fra søknadstidspunkt.",
            )
        }.slåSammenLikeEndretUtbetalingAndeler()
}

private fun List<EndretUtbetalingAndel>.slåSammenLikeEndretUtbetalingAndeler() =
    this
        .sortedBy { it.fom }
        .fold(listOf<EndretUtbetalingAndel>()) { endretUtbetalingAndeler, endretUtbetalingAndel ->
            val andelSomKanSammenslås = endretUtbetalingAndeler.find { kanSammenslås(it, endretUtbetalingAndel) }

            val nyAndel =
                if (andelSomKanSammenslås != null) {
                    endretUtbetalingAndel.copy(
                        fom = minOf(endretUtbetalingAndel.fom!!, andelSomKanSammenslås.fom!!),
                        tom = maxOf(endretUtbetalingAndel.tom!!, andelSomKanSammenslås.tom!!),
                    )
                } else {
                    endretUtbetalingAndel
                }

            endretUtbetalingAndeler.filter { it != andelSomKanSammenslås } + nyAndel
        }

private fun kanSammenslås(
    førsteEndretUtbetalingAndel: EndretUtbetalingAndel,
    andreEndretUtbetalingAndel: EndretUtbetalingAndel,
): Boolean =
    førsteEndretUtbetalingAndel.behandlingId == andreEndretUtbetalingAndel.behandlingId &&
        førsteEndretUtbetalingAndel.personer == andreEndretUtbetalingAndel.personer &&
        førsteEndretUtbetalingAndel.prosent == andreEndretUtbetalingAndel.prosent &&
        førsteEndretUtbetalingAndel.årsak == andreEndretUtbetalingAndel.årsak &&
        førsteEndretUtbetalingAndel.avtaletidspunktDeltBosted == andreEndretUtbetalingAndel.avtaletidspunktDeltBosted &&
        førsteEndretUtbetalingAndel.søknadstidspunkt == andreEndretUtbetalingAndel.søknadstidspunkt &&
        førsteEndretUtbetalingAndel.begrunnelse == andreEndretUtbetalingAndel.begrunnelse &&
        førsteEndretUtbetalingAndel.tom?.plusMonths(1) == andreEndretUtbetalingAndel.fom
