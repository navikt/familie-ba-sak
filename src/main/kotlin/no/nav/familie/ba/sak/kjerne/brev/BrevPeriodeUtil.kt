package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.erSenereEnnInneværendeMåned
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevPeriodeGrunnlagMedPersoner
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevperiodeData
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.GenerellBrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertPersonResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertRestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.brev.domene.totaltUtbetalt
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilMinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype.AVSLAG
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype.ENDRET_UTBETALING
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype.FORTSATT_INNVILGET
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype.OPPHØR
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype.UTBETALING
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.slf4j.LoggerFactory
import java.time.LocalDate

private val secureLogger = LoggerFactory.getLogger("secureLogger")

fun List<MinimertRestPerson>.tilBarnasFødselsdatoer(): String =
    Utils.slåSammen(
        this
            .filter { it.type == PersonType.BARN }
            .sortedBy { person ->
                person.fødselsdato
            }
            .map { person ->
                person.fødselsdato.tilKortString()
            }
    )

fun hentBrevPerioder(
    brevperioderData: List<BrevperiodeData>,
) = brevperioderData
    .sorted()
    .mapNotNull {

        it.minimertVedtaksperiode.tilBrevPeriode(
            restBehandlingsgrunnlagForBrev = it.restBehandlingsgrunnlagForBrev,
            uregistrerteBarn = it.uregistrerteBarn,
            erFørsteVedtaksperiodePåFagsak = it.erFørsteVedtaksperiodePåFagsak,
            brevMålform = it.brevMålform,
            barnPersonIdentMedReduksjon = it.barnPersonIdentMedReduksjon,
        )
    }

fun MinimertVedtaksperiode.tilBrevPeriode(
    restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
    uregistrerteBarn: List<MinimertUregistrertBarn> = emptyList(),
    barnPersonIdentMedReduksjon: List<String> = emptyList(),
    erFørsteVedtaksperiodePåFagsak: Boolean,
    brevMålform: Målform,
): BrevPeriode? {
    val brevPeriodeGrunnlagMedPersoner =
        this.tilBrevPeriodeGrunnlagMedPersoner(
            restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
            erUregistrerteBarnPåbehandling = uregistrerteBarn.isNotEmpty(),
            barnPersonIdentMedReduksjon = barnPersonIdentMedReduksjon,
        )

    val begrunnelserOgFritekster = brevPeriodeGrunnlagMedPersoner.byggBegrunnelserOgFritekster(
        uregistrerteBarn = uregistrerteBarn,
        restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
        brevMålform = brevMålform,
    )

    if (begrunnelserOgFritekster.isEmpty()) return null

    val tomDato =
        if (brevPeriodeGrunnlagMedPersoner.tom?.erSenereEnnInneværendeMåned() == false)
            brevPeriodeGrunnlagMedPersoner.tom.tilDagMånedÅr()
        else null

    return brevPeriodeGrunnlagMedPersoner.byggBrevPeriode(
        restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
        brevMålform = brevMålform,
        tomDato = tomDato,
        begrunnelserOgFritekster = begrunnelserOgFritekster
    )
}

private fun BrevPeriodeGrunnlagMedPersoner.byggBrevPeriode(
    restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
    brevMålform: Målform,
    tomDato: String?,
    begrunnelserOgFritekster: List<Begrunnelse>
): GenerellBrevPeriode {
    val (utbetalingerBarn, nullutbetalingerBarn) = this.minimerteUtbetalingsperiodeDetaljer
        .filter { it.person.type == PersonType.BARN }
        .partition { it.utbetaltPerMnd != 0 }

    val barnMedUtbetaling = utbetalingerBarn.map { it.person }
    val barnMedNullutbetaling = nullutbetalingerBarn.map { it.person }

    val barnIPeriode: List<MinimertRestPerson> = when (this.type) {
        UTBETALING -> this.finnBarnIInnvilgelsePeriode(restBehandlingsgrunnlagForBrev.personerPåBehandling)
        UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING -> this.finnBarnIUtbetalingMedReduksjonFraForrigeBehandlignPeriode(
            restBehandlingsgrunnlagForBrev.personerPåBehandling
        )
        OPPHØR -> emptyList()
        AVSLAG -> emptyList()
        FORTSATT_INNVILGET -> barnMedUtbetaling + barnMedNullutbetaling
        ENDRET_UTBETALING -> throw Feil("Endret utbetaling skal ikke benyttes lenger.")
    }

    val utbetalingsbeløp = this.minimerteUtbetalingsperiodeDetaljer.totaltUtbetalt()
    val brevPeriodeType = hentPeriodetype(this.fom, this, barnMedUtbetaling, utbetalingsbeløp)
    return BrevPeriode(

        fom = this.hentFomTekst(brevMålform),
        tom = when {
            this.type == FORTSATT_INNVILGET -> ""
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
        fodselsdagerBarnMedNullutbetaling = barnMedNullutbetaling.tilBarnasFødselsdatoer(),
    )
}

private fun BrevPeriodeGrunnlagMedPersoner.hentFomTekst(
    brevMålform: Målform,
): String = when (this.type) {
    FORTSATT_INNVILGET -> hentFomtekstFortsattInnvilget(
        brevMålform,
        this.fom,
        this.begrunnelser.map { it.standardbegrunnelse }
    ) ?: "Du får:"
    UTBETALING -> fom!!.tilDagMånedÅr()
    ENDRET_UTBETALING -> throw Feil("Endret utbetaling skal ikke benyttes lenger.")
    OPPHØR -> fom!!.tilDagMånedÅr()
    AVSLAG -> if (fom != null) fom.tilDagMånedÅr() else ""
    UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING -> fom!!.tilDagMånedÅr()
}

private fun hentPeriodetype(
    fom: LocalDate?,
    brevPeriodeGrunnlagMedPersoner: BrevPeriodeGrunnlagMedPersoner,
    barnMedUtbetaling: List<MinimertRestPerson>,
    utbetalingsbeløp: Int,
) = when (brevPeriodeGrunnlagMedPersoner.type) {
    FORTSATT_INNVILGET -> BrevPeriodeType.FORTSATT_INNVILGET
    UTBETALING -> when {
        utbetalingsbeløp == 0 -> BrevPeriodeType.INNVILGELSE_INGEN_UTBETALING
        barnMedUtbetaling.isEmpty() -> BrevPeriodeType.INNVILGELSE_KUN_UTBETALING_PÅ_SØKER
        else -> BrevPeriodeType.INNVILGELSE
    }
    ENDRET_UTBETALING -> throw Feil("Endret utbetaling skal ikke benyttes lenger.")
    AVSLAG -> if (fom != null) BrevPeriodeType.AVSLAG else BrevPeriodeType.AVSLAG_UTEN_PERIODE
    OPPHØR -> BrevPeriodeType.OPPHOR
    UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING -> BrevPeriodeType.INNVILGELSE
}

fun BrevPeriodeGrunnlagMedPersoner.finnBarnIInnvilgelsePeriode(
    personerPåBehandling: List<MinimertRestPerson>,
): List<MinimertRestPerson> {
    val identerIBegrunnelene = this.begrunnelser
        .filter { it.vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGET }
        .flatMap { it.personIdenter }

    val identerMedUtbetaling = this.minimerteUtbetalingsperiodeDetaljer.map { it.person.personIdent }

    val barnIPeriode = (identerIBegrunnelene + identerMedUtbetaling)
        .toSet()
        .mapNotNull { personIdent ->
            personerPåBehandling.find { it.personIdent == personIdent }
        }
        .filter { it.type == PersonType.BARN }

    return barnIPeriode
}

fun BrevPeriodeGrunnlagMedPersoner.finnBarnIUtbetalingMedReduksjonFraForrigeBehandlignPeriode(
    personerPåBehandling: List<MinimertRestPerson>,
): List<MinimertRestPerson> {
    val identerIBegrunnelsene = this.begrunnelser
        .filter { it.vedtakBegrunnelseType == VedtakBegrunnelseType.REDUKSJON }
        .flatMap { it.personIdenter }

    val identerMedUtbetaling = this.minimerteUtbetalingsperiodeDetaljer.map { it.person.personIdent }

    val barnIPeriode = (identerIBegrunnelsene + identerMedUtbetaling)
        .toSet()
        .mapNotNull { personIdent ->
            personerPåBehandling.find { it.personIdent == personIdent }
        }
        .filter { it.type == PersonType.BARN }

    return barnIPeriode
}

private fun hentFomtekstFortsattInnvilget(
    målform: Målform,
    fom: LocalDate?,
    begrunnelser: List<Standardbegrunnelse>,
): String? {
    val erAutobrev = begrunnelser.any {
        it == Standardbegrunnelse.REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK ||
            it == Standardbegrunnelse.REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK
    }
    return if (erAutobrev && fom != null) {
        val fra = if (målform == Målform.NB) "Fra" else "Frå"
        "$fra ${fom.tilDagMånedÅr()} får du:"
    } else null
}

fun hentRestBehandlingsgrunnlagForBrev(
    persongrunnlag: PersonopplysningGrunnlag,
    vilkårsvurdering: Vilkårsvurdering,
    endredeUtbetalingAndeler: List<EndretUtbetalingAndel>
): RestBehandlingsgrunnlagForBrev {

    return RestBehandlingsgrunnlagForBrev(
        personerPåBehandling = persongrunnlag.søkerOgBarn.map { it.tilMinimertPerson() },
        minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
        minimerteEndredeUtbetalingAndeler = endredeUtbetalingAndeler.map { it.tilMinimertRestEndretUtbetalingAndel() },
    )
}
