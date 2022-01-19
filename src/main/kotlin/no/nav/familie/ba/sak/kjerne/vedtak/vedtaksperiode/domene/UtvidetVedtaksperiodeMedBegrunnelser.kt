package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.tilBrevPeriodeForLogging
import no.nav.familie.ba.sak.kjerne.brev.domene.tilBrevPeriodeGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilRestVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.RestVedtaksbegrunnelse
import org.slf4j.LoggerFactory
import java.time.LocalDate

private val secureLogger = LoggerFactory.getLogger("secureLogger")

data class UtvidetVedtaksperiodeMedBegrunnelser(
    val id: Long,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<RestVedtaksbegrunnelse>,
    val fritekster: List<String> = emptyList(),
    val gyldigeBegrunnelser: List<VedtakBegrunnelseSpesifikasjon> = emptyList(),
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj> = emptyList(),
) {
    fun hentMånedPeriode() = MånedPeriode(
        (this.fom ?: TIDENES_MORGEN).toYearMonth(),
        (this.tom ?: TIDENES_ENDE).toYearMonth()
    )

    fun hentBegrunnelserOgFritekster(
        restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
        sanityBegrunnelser: List<SanityBegrunnelse>,
        erFørsteVedtaksperiodePåFagsak: Boolean,
        uregistrerteBarn: List<MinimertUregistrertBarn>,
        brevMålform: Målform,
    ): List<Begrunnelse> {
        val brevPeriodeGrunnlag = this
            .tilBrevPeriodeGrunnlag(sanityBegrunnelser)
        return try {
            brevPeriodeGrunnlag
                .tilBrevPeriodeGrunnlagMedPersoner(
                    restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
                    erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak
                )
                .byggBegrunnelserOgFritekster(
                    restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
                    uregistrerteBarn = uregistrerteBarn,
                    brevMålform = brevMålform
                )
        } catch (exception: Exception) {
            val brevPeriodeForLogging = brevPeriodeGrunnlag.tilBrevPeriodeForLogging(
                restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
                uregistrerteBarn = uregistrerteBarn,
                brevMålform = brevMålform,
            )

            secureLogger.error(
                "Feil ved generering av brevbegrunnelse. Data som ble sendt inn var: ${
                brevPeriodeForLogging.convertDataClassToJson()
                }",
                exception
            )
            throw Feil(message = "Feil ved generering av brevbegrunnelse: ", throwable = exception)
        }
    }
}

fun List<UtvidetVedtaksperiodeMedBegrunnelser>.sorter(): List<UtvidetVedtaksperiodeMedBegrunnelser> {
    val (perioderMedFom, perioderUtenFom) = this.partition { it.fom != null }
    return perioderMedFom.sortedWith(compareBy { it.fom }) + perioderUtenFom
}

fun VedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    sanityBegrunnelser: List<SanityBegrunnelse>
): UtvidetVedtaksperiodeMedBegrunnelser {

    val utbetalingsperiodeDetaljer = hentUtbetalingsperiodeDetaljer(
        andelerTilkjentYtelse = andelerTilkjentYtelse,
        personopplysningGrunnlag = personopplysningGrunnlag,
        sanityBegrunnelser = sanityBegrunnelser
    )

    return UtvidetVedtaksperiodeMedBegrunnelser(
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        begrunnelser = this.begrunnelser.map { it.tilRestVedtaksbegrunnelse() },
        fritekster = this.fritekster.sortedBy { it.id }.map { it.fritekst },
        utbetalingsperiodeDetaljer = utbetalingsperiodeDetaljer
    )
}
