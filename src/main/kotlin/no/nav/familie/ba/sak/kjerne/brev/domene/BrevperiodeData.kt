package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.brev.UtvidetScenarioForEndringsperiode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse
import org.slf4j.LoggerFactory

data class BrevperiodeData(
    val restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
    val erFørsteVedtaksperiodePåFagsak: Boolean,
    val uregistrerteBarn: List<MinimertUregistrertBarn>,
    val brevMålform: Målform,
    val minimertVedtaksperiode: MinimertVedtaksperiode,
    val utvidetScenarioForEndringsperiode: UtvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
) {
    fun hentBegrunnelserOgFritekster(): List<Begrunnelse> {
        return try {
            minimertVedtaksperiode
                .tilBrevPeriodeGrunnlagMedPersoner(
                    restBehandlingsgrunnlagForBrev = this.restBehandlingsgrunnlagForBrev,
                    erFørsteVedtaksperiodePåFagsak = this.erFørsteVedtaksperiodePåFagsak,
                    erUregistrerteBarnPåbehandling = this.uregistrerteBarn.isNotEmpty(),
                )
                .byggBegrunnelserOgFritekster(
                    restBehandlingsgrunnlagForBrev = this.restBehandlingsgrunnlagForBrev,
                    uregistrerteBarn = this.uregistrerteBarn,
                    brevMålform = this.brevMålform
                )
        } catch (exception: Exception) {
            val brevPeriodeForLogging = this.tilBrevperiodeForLogging()

            secureLogger.error(
                "Feil ved generering av brevbegrunnelse. Data som ble sendt inn var: ${
                brevPeriodeForLogging.convertDataClassToJson()
                }",
                exception
            )
            throw Feil(message = "Feil ved generering av brevbegrunnelse: ", throwable = exception)
        }
    }

    fun tilBrevperiodeForLogging() =
        minimertVedtaksperiode.tilBrevPeriodeForLogging(
            restBehandlingsgrunnlagForBrev = this.restBehandlingsgrunnlagForBrev,
            uregistrerteBarn = this.uregistrerteBarn,
            brevMålform = this.brevMålform,
        )

    companion object {

        private val secureLogger = LoggerFactory.getLogger("secureLogger")
        private val logger = LoggerFactory.getLogger(BrevperiodeData::class.java)
    }
}
