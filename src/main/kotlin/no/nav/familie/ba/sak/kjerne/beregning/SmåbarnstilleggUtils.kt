package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.erUlike
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import java.time.LocalDate

fun vedtakOmOvergangsstønadPåvirkerFagsak(
    småbarnstilleggBarnetrygdGenerator: SmåbarnstilleggBarnetrygdGenerator,
    nyePerioderMedFullOvergangsstønad: List<PeriodeOvergangsstønad>,
    forrigeSøkersAndeler: List<AndelTilkjentYtelse>,
    barnasFødselsdatoer: List<LocalDate>
): Boolean {
    val (forrigeSøkersSmåbarnstilleggAndeler, forrigeSøkersAndreAndeler) = forrigeSøkersAndeler.partition { it.erSmåbarnstillegg() }

    val nyeSmåbarnstilleggAndeler = småbarnstilleggBarnetrygdGenerator.lagSmåbarnstilleggAndeler(
        perioderMedFullOvergangsstønad = nyePerioderMedFullOvergangsstønad,
        andelerSøker = forrigeSøkersAndreAndeler,
        barnasFødselsdatoer = barnasFødselsdatoer
    )

    return forrigeSøkersSmåbarnstilleggAndeler.erUlike(nyeSmåbarnstilleggAndeler)
}

fun utledVedtaksperioderTilAutovedtakVedOSVedtak(
    vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
    vedtak: Vedtak,
    forrigeSmåbarnstilleggAndeler: List<AndelTilkjentYtelse>,
    nyeSmåbarnstilleggAndeler: List<AndelTilkjentYtelse>
): List<VedtaksperiodeMedBegrunnelser> {
    val reduksjonsperioder = hentReduserteAndelerSmåbarnstillegg(
        forrigeSmåbarnstilleggAndeler = forrigeSmåbarnstilleggAndeler,
        nyeSmåbarnstilleggAndeler = nyeSmåbarnstilleggAndeler
    )
    val innvilgelsesperioder =
        nyeSmåbarnstilleggAndeler.map { MånedPeriode(fom = it.stønadFom, tom = it.stønadTom) }

    return genererVedtaksperioderMedBegrunnelser(
        vedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
        vedtak = vedtak,
        søkersIdent = vedtak.behandling.fagsak.hentAktivIdent().ident,
        månedPerioder = reduksjonsperioder,
        vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
    ) + genererVedtaksperioderMedBegrunnelser(
        vedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
        vedtak = vedtak,
        søkersIdent = vedtak.behandling.fagsak.hentAktivIdent().ident,
        månedPerioder = innvilgelsesperioder,
        vedtaksperiodetype = Vedtaksperiodetype.UTBETALING
    )
}

fun hentReduserteAndelerSmåbarnstillegg(
    forrigeSmåbarnstilleggAndeler: List<AndelTilkjentYtelse>,
    nyeSmåbarnstilleggAndeler: List<AndelTilkjentYtelse>,
): List<MånedPeriode> {
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

    val segmenterFjernet = forrigeAndelerTidslinje.disjoint(andelerTidslinje)

    return segmenterFjernet.toSegments().map { MånedPeriode(fom = it.fom.toYearMonth(), tom = it.tom.toYearMonth()) }
}

fun genererVedtaksperioderMedBegrunnelser(
    vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
    vedtak: Vedtak,
    søkersIdent: String,
    månedPerioder: List<MånedPeriode>,
    vedtaksperiodetype: Vedtaksperiodetype
): List<VedtaksperiodeMedBegrunnelser> {
    val vedtakBegrunnelseSpesifikasjon = hentVedtakBegrunnelseSpesifikasjonForSmåbarnstillegg(vedtaksperiodetype)

    return månedPerioder.map { månedPeriode ->
        val vedtaksperiodeMedBegrunnelser =
            vedtaksperioderMedBegrunnelser.find {
                it.fom == månedPeriode.fom.førsteDagIInneværendeMåned() &&
                    it.tom == månedPeriode.tom.sisteDagIInneværendeMåned() &&
                    it.type == vedtaksperiodetype
            }

        if (vedtaksperiodeMedBegrunnelser != null) {
            vedtaksperiodeMedBegrunnelser.settBegrunnelser(
                (
                    vedtaksperiodeMedBegrunnelser.begrunnelser +
                        Vedtaksbegrunnelse(
                            vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                            vedtakBegrunnelseSpesifikasjon = vedtakBegrunnelseSpesifikasjon,
                            personIdenter = listOf(søkersIdent)
                        )
                    ).toList()
            )

            vedtaksperiodeMedBegrunnelser
        } else {
            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = månedPeriode.fom.førsteDagIInneværendeMåned(),
                tom = månedPeriode.tom.sisteDagIInneværendeMåned(),
                type = vedtaksperiodetype
            )
                .apply {
                    begrunnelser.add(
                        Vedtaksbegrunnelse(
                            vedtaksperiodeMedBegrunnelser = this,
                            vedtakBegrunnelseSpesifikasjon = vedtakBegrunnelseSpesifikasjon,
                            personIdenter = listOf(søkersIdent)
                        )
                    )
                }
        }
    }
}

fun hentVedtakBegrunnelseSpesifikasjonForSmåbarnstillegg(vedtaksperiodetype: Vedtaksperiodetype) =
    if (vedtaksperiodetype == Vedtaksperiodetype.UTBETALING) VedtakBegrunnelseSpesifikasjon.INNVILGET_SMÅBARNSTILLEGG else VedtakBegrunnelseSpesifikasjon.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD
