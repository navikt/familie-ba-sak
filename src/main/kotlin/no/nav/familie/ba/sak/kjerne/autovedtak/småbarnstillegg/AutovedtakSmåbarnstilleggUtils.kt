package no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.springframework.http.HttpStatus

class VedtaksperiodefinnerSmåbarnstilleggFeil(
    melding: String,
    override val frontendFeilmelding: String? = null,
    override val httpStatus: HttpStatus = HttpStatus.OK,
    override val throwable: Throwable? = null,
) : Feil(
        melding,
        frontendFeilmelding,
        httpStatus,
        throwable,
    )

@Throws(VedtaksperiodefinnerSmåbarnstilleggFeil::class)
internal fun finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse(
    innvilgetMånedPeriode: MånedPeriode?,
    redusertMånedPeriode: MånedPeriode?,
    vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
): VedtaksperiodeMedBegrunnelser {
    val vedtaksperiodeSomSkalOppdateresOgBegrunnelse: Pair<VedtaksperiodeMedBegrunnelser?, Standardbegrunnelse>? =
        when {
            innvilgetMånedPeriode == null && redusertMånedPeriode == null -> {
                null
            }

            innvilgetMånedPeriode != null && redusertMånedPeriode == null -> {
                Pair(
                    vedtaksperioderMedBegrunnelser.find { it.fom?.toYearMonth() == innvilgetMånedPeriode.fom && it.type == Vedtaksperiodetype.UTBETALING },
                    Standardbegrunnelse.INNVILGET_SMÅBARNSTILLEGG,
                )
            }

            innvilgetMånedPeriode == null && redusertMånedPeriode != null -> {
                Pair(
                    vedtaksperioderMedBegrunnelser.find { it.fom?.toYearMonth() == redusertMånedPeriode.fom && it.type == Vedtaksperiodetype.UTBETALING },
                    Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD,
                )
            }

            else -> {
                null
            }
        }

    val vedtaksperiodeSomSkalOppdateres = vedtaksperiodeSomSkalOppdateresOgBegrunnelse?.first
    if (vedtaksperiodeSomSkalOppdateres == null) {
        secureLogger.info(
            "Finner ikke aktuell periode å begrunne ved autovedtak småbarnstillegg.\n" +
                "Innvilget periode: $innvilgetMånedPeriode.\n" +
                "Redusert periode: $redusertMånedPeriode.\n" +
                "Perioder: ${vedtaksperioderMedBegrunnelser.map { "Periode(type=${it.type}, fom=${it.fom}, tom=${it.tom})" }}",
        )

        throw VedtaksperiodefinnerSmåbarnstilleggFeil("Finner ikke aktuell periode å begrunne ved autovedtak småbarnstillegg. Se securelogger for å periodene som ble generert.")
    }

    vedtaksperiodeSomSkalOppdateres.settBegrunnelser(
        vedtaksperiodeSomSkalOppdateres.begrunnelser.toList() +
            listOf(
                Vedtaksbegrunnelse(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeSomSkalOppdateres,
                    standardbegrunnelse = vedtaksperiodeSomSkalOppdateresOgBegrunnelse.second,
                ),
            ),
    )

    return vedtaksperiodeSomSkalOppdateres
}
