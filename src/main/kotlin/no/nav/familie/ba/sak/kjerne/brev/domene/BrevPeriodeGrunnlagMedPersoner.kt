package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.brev.totaltUtbetalt
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.FritekstBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilBrevBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import java.time.LocalDate

data class BrevPeriodeGrunnlagMedPersoner(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<BrevBegrunnelseGrunnlagMedPersoner>,
    val fritekster: List<String> = emptyList(),
    val minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj> = emptyList(),
    val erFørsteVedtaksperiodePåFagsak: Boolean,
) {
    fun byggBegrunnelserOgFritekster(
        restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
        uregistrerteBarn: List<MinimertUregistrertBarn> = emptyList(),
        brevMålform: Målform
    ): List<Begrunnelse> {

        val begrunnelser = this.begrunnelser
            .sortedBy { it.vedtakBegrunnelseType }
            .map {
                it.tilBrevBegrunnelse(
                    vedtaksperiode = NullablePeriode(this.fom, this.tom),
                    personerIPersongrunnlag = restBehandlingsgrunnlagForBrev.personerPåBehandling,
                    brevMålform = brevMålform,
                    uregistrerteBarn = uregistrerteBarn,
                    beløp = Utils.formaterBeløp(this.beløpUtbetaltFor(it.personIdenter)),
                )
            }

        val fritekster = fritekster.map { FritekstBegrunnelse(it) }

        return begrunnelser + fritekster
    }

    private fun beløpUtbetaltFor(personIdenter: List<String>) =
        this.minimerteUtbetalingsperiodeDetaljer
            .filter { utbetalingsperiodeDetalj -> personIdenter.contains(utbetalingsperiodeDetalj.person.personIdent) }
            .totaltUtbetalt()
}
