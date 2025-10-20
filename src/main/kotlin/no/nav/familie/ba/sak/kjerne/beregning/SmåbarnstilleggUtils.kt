package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.erTilogMed3ÅrTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.tilMåned
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.beskjærEtter
import no.nav.familie.tidslinje.utvidelser.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import java.math.BigDecimal
import java.time.LocalDate

fun kombinerBarnasTidslinjerTilUnder3ÅrResultat(
    alleAndelerForBarnUnder3År: Iterable<AndelTilkjentYtelseMedEndreteUtbetalinger>,
): BarnSinRettTilSmåbarnstillegg? {
    val høyesteProsentIPeriode = alleAndelerForBarnUnder3År.maxOfOrNull { it.prosent }

    return when {
        høyesteProsentIPeriode == null -> null
        høyesteProsentIPeriode > BigDecimal.ZERO -> BarnSinRettTilSmåbarnstillegg.UNDER_3_ÅR_UTBETALING
        høyesteProsentIPeriode == BigDecimal.ZERO -> BarnSinRettTilSmåbarnstillegg.UNDER_3_ÅR_NULLUTBETALING
        else -> throw Feil("Høyeste prosent for barna i perioden er et negativt tall.")
    }
}

fun lagTidslinjeForPerioderMedBarnSomGirRettTilSmåbarnstillegg(
    barnasAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    barnasAktørerOgFødselsdatoer: List<Pair<Aktør, LocalDate>>,
): Tidslinje<BarnSinRettTilSmåbarnstillegg> {
    val barnasOrdinæreAndelerTidslinjer =
        barnasAndeler.filter { it.type == YtelseType.ORDINÆR_BARNETRYGD }.groupBy { it.aktør }.mapValues { it.value.tilTidslinje() }

    val barnasAndelerUnder3ÅrTidslinje =
        barnasOrdinæreAndelerTidslinjer.map { (barnAktør, barnTidslinje) ->
            val barnetsFødselsdato =
                barnasAktørerOgFødselsdatoer.find { it.first == barnAktør }?.second
                    ?: throw Feil("Kan ikke beregne småbarnstillegg for et barn som ikke har fødselsdato.")

            val erTilOgMed3ÅrTidslinje = erTilogMed3ÅrTidslinje(barnetsFødselsdato)

            barnTidslinje.beskjærEtter(erTilOgMed3ÅrTidslinje)
        }

    return barnasAndelerUnder3ÅrTidslinje
        .kombinerUtenNull { kombinerBarnasTidslinjerTilUnder3ÅrResultat(it) }
        .filtrerIkkeNull()
}

data class SmåbarnstilleggPeriode(
    val overgangsstønadPeriode: InternPeriodeOvergangsstønad,
    val prosent: BigDecimal,
)

fun kombinerAlleTidslinjerTilProsentTidslinje(
    perioderMedFullOvergangsstønadTidslinje: Tidslinje<InternPeriodeOvergangsstønad>,
    utvidetBarnetrygdTidslinje: Tidslinje<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    barnSomGirRettTilSmåbarnstilleggTidslinje: Tidslinje<BarnSinRettTilSmåbarnstillegg>,
): Tidslinje<SmåbarnstilleggPeriode> =
    perioderMedFullOvergangsstønadTidslinje
        .tilMåned { kombinatorInternPeriodeOvergangsstønadDagTilMåned(it) }
        .kombinerMed(
            tidslinje2 = utvidetBarnetrygdTidslinje,
            tidslinje3 = barnSomGirRettTilSmåbarnstilleggTidslinje,
        ) { overgangsstønad, utvidet, under3År ->
            if (overgangsstønad == null || utvidet == null || under3År == null) {
                null
            } else if (utvidet.prosent > BigDecimal.ZERO && under3År == BarnSinRettTilSmåbarnstillegg.UNDER_3_ÅR_UTBETALING) {
                SmåbarnstilleggPeriode(
                    overgangsstønad,
                    BigDecimal(100),
                )
            } else if (utvidet.prosent == BigDecimal.ZERO || under3År == BarnSinRettTilSmåbarnstillegg.UNDER_3_ÅR_NULLUTBETALING) {
                SmåbarnstilleggPeriode(
                    overgangsstønad,
                    BigDecimal.ZERO,
                )
            } else {
                throw Feil("Ugyldig kombinasjon av overgangsstønad, utvidet og barn under 3 år ved generering av småbarnstillegg.")
            }
        }.filtrerIkkeNull()

/**
 * EF sender alltid overgangsstønad-perioder som gjelder hele måneder, men formatet vi får er på LocalDate
 * Returverdier:
 * Null - Søker får ikke overgangsstønad noen dager den måneden
 * InternPeriodeOvergangsstønad - Det finnes minst 1 dag i måneden hvor søker får overgangsstønad, den første av disse blir returnert
 */
fun kombinatorInternPeriodeOvergangsstønadDagTilMåned(dagverdier: List<InternPeriodeOvergangsstønad?>): InternPeriodeOvergangsstønad? {
    val dagverdierSomErSatt = dagverdier.filterNotNull()
    return if (dagverdierSomErSatt.isEmpty()) {
        null
    } else {
        dagverdierSomErSatt.first()
    }
}

enum class BarnSinRettTilSmåbarnstillegg {
    UNDER_3_ÅR_UTBETALING,
    UNDER_3_ÅR_NULLUTBETALING,
}

fun validerUtvidetOgBarnasAndeler(
    utvidetAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    barnasAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
) {
    if (utvidetAndeler.any { !it.erUtvidet() }) throw Feil("Det finnes andre ytelser enn utvidet blandt utvidet-andelene")
    if (barnasAndeler.any { it.erSøkersAndel() }) throw Feil("Finner andeler for søker blandt barnas andeler")
}
