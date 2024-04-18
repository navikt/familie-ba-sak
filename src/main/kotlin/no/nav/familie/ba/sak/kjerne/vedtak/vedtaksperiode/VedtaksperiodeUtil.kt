package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.lagVertikaleSegmenter
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.hentUtbetalingsperiodeDetaljer
import no.nav.fpsak.tidsserie.LocalDateSegment
import java.time.LocalDate

fun oppdaterUtbetalingsperioderMedReduksjonFraForrigeBehandling(
    utbetalingsperioder: List<VedtaksperiodeMedBegrunnelser>,
    reduksjonsperioder: List<VedtaksperiodeMedBegrunnelser>,
): List<VedtaksperiodeMedBegrunnelser> {
    if (reduksjonsperioder.isNotEmpty()) {
        val utbetalingsperioderTidslinje = VedtaksperiodeMedBegrunnelserTidslinje(utbetalingsperioder)
        val reduksjonsperioderTidslinje = ReduksjonsperioderFraForrigeBehandlingTidslinje(reduksjonsperioder)

        val kombinertTidslinje =
            utbetalingsperioderTidslinje.kombinerMed(
                reduksjonsperioderTidslinje,
            ) { utbetalingsperiode, reduksjonsperiode ->
                when {
                    reduksjonsperiode != null && utbetalingsperiode == null -> reduksjonsperiode
                    reduksjonsperiode != null && utbetalingsperiode != null -> utbetalingsperiode.copy(type = reduksjonsperiode.type)
                    else -> utbetalingsperiode
                }
            }
        return kombinertTidslinje.lagVedtaksperioderMedBegrunnelser()
    }
    return utbetalingsperioder
}

fun validerSatsendring(
    fom: LocalDate?,
    harBarnMedSeksårsdagPåFom: Boolean,
) {
    val satsendring = SatsService.finnSatsendring(fom ?: TIDENES_MORGEN)

    if (satsendring.isEmpty() && !harBarnMedSeksårsdagPåFom) {
        throw FunksjonellFeil(
            melding = "Begrunnelsen stemmer ikke med satsendring.",
            frontendFeilmelding = "Begrunnelsen stemmer ikke med satsendring. Vennligst velg en annen begrunnelse.",
        )
    }
}

fun validerVedtaksperiodeMedBegrunnelser(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser) {
    if ((vedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.OPPHØR || vedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.AVSLAG) && vedtaksperiodeMedBegrunnelser.harFriteksterUtenStandardbegrunnelser()) {
        val fritekstUtenStandardbegrunnelserFeilmelding =
            "Fritekst kan kun brukes i kombinasjon med en eller flere begrunnelser. " + "Legg først til en ny begrunnelse eller fjern friteksten(e)."
        throw FunksjonellFeil(
            melding = fritekstUtenStandardbegrunnelserFeilmelding,
            frontendFeilmelding = fritekstUtenStandardbegrunnelserFeilmelding,
        )
    }
}

fun identifiserReduksjonsperioderFraSistIverksatteBehandling(
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    vedtak: Vedtak,
    utbetalingsperioder: List<VedtaksperiodeMedBegrunnelser>,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    opphørsperioder: List<VedtaksperiodeMedBegrunnelser>,
    aktørerIForrigePersonopplysningGrunnlag: List<Aktør>,
): List<VedtaksperiodeMedBegrunnelser> {
    val forrigeSegmenter = forrigeAndelerTilkjentYtelse.lagVertikaleSegmenter()

    // henter segmenter for personer som finnes i forrige behandling
    val nåværendeSegmenter =
        andelerTilkjentYtelse.filter {
            aktørerIForrigePersonopplysningGrunnlag.any { forrigeAktør -> forrigeAktør == it.aktør }
        }.lagVertikaleSegmenter()

    val segmenter =
        forrigeSegmenter.filterNot { (forrigeSegment, _) ->
            nåværendeSegmenter.any { (nyttSegment, _) ->
                forrigeSegment.fom == nyttSegment.fom && forrigeSegment.tom == nyttSegment.tom && forrigeSegment.value == nyttSegment.value
            }
        }
    val reduksjonsperioderFraInnvilgelsesTidspunkt =
        segmenter.filter { (forrigeSegment, _) ->
            nåværendeSegmenter.any { (nyttSegment, _) ->
                nyttSegment.overlapper(
                    forrigeSegment,
                )
            }
        }.toList().fold(emptyList<VedtaksperiodeMedBegrunnelser>()) { acc, (gammeltSegment, gammeltAndelerTyForSegment) ->
            val overlappendePerioder =
                nåværendeSegmenter.filter { (nåSegment, nåAndelTilkjentYtelserForSegment) ->
                    nåSegment.overlapper(gammeltSegment) &&
                        gammeltAndelerTyForSegment.any { gammelAndelTyForSegment ->
                            val fom = nåSegment.fom
                            nåAndelTilkjentYtelserForSegment.all { nåAndelTyForSegment ->
                                // Når en person mister utbetaling på et segment i behandling
                                !(nåAndelTyForSegment.aktør.aktørId == gammelAndelTyForSegment.aktør.aktørId && nåAndelTyForSegment.type == gammelAndelTyForSegment.type) &&
                                    // Når den personen som mister utbetaling ikke har en utbetaling av samme type i forrige måned
                                    utbetalingsperioder.none { utbetalingsperiode ->
                                        utbetalingsperiode.tom == fom.minusDays(1) &&
                                            utbetalingsperiode.hentUtbetalingsperiodeDetaljer(
                                                andelerTilkjentYtelse = andelerTilkjentYtelse,
                                                personopplysningGrunnlag = personopplysningGrunnlag,
                                            ).any {
                                                it.person.personIdent == gammelAndelTyForSegment.aktør.aktivFødselsnummer() && it.ytelseType == gammelAndelTyForSegment.type
                                            }
                                    }
                            }
                        }
                }.keys

            acc +
                overlappendePerioder.map { overlappendePeriode ->
                    VedtaksperiodeMedBegrunnelser(
                        vedtak = vedtak,
                        fom = utledFom(gammeltSegment, overlappendePeriode),
                        tom = utledTom(gammeltSegment, overlappendePeriode),
                        type = Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
                    )
                }
        }
    // opphørsperioder kan ikke være inkludert i reduksjonsperioder
    return reduksjonsperioderFraInnvilgelsesTidspunkt.filterNot { reduksjonsperiode ->
        opphørsperioder.any { it.fom == reduksjonsperiode.fom || it.tom == reduksjonsperiode.tom }
    }
}

private fun utledFom(
    gammeltSegment: LocalDateSegment<Int>,
    overlappendePeriode: LocalDateSegment<Int>,
) = if (gammeltSegment.fom > overlappendePeriode.fom) gammeltSegment.fom else overlappendePeriode.fom

private fun utledTom(
    gammeltSegment: LocalDateSegment<Int>,
    overlappendePeriode: LocalDateSegment<Int>,
) = if (gammeltSegment.tom > overlappendePeriode.tom) overlappendePeriode.tom else gammeltSegment.tom
