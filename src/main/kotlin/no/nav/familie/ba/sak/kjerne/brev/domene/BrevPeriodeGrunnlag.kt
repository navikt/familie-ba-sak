package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.brev.UtvidetScenarioForEndringsperiode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilBrevPeriodeTestPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import java.time.LocalDate

data class BrevPeriodeGrunnlag(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<BrevBegrunnelseGrunnlag>,
    val fritekster: List<String> = emptyList(),
    val minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj> = emptyList(),
) {
    fun tilBrevPeriodeGrunnlagMedPersoner(
        restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
        erFørsteVedtaksperiodePåFagsak: Boolean
    ): BrevPeriodeGrunnlagMedPersoner {
        return BrevPeriodeGrunnlagMedPersoner(
            fom = this.fom,
            tom = this.tom,
            type = this.type,
            begrunnelser = this.begrunnelser.map {
                it.tilBrevBegrunnelseGrunnlagMedPersoner(
                    periode = NullablePeriode(
                        fom = this.fom,
                        tom = this.tom
                    ),
                    restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
                    identerMedUtbetalingPåPeriode = this.minimerteUtbetalingsperiodeDetaljer
                        .map { utbetalingsperiodeDetalj -> utbetalingsperiodeDetalj.person.personIdent },
                    erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak
                )
            },
            fritekster = this.fritekster,
            minimerteUtbetalingsperiodeDetaljer = this.minimerteUtbetalingsperiodeDetaljer,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
        )
    }

    fun hentMånedPeriode() = MånedPeriode(
        (fom ?: TIDENES_MORGEN).toYearMonth(),
        (tom ?: TIDENES_ENDE).toYearMonth()
    )
}

fun UtvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriodeGrunnlag(
    sanityBegrunnelser: List<SanityBegrunnelse>
): BrevPeriodeGrunnlag {
    return BrevPeriodeGrunnlag(
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        fritekster = this.fritekster,
        minimerteUtbetalingsperiodeDetaljer = this.utbetalingsperiodeDetaljer.map { it.tilMinimertUtbetalingsperiodeDetalj() },
        begrunnelser = this.begrunnelser.map { it.tilBrevBegrunnelseGrunnlag(sanityBegrunnelser) }
    )
}

fun BrevPeriodeGrunnlag.tilBrevPeriodeForLogging(
    restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
    utvidetScenarioForEndringsperiode: UtvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
    uregistrerteBarn: List<MinimertUregistrertBarn> = emptyList(),
    erFørsteVedtaksperiodePåFagsak: Boolean = false,
    brevMålform: Målform
): BrevPeriodeForLogging {

    return BrevPeriodeForLogging(
        fom = this.fom,
        tom = this.tom,
        vedtaksperiodetype = this.type,
        begrunnelser = this.begrunnelser.map { it.tilBrevBegrunnelseGrunnlagForLogging() },
        fritekster = this.fritekster,
        personerPåBehandling = restBehandlingsgrunnlagForBrev.personerPåBehandling.map {
            it.tilBrevPeriodeTestPerson(
                brevPeriodeGrunnlag = this,
                restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev
            )
        },
        utvidetScenarioForEndringsperiode = utvidetScenarioForEndringsperiode,
        uregistrerteBarn = uregistrerteBarn,
        erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
        brevMålform = brevMålform,
    )
}
