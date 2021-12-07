package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.UregistrertBarnEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.FritekstBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilBrevBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.utbetaltForPersonerIBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import java.time.LocalDate

data class BrevPeriodeGrunnlagMedPersoner(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<BrevBegrunnelseGrunnlagMedPersoner>,
    val fritekster: List<String> = emptyList(),
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj> = emptyList(),
    val erFørsteVedtaksperiodePåFagsak: Boolean,
) {
    fun byggBegrunnelserOgFritekster(
        begrunnelseGrunnlag: BegrunnelseGrunnlag,
        uregistrerteBarn: List<UregistrertBarnEnkel> = emptyList(),
        målformSøker: Målform
    ): List<Begrunnelse> {

        val begrunnelser =
            this.begrunnelser.sortedBy { it.vedtakBegrunnelseType }.map { brevBegrunnelseGrunnlag ->
                val beløp = Utils.formaterBeløp(
                    this.utbetaltForPersonerIBegrunnelse(brevBegrunnelseGrunnlag.personIdenter)
                )

                brevBegrunnelseGrunnlag.tilBrevBegrunnelse(
                    vedtaksperiode = NullablePeriode(this.fom, this.tom),
                    personerIPersongrunnlag = begrunnelseGrunnlag.begrunnelsePersoner,
                    målformSøker = målformSøker,
                    uregistrerteBarn = uregistrerteBarn,
                    beløp = beløp,
                )
            }

        val fritekster = fritekster.map { FritekstBegrunnelse(it) }

        return begrunnelser + fritekster
    }
}