package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import no.nav.familie.ba.sak.brev.domene.maler.BrevPeriode
import no.nav.familie.ba.sak.brev.domene.maler.PeriodeType
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.erSenereEnnInneværendeMåned
import no.nav.familie.ba.sak.common.erSenereEnnPåfølgendeDag
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.tilKortString
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BrevPeriodeService(
        private val vedtaksperiodeService: VedtaksperiodeService
) {

    fun hentBrevPerioder(vedtak: Vedtak): List<BrevPeriode> {
        val vedtaksperioder = vedtaksperiodeService.hentVedtaksperioder(vedtak.behandling).filterIsInstance<Utbetalingsperiode>()

        return vedtaksperioder
                .foldRightIndexed(mutableListOf<BrevPeriode>()) { idx, utbetalingsperiode, acc ->
                    /* Temporær løsning for å støtte begrunnelse av perioder som er opphørt eller avslått.
                * Begrunnelsen settes på den tidligere (før den opphøret- eller avslåtteperioden) innvilgte perioden.
                */
                    val nesteUtbetalingsperiodeFom = if (idx < vedtaksperioder.lastIndex) {
                        vedtaksperioder[idx + 1].periodeFom
                    } else {
                        null
                    }

                    val begrunnelserOpphør =
                            filtrerBegrunnelserForPeriodeOgVedtaksType(vedtak,
                                                                       utbetalingsperiode,
                                                                       listOf(VedtakBegrunnelseType.OPPHØR))

                    if (etterfølgesAvOpphørtEllerAvslåttPeriode(nesteUtbetalingsperiodeFom, utbetalingsperiode.periodeTom) &&
                        begrunnelserOpphør.isNotEmpty())

                        acc.add(BrevPeriode(
                                fom = utbetalingsperiode.periodeTom.plusDays(1).tilDagMånedÅr(),
                                tom = nesteUtbetalingsperiodeFom?.minusDays(1)?.tilDagMånedÅr(),
                                belop = "0",
                                antallBarn = "0",
                                barnasFodselsdager = "",
                                begrunnelser = begrunnelserOpphør,
                                type = PeriodeType.OPPHOR
                        ))
                    /* Slutt temporær løsning */

                    val barnasFødselsdatoer = finnAlleBarnsFødselsDatoerForPerioden(utbetalingsperiode)

                    val begrunnelser =
                            filtrerBegrunnelserForPeriodeOgVedtaksType(vedtak, utbetalingsperiode,
                                                                       listOf(VedtakBegrunnelseType.INNVILGELSE,
                                                                              VedtakBegrunnelseType.REDUKSJON))

                    if (begrunnelser.isNotEmpty()) {
                        acc.add(BrevPeriode(
                                fom = utbetalingsperiode.periodeFom.tilDagMånedÅr(),
                                tom = if (!utbetalingsperiode.periodeTom.erSenereEnnInneværendeMåned())
                                    utbetalingsperiode.periodeTom.tilDagMånedÅr() else null,
                                belop = Utils.formaterBeløp(utbetalingsperiode.utbetaltPerMnd),
                                antallBarn = utbetalingsperiode.antallBarn.toString(),
                                barnasFodselsdager = barnasFødselsdatoer,
                                begrunnelser = begrunnelser,
                                type = PeriodeType.INNVILGELSE
                        ))
                    }

                    acc
                }.reversed()
    }

    private fun etterfølgesAvOpphørtEllerAvslåttPeriode(nesteUtbetalingsperiodeFom: LocalDate?,
                                                        utbetalingsperiodeTom: LocalDate) =
            nesteUtbetalingsperiodeFom == null ||
            nesteUtbetalingsperiodeFom.erSenereEnnPåfølgendeDag(utbetalingsperiodeTom)

    private fun filtrerBegrunnelserForPeriodeOgVedtaksType(vedtak: Vedtak,
                                                           utbetalingsperiode: Utbetalingsperiode,
                                                           vedtakBegrunnelseTyper: List<VedtakBegrunnelseType>) =
            vedtak.vedtakBegrunnelser
                    .filter { it.fom == utbetalingsperiode.periodeFom && it.tom == utbetalingsperiode.periodeTom }
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