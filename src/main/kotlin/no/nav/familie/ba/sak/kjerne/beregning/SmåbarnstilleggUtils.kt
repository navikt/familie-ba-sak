package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.erTilogMed3ÅrTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon.tilMåned
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.beskjærEtter
import no.nav.familie.tidslinje.utvidelser.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class VedtaksperiodefinnerSmåbarnstilleggFeil(
    melding: String,
    override val frontendFeilmelding: String? = null,
    override val httpStatus: HttpStatus = HttpStatus.OK,
    override val throwable: Throwable? = null,
) : Feil(
        melding,
        frontendFeilmelding,
        httpStatus,
        throwable,
    )

fun vedtakOmOvergangsstønadPåvirkerFagsak(
    småbarnstilleggGenerator: SmåbarnstilleggGenerator,
    nyePerioderMedFullOvergangsstønad: List<InternPeriodeOvergangsstønad>,
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    barnasAktørerOgFødselsdatoer: List<Pair<Aktør, LocalDate>>,
): Boolean {
    val (forrigeSmåbarnstilleggAndeler, forrigeAndelerIkkeSmåbarnstillegg) = forrigeAndelerTilkjentYtelse.partition { it.erSmåbarnstillegg() }

    val (forrigeUtvidetAndeler, forrigeBarnasAndeler) = forrigeAndelerIkkeSmåbarnstillegg.partition { it.erUtvidet() }

    val nyeSmåbarnstilleggAndeler =
        småbarnstilleggGenerator.lagSmåbarnstilleggAndeler(
            perioderMedFullOvergangsstønad = nyePerioderMedFullOvergangsstønad,
            barnasAndeler = forrigeBarnasAndeler,
            utvidetAndeler = forrigeUtvidetAndeler,
            barnasAktørerOgFødselsdatoer = barnasAktørerOgFødselsdatoer,
        )

    return nyeSmåbarnstilleggAndeler.førerTilEndringIUtbetalingFraForrigeBehandling(
        forrigeAndeler = forrigeSmåbarnstilleggAndeler,
    )
}

private fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.førerTilEndringIUtbetalingFraForrigeBehandling(
    forrigeAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
): Boolean {
    val endringstidslinje =
        EndringIUtbetalingUtil.lagEndringIUtbetalingTidslinje(
            nåværendeAndeler = this.map { it.andel },
            forrigeAndeler = forrigeAndeler.map { it.andel },
        )

    return endringstidslinje.tilPerioder().any { it.verdi == true }
}

fun hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
    forrigeSmåbarnstilleggAndeler: List<AndelTilkjentYtelse>,
    nyeSmåbarnstilleggAndeler: List<AndelTilkjentYtelse>,
): Pair<List<MånedPeriode>, List<MånedPeriode>> {
    val forrigeAndelerTidslinje = forrigeSmåbarnstilleggAndeler.tilTidslinjeForSøkersYtelse(YtelseType.SMÅBARNSTILLEGG)
    val andelerTidslinje = nyeSmåbarnstilleggAndeler.tilTidslinjeForSøkersYtelse(YtelseType.SMÅBARNSTILLEGG)

    val nyeSmåbarnstilleggPerioder =
        forrigeAndelerTidslinje.kombinerMed(andelerTidslinje) { gammel, ny -> ny.takeIf { gammel == null } }

    val fjernedeSmåbarnstilleggPerioder =
        forrigeAndelerTidslinje.kombinerMed(andelerTidslinje) { gammel, ny -> gammel.takeIf { ny == null } }

    return Pair(nyeSmåbarnstilleggPerioder.tilMånedPerioder(), fjernedeSmåbarnstilleggPerioder.tilMånedPerioder())
}

private fun Tidslinje<AndelTilkjentYtelse>.tilMånedPerioder() =
    this.tilPerioderIkkeNull().map {
        MånedPeriode(
            fom = it.fom?.toYearMonth() ?: throw Feil("Fra og med-dato kan ikke være null"),
            tom = it.tom?.toYearMonth() ?: throw Feil("Til og med-dato kan ikke være null"),
        )
    }

fun kanAutomatiskIverksetteSmåbarnstillegg(
    innvilgedeMånedPerioder: List<MånedPeriode>,
    reduserteMånedPerioder: List<MånedPeriode>,
): Boolean =
    innvilgedeMånedPerioder.all {
        it.fom.isSameOrAfter(
            YearMonth.now(),
        )
    } &&
        reduserteMånedPerioder.all {
            it.fom.isSameOrAfter(
                YearMonth.now(),
            )
        }

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
    val barnasAndelerTidslinjer =
        barnasAndeler.groupBy { it.aktør }.mapValues { it.value.tilTidslinje() }

    val barnasAndelerUnder3ÅrTidslinje =
        barnasAndelerTidslinjer.map { (barnAktør, barnTidslinje) ->
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
