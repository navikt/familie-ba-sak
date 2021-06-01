package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.vedtak.VedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Avslagsperiode
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Opphørsperiode
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiode
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.hentUtbetalingsperiodeForVedtaksperiode
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import no.nav.familie.ba.sak.brev.domene.maler.AvslagBrevPeriode
import no.nav.familie.ba.sak.brev.domene.maler.AvslagUtenPeriodeBrevPeriode
import no.nav.familie.ba.sak.brev.domene.maler.BrevPeriode
import no.nav.familie.ba.sak.brev.domene.maler.FortsattInnvilgetBrevPeriode
import no.nav.familie.ba.sak.brev.domene.maler.InnvilgelseBrevPeriode
import no.nav.familie.ba.sak.brev.domene.maler.OpphørBrevPeriode
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.erSenereEnnInneværendeMåned
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilMånedÅr

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
                .filter { it.fom == vedtaksperiode.periodeFom && it.tom == vedtaksperiode.periodeTom }
                .filter { vedtakBegrunnelseTyper.contains(it.begrunnelse.vedtakBegrunnelseType) }
                .sortedBy { it.opprettetTidspunkt }
                .sortedBy { it.begrunnelse.erFritekstBegrunnelse() }
                .map {
                    it.brevBegrunnelse?.lines() ?: listOf("Ikke satt")
                }
                .flatten()

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

fun vedtaksperioderTilBrevPerioder(
        vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        utbetalingsperioder: List<Utbetalingsperiode>,
): List<BrevPeriode> =
        vedtaksperioder.mapNotNull {
            when (it.type) {
                Vedtaksperiodetype.FORTSATT_INNVILGET ->
                    byggFortsattInnvilgetBrevperiode(it, personopplysningGrunnlag, utbetalingsperioder)
                else -> throw Feil("Kun fortsatt innvilget er støttet med de nye vedtaksperiodene.")
            }
        }

fun byggFortsattInnvilgetBrevperiode(
        vedtaksperiode: VedtaksperiodeMedBegrunnelser,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        utbetalingsperioder: List<Utbetalingsperiode>,
): BrevPeriode? {
    val utbetalingsperiodeForVedtaksperiode = hentUtbetalingsperiodeForVedtaksperiode(utbetalingsperioder, vedtaksperiode.fom)

    val begrunnelserOgFritekster = byggBegrunnelserOgFriteksterForVedtaksperiode(
            vedtaksperiode,
            personopplysningGrunnlag,
    )

    return if (begrunnelserOgFritekster.isNotEmpty()) {
        FortsattInnvilgetBrevPeriode(
                belop = Utils.formaterBeløp(utbetalingsperiodeForVedtaksperiode.utbetaltPerMnd),
                antallBarn = utbetalingsperiodeForVedtaksperiode.antallBarn.toString(),
                barnasFodselsdager = finnAlleBarnsFødselsDatoerIUtbetalingsperiode(utbetalingsperiodeForVedtaksperiode),
                begrunnelser = begrunnelserOgFritekster,
        )
    } else null
}

private fun byggBegrunnelserOgFriteksterForVedtaksperiode(
        vedtaksperiode: VedtaksperiodeMedBegrunnelser,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
): List<String> {
    val fritekster = vedtaksperiode.fritekster.map { it.fritekst }
    val begrunnelser = byggBegrunnelserForVedtaksperiode(vedtaksperiode, personopplysningGrunnlag)

    return begrunnelser + fritekster
}

private fun byggBegrunnelserForVedtaksperiode(
        vedtaksperiode: VedtaksperiodeMedBegrunnelser,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
): List<String> = vedtaksperiode
        .begrunnelser.map {
            it.vedtakBegrunnelseSpesifikasjon.hentBeskrivelse(
                    gjelderSøker = it.personIdenter.contains(personopplysningGrunnlag.søker.personIdent.ident),
                    barnasFødselsdatoer = it.personIdenter.map { ident ->
                        hentBursdagsdatoFraPersonopplysningsgrunnlag(personopplysningGrunnlag, ident)
                    },
                    månedOgÅrBegrunnelsenGjelderFor = vedtaksperiode.fom?.tilMånedÅr() ?: "",
                    målform = personopplysningGrunnlag.søker.målform
            )
        }

private fun hentBursdagsdatoFraPersonopplysningsgrunnlag(personopplysningGrunnlag: PersonopplysningGrunnlag, ident: String) =
        personopplysningGrunnlag.personer.find { person -> person.personIdent.ident == ident }?.fødselsdato
        ?: throw Feil("Fant ikke person i personopplysningsgrunnlag")