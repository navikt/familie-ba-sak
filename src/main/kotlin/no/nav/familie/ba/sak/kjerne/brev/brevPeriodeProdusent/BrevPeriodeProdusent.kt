package no.nav.familie.ba.sak.kjerne.brev.brevPeriodeProdusent

import lagBrevBegrunnelse
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.GrunnlagForBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.lagBrevBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BrevBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.FritekstBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.hentBrevPeriodeType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.IBegrunnelseGrunnlagForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.erUtbetalingEllerDeltBostedIPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.finnBegrunnelseGrunnlagPerPerson
import java.math.BigDecimal

fun VedtaksperiodeMedBegrunnelser.lagBrevPeriode(
    grunnlagForBegrunnelse: GrunnlagForBegrunnelse,
    landkoder: Map<String, String>,
): BrevPeriode? {
    val begrunnelsesGrunnlagPerPerson = this.finnBegrunnelseGrunnlagPerPerson(grunnlagForBegrunnelse)

    val begrunnelserOgFritekster =
        hentBegrunnelser(grunnlagForBegrunnelse, begrunnelsesGrunnlagPerPerson, landkoder)

    if (begrunnelserOgFritekster.isEmpty()) return null

    return this.byggBrevPeriode(
        begrunnelserOgFritekster = begrunnelserOgFritekster,
        begrunnelseGrunnlagPerPerson = begrunnelsesGrunnlagPerPerson,
        grunnlagForBegrunnelse = grunnlagForBegrunnelse,
    )
}

fun VedtaksperiodeMedBegrunnelser.hentBegrunnelser(
    grunnlagForBegrunnelse: GrunnlagForBegrunnelse,
    begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>,
    landkoder: Map<String, String>,
): List<BrevBegrunnelse> {
    val standardbegrunnelser =
        this.begrunnelser.map { it.standardbegrunnelse }.toSet().flatMap {
            it.lagBrevBegrunnelse(
                this,
                grunnlagForBegrunnelse,
                begrunnelsesGrunnlagPerPerson,
            )
        }

    val eøsBegrunnelser =
        this.eøsBegrunnelser.map { it.begrunnelse }.toSet().flatMap {
            it.lagBrevBegrunnelse(
                this,
                grunnlagForBegrunnelse,
                begrunnelsesGrunnlagPerPerson,
                landkoder,
            )
        }

    val fritekster = this.fritekster.map { FritekstBegrunnelse(it.fritekst) }

    return standardbegrunnelser + eøsBegrunnelser + fritekster
}

private fun VedtaksperiodeMedBegrunnelser.byggBrevPeriode(
    begrunnelserOgFritekster: List<BrevBegrunnelse>,
    begrunnelseGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>,
    grunnlagForBegrunnelse: GrunnlagForBegrunnelse,
): BrevPeriode {
    val barnMedUtbetaling = begrunnelseGrunnlagPerPerson.finnBarnMedUtbetaling()
    val erUtvidetIPeriode = begrunnelseGrunnlagPerPerson.erUtvidetIPeriode()
    val barnMedUtbetalingEllerAlleredeBetaltIUtvidetPeriode =
        if (barnMedUtbetaling.isEmpty() && erUtvidetIPeriode) {
            begrunnelseGrunnlagPerPerson.finnBarnMedAlleredeUtbetalt()
        } else {
            barnMedUtbetaling
        }
    val beløp = begrunnelseGrunnlagPerPerson.hentTotaltUtbetaltIPeriode()

    val brevPeriodeType =
        hentBrevPeriodeType(
            vedtaksperiodeMedBegrunnelser = this,
            erUtbetalingEllerDeltBostedIPeriode = erUtbetalingEllerDeltBostedIPeriode(begrunnelseGrunnlagPerPerson),
        )

    return BrevPeriode(
        fom = this.fom?.tilMånedÅr() ?: "",
        tom = hentTomTekstForBrev(brevPeriodeType),
        beløp = beløp.toString(),
        begrunnelser = begrunnelserOgFritekster,
        brevPeriodeType = brevPeriodeType,
        antallBarn = barnMedUtbetalingEllerAlleredeBetaltIUtvidetPeriode.size.toString(),
        barnasFodselsdager = barnMedUtbetalingEllerAlleredeBetaltIUtvidetPeriode.tilBarnasFødselsdatoer(),
        duEllerInstitusjonen =
            hentDuEllerInstitusjonenTekst(
                brevPeriodeType = brevPeriodeType,
                fagsakType = grunnlagForBegrunnelse.behandlingsGrunnlagForVedtaksperioder.behandling.fagsak.type,
            ),
    )
}

private fun VedtaksperiodeMedBegrunnelser.hentTomTekstForBrev(
    brevPeriodeType: BrevPeriodeType,
) = if (this.tom == null) {
    ""
} else {
    val tomDato = this.tom.tilMånedÅr()
    when (brevPeriodeType) {
        BrevPeriodeType.UTBETALING -> "til $tomDato"
        BrevPeriodeType.INGEN_UTBETALING -> if (this.type == Vedtaksperiodetype.AVSLAG) "til og med $tomDato " else ""
        BrevPeriodeType.INGEN_UTBETALING_UTEN_PERIODE -> ""
        BrevPeriodeType.FORTSATT_INNVILGET -> ""
        else -> error("$brevPeriodeType skal ikke brukes")
    }
}

private fun Map<Person, IBegrunnelseGrunnlagForPeriode>.hentTotaltUtbetaltIPeriode() =
    this.values.sumOf { it.dennePerioden.andeler.sumOf { andeler -> andeler.kalkulertUtbetalingsbeløp } }

private fun Map<Person, IBegrunnelseGrunnlagForPeriode>.finnBarnMedUtbetaling(): Set<Person> =
    this
        .filterKeys { it.type == PersonType.BARN }
        .filterValues { grunnlag ->
            val endretUtbetalingGjelderDeltBosted =
                grunnlag.dennePerioden.endretUtbetalingAndel?.årsak == Årsak.DELT_BOSTED
            val harAndelerSomIkkeErPåNullProsent =
                grunnlag.dennePerioden.andeler.any { it.prosent != BigDecimal.ZERO }

            harAndelerSomIkkeErPåNullProsent || endretUtbetalingGjelderDeltBosted
        }.keys

private fun Map<Person, IBegrunnelseGrunnlagForPeriode>.erUtvidetIPeriode(): Boolean =
    this.any {
        it.value.dennePerioden.andeler
            .any { andel -> andel.type == YtelseType.UTVIDET_BARNETRYGD && andel.kalkulertUtbetalingsbeløp > 0 }
    }

private fun Map<Person, IBegrunnelseGrunnlagForPeriode>.finnBarnMedAlleredeUtbetalt(): Set<Person> =
    this
        .filterKeys { it.type == PersonType.BARN }
        .filterValues { grunnlag -> grunnlag.dennePerioden.endretUtbetalingAndel?.årsak == Årsak.ALLEREDE_UTBETALT }
        .keys

fun Set<Person>.tilBarnasFødselsdatoer(): String {
    val barnasFødselsdatoerListe: List<String> =
        this
            .filter { it.type == PersonType.BARN }
            .sortedBy { it.fødselsdato }
            .map { it.fødselsdato.tilKortString() }

    return Utils.slåSammen(barnasFødselsdatoerListe)
}

private fun hentDuEllerInstitusjonenTekst(
    brevPeriodeType: BrevPeriodeType,
    fagsakType: FagsakType,
): String =
    when (fagsakType) {
        FagsakType.INSTITUSJON -> {
            when (brevPeriodeType) {
                BrevPeriodeType.UTBETALING, BrevPeriodeType.INGEN_UTBETALING -> "institusjonen"
                else -> "Institusjonen"
            }
        }

        FagsakType.NORMAL, FagsakType.BARN_ENSLIG_MINDREÅRIG -> {
            when (brevPeriodeType) {
                BrevPeriodeType.UTBETALING, BrevPeriodeType.INGEN_UTBETALING -> "du"
                else -> "Du"
            }
        }
    }
