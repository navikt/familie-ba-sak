package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.behandlingsresultat.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.brev.UtvidetScenarioForEndringsperiode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse

data class BrevperiodeData(
    val restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
    val erFørsteVedtaksperiodePåFagsak: Boolean,
    val uregistrerteBarn: List<MinimertUregistrertBarn>,
    val brevMålform: Målform,
    val minimertVedtaksperiode: MinimertVedtaksperiode,
    val utvidetScenarioForEndringsperiode: UtvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
) {
    fun hentBegrunnelserOgFritekster(erIngenOverlappVedtaksperiodeTogglePå: Boolean): List<Begrunnelse> =
        minimertVedtaksperiode
            .tilBrevPeriodeGrunnlagMedPersoner(
                restBehandlingsgrunnlagForBrev = this.restBehandlingsgrunnlagForBrev,
                erFørsteVedtaksperiodePåFagsak = this.erFørsteVedtaksperiodePåFagsak,
                erUregistrerteBarnPåbehandling = this.uregistrerteBarn.isNotEmpty(),
            )
            .byggBegrunnelserOgFritekster(
                restBehandlingsgrunnlagForBrev = this.restBehandlingsgrunnlagForBrev,
                uregistrerteBarn = this.uregistrerteBarn,
                brevMålform = this.brevMålform,
                erIngenOverlappVedtaksperiodeTogglePå = erIngenOverlappVedtaksperiodeTogglePå
            )

    fun tilBrevperiodeForLogging() =
        minimertVedtaksperiode.tilBrevPeriodeForLogging(
            restBehandlingsgrunnlagForBrev = this.restBehandlingsgrunnlagForBrev,
            uregistrerteBarn = this.uregistrerteBarn,
            brevMålform = this.brevMålform,
        )
}
