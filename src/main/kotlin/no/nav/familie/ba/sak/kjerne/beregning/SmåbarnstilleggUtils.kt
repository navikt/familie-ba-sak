package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.erUnder3ÅrTidslinje
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønadTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.erUlike
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærEtter
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.tilMåned
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class VedtaksperiodefinnerSmåbarnstilleggFeil(
    melding: String,
    override val frontendFeilmelding: String? = null,
    override val httpStatus: HttpStatus = HttpStatus.OK,
    override val throwable: Throwable? = null
) : Feil(
    melding,
    frontendFeilmelding,
    httpStatus,
    throwable
)

fun vedtakOmOvergangsstønadPåvirkerFagsak(
    småbarnstilleggBarnetrygdGenerator: SmåbarnstilleggBarnetrygdGenerator,
    nyePerioderMedFullOvergangsstønad: List<InternPeriodeOvergangsstønad>,
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    barnasAktørerOgFødselsdatoer: List<Pair<Aktør, LocalDate>>,
): Boolean {
    val (forrigeSøkersSmåbarnstilleggAndeler, forrigeSøkersAndreAndeler) = forrigeAndelerTilkjentYtelse.partition { it.erSmåbarnstillegg() }

    val nyeSmåbarnstilleggAndeler = småbarnstilleggBarnetrygdGenerator.lagSmåbarnstilleggAndelerGammel(
        perioderMedFullOvergangsstønad = nyePerioderMedFullOvergangsstønad,
        andelerTilkjentYtelse = forrigeSøkersAndreAndeler,
        barnasAktørerOgFødselsdatoer = barnasAktørerOgFødselsdatoer,
    )

    return forrigeSøkersSmåbarnstilleggAndeler.erUlike(nyeSmåbarnstilleggAndeler)
}

fun hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
    forrigeSmåbarnstilleggAndeler: List<AndelTilkjentYtelse>,
    nyeSmåbarnstilleggAndeler: List<AndelTilkjentYtelse>,
): Pair<List<MånedPeriode>, List<MånedPeriode>> {
    val forrigeAndelerTidslinje = LocalDateTimeline(
        forrigeSmåbarnstilleggAndeler.map {
            LocalDateSegment(
                it.stønadFom.førsteDagIInneværendeMåned(),
                it.stønadTom.sisteDagIInneværendeMåned(),
                it
            )
        }
    )
    val andelerTidslinje = LocalDateTimeline(
        nyeSmåbarnstilleggAndeler.map {
            LocalDateSegment(
                it.stønadFom.førsteDagIInneværendeMåned(),
                it.stønadTom.sisteDagIInneværendeMåned(),
                it
            )
        }
    )

    val segmenterLagtTil = andelerTidslinje.disjoint(forrigeAndelerTidslinje)
    val segmenterFjernet = forrigeAndelerTidslinje.disjoint(andelerTidslinje)

    return Pair(
        segmenterLagtTil.toSegments().map { MånedPeriode(fom = it.fom.toYearMonth(), tom = it.tom.toYearMonth()) },
        segmenterFjernet.toSegments().map { MånedPeriode(fom = it.fom.toYearMonth(), tom = it.tom.toYearMonth()) }
    )
}

fun kanAutomatiskIverksetteSmåbarnstillegg(
    innvilgedeMånedPerioder: List<MånedPeriode>,
    reduserteMånedPerioder: List<MånedPeriode>
): Boolean {
    // Kan ikke automatisk innvilge perioder mer enn en måned frem i tid
    if ((innvilgedeMånedPerioder + reduserteMånedPerioder).any {
        it.fom.isAfter(
                YearMonth.now().nesteMåned()
            )
    }
    ) return false

    return innvilgedeMånedPerioder.all {
        it.fom.isSameOrAfter(
            YearMonth.now()
        )
    } && reduserteMånedPerioder.all {
        it.fom.isSameOrAfter(
            YearMonth.now()
        )
    }
}

fun finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse(
    innvilgetMånedPeriode: MånedPeriode?,
    redusertMånedPeriode: MånedPeriode?,
    vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
): VedtaksperiodeMedBegrunnelser {
    val vedtaksperiodeSomSkalOppdateresOgBegrunnelse: Pair<VedtaksperiodeMedBegrunnelser?, Standardbegrunnelse>? =
        when {
            innvilgetMånedPeriode == null && redusertMånedPeriode == null -> null
            innvilgetMånedPeriode != null && redusertMånedPeriode == null -> {
                Pair(
                    vedtaksperioderMedBegrunnelser.find { it.fom?.toYearMonth() == innvilgetMånedPeriode.fom && it.type == Vedtaksperiodetype.UTBETALING },
                    Standardbegrunnelse.INNVILGET_SMÅBARNSTILLEGG
                )
            }
            innvilgetMånedPeriode == null && redusertMånedPeriode != null -> {
                Pair(
                    vedtaksperioderMedBegrunnelser.find { it.fom?.toYearMonth() == redusertMånedPeriode.fom && it.type == Vedtaksperiodetype.UTBETALING },
                    Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD
                )
            }
            else -> null
        }

    val vedtaksperiodeSomSkalOppdateres = vedtaksperiodeSomSkalOppdateresOgBegrunnelse?.first
    if (vedtaksperiodeSomSkalOppdateres == null) {
        LoggerFactory.getLogger("secureLogger")
            .info(
                "Finner ikke aktuell periode å begrunne ved autovedtak småbarnstillegg.\n" +
                    "Innvilget periode: $innvilgetMånedPeriode.\n" +
                    "Redusert periode: $redusertMånedPeriode.\n" +
                    "Perioder: ${vedtaksperioderMedBegrunnelser.map { "Periode(type=${it.type}, fom=${it.fom}, tom=${it.tom})" }}"
            )

        throw VedtaksperiodefinnerSmåbarnstilleggFeil("Finner ikke aktuell periode å begrunne ved autovedtak småbarnstillegg. Se securelogger for å periodene som ble generert.")
    }

    vedtaksperiodeSomSkalOppdateres.settBegrunnelser(
        vedtaksperiodeSomSkalOppdateres.begrunnelser.toList() + listOf(
            Vedtaksbegrunnelse(
                vedtaksperiodeMedBegrunnelser = vedtaksperiodeSomSkalOppdateres,
                standardbegrunnelse = vedtaksperiodeSomSkalOppdateresOgBegrunnelse.second
            )
        )
    )

    return vedtaksperiodeSomSkalOppdateres
}

fun kombinerBarnasTidslinjerTilUnder3ÅrResultat(
    alleAndelerForBarnUnder3År: Iterable<AndelTilkjentYtelse>
): BarnSinRettTilSmåbarnstillegg? {
    val høyesteProsentIPeriode = alleAndelerForBarnUnder3År.maxOfOrNull { it.prosent }

    return when (høyesteProsentIPeriode) {
        null -> null
        BigDecimal.ZERO -> BarnSinRettTilSmåbarnstillegg.UNDER_3_ÅR_NULLUTBETALING
        else -> BarnSinRettTilSmåbarnstillegg.UNDER_3_ÅR_UTBETALING
    }
}

fun lagTidslinjeForPerioderMedBarnSomGirRettTilSmåbarnstillegg(
    barnasAndeler: List<AndelTilkjentYtelse>,
    barnasAktørerOgFødselsdatoer: List<Pair<Aktør, LocalDate>>
): Tidslinje<BarnSinRettTilSmåbarnstillegg, Måned> {
    val barnasAndelerTidslinjer = barnasAndeler.groupBy { it.aktør }.mapValues { AndelTilkjentYtelseTidslinje(it.value) }

    val barnasAndelerUnder3ÅrTidslinje = barnasAndelerTidslinjer.map { (barnAktør, barnTidslinje) ->
        val barnetsFødselsdato = barnasAktørerOgFødselsdatoer.find { it.first == barnAktør }?.second ?: throw Feil("Kan ikke beregne småbarnstillegg for et barn som ikke har fødselsdato.")

        val under3ÅrTidslinje = erUnder3ÅrTidslinje(barnetsFødselsdato)

        barnTidslinje.beskjærEtter(under3ÅrTidslinje)
    }

    return barnasAndelerUnder3ÅrTidslinje.kombinerUtenNull { kombinerBarnasTidslinjerTilUnder3ÅrResultat(it) }.filtrerIkkeNull()
}

data class SmåbarnstilleggPeriode(
    val overgangsstønadPeriode: InternPeriodeOvergangsstønad,
    val prosent: BigDecimal
)

fun kombinerAlleTidslinjerTilProsentTidslinje(
    perioderMedFullOvergangsstønadTidslinje: InternPeriodeOvergangsstønadTidslinje,
    utvidetBarnetrygdTidslinje: AndelTilkjentYtelseTidslinje,
    barnSomGirRettTilSmåbarnstilleggTidslinje: Tidslinje<BarnSinRettTilSmåbarnstillegg, Måned>
): Tidslinje<SmåbarnstilleggPeriode, Måned> {
    return perioderMedFullOvergangsstønadTidslinje
        .tilMåned { kombinatorInternPeriodeOvergangsstønadDagTilMåned(it) }
        .kombinerMed(
            tidslinjeB = utvidetBarnetrygdTidslinje,
            tidslinjeC = barnSomGirRettTilSmåbarnstilleggTidslinje
        ) { overgangsstønad, utvidet, under3År ->
            if (overgangsstønad == null || utvidet == null || under3År == null) null
            else if (utvidet.prosent > BigDecimal.ZERO && under3År == BarnSinRettTilSmåbarnstillegg.UNDER_3_ÅR_UTBETALING) SmåbarnstilleggPeriode(overgangsstønad, BigDecimal(100))
            else if (utvidet.prosent == BigDecimal.ZERO || under3År == BarnSinRettTilSmåbarnstillegg.UNDER_3_ÅR_NULLUTBETALING) SmåbarnstilleggPeriode(overgangsstønad, BigDecimal.ZERO)
            else null
        }
        .filtrerIkkeNull()
}

/**
 * EF sender alltid overgangsstønad-perioder som gjelder hele måneder, men formatet vi får er på LocalDate
 * Returverdier:
 * True - Det finnes minst 1 dag i måneden hvor søker får overgangsstønad
 * False - Søker får ikke overgangsstønad noen dager den måneden
 */
fun kombinatorInternPeriodeOvergangsstønadDagTilMåned(dagverdier: List<InternPeriodeOvergangsstønad?>): InternPeriodeOvergangsstønad? {
    val x = dagverdier.filterNotNull()
    return if (x.isEmpty()) null
    else x.first()
}

enum class BarnSinRettTilSmåbarnstillegg {
    UNDER_3_ÅR_UTBETALING,
    UNDER_3_ÅR_NULLUTBETALING
}

fun validerUtvidetOgBarnasAndeler(
    utvidetAndeler: List<AndelTilkjentYtelse>,
    barnasAndeler: List<AndelTilkjentYtelse>
) {
    if (utvidetAndeler.any { !it.erUtvidet() }) throw Feil("Det finnes andre ytelser enn utvidet blandt utvidet-andelene")
    if (barnasAndeler.any { it.erSøkersAndel() }) throw Feil("Finner andeler for søker blandt barnas andeler")
}
