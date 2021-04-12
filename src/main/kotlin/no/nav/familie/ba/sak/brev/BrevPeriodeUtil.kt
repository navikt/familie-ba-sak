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
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.erSenereEnnInneværendeMåned
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.tilKortString

fun vedtaksperioderTilBrevPerioder(vedtaksperioder: List<Vedtaksperiode>,
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
            } else if (vedtaksperiode is Opphørsperiode) {
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


private fun filtrerBegrunnelserForPeriodeOgVedtaksbegrunnelsetype(vedtakBegrunnelser: MutableSet<VedtakBegrunnelse>,
                                                                  vedtaksperiode: Vedtaksperiode,
                                                                  vedtakBegrunnelseTyper: List<VedtakBegrunnelseType>) =
        vedtakBegrunnelser
                .filter { it.fom == vedtaksperiode.periodeFom && it.tom == vedtaksperiode.periodeTom }
                .filter { vedtakBegrunnelseTyper.contains(it.begrunnelse.vedtakBegrunnelseType) }
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