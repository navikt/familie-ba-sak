package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevBegrunnelseGrunnlagMedPersoner
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertKompetanse
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.EØSBegrunnelseMedKompetanser
import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.hentKompetanserForEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BrevBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseDataMedKompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseDataUtenKompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.FritekstBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilBrevBegrunnelse

class BrevPeriodeGenerator(
    private val restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
    private val erFørsteVedtaksperiodePåFagsak: Boolean,
    private val uregistrerteBarn: List<MinimertUregistrertBarn>,
    private val brevMålform: Målform,
    private val minimertVedtaksperiode: MinimertVedtaksperiode,
    private val barnMedReduksjonFraForrigeBehandlingIdent: List<String>,
    private val minimerteKompetanserForPeriode: List<MinimertKompetanse>,
    private val minimerteKompetanserSomStopperRettFørPeriode: List<MinimertKompetanse>,
    private val dødeBarnForrigePeriode: List<String>,
) {

    fun hentEØSBegrunnelseData(eøsBegrunnelserMedKompetanser: List<EØSBegrunnelseMedKompetanser>): List<EØSBegrunnelseData> =
        eøsBegrunnelserMedKompetanser.flatMap { begrunnelseMedData ->
            val begrunnelse = begrunnelseMedData.begrunnelse

            if (begrunnelseMedData.kompetanser.isEmpty() && begrunnelse.vedtakBegrunnelseType == VedtakBegrunnelseType.EØS_AVSLAG) {
                val minimertePersonResultater =
                    restBehandlingsgrunnlagForBrev.minimertePersonResultater.filter { personResultat ->
                        personResultat.minimerteVilkårResultater.any {
                            it.erEksplisittAvslagPåSøknad == true &&
                                it.periodeFom?.førsteDagINesteMåned() == minimertVedtaksperiode.fom &&
                                it.standardbegrunnelser.contains(begrunnelse)
                        }
                    }

                val personerIBegrunnelse =
                    restBehandlingsgrunnlagForBrev.personerPåBehandling.filter { person -> minimertePersonResultater.any { personResultat -> personResultat.personIdent == person.personIdent } }
                val barnPåBehandling =
                    restBehandlingsgrunnlagForBrev.personerPåBehandling.filter { it.type == PersonType.BARN }
                val barnIBegrunnelse = personerIBegrunnelse.filter { it.type == PersonType.BARN }
                val gjelderSøker = personerIBegrunnelse.any { it.type == PersonType.SØKER }

                val barnasFødselsdatoer =
                    hentBarnasFødselsdatoerForAvslagsbegrunnelse(
                        barnIBegrunnelse = barnIBegrunnelse,
                        barnPåBehandling = barnPåBehandling,
                        uregistrerteBarn = uregistrerteBarn,
                        gjelderSøker = gjelderSøker,
                    )
                val antallBarn =
                    hentAntallBarnForAvslagsbegrunnelse(
                        barnIBegrunnelse = barnIBegrunnelse,
                        barnPåBehandling = barnPåBehandling,
                        uregistrerteBarn = uregistrerteBarn,
                        gjelderSøker = gjelderSøker,
                    )

                listOf(
                    EØSBegrunnelseDataUtenKompetanse(
                        vedtakBegrunnelseType = begrunnelse.vedtakBegrunnelseType,
                        apiNavn = begrunnelse.sanityApiNavn,
                        barnasFodselsdatoer = barnasFødselsdatoer,
                        antallBarn = antallBarn,
                        maalform = brevMålform.tilSanityFormat(),
                        gjelderSoker = gjelderSøker,
                    ),
                )
            } else {
                begrunnelseMedData.kompetanser.map { kompetanse ->
                    EØSBegrunnelseDataMedKompetanse(
                        vedtakBegrunnelseType = begrunnelse.vedtakBegrunnelseType,
                        apiNavn = begrunnelse.sanityApiNavn,
                        annenForeldersAktivitet = kompetanse.annenForeldersAktivitet,
                        annenForeldersAktivitetsland = kompetanse.annenForeldersAktivitetslandNavn?.navn,
                        barnetsBostedsland = kompetanse.barnetsBostedslandNavn.navn,
                        barnasFodselsdatoer = Utils.slåSammen(kompetanse.personer.map { it.fødselsdato.tilKortString() }),
                        antallBarn = kompetanse.personer.size,
                        maalform = brevMålform.tilSanityFormat(),
                        sokersAktivitet = kompetanse.søkersAktivitet,
                        sokersAktivitetsland = kompetanse.søkersAktivitetsland?.navn,
                    )
                }
            }
        }

    fun hentEøsBegrunnelserMedKompetanser(): List<EØSBegrunnelseMedKompetanser> =
        minimertVedtaksperiode.eøsBegrunnelser.map { eøsBegrunnelseMedTriggere ->
            val kompetanser =
                when (eøsBegrunnelseMedTriggere.eøsBegrunnelse.vedtakBegrunnelseType) {
                    VedtakBegrunnelseType.EØS_INNVILGET, VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET ->
                        hentKompetanserForEØSBegrunnelse(
                            eøsBegrunnelseMedTriggere,
                            minimerteKompetanserForPeriode,
                        )

                    VedtakBegrunnelseType.EØS_OPPHØR, VedtakBegrunnelseType.EØS_REDUKSJON ->
                        hentKompetanserForEØSBegrunnelse(
                            eøsBegrunnelseMedTriggere,
                            minimerteKompetanserSomStopperRettFørPeriode,
                        )

                    else -> emptyList()
                }
            EØSBegrunnelseMedKompetanser(
                begrunnelse = eøsBegrunnelseMedTriggere.eøsBegrunnelse,
                kompetanser = kompetanser,
            )
        }

    fun hentBegrunnelsegrunnlagMedPersoner() =
        minimertVedtaksperiode.begrunnelser.flatMap {
            it.tilBrevBegrunnelseGrunnlagMedPersoner(
                periode =
                    NullablePeriode(
                        fom = minimertVedtaksperiode.fom,
                        tom = minimertVedtaksperiode.tom,
                    ),
                vedtaksperiodetype = minimertVedtaksperiode.type,
                restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
                identerMedUtbetalingPåPeriode =
                    minimertVedtaksperiode.minimerteUtbetalingsperiodeDetaljer
                        .map { utbetalingsperiodeDetalj -> utbetalingsperiodeDetalj.person.personIdent },
                minimerteUtbetalingsperiodeDetaljer = minimertVedtaksperiode.minimerteUtbetalingsperiodeDetaljer,
                erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
                erUregistrerteBarnPåbehandling = uregistrerteBarn.isNotEmpty(),
                barnMedReduksjonFraForrigeBehandlingIdent = barnMedReduksjonFraForrigeBehandlingIdent,
                dødeBarnForrigePeriode = dødeBarnForrigePeriode,
            )
        }

    fun byggBegrunnelserOgFritekster(
        begrunnelserGrunnlagMedPersoner: List<BrevBegrunnelseGrunnlagMedPersoner>,
        eøsBegrunnelserMedKompetanser: List<EØSBegrunnelseMedKompetanser>,
    ): List<BrevBegrunnelse> {
        val brevBegrunnelser =
            begrunnelserGrunnlagMedPersoner
                .map {
                    it.tilBrevBegrunnelse(
                        vedtaksperiode = NullablePeriode(minimertVedtaksperiode.fom, minimertVedtaksperiode.tom),
                        personerIPersongrunnlag = restBehandlingsgrunnlagForBrev.personerPåBehandling,
                        brevMålform = brevMålform,
                        uregistrerteBarn = uregistrerteBarn,
                        minimerteUtbetalingsperiodeDetaljer = minimertVedtaksperiode.minimerteUtbetalingsperiodeDetaljer,
                        minimerteRestEndredeAndeler = restBehandlingsgrunnlagForBrev.minimerteEndredeUtbetalingAndeler,
                    )
                }

        val eøsBegrunnelser = hentEØSBegrunnelseData(eøsBegrunnelserMedKompetanser)

        val fritekster = minimertVedtaksperiode.fritekster.map { FritekstBegrunnelse(it) }

        return (brevBegrunnelser + eøsBegrunnelser + fritekster).sorted()
    }
}
