package no.nav.familie.ba.sak.kjerne.dokument

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.erSenereEnnInneværendeMåned
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.AvslagBrevPeriode
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.AvslagUtenPeriodeBrevPeriode
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.BrevPeriode
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.InnvilgelseBrevPeriode
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.OpphørBrevPeriode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Avslagsperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Opphørsperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiode

fun vedtaksperioderTilBrevPerioder(vedtaksperioder: List<Vedtaksperiode>,
                                   vedtakbegrunnelser: MutableSet<VedtakBegrunnelse>,
                                   grupperteAvslagsbegrunnelser: Map<NullablePeriode, List<String>>) = vedtaksperioder
        .foldRightIndexed(mutableListOf<BrevPeriode>()) { _, vedtaksperiode, acc ->
            if (vedtaksperiode is Utbetalingsperiode) {
                val barnasFødselsdatoer = finnAlleBarnsFødselsDatoerIUtbetalingsperiode(vedtaksperiode)

                val begrunnelser =
                        filtrerBegrunnelserForPeriodeOgVedtaksbegrunnelsetype(vedtakbegrunnelser, vedtaksperiode,
                                                                              listOf(VedtakBegrunnelseType.INNVILGELSE,
                                                                                     VedtakBegrunnelseType.REDUKSJON))

                if (begrunnelser.isNotEmpty()) {
                    acc.add(InnvilgelseBrevPeriode(
                            fom = vedtaksperiode.periodeFom.tilDagMånedÅr(),
                            tom = if (!vedtaksperiode.periodeTom.erSenereEnnInneværendeMåned())
                                vedtaksperiode.periodeTom.tilDagMånedÅr() else null,
                            belop = Utils.formaterBeløp(vedtaksperiode.utbetaltPerMnd),
                            antallBarn = vedtaksperiode.antallBarn.toString(),
                            barnasFodselsdager = barnasFødselsdatoer,
                            begrunnelser = begrunnelser,
                    ))
                }
            } else if (vedtaksperiode is Avslagsperiode) {
                val begrunnelserAvslag =
                        grupperteAvslagsbegrunnelser.getValue(NullablePeriode(vedtaksperiode.periodeFom,
                                                                              vedtaksperiode.periodeTom))

                if (begrunnelserAvslag.isNotEmpty()) {
                    if (vedtaksperiode.periodeFom != null) {
                        val vedtaksperiodeTom = vedtaksperiode.periodeTom ?: TIDENES_ENDE
                        acc.add(AvslagBrevPeriode(
                                fom = vedtaksperiode.periodeFom!!.tilDagMånedÅr(),
                                tom = if (!vedtaksperiodeTom.erSenereEnnInneværendeMåned())
                                    vedtaksperiodeTom.tilDagMånedÅr() else null,
                                begrunnelser = begrunnelserAvslag,
                        ))
                    } else {
                        acc.add(AvslagUtenPeriodeBrevPeriode(
                                begrunnelser = begrunnelserAvslag,
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
                    acc.add(OpphørBrevPeriode(
                            fom = vedtaksperiode.periodeFom.tilDagMånedÅr(),
                            tom = if (!vedtaksperiodeTom.erSenereEnnInneværendeMåned())
                                vedtaksperiodeTom.tilDagMånedÅr() else null,
                            begrunnelser = begrunnelserOpphør,
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
                .asSequence()
                .filter { it.fom == vedtaksperiode.periodeFom && it.tom == vedtaksperiode.periodeTom }
                .filter { vedtakBegrunnelseTyper.contains(it.begrunnelse.vedtakBegrunnelseType) }
                .sortedBy { it.opprettetTidspunkt }
                .sortedBy { it.begrunnelse.erFritekstBegrunnelse() }
                .map {
                    it.brevBegrunnelse?.lines() ?: listOf("Ikke satt")
                }
                .flatten()
                .toList()

fun finnAlleBarnsFødselsDatoerIUtbetalingsperiode(utbetalingsperiode: Utbetalingsperiode): String =
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