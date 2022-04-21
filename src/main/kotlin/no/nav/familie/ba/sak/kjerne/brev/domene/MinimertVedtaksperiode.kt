package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilBrevPeriodeTestPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import java.time.LocalDate

data class MinimertVedtaksperiode(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<BrevBegrunnelseGrunnlag>,
    val fritekster: List<String> = emptyList(),
    val minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj> = emptyList(),
) {
    fun tilBrevPeriodeGrunnlagMedPersoner(
        restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
        erFørsteVedtaksperiodePåFagsak: Boolean,
        erUregistrerteBarnPåbehandling: Boolean,
        barnPersonIdentMedReduksjon: List<String> = emptyList(),
    ): BrevPeriodeGrunnlagMedPersoner {
        return BrevPeriodeGrunnlagMedPersoner(
            fom = this.fom,
            tom = this.tom,
            type = this.type,
            begrunnelser = this.begrunnelser.flatMap {
                it.tilBrevBegrunnelseGrunnlagMedPersoner(
                    periode = NullablePeriode(
                        fom = this.fom,
                        tom = this.tom
                    ),
                    vedtaksperiodetype = type,
                    restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
                    identerMedUtbetalingPåPeriode = this.minimerteUtbetalingsperiodeDetaljer
                        .map { utbetalingsperiodeDetalj -> utbetalingsperiodeDetalj.person.personIdent },
                    minimerteUtbetalingsperiodeDetaljer = this.minimerteUtbetalingsperiodeDetaljer,
                    erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
                    erUregistrerteBarnPåbehandling = erUregistrerteBarnPåbehandling,
                    barnPersonIdentMedReduksjon = barnPersonIdentMedReduksjon,
                )
            },
            fritekster = this.fritekster,
            minimerteUtbetalingsperiodeDetaljer = this.minimerteUtbetalingsperiodeDetaljer,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
        )
    }
}

fun UtvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(
    sanityBegrunnelser: List<SanityBegrunnelse>
): MinimertVedtaksperiode {
    return MinimertVedtaksperiode(
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        fritekster = this.fritekster,
        minimerteUtbetalingsperiodeDetaljer = this.utbetalingsperiodeDetaljer.map { it.tilMinimertUtbetalingsperiodeDetalj() },
        begrunnelser = this.begrunnelser.map { it.tilBrevBegrunnelseGrunnlag(sanityBegrunnelser) }
    )
}

fun MinimertVedtaksperiode.tilBrevPeriodeForLogging(
    restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
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
        uregistrerteBarn = uregistrerteBarn.map { it.copy(personIdent = "", navn = "") },
        erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
        brevMålform = brevMålform,
    )
}
