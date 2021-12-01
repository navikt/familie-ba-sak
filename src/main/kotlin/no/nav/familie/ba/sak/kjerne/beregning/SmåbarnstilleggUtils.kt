package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.erUlike
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
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
    barnasIdenterOgFødselsdatoer: List<Pair<String, LocalDate>>,
): Boolean {
    val (forrigeSøkersSmåbarnstilleggAndeler, forrigeSøkersAndreAndeler) = forrigeAndelerTilkjentYtelse.partition { it.erSmåbarnstillegg() }

    val nyeSmåbarnstilleggAndeler = småbarnstilleggBarnetrygdGenerator.lagSmåbarnstilleggAndeler(
        perioderMedFullOvergangsstønad = nyePerioderMedFullOvergangsstønad,
        andelerTilkjentYtelse = forrigeSøkersAndreAndeler,
        barnasIdenterOgFødselsdatoer = barnasIdenterOgFødselsdatoer,
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
    val vedtaksperiodeSomSkalOppdateresOgBegrunnelse: Pair<VedtaksperiodeMedBegrunnelser?, VedtakBegrunnelseSpesifikasjon>? =
        when {
            innvilgetMånedPeriode == null && redusertMånedPeriode == null -> null
            innvilgetMånedPeriode != null && redusertMånedPeriode == null -> {
                Pair(
                    vedtaksperioderMedBegrunnelser.find { it.fom?.toYearMonth() == innvilgetMånedPeriode.fom && it.type == Vedtaksperiodetype.UTBETALING },
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_SMÅBARNSTILLEGG
                )
            }
            innvilgetMånedPeriode == null && redusertMånedPeriode != null -> {
                Pair(
                    vedtaksperioderMedBegrunnelser.find { it.fom?.toYearMonth() == redusertMånedPeriode.fom && it.type == Vedtaksperiodetype.UTBETALING },
                    VedtakBegrunnelseSpesifikasjon.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD
                )
            }
            else -> null
        }

    val vedtaksperiodeSomSkalOppdateres = vedtaksperiodeSomSkalOppdateresOgBegrunnelse?.first
    if (vedtaksperiodeSomSkalOppdateres == null) {
        LoggerFactory.getLogger("secureLogger")
            .info(
                "Finner ikke aktuell periode å begrunne ved autovedtak småbarnstillegg. " +
                    "Perioder: ${vedtaksperioderMedBegrunnelser.map { "Periode(type=${it.type}, fom=${it.fom}, tom=${it.tom})" }}"
            )

        throw VedtaksperiodefinnerSmåbarnstilleggFeil("Finner ikke aktuell periode å begrunne ved autovedtak småbarnstillegg. Se securelogger for å periodene som ble generert.")
    }

    vedtaksperiodeSomSkalOppdateres.settBegrunnelser(
        vedtaksperiodeSomSkalOppdateres.begrunnelser.toList() + listOf(
            Vedtaksbegrunnelse(
                vedtaksperiodeMedBegrunnelser = vedtaksperiodeSomSkalOppdateres,
                personIdenter = emptyList(),
                vedtakBegrunnelseSpesifikasjon = vedtaksperiodeSomSkalOppdateresOgBegrunnelse.second
            )
        )
    )

    return vedtaksperiodeSomSkalOppdateres
}
