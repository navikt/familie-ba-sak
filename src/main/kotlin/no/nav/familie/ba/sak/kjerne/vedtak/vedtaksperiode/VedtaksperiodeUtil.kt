package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.erDagenFør
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.hentUtvidetScenarioForEndringsperiode
import no.nav.familie.ba.sak.kjerne.beregning.domene.lagVertikaleSegmenter
import no.nav.familie.ba.sak.kjerne.brev.UtvidetScenarioForEndringsperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertEndretAndel
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertRestPersonResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertPersonResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.tilTriggesAv
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.MinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.MinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.tilMinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.tilMinimertePersoner
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.triggesForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

fun hentVedtaksperioderMedBegrunnelserForEndredeUtbetalingsperioder(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    vedtak: Vedtak,
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
                                )
                            }
                    )
                }
            }
    }

@Deprecated("Bruk hentVedtaksperioderMedBegrunnelserForEndredeUtbetalingsperioder")
fun hentVedtaksperioderMedBegrunnelserForUtbetalingsperioderGammel(
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

fun hentVedtaksperioderMedBegrunnelserForUtbetalingsperioder(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    vedtak: Vedtak
) = andelerTilkjentYtelse.lagVertikaleSegmenter()
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

fun identifiserReduksjonsperioder(
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    vedtak: Vedtak,
    opphørsperioder: List<VedtaksperiodeMedBegrunnelser>
): List<VedtaksperiodeMedBegrunnelser> {
    val forrigeSegmenter = forrigeAndelerTilkjentYtelse.lagVertikaleSegmenter().keys
    val nåværendeSegmenter = andelerTilkjentYtelse.lagVertikaleSegmenter().keys
    val segmenter = forrigeSegmenter.filterNot {
        nåværendeSegmenter.any { segment -> it.fom == segment.fom && it.tom == segment.tom && it.value == segment.value }
    }
    val reduksjonsperioder = mutableListOf<VedtaksperiodeMedBegrunnelser>()
    segmenter.filter { nåværendeSegmenter.any { segment -> segment.overlapper(it) } }
        .forEach {
            val overlappendePerioder =
                nåværendeSegmenter.filter { nåSegment -> nåSegment.overlapper(it) && nåSegment.value < it.value }
            overlappendePerioder.forEach { overlappendePeriode ->
                reduksjonsperioder.add(
                    VedtaksperiodeMedBegrunnelser(
                        vedtak = vedtak,
                        fom = if (it.fom > overlappendePeriode.fom) it.fom else overlappendePeriode.fom,
                        tom = if (it.tom > overlappendePeriode.tom) overlappendePeriode.tom else it.tom,
                        type = Vedtaksperiodetype.REDUKSJON,
                    )
                )
            }
        }

    // opphørsperioder kan ikke være inkludert i reduksjonsperioder
    return reduksjonsperioder.filterNot { reduksjonsperiode ->
        opphørsperioder.any { it.fom == reduksjonsperiode.fom || it.tom == reduksjonsperiode.tom }
    }
}

fun finnOgOppdaterOverlappendeUtbetalingsperiode(
    utbetalingsperioder: List<VedtaksperiodeMedBegrunnelser>,
    reduksjonsperioder: List<VedtaksperiodeMedBegrunnelser>
): List<VedtaksperiodeMedBegrunnelser> {
    val overlappendePerioder =
        utbetalingsperioder.filter {
            reduksjonsperioder.any { reduksjonsperiode ->
                reduksjonsperiode.fom!!.isSameOrAfter(it.fom!!) && reduksjonsperiode.tom!!.isSameOrBefore(
                    it.tom!!
                )
            }
        }
    val oppdatertUtbetalingsperioder = mutableListOf<VedtaksperiodeMedBegrunnelser>()
    utbetalingsperioder.forEach {
        val overlappendePeriode =
            overlappendePerioder.firstOrNull { periode -> it.fom == periode.fom && it.tom == periode.tom }
        if (overlappendePeriode != null) {
            oppdatertUtbetalingsperioder.addAll(
                reduksjonsperioder.filter { reduksjonsperiode ->
                    reduksjonsperiode.fom!! >= overlappendePeriode.fom &&
                        reduksjonsperiode.tom!! <= overlappendePeriode.tom
                }
            )
        } else {
            oppdatertUtbetalingsperioder.add(it)
        }
    }
    return oppdatertUtbetalingsperioder.sortedBy { it.fom }
}

fun hentGyldigeBegrunnelserForVedtaksperiode(
    utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
    sanityBegrunnelser: List<SanityBegrunnelse>,
    persongrunnlag: PersonopplysningGrunnlag,
    vilkårsvurdering: Vilkårsvurdering,
    aktørIderMedUtbetaling: List<String>,
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    erIngenOverlappVedtaksperiodeToggelPå: Boolean
) = hentGyldigeBegrunnelserForVedtaksperiodeMinimert(
    minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(),
    sanityBegrunnelser = sanityBegrunnelser,
    minimertePersoner = persongrunnlag.tilMinimertePersoner(),
    minimertePersonresultater = vilkårsvurdering.personResultater
        .map { it.tilMinimertPersonResultat() },
    aktørIderMedUtbetaling = aktørIderMedUtbetaling,
    minimerteEndredeUtbetalingAndeler = endretUtbetalingAndeler
        .map { it.tilMinimertEndretUtbetalingAndel() },
    erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak(
        andelerTilkjentYtelse,
        utvidetVedtaksperiodeMedBegrunnelser.fom
    ),
    ytelserForSøkerForrigeMåned = hentYtelserForSøkerForrigeMåned(
        andelerTilkjentYtelse,
        utvidetVedtaksperiodeMedBegrunnelser
    ),
    utvidetScenarioForEndringsperiode = andelerTilkjentYtelse
        .hentUtvidetScenarioForEndringsperiode(
            utvidetVedtaksperiodeMedBegrunnelser.hentMånedPeriode()
        ),
    erIngenOverlappVedtaksperiodeToggelPå = erIngenOverlappVedtaksperiodeToggelPå
)

fun hentGyldigeBegrunnelserForVedtaksperiodeMinimert(
    minimertVedtaksperiode: MinimertVedtaksperiode,
    sanityBegrunnelser: List<SanityBegrunnelse>,
    minimertePersoner: List<MinimertPerson>,
    minimertePersonresultater: List<MinimertRestPersonResultat>,
    aktørIderMedUtbetaling: List<String>,
    minimerteEndredeUtbetalingAndeler: List<MinimertEndretAndel>,
    erFørsteVedtaksperiodePåFagsak: Boolean,
    ytelserForSøkerForrigeMåned: List<YtelseType>,
    utvidetScenarioForEndringsperiode: UtvidetScenarioForEndringsperiode,
    erIngenOverlappVedtaksperiodeToggelPå: Boolean,
): List<VedtakBegrunnelseSpesifikasjon> {
    val tillateBegrunnelserForVedtakstype = VedtakBegrunnelseSpesifikasjon.values()
        .filter {
            minimertVedtaksperiode
                .type
                .tillatteBegrunnelsestyper(erIngenOverlappVedtaksperiodeToggelPå)
                .contains(it.vedtakBegrunnelseType)
        }

    return when (minimertVedtaksperiode.type) {
        Vedtaksperiodetype.FORTSATT_INNVILGET,
        Vedtaksperiodetype.AVSLAG -> tillateBegrunnelserForVedtakstype
        Vedtaksperiodetype.REDUKSJON -> velgRedusertBegrunnelser(
            sanityBegrunnelser,
            minimertVedtaksperiode,
            minimertePersonresultater,
            minimertePersoner,
            aktørIderMedUtbetaling,
            minimerteEndredeUtbetalingAndeler,
            erFørsteVedtaksperiodePåFagsak,
            ytelserForSøkerForrigeMåned,
            utvidetScenarioForEndringsperiode,
            erIngenOverlappVedtaksperiodeToggelPå
        )
        else -> {
            velgUtbetalingsbegrunnelser(
                tillateBegrunnelserForVedtakstype,
                sanityBegrunnelser,
                minimertVedtaksperiode,
                minimertePersonresultater,
                minimertePersoner,
                aktørIderMedUtbetaling,
                minimerteEndredeUtbetalingAndeler,
                erFørsteVedtaksperiodePåFagsak,
                ytelserForSøkerForrigeMåned,
                utvidetScenarioForEndringsperiode,
                erIngenOverlappVedtaksperiodeToggelPå
            )
        }
    }
}

private fun velgRedusertBegrunnelser(
    sanityBegrunnelser: List<SanityBegrunnelse>,
    minimertVedtaksperiode: MinimertVedtaksperiode,
    minimertePersonresultater: List<MinimertRestPersonResultat>,
    minimertePersoner: List<MinimertPerson>,
    aktørIderMedUtbetaling: List<String>,
    minimerteEndredeUtbetalingAndeler: List<MinimertEndretAndel>,
    erFørsteVedtaksperiodePåFagsak: Boolean,
    ytelserForSøkerForrigeMåned: List<YtelseType>,
    utvidetScenarioForEndringsperiode: UtvidetScenarioForEndringsperiode,
    erIngenOverlappVedtaksperiodeToggelPå: Boolean
): List<VedtakBegrunnelseSpesifikasjon> {
    val redusertBegrunnelser = VedtakBegrunnelseSpesifikasjon.begrunnelserForRedusertPerioderFraInnvilgelsestidspunkt()
    if (minimertVedtaksperiode.utbetalingsperioder.any { it.utbetaltPerMnd > 0 }) {
        val tillateBegrunnelserForVedtakstype = VedtakBegrunnelseSpesifikasjon.values()
            .filter { it.vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGET }
        val utbetalingsbegrunnelser = velgUtbetalingsbegrunnelser(
            tillateBegrunnelserForVedtakstype,
            sanityBegrunnelser,
            minimertVedtaksperiode,
            minimertePersonresultater,
            minimertePersoner,
            aktørIderMedUtbetaling,
            minimerteEndredeUtbetalingAndeler,
            erFørsteVedtaksperiodePåFagsak,
            ytelserForSøkerForrigeMåned,
            utvidetScenarioForEndringsperiode,
            erIngenOverlappVedtaksperiodeToggelPå
        )
        return redusertBegrunnelser + utbetalingsbegrunnelser
    }
    return redusertBegrunnelser
}

private fun velgUtbetalingsbegrunnelser(
    tillateBegrunnelserForVedtakstype: List<VedtakBegrunnelseSpesifikasjon>,
    sanityBegrunnelser: List<SanityBegrunnelse>,
    minimertVedtaksperiode: MinimertVedtaksperiode,
    minimertePersonresultater: List<MinimertRestPersonResultat>,
    minimertePersoner: List<MinimertPerson>,
    aktørIderMedUtbetaling: List<String>,
    minimerteEndredeUtbetalingAndeler: List<MinimertEndretAndel>,
    erFørsteVedtaksperiodePåFagsak: Boolean,
    ytelserForSøkerForrigeMåned: List<YtelseType>,
    utvidetScenarioForEndringsperiode: UtvidetScenarioForEndringsperiode,
    erIngenOverlappVedtaksperiodeToggelPå: Boolean
): List<VedtakBegrunnelseSpesifikasjon> {
    val standardbegrunnelser: MutableSet<VedtakBegrunnelseSpesifikasjon> =
        tillateBegrunnelserForVedtakstype
            .filter { it.vedtakBegrunnelseType != VedtakBegrunnelseType.FORTSATT_INNVILGET }
            .filter { it.tilSanityBegrunnelse(sanityBegrunnelser)?.tilTriggesAv()?.valgbar ?: false }
            .fold(mutableSetOf()) { acc, standardBegrunnelse ->
                if (standardBegrunnelse.triggesForPeriode(
                        minimertVedtaksperiode = minimertVedtaksperiode,
                        minimertePersonResultater = minimertePersonresultater,
                        minimertePersoner = minimertePersoner,
                        aktørIderMedUtbetaling = aktørIderMedUtbetaling,
                        minimerteEndredeUtbetalingAndeler = minimerteEndredeUtbetalingAndeler,
                        sanityBegrunnelser = sanityBegrunnelser,
                        erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
                        ytelserForSøkerForrigeMåned = ytelserForSøkerForrigeMåned,
                        utvidetScenarioForEndringsperiode = utvidetScenarioForEndringsperiode,
                        erIngenOverlappVedtaksperiodeToggelPå = erIngenOverlappVedtaksperiodeToggelPå,
                    )
                ) {
                    acc.add(standardBegrunnelse)
                }

                acc
            }

    val fantIngenbegrunnelserOgSkalDerforBrukeFortsattInnvilget =
        minimertVedtaksperiode.type == Vedtaksperiodetype.UTBETALING &&
            standardbegrunnelser.isEmpty()

    return if (fantIngenbegrunnelserOgSkalDerforBrukeFortsattInnvilget) {
        tillateBegrunnelserForVedtakstype
            .filter { it.vedtakBegrunnelseType == VedtakBegrunnelseType.FORTSATT_INNVILGET }
    } else {
        standardbegrunnelser.toList()
    }
}

fun hentYtelserForSøkerForrigeMåned(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser
) = andelerTilkjentYtelse.filter {
    it.type.erKnyttetTilSøker() &&
        it.stønadTom.sisteDagIInneværendeMåned()
            .erDagenFør(utvidetVedtaksperiodeMedBegrunnelser.fom)
}.map { it.type }
