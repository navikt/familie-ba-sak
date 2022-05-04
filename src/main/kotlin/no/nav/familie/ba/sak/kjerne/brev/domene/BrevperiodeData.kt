package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype

data class BrevperiodeData(
    val restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
    val erFørsteVedtaksperiodePåFagsak: Boolean,
    val uregistrerteBarn: List<MinimertUregistrertBarn>,
    val brevMålform: Målform,
    val minimertVedtaksperiode: MinimertVedtaksperiode,
    val barnMedReduksjonFraForrigeBehandlingIdent: List<String> = emptyList()
) : Comparable<BrevperiodeData> {
    fun hentBegrunnelserOgFritekster(): List<Begrunnelse> =
        minimertVedtaksperiode
            .tilBrevPeriodeGrunnlagMedPersoner(
                restBehandlingsgrunnlagForBrev = this.restBehandlingsgrunnlagForBrev,
                erFørsteVedtaksperiodePåFagsak = this.erFørsteVedtaksperiodePåFagsak,
                erUregistrerteBarnPåbehandling = this.uregistrerteBarn.isNotEmpty(),
                barnMedReduksjonFraForrigeBehandlingIdent = barnMedReduksjonFraForrigeBehandlingIdent,
            )
            .byggBegrunnelserOgFritekster(
                restBehandlingsgrunnlagForBrev = this.restBehandlingsgrunnlagForBrev,
                uregistrerteBarn = this.uregistrerteBarn,
                brevMålform = this.brevMålform,
            )

    fun tilBrevperiodeForLogging() =
        minimertVedtaksperiode.tilBrevPeriodeForLogging(
            restBehandlingsgrunnlagForBrev = this.restBehandlingsgrunnlagForBrev,
            uregistrerteBarn = this.uregistrerteBarn,
            brevMålform = this.brevMålform,
            barnMedReduksjonFraForrigeBehandlingIdent = this.barnMedReduksjonFraForrigeBehandlingIdent
        )

    override fun compareTo(other: BrevperiodeData): Int {
        val fomCompared = (this.minimertVedtaksperiode.fom ?: TIDENES_MORGEN)
            .compareTo(other.minimertVedtaksperiode.fom ?: TIDENES_MORGEN)

        return when {
            this.minimertVedtaksperiode.type == Vedtaksperiodetype.AVSLAG &&
                other.minimertVedtaksperiode.type == Vedtaksperiodetype.AVSLAG -> fomCompared
            this.minimertVedtaksperiode.type == Vedtaksperiodetype.AVSLAG -> 1
            other.minimertVedtaksperiode.type == Vedtaksperiodetype.AVSLAG -> -1
            else -> fomCompared
        }
    }
}
