package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Avslagsperiode
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Opphørsperiode
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiode
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import no.nav.familie.ba.sak.brev.domene.maler.BrevPeriode
import no.nav.familie.ba.sak.brev.domene.maler.PeriodeType
import no.nav.familie.ba.sak.common.*
import java.time.LocalDate

fun vedtaksperioderTilBrevPerioder(vedtaksperioder: List<Vedtaksperiode>,
                                   visOpphørsperioder: Boolean,
                                   vedtakbegrunnelser: MutableSet<VedtakBegrunnelse>,
                                   grupperteAvslagsbegrunnelser: Map<NullablePeriode, List<String>>) = vedtaksperioder
        .foldRightIndexed(mutableListOf<BrevPeriode>()) { idx, vedtaksperiode, acc ->
            if (vedtaksperiode is Utbetalingsperiode) {
                val barnasFødselsdatoer = finnAlleBarnsFødselsDatoerForPerioden(vedtaksperiode)

                val begrunnelser =
                        filtrerBegrunnelserForPeriodeOgVedtaksbegrunnelsetype(vedtakbegrunnelser, vedtaksperiode,
                                                                              listOf(VedtakBegrunnelseType.INNVILGELSE,
                                                                                     VedtakBegrunnelseType.REDUKSJON))

                if (begrunnelser.isNotEmpty()) {
                    acc.add(BrevPeriode(
                            fom = vedtaksperiode.periodeFom.tilDagMånedÅr(),
                            tom = if (!vedtaksperiode.periodeTom.erSenereEnnInneværendeMåned())
                                vedtaksperiode.periodeTom.tilDagMånedÅr() else null,
                            belop = Utils.formaterBeløp(vedtaksperiode.utbetaltPerMnd),
                            antallBarn = vedtaksperiode.antallBarn.toString(),
                            barnasFodselsdager = barnasFødselsdatoer,
                            begrunnelser = begrunnelser,
                            type = PeriodeType.INNVILGELSE
                    ))
                }

                /* Temporær løsning for å støtte begrunnelse av perioder som er opphørt eller avslått.
                * Begrunnelsen settes på den tidligere (før den opphøret- eller avslåtteperioden) innvilgte perioden.
                */
                if (!visOpphørsperioder) {
                    leggTilOpphørsperiodeMidlertidigLøsning(idx, vedtaksperioder, vedtakbegrunnelser, vedtaksperiode, acc)
                }
                /* Slutt temporær løsning */

            } else if (vedtaksperiode is Avslagsperiode) {
                val begrunnelserAvslag =
                        grupperteAvslagsbegrunnelser.getValue(NullablePeriode(vedtaksperiode.periodeFom,
                                                                              vedtaksperiode.periodeTom))

                if (begrunnelserAvslag.isNotEmpty()) {
                    if (vedtaksperiode.periodeFom != null) {
                        val vedtaksperiodeTom = vedtaksperiode.periodeTom ?: TIDENES_ENDE
                        acc.add(BrevPeriode(
                                fom = vedtaksperiode.periodeFom!!.tilDagMånedÅr(),
                                tom = if (!vedtaksperiodeTom.erSenereEnnInneværendeMåned())
                                    vedtaksperiodeTom.tilDagMånedÅr() else null,
                                begrunnelser = begrunnelserAvslag,
                                type = PeriodeType.AVSLAG
                        ))
                    } else {
                        acc.add(BrevPeriode(
                                fom = null,
                                tom = null,
                                begrunnelser = begrunnelserAvslag,
                                type = PeriodeType.AVSLAG_UTEN_PERIODE
                        ))
                    }
                }
            } else if (vedtaksperiode is Opphørsperiode && visOpphørsperioder) {
                val begrunnelserOpphør =
                        filtrerBegrunnelserForPeriodeOgVedtaksbegrunnelsetype(vedtakbegrunnelser,
                                                                              vedtaksperiode,
                                                                              listOf(VedtakBegrunnelseType.OPPHØR))

                val vedtaksperiodeTom = vedtaksperiode.periodeTom ?: TIDENES_ENDE
                if (begrunnelserOpphør.isNotEmpty()) {
                    acc.add(BrevPeriode(
                            fom = vedtaksperiode.periodeFom.tilDagMånedÅr(),
                            tom = if (!vedtaksperiodeTom.erSenereEnnInneværendeMåned())
                                vedtaksperiodeTom.tilDagMånedÅr() else null,
                            begrunnelser = begrunnelserOpphør,
                            type = PeriodeType.OPPHOR
                    ))
                }
            } else {
                throw Feil("Vedtaksperiode av typen ${vedtaksperiode.vedtaksperiodetype} er ikke støttet i brev")
            }
            acc
        }

private fun leggTilOpphørsperiodeMidlertidigLøsning(idx: Int,
                                                    vedtaksperioder: List<Vedtaksperiode>,
                                                    vedtakbegrunnelser: MutableSet<VedtakBegrunnelse>,
                                                    vedtaksperiode: Utbetalingsperiode,
                                                    acc: MutableList<BrevPeriode>) {
    val nesteUtbetalingsperiodeFom = if (idx < vedtaksperioder.lastIndex) {
        vedtaksperioder[idx + 1].periodeFom
    } else {
        null
    }

    val begrunnelserOpphør =
            filtrerBegrunnelserForPeriodeOgVedtaksbegrunnelsetype(vedtakbegrunnelser,
                                                                  vedtaksperiode,
                                                                  listOf(VedtakBegrunnelseType.OPPHØR))

    if (etterfølgesAvOpphørtEllerAvslåttPeriode(nesteUtbetalingsperiodeFom,
                                                vedtaksperiode.periodeTom) &&
        begrunnelserOpphør.isNotEmpty())

        acc.add(BrevPeriode(
                fom = vedtaksperiode.periodeTom.plusDays(1).tilDagMånedÅr(),
                tom = nesteUtbetalingsperiodeFom?.minusDays(1)?.tilDagMånedÅr(),
                begrunnelser = begrunnelserOpphør,
                type = PeriodeType.OPPHOR
        ))
}


private fun etterfølgesAvOpphørtEllerAvslåttPeriode(nesteUtbetalingsperiodeFom: LocalDate?,
                                                    vedtaksperiodeTom: LocalDate) =
        nesteUtbetalingsperiodeFom == null ||
        nesteUtbetalingsperiodeFom.erSenereEnnPåfølgendeDag(vedtaksperiodeTom)

private fun filtrerBegrunnelserForPeriodeOgVedtaksbegrunnelsetype(vedtakBegrunnelser: MutableSet<VedtakBegrunnelse>,
                                                                  vedtaksperiode: Vedtaksperiode,
                                                                  vedtakBegrunnelseTyper: List<VedtakBegrunnelseType>) =
        vedtakBegrunnelser
                .filter { it.fom == vedtaksperiode.periodeFom && it.tom == vedtaksperiode.periodeTom }
                .filter { vedtakBegrunnelseTyper.contains(it.begrunnelse.vedtakBegrunnelseType) }
                .sortedBy { it.begrunnelse.erFritekstBegrunnelse() }
                .map {
                    it.brevBegrunnelse?.lines() ?: listOf("Ikke satt")
                }
                .flatten()

private fun finnAlleBarnsFødselsDatoerForPerioden(utbetalingsperiode: Utbetalingsperiode) =
        Utils.slåSammen(utbetalingsperiode.utbetalingsperiodeDetaljer
                                .filter { utbetalingsperiodeDetalj ->
                                    utbetalingsperiodeDetalj.person.type == PersonType.BARN
                                }
                                .sortedBy { utbetalingsperiodeDetalj ->
                                    utbetalingsperiodeDetalj.person.fødselsdato
                                }
                                .map { utbetalingsperiodeDetalj ->
                                    utbetalingsperiodeDetalj.person.fødselsdato?.tilKortString() ?: ""
                                })