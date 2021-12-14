package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.lagVertikaleSegmenter
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.tilTriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import java.time.LocalDate
import java.time.YearMonth

fun hentVedtaksperioderMedBegrunnelserForEndredeUtbetalingsperioder(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    vedtak: Vedtak
) = andelerTilkjentYtelse.filter { it.endretUtbetalingAndeler.isNotEmpty() }.groupBy { it.prosent }
    .flatMap { (_, andeler) ->
        andeler.lagVertikaleSegmenter()
            .map { (segmenter, andelerForSegment) ->
                VedtaksperiodeMedBegrunnelser(
                    fom = segmenter.fom,
                    tom = segmenter.tom,
                    vedtak = vedtak,
                    type = Vedtaksperiodetype.ENDRET_UTBETALING
                ).also { vedtaksperiodeMedBegrunnelse ->
                    val endretUtbetalingAndeler = andelerForSegment.flatMap { it.endretUtbetalingAndeler }
                    vedtaksperiodeMedBegrunnelse.begrunnelser.addAll(
                        endretUtbetalingAndeler
                            .flatMap { it.vedtakBegrunnelseSpesifikasjoner }.toSet()
                            .map { vedtakBegrunnelseSpesifikasjon ->
                                Vedtaksbegrunnelse(
                                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelse,
                                    vedtakBegrunnelseSpesifikasjon = vedtakBegrunnelseSpesifikasjon,
                                    personIdenter = endretUtbetalingAndeler.filter {
                                        it.harVedtakBegrunnelseSpesifikasjon(vedtakBegrunnelseSpesifikasjon)
                                    }.mapNotNull { it.person?.aktør?.aktivFødselsnummer() }
                                )
                            }
                    )
                }
            }
    }

fun hentVedtaksperioderMedBegrunnelserForUtbetalingsperioder(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    vedtak: Vedtak
) = andelerTilkjentYtelse.filter { it.endretUtbetalingAndeler.isEmpty() }.lagVertikaleSegmenter()
    .map { (segmenter, _) ->
        VedtaksperiodeMedBegrunnelser(
            fom = segmenter.fom,
            tom = segmenter.tom,
            vedtak = vedtak,
            type = Vedtaksperiodetype.UTBETALING
        )
    }

fun validerSatsendring(fom: LocalDate?, harBarnMedSeksårsdagPåFom: Boolean) {
    val satsendring = SatsService
        .finnSatsendring(fom ?: TIDENES_MORGEN)

    if (satsendring.isEmpty() && !harBarnMedSeksårsdagPåFom) {
        throw FunksjonellFeil(
            melding = "Begrunnelsen stemmer ikke med satsendring.",
            frontendFeilmelding = "Begrunnelsen stemmer ikke med satsendring. Vennligst velg en annen begrunnelse."
        )
    }
}

fun kastFeilmeldingForBegrunnelserMedFeil(
    begrunnelserMedFeil: List<VedtakBegrunnelseSpesifikasjon>,
    sanityBegrunnelser: List<SanityBegrunnelse>
) {
    throw FunksjonellFeil(
        melding = "Begrunnelsen passer ikke til vilkårsvurderingen. For å rette opp, gå tilbake til " +
            "vilkårsvurderingen eller velg en annen begrunnelse.",
        frontendFeilmelding = "Begrunnelsen passer ikke til vilkårsvurderingen. For å rette opp, gå tilbake " +
            "til vilkårsvurderingen eller velg en annen begrunnelse.\n" +
            begrunnelserMedFeil.fold("") { acc, vedtakBegrunnelseSpesifikasjon ->
                val sanityBegrunnelse =
                    vedtakBegrunnelseSpesifikasjon
                        .tilSanityBegrunnelse(sanityBegrunnelser)
                        ?: error(
                            "Finner ikke begrunnelse med apiNavn ${vedtakBegrunnelseSpesifikasjon.sanityApiNavn} " +
                                "i Sanity"
                        )

                val vilkårsbeskrivelse =
                    sanityBegrunnelse.tilTriggesAv().vilkår.first().beskrivelse
                val tittel =
                    sanityBegrunnelse.navnISystem

                "$acc'$tittel' forventer vurdering på '$vilkårsbeskrivelse'"
            }
    )
}

fun validerVedtaksperiodeMedBegrunnelser(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser) {
    if ((
            vedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.OPPHØR ||
                vedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.AVSLAG
            ) &&
        vedtaksperiodeMedBegrunnelser.harFriteksterUtenStandardbegrunnelser()
    ) {
        val fritekstUtenStandardbegrunnelserFeilmelding =
            "Fritekst kan kun brukes i kombinasjon med en eller flere begrunnelser. " +
                "Legg først til en ny begrunnelse eller fjern friteksten(e)."
        throw FunksjonellFeil(
            melding = fritekstUtenStandardbegrunnelserFeilmelding,
            frontendFeilmelding = fritekstUtenStandardbegrunnelserFeilmelding
        )
    }

    if (vedtaksperiodeMedBegrunnelser.vedtak.behandling.resultat == BehandlingResultat.FORTSATT_INNVILGET &&
        vedtaksperiodeMedBegrunnelser.harFriteksterOgStandardbegrunnelser()
    ) {
        throw FunksjonellFeil(
            "Det ble sendt med både fritekst og begrunnelse. " +
                "Vedtaket skal enten ha fritekst eller bregrunnelse, men ikke begge deler."
        )
    }
}

/**
 * Brukes for opphør som har egen logikk dersom det er første periode.
 */
fun erFørsteVedtaksperiodePåFagsak(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    periodeFom: LocalDate?
): Boolean = !andelerTilkjentYtelse.any {
    it.stønadFom.isBefore(
        periodeFom?.toYearMonth() ?: TIDENES_MORGEN.toYearMonth()
    )
}

fun hentPersonerForAutovedtakBegrunnelse(
    vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon,
    barna: List<Person>,
    nåværendeUtbetalingsperiode: Utbetalingsperiode
): List<String> = when (vedtakBegrunnelseSpesifikasjon) {
    VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR,
    VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK ->
        hentBarnSomFyller18År(barna)

    VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR,
    VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK ->
        hentbarnMedSeksårsdagPåPeriode(nåværendeUtbetalingsperiode)

    else -> hentPersonIdenterFraUtbetalingsperiode(nåværendeUtbetalingsperiode)
}

fun hentBarnSomFyller18År(barna: List<Person>): List<String> {
    // Barn som har fylt 18 år har ingen utbetalingsperioder og må hentes fra persongrunnlaget.
    val fødselsMånedOgÅrForAlder18 = YearMonth.from(LocalDate.now()).minusYears(18)

    return barna.filter { barn ->
        barn.fødselsdato.toYearMonth().equals(fødselsMånedOgÅrForAlder18) ||
            barn.fødselsdato.toYearMonth().equals(fødselsMånedOgÅrForAlder18.plusMonths(1))
    }.map { it.aktør.aktivFødselsnummer() }
}
