package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.erSenereEnnInneværendeMåned
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevBegrunnelseGrunnlagMedPersoner
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertKompetanse
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.EØSBegrunnelseMedTriggere
import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.hentBarnFraVilkårResultaterSomPasserMedBegrunnelseOgPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.hentMinimerteKompetanserGyldigeForEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.hentVilkårResultaterPasserMedBegrunnelseOgPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.totaltUtbetalt
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseMedKompetanseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.FritekstBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.IEØSBegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilBrevBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import java.time.LocalDate

class BrevPeriodeGenerator(
    private val restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
    private val erFørsteVedtaksperiodePåFagsak: Boolean,
    private val uregistrerteBarn: List<MinimertUregistrertBarn>,
    private val brevMålform: Målform,
    private val minimertVedtaksperiode: MinimertVedtaksperiode,
    private val barnMedReduksjonFraForrigeBehandlingIdent: List<String>,
    private val minimerteKompetanserForPeriode: List<MinimertKompetanse>,
    private val minimerteKompetanserSomStopperRettFørPeriode: List<MinimertKompetanse>,
    private val dødeBarnForrigePeriode: List<String>
) {

    fun genererBrevPeriode(): BrevPeriode? {
        val begrunnelseGrunnlagMedPersoner = hentBegrunnelsegrunnlagMedPersoner()
        val eøsBegrunnelser = hentEøsBegrunnelser()

        val begrunnelserOgFritekster =
            byggBegrunnelserOgFritekster(
                begrunnelserGrunnlagMedPersoner = begrunnelseGrunnlagMedPersoner,
                eøsBegrunnelser = eøsBegrunnelser
            )

        if (begrunnelserOgFritekster.isEmpty()) return null

        val tomDato =
            if (minimertVedtaksperiode.tom?.erSenereEnnInneværendeMåned() == false) {
                minimertVedtaksperiode.tom.tilDagMånedÅr()
            } else {
                null
            }

        val identerIBegrunnelene = begrunnelseGrunnlagMedPersoner
            .filter { it.vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGET }
            .flatMap { it.personIdenter }

        return byggBrevPeriode(
            tomDato = tomDato,
            begrunnelserOgFritekster = begrunnelserOgFritekster,
            identerIBegrunnelene = identerIBegrunnelene
        )
    }

    fun hentEøsBegrunnelser(): List<IEØSBegrunnelseData> {
        return minimertVedtaksperiode.eøsBegrunnelser.flatMap { eøsBegrunnelseMedTriggere ->
            val kompetanserSomPasserMedBegrunnelse = hentKompetanserForBegrunnelse(eøsBegrunnelseMedTriggere)
            val barnFraVilkårResultaterSomPasserMedBegrunnelseOgPeriode =
                hentBarnFraVilkårResultaterSomPasserMedBegrunnelseOgPeriode(eøsBegrunnelseMedTriggere)
            val gjelderSøker = gjelderSøker(eøsBegrunnelseMedTriggere)

            when {
                eøsBegrunnelseMedTriggere.sanityEØSBegrunnelse.skalBrukeKompetanseData() -> {
                    kompetanserSomPasserMedBegrunnelse.map { kompetanse ->
                        val barnIbegrunnelse =
                            if (eøsBegrunnelseMedTriggere.sanityEØSBegrunnelse.skalBrukeVilkårData()) {
                                barnFraVilkårResultaterSomPasserMedBegrunnelseOgPeriode.intersect(kompetanse.personer.toSet())
                            } else kompetanse.personer

                        hentEøsBegrunnelseMedKompetanseData(
                            eøsBegrunnelse = eøsBegrunnelseMedTriggere.eøsBegrunnelse,
                            barnIbegrunnelse = barnIbegrunnelse,
                            gjelderSøker = gjelderSøker,
                            kompetanse = kompetanse,
                        )
                    }
                }
                eøsBegrunnelseMedTriggere.sanityEØSBegrunnelse.skalBrukeVilkårData() -> {
                    listOf(
                        hentEøsBegrunnelseData(
                            eøsBegrunnelse = eøsBegrunnelseMedTriggere.eøsBegrunnelse,
                            barnIbegrunnelse = barnFraVilkårResultaterSomPasserMedBegrunnelseOgPeriode,
                            gjelderSøker = gjelderSøker
                        )
                    )
                }
                else -> throw Feil("Ingen triggere i bruk for begrunnelse med apiNavn=${eøsBegrunnelseMedTriggere.sanityEØSBegrunnelse.apiNavn}. Dette er mest sannsynilg en feil i Sanity.")
            }
        }
    }

    fun hentBegrunnelsegrunnlagMedPersoner() = minimertVedtaksperiode.begrunnelser.flatMap {
        it.tilBrevBegrunnelseGrunnlagMedPersoner(
            periode = NullablePeriode(
                fom = minimertVedtaksperiode.fom,
                tom = minimertVedtaksperiode.tom
            ),
            vedtaksperiodetype = minimertVedtaksperiode.type,
            restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
            identerMedUtbetalingPåPeriode = minimertVedtaksperiode.minimerteUtbetalingsperiodeDetaljer
                .map { utbetalingsperiodeDetalj -> utbetalingsperiodeDetalj.person.personIdent },
            minimerteUtbetalingsperiodeDetaljer = minimertVedtaksperiode.minimerteUtbetalingsperiodeDetaljer,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
            erUregistrerteBarnPåbehandling = uregistrerteBarn.isNotEmpty(),
            barnMedReduksjonFraForrigeBehandlingIdent = barnMedReduksjonFraForrigeBehandlingIdent,
            dødeBarnForrigePeriode = dødeBarnForrigePeriode
        )
    }

    fun byggBegrunnelserOgFritekster(
        begrunnelserGrunnlagMedPersoner: List<BrevBegrunnelseGrunnlagMedPersoner>,
        eøsBegrunnelser: List<IEØSBegrunnelseData>
    ): List<Begrunnelse> {
        val brevBegrunnelser = begrunnelserGrunnlagMedPersoner
            .map {
                it.tilBrevBegrunnelse(
                    vedtaksperiode = NullablePeriode(minimertVedtaksperiode.fom, minimertVedtaksperiode.tom),
                    personerIPersongrunnlag = restBehandlingsgrunnlagForBrev.personerPåBehandling,
                    brevMålform = brevMålform,
                    uregistrerteBarn = uregistrerteBarn,
                    minimerteUtbetalingsperiodeDetaljer = minimertVedtaksperiode.minimerteUtbetalingsperiodeDetaljer,
                    minimerteRestEndredeAndeler = restBehandlingsgrunnlagForBrev.minimerteEndredeUtbetalingAndeler
                )
            }

        val fritekster = minimertVedtaksperiode.fritekster.map { FritekstBegrunnelse(it) }

        return (brevBegrunnelser + eøsBegrunnelser + fritekster).sorted()
    }

    private fun byggBrevPeriode(
        tomDato: String?,
        begrunnelserOgFritekster: List<Begrunnelse>,
        identerIBegrunnelene: List<String>
    ): BrevPeriode {
        val (utbetalingerBarn, nullutbetalingerBarn) = minimertVedtaksperiode.minimerteUtbetalingsperiodeDetaljer
            .filter { it.person.type == PersonType.BARN }
            .partition { it.utbetaltPerMnd != 0 }

        val barnMedUtbetaling = utbetalingerBarn.map { it.person }
        val barnMedNullutbetaling = nullutbetalingerBarn.map { it.person }

        val barnIPeriode: List<MinimertRestPerson> = when (minimertVedtaksperiode.type) {
            Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
            Vedtaksperiodetype.UTBETALING -> finnBarnIUtbetalingPeriode(identerIBegrunnelene)
            Vedtaksperiodetype.OPPHØR -> emptyList()
            Vedtaksperiodetype.AVSLAG -> emptyList()
            Vedtaksperiodetype.FORTSATT_INNVILGET -> barnMedUtbetaling + barnMedNullutbetaling
            Vedtaksperiodetype.ENDRET_UTBETALING -> throw Feil("Endret utbetaling skal ikke benyttes lenger.")
        }

        val utbetalingsbeløp = minimertVedtaksperiode.minimerteUtbetalingsperiodeDetaljer.totaltUtbetalt()
        val brevPeriodeType = hentPeriodetype(minimertVedtaksperiode.fom, barnMedUtbetaling, utbetalingsbeløp)
        return BrevPeriode(

            fom = this.hentFomTekst(),
            tom = when {
                minimertVedtaksperiode.type == Vedtaksperiodetype.FORTSATT_INNVILGET -> ""
                tomDato.isNullOrBlank() -> ""
                brevPeriodeType == BrevPeriodeType.INNVILGELSE_INGEN_UTBETALING -> " til $tomDato"
                else -> "til $tomDato "
            },
            belop = Utils.formaterBeløp(utbetalingsbeløp),
            begrunnelser = begrunnelserOgFritekster,
            brevPeriodeType = brevPeriodeType,
            antallBarn = barnIPeriode.size.toString(),
            barnasFodselsdager = barnIPeriode.tilBarnasFødselsdatoer(),
            antallBarnMedUtbetaling = barnMedUtbetaling.size.toString(),
            antallBarnMedNullutbetaling = barnMedNullutbetaling.size.toString(),
            fodselsdagerBarnMedUtbetaling = barnMedUtbetaling.tilBarnasFødselsdatoer(),
            fodselsdagerBarnMedNullutbetaling = barnMedNullutbetaling.tilBarnasFødselsdatoer()
        )
    }

    private fun hentFomTekst(): String = when (minimertVedtaksperiode.type) {
        Vedtaksperiodetype.FORTSATT_INNVILGET -> hentFomtekstFortsattInnvilget(
            brevMålform,
            minimertVedtaksperiode.fom,
            minimertVedtaksperiode.begrunnelser.map { it.standardbegrunnelse }
        ) ?: "Du får:"
        Vedtaksperiodetype.UTBETALING -> minimertVedtaksperiode.fom!!.tilDagMånedÅr()
        Vedtaksperiodetype.ENDRET_UTBETALING -> throw Feil("Endret utbetaling skal ikke benyttes lenger.")
        Vedtaksperiodetype.OPPHØR -> minimertVedtaksperiode.fom!!.tilDagMånedÅr()
        Vedtaksperiodetype.AVSLAG -> if (minimertVedtaksperiode.fom != null) minimertVedtaksperiode.fom.tilDagMånedÅr() else ""
        Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING -> minimertVedtaksperiode.fom!!.tilDagMånedÅr()
    }

    private fun hentPeriodetype(
        fom: LocalDate?,
        barnMedUtbetaling: List<MinimertRestPerson>,
        utbetalingsbeløp: Int
    ) = when (minimertVedtaksperiode.type) {
        Vedtaksperiodetype.FORTSATT_INNVILGET -> BrevPeriodeType.FORTSATT_INNVILGET
        Vedtaksperiodetype.UTBETALING -> when {
            utbetalingsbeløp == 0 -> BrevPeriodeType.INNVILGELSE_INGEN_UTBETALING
            barnMedUtbetaling.isEmpty() -> BrevPeriodeType.INNVILGELSE_KUN_UTBETALING_PÅ_SØKER
            else -> BrevPeriodeType.INNVILGELSE
        }
        Vedtaksperiodetype.ENDRET_UTBETALING -> throw Feil("Endret utbetaling skal ikke benyttes lenger.")
        Vedtaksperiodetype.AVSLAG -> if (fom != null) BrevPeriodeType.AVSLAG else BrevPeriodeType.AVSLAG_UTEN_PERIODE
        Vedtaksperiodetype.OPPHØR -> BrevPeriodeType.OPPHOR
        Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING -> BrevPeriodeType.INNVILGELSE
    }

    fun finnBarnIUtbetalingPeriode(identerIBegrunnelene: List<String>): List<MinimertRestPerson> {
        val identerMedUtbetaling =
            minimertVedtaksperiode.minimerteUtbetalingsperiodeDetaljer.map { it.person.personIdent }

        val barnIPeriode = (identerIBegrunnelene + identerMedUtbetaling)
            .toSet()
            .mapNotNull { personIdent ->
                restBehandlingsgrunnlagForBrev.personerPåBehandling.find { it.personIdent == personIdent }
            }
            .filter { it.type == PersonType.BARN }

        return barnIPeriode
    }

    private fun hentFomtekstFortsattInnvilget(
        målform: Målform,
        fom: LocalDate?,
        begrunnelser: List<Standardbegrunnelse>
    ): String? {
        val erAutobrev = begrunnelser.any {
            it == Standardbegrunnelse.REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK ||
                it == Standardbegrunnelse.REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK
        }
        return if (erAutobrev && fom != null) {
            val fra = if (målform == Målform.NB) "Fra" else "Frå"
            "$fra ${fom.tilDagMånedÅr()} får du:"
        } else {
            null
        }
    }

    fun hentKompetanserForBegrunnelse(
        eøsBegrunnelseMedTriggere: EØSBegrunnelseMedTriggere,
    ): List<MinimertKompetanse> {
        val relevanteKompetanser = when (eøsBegrunnelseMedTriggere.eøsBegrunnelse.vedtakBegrunnelseType) {
            VedtakBegrunnelseType.EØS_INNVILGET -> minimerteKompetanserForPeriode
            VedtakBegrunnelseType.EØS_OPPHØR -> minimerteKompetanserSomStopperRettFørPeriode
            else -> emptyList()
        }

        return hentMinimerteKompetanserGyldigeForEØSBegrunnelse(
            eøsBegrunnelseMedTriggere = eøsBegrunnelseMedTriggere,
            minimerteKompetanser = relevanteKompetanser
        )
    }

    private fun gjelderSøker(eøsBegrunnelse: EØSBegrunnelseMedTriggere): Boolean =
        restBehandlingsgrunnlagForBrev.minimertePersonResultater.any { personResultat ->
            personResultat.hentVilkårResultaterPasserMedBegrunnelseOgPeriode(
                vedtaksperiode = minimertVedtaksperiode,
                begrunnelse = eøsBegrunnelse.sanityEØSBegrunnelse
            ).any {
                it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BARN_BOR_I_STORBRITANNIA_MED_SØKER)
            }
        }

    private fun hentEøsBegrunnelseMedKompetanseData(
        eøsBegrunnelse: EØSStandardbegrunnelse,
        barnIbegrunnelse: Collection<MinimertRestPerson>,
        gjelderSøker: Boolean,
        kompetanse: MinimertKompetanse,
    ) = EØSBegrunnelseMedKompetanseData(
        vedtakBegrunnelseType = eøsBegrunnelse.vedtakBegrunnelseType,
        apiNavn = eøsBegrunnelse.sanityApiNavn,
        annenForeldersAktivitet = kompetanse.annenForeldersAktivitet,
        annenForeldersAktivitetsland = kompetanse.annenForeldersAktivitetslandNavn?.navn,
        barnetsBostedsland = kompetanse.barnetsBostedslandNavn.navn,
        barnasFodselsdatoer = Utils.slåSammen(barnIbegrunnelse.map { it.fødselsdato.tilKortString() }),
        antallBarn = barnIbegrunnelse.size,
        maalform = brevMålform.tilSanityFormat(),
        sokersAktivitet = kompetanse.søkersAktivitet,
        gjelderSøker = gjelderSøker
    )

    private fun hentEøsBegrunnelseData(
        eøsBegrunnelse: EØSStandardbegrunnelse,
        barnIbegrunnelse: List<MinimertRestPerson>,
        gjelderSøker: Boolean
    ) = EØSBegrunnelseData(
        vedtakBegrunnelseType = eøsBegrunnelse.vedtakBegrunnelseType,
        apiNavn = eøsBegrunnelse.sanityApiNavn,
        barnasFodselsdatoer = Utils.slåSammen(barnIbegrunnelse.map { it.fødselsdato.tilKortString() }),
        antallBarn = barnIbegrunnelse.size,
        maalform = brevMålform.tilSanityFormat(),
        gjelderSøker = gjelderSøker,
    )

    private fun hentBarnFraVilkårResultaterSomPasserMedBegrunnelseOgPeriode(eøsBegrunnelseMedTriggere: EØSBegrunnelseMedTriggere): List<MinimertRestPerson> {
        return hentBarnFraVilkårResultaterSomPasserMedBegrunnelseOgPeriode(
            eøsBegrunnelseMedTriggere,
            restBehandlingsgrunnlagForBrev.minimertePersonResultater,
            restBehandlingsgrunnlagForBrev.personerPåBehandling,
            minimertVedtaksperiode
        )
    }
}
