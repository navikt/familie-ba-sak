package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Opphørsperiode
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiode
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import no.nav.familie.ba.sak.brev.domene.maler.BrevPeriode
import no.nav.familie.ba.sak.brev.domene.maler.PeriodeType
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.erSenereEnnInneværendeMåned
import no.nav.familie.ba.sak.common.erSenereEnnPåfølgendeDag
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.config.FeatureToggleService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BrevPeriodeService(
        private val vedtaksperiodeService: VedtaksperiodeService,
        private val featureToggleService: FeatureToggleService
) {

    fun hentBrevPerioder(vedtak: Vedtak): List<BrevPeriode> {
        val vedtaksperioder = vedtaksperiodeService.hentVedtaksperioder(vedtak.behandling)
        val visOpphørsperioder = featureToggleService.isEnabled("familie-ba-sak.behandling.vis-opphoersperioder")

        return vedtaksperioder
                .foldRightIndexed(mutableListOf<BrevPeriode>()) { idx, vedtaksperiode, acc ->
                    if (vedtaksperiode is Utbetalingsperiode) {
                        /* Temporær løsning for å støtte begrunnelse av perioder som er opphørt eller avslått.
                        * Begrunnelsen settes på den tidligere (før den opphøret- eller avslåtteperioden) innvilgte perioden.
                        */
                        if (!visOpphørsperioder) {
                            val nesteUtbetalingsperiodeFom = if (idx < vedtaksperioder.lastIndex) {
                                vedtaksperioder[idx + 1].periodeFom
                            } else {
                                null
                            }

                            val begrunnelserOpphør =
                                    filtrerBegrunnelserForPeriodeOgVedtaksbegrunnelsetype(vedtak,
                                                                                          vedtaksperiode,
                                                                                          listOf(VedtakBegrunnelseType.OPPHØR))

                            if (etterfølgesAvOpphørtEllerAvslåttPeriode(nesteUtbetalingsperiodeFom,
                                                                        vedtaksperiode.periodeTom) &&
                                begrunnelserOpphør.isNotEmpty())

                                acc.add(BrevPeriode(
                                        fom = vedtaksperiode.periodeTom.plusDays(1).tilDagMånedÅr(),
                                        tom = nesteUtbetalingsperiodeFom?.minusDays(1)?.tilDagMånedÅr(),
                                        belop = "0",
                                        antallBarn = "0",
                                        barnasFodselsdager = "",
                                        begrunnelser = begrunnelserOpphør,
                                        type = PeriodeType.OPPHOR
                                ))
                        }
                        /* Slutt temporær løsning */

                        val barnasFødselsdatoer = finnAlleBarnsFødselsDatoerForPerioden(vedtaksperiode)

                        val begrunnelser =
                                filtrerBegrunnelserForPeriodeOgVedtaksbegrunnelsetype(vedtak, vedtaksperiode,
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
                    } else if (vedtaksperiode is Opphørsperiode && visOpphørsperioder) {
                        val begrunnelserOpphør =
                                filtrerBegrunnelserForPeriodeOgVedtaksbegrunnelsetype(vedtak,
                                                                                      vedtaksperiode,
                                                                                      listOf(VedtakBegrunnelseType.OPPHØR))

                        val vedtaksperiodeTom = vedtaksperiode.periodeTom ?: TIDENES_ENDE
                        acc.add(BrevPeriode(
                                fom = vedtaksperiode.periodeFom.tilDagMånedÅr(),
                                tom = if (!vedtaksperiodeTom.erSenereEnnInneværendeMåned())
                                    vedtaksperiodeTom.tilDagMånedÅr() else null,
                                belop = "0",
                                antallBarn = "0",
                                barnasFodselsdager = "",
                                begrunnelser = begrunnelserOpphør,
                                type = PeriodeType.OPPHOR
                        ))
                    }

                    acc
                }.reversed()
    }

    private fun etterfølgesAvOpphørtEllerAvslåttPeriode(nesteUtbetalingsperiodeFom: LocalDate?,
                                                        vedtaksperiodeTom: LocalDate) =
            nesteUtbetalingsperiodeFom == null ||
            nesteUtbetalingsperiodeFom.erSenereEnnPåfølgendeDag(vedtaksperiodeTom)

    private fun filtrerBegrunnelserForPeriodeOgVedtaksbegrunnelsetype(vedtak: Vedtak,
                                                                      vedtaksperiode: Vedtaksperiode,
                                                                      vedtakBegrunnelseTyper: List<VedtakBegrunnelseType>) =
            vedtak.vedtakBegrunnelser
                    .filter { it.fom == vedtaksperiode.periodeFom && it.tom == vedtaksperiode.periodeTom }
                    .filter { vedtakBegrunnelseTyper.contains(it.begrunnelse?.vedtakBegrunnelseType) }
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
}