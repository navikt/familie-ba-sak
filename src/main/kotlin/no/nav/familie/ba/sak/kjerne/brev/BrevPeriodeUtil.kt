package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.erSenereEnnInneværendeMåned
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevPeriodeGrunnlagMedPersoner
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevperiodeData
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.antallBarn
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.EndretUtbetalingBrevPeriodeType
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.AvslagBrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.AvslagUtenPeriodeBrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.EndretUtbetalingBarnetrygdType
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.EndretUtbetalingBrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.FortsattInnvilgetBrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.GenerellBrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.InnvilgelseBrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.OpphørBrevPeriode
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
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype.REDUKSJON
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype.UTBETALING
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.slf4j.LoggerFactory
import java.math.BigDecimal
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
    erIngenOverlappVedtaksperiodeTogglePå: Boolean,
) = brevperioderData
    .sorted()
    .mapNotNull {

        it.minimertVedtaksperiode.tilBrevPeriode(
            restBehandlingsgrunnlagForBrev = it.restBehandlingsgrunnlagForBrev,
            uregistrerteBarn = it.uregistrerteBarn,
            utvidetScenarioForEndringsperiode = it.utvidetScenarioForEndringsperiode,
            erFørsteVedtaksperiodePåFagsak = it.erFørsteVedtaksperiodePåFagsak,
            brevMålform = it.brevMålform,
            erIngenOverlappVedtaksperiodeTogglePå = erIngenOverlappVedtaksperiodeTogglePå,
            barnPersonIdentMedReduksjon = it.barnPersonIdentMedReduksjon,
        )
    }

@Deprecated("Kan fjernes når INGEN_OVERLAPP_VEDTAKSPERIODER-toggle fjernes. Kan utledes fra perioden.")
enum class UtvidetScenarioForEndringsperiode {
    IKKE_UTVIDET_YTELSE,
    UTVIDET_YTELSE_ENDRET,
    UTVIDET_YTELSE_IKKE_ENDRET
}

fun MinimertVedtaksperiode.tilBrevPeriode(
    restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
    utvidetScenarioForEndringsperiode: UtvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
    uregistrerteBarn: List<MinimertUregistrertBarn> = emptyList(),
    barnPersonIdentMedReduksjon: List<String> = emptyList(),
    erFørsteVedtaksperiodePåFagsak: Boolean,
    brevMålform: Målform,
    erIngenOverlappVedtaksperiodeTogglePå: Boolean
): BrevPeriode? {
    val brevPeriodeGrunnlagMedPersoner =
        this.tilBrevPeriodeGrunnlagMedPersoner(
            restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
            erUregistrerteBarnPåbehandling = uregistrerteBarn.isNotEmpty(),
            barnPersonIdentMedReduksjon = barnPersonIdentMedReduksjon,
            erIngenOverlappVedtaksperiodeTogglePå = erIngenOverlappVedtaksperiodeTogglePå,
        )

    val begrunnelserOgFritekster = brevPeriodeGrunnlagMedPersoner.byggBegrunnelserOgFritekster(
        uregistrerteBarn = uregistrerteBarn,
        restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
        brevMålform = brevMålform,
        erIngenOverlappVedtaksperiodeTogglePå = erIngenOverlappVedtaksperiodeTogglePå,
    )

    if (begrunnelserOgFritekster.isEmpty()) return null

    val tomDato =
        if (brevPeriodeGrunnlagMedPersoner.tom?.erSenereEnnInneværendeMåned() == false)
            brevPeriodeGrunnlagMedPersoner.tom.tilDagMånedÅr()
        else null

    return if (erIngenOverlappVedtaksperiodeTogglePå) {
        brevPeriodeGrunnlagMedPersoner.byggBrevPeriode(
            restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
            brevMålform = brevMålform,
            tomDato = tomDato,
            begrunnelserOgFritekster = begrunnelserOgFritekster
        )
    } else when (brevPeriodeGrunnlagMedPersoner.type) {
        FORTSATT_INNVILGET -> brevPeriodeGrunnlagMedPersoner.hentFortsattInnvilgetBrevPeriode(
            målform = brevMålform,
            begrunnelserOgFritekster = begrunnelserOgFritekster
        )

        UTBETALING -> brevPeriodeGrunnlagMedPersoner.hentInnvilgelseBrevPeriode(
            tomDato = tomDato,
            begrunnelserOgFritekster = begrunnelserOgFritekster,
            personerPåBehandling = restBehandlingsgrunnlagForBrev.personerPåBehandling,
        )
        REDUKSJON -> brevPeriodeGrunnlagMedPersoner.hentReduksjonBrevPeriode(
            tomDato = tomDato,
            begrunnelserOgFritekster = begrunnelserOgFritekster,
            personerPåBehandling = restBehandlingsgrunnlagForBrev.personerPåBehandling,
        )

        ENDRET_UTBETALING -> brevPeriodeGrunnlagMedPersoner.hentEndretUtbetalingBrevPeriode(
            tomDato = tomDato,
            begrunnelserOgFritekster = begrunnelserOgFritekster,
            utvidetScenario = utvidetScenarioForEndringsperiode,
            målform = brevMålform
        )

        AVSLAG -> brevPeriodeGrunnlagMedPersoner.hentAvslagBrevPeriode(
            tomDato = tomDato,
            begrunnelserOgFritekster = begrunnelserOgFritekster
        )

        OPPHØR -> OpphørBrevPeriode(
            fom = brevPeriodeGrunnlagMedPersoner.fom!!.tilDagMånedÅr(),
            tom = tomDato,
            begrunnelser = begrunnelserOgFritekster
        )
    }
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
        REDUKSJON -> this.finnBarnIReduksjonPeriode(restBehandlingsgrunnlagForBrev.personerPåBehandling)
        OPPHØR -> emptyList()
        AVSLAG -> emptyList()
        FORTSATT_INNVILGET -> barnMedUtbetaling + barnMedNullutbetaling
        ENDRET_UTBETALING -> error("Skal ikke være endret utbetaling perioder når erIngenOverlappVedtaksperiodeTogglePå=true")
    }

    val utbetalingsbeløp = this.minimerteUtbetalingsperiodeDetaljer.totaltUtbetalt()
    return GenerellBrevPeriode(

        fom = this.hentFomTekst(brevMålform),
        tom = when {
            this.type == FORTSATT_INNVILGET -> ""
            tomDato.isNullOrBlank() -> ""
            else -> " til $tomDato"
        },
        belop = Utils.formaterBeløp(utbetalingsbeløp),
        begrunnelser = begrunnelserOgFritekster,
        brevPeriodeType = hentPeriodetype(this.fom, this, barnMedUtbetaling, utbetalingsbeløp),
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
    ENDRET_UTBETALING -> error("Skal ikke være endret utbetaling perioder når erIngenOverlappVedtaksperiodeTogglePå=true")
    OPPHØR -> fom!!.tilDagMånedÅr()
    AVSLAG -> if (fom != null) fom.tilDagMånedÅr() else ""
    REDUKSJON -> fom!!.tilDagMånedÅr()
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
    ENDRET_UTBETALING -> error("Skal ikke være endret utbetaling med erIngenOverlappVedtaksperiodeTogglePå=true")
    AVSLAG -> if (fom != null) BrevPeriodeType.AVSLAG else BrevPeriodeType.AVSLAG_UTEN_PERIODE
    OPPHØR -> BrevPeriodeType.OPPHOR
    REDUKSJON -> BrevPeriodeType.INNVILGELSE
}

private fun BrevPeriodeGrunnlagMedPersoner.hentAvslagBrevPeriode(
    tomDato: String?,
    begrunnelserOgFritekster: List<Begrunnelse>,
) =
    if (this.fom != null)
        AvslagBrevPeriode(
            fom = fom.tilDagMånedÅr(),
            tom = tomDato,
            begrunnelser = begrunnelserOgFritekster
        )
    else AvslagUtenPeriodeBrevPeriode(begrunnelser = begrunnelserOgFritekster)

fun BrevPeriodeGrunnlagMedPersoner.hentEndretUtbetalingBrevPeriode(
    tomDato: String?,
    begrunnelserOgFritekster: List<Begrunnelse>,
    utvidetScenario: UtvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
    målform: Målform = Målform.NB,
): EndretUtbetalingBrevPeriode {
    val ingenUtbetalingForEndringsperioden =
        minimerteUtbetalingsperiodeDetaljer.all { it.prosent == BigDecimal.ZERO }

    return EndretUtbetalingBrevPeriode(
        fom = this.fom!!.tilDagMånedÅr(),
        tom = tomDato,
        barnasFodselsdager = this.minimerteUtbetalingsperiodeDetaljer.map { it.person }.tilBarnasFødselsdatoer(),
        begrunnelser = begrunnelserOgFritekster,
        belop = Utils.formaterBeløp(this.minimerteUtbetalingsperiodeDetaljer.totaltUtbetalt()),
        type = when {
            ingenUtbetalingForEndringsperioden && utvidetScenario == UtvidetScenarioForEndringsperiode.UTVIDET_YTELSE_IKKE_ENDRET ->
                EndretUtbetalingBrevPeriodeType.ENDRET_UTBETALINGSPERIODE_DELVIS_UTBETALING
            ingenUtbetalingForEndringsperioden ->
                EndretUtbetalingBrevPeriodeType.ENDRET_UTBETALINGSPERIODE_INGEN_UTBETALING
            else ->
                EndretUtbetalingBrevPeriodeType.ENDRET_UTBETALINGSPERIODE
        },
        typeBarnetrygd = if (utvidetScenario == UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE)
            EndretUtbetalingBarnetrygdType.DELT
        else when (målform) {
            Målform.NB -> EndretUtbetalingBarnetrygdType.DELT_UTVIDET_NB
            Målform.NN -> EndretUtbetalingBarnetrygdType.DELT_UTVIDET_NN
        }
    )
}

private fun BrevPeriodeGrunnlagMedPersoner.hentInnvilgelseBrevPeriode(
    tomDato: String?,
    begrunnelserOgFritekster: List<Begrunnelse>,
    personerPåBehandling: List<MinimertRestPerson>,
): InnvilgelseBrevPeriode {
    val barnIPeriode = this.finnBarnIInnvilgelsePeriode(personerPåBehandling)

    return InnvilgelseBrevPeriode(
        fom = this.fom!!.tilDagMånedÅr(),
        tom = tomDato,
        belop = Utils.formaterBeløp(this.minimerteUtbetalingsperiodeDetaljer.totaltUtbetalt()),
        antallBarn = barnIPeriode.size.toString(),
        barnasFodselsdager = barnIPeriode.tilBarnasFødselsdatoer(),
        begrunnelser = begrunnelserOgFritekster
    )
}

private fun BrevPeriodeGrunnlagMedPersoner.hentReduksjonBrevPeriode(
    tomDato: String?,
    begrunnelserOgFritekster: List<Begrunnelse>,
    personerPåBehandling: List<MinimertRestPerson>,
): InnvilgelseBrevPeriode {
    val barnIPeriode = this.finnBarnIReduksjonPeriode(personerPåBehandling)

    return InnvilgelseBrevPeriode(
        fom = this.fom!!.tilDagMånedÅr(),
        tom = tomDato,
        belop = Utils.formaterBeløp(this.minimerteUtbetalingsperiodeDetaljer.totaltUtbetalt()),
        antallBarn = barnIPeriode.size.toString(),
        barnasFodselsdager = barnIPeriode.tilBarnasFødselsdatoer(),
        begrunnelser = begrunnelserOgFritekster
    )
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

fun BrevPeriodeGrunnlagMedPersoner.finnBarnIReduksjonPeriode(
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

private fun BrevPeriodeGrunnlagMedPersoner.hentFortsattInnvilgetBrevPeriode(
    målform: Målform,
    begrunnelserOgFritekster: List<Begrunnelse>
): FortsattInnvilgetBrevPeriode {

    val fom = hentFomtekstFortsattInnvilget(
        målform = målform,
        fom = this.fom,
        begrunnelser = this.begrunnelser.map { it.standardbegrunnelse }
    )
    return FortsattInnvilgetBrevPeriode(
        fom = fom ?: "Du får:",
        belop = Utils.formaterBeløp(this.minimerteUtbetalingsperiodeDetaljer.totaltUtbetalt()),
        antallBarn = this.minimerteUtbetalingsperiodeDetaljer.antallBarn().toString(),
        barnasFodselsdager = this.minimerteUtbetalingsperiodeDetaljer.map { it.person }.tilBarnasFødselsdatoer(),
        begrunnelser = begrunnelserOgFritekster
    )
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
