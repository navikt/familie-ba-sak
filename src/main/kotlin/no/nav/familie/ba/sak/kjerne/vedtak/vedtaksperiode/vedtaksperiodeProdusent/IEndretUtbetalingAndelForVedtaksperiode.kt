package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent

import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.IUtfyltEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.UtfyltEndretUtbetalingAndelDeltBosted
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import java.math.BigDecimal
import java.time.LocalDate

sealed interface IEndretUtbetalingAndelForVedtaksperiode {
    val prosent: BigDecimal
    val årsak: Årsak
    val søknadstidspunkt: LocalDate
}

data class EndretUtbetalingAndelForVedtaksperiode(
    override val prosent: BigDecimal,
    override val årsak: Årsak,
    override val søknadstidspunkt: LocalDate,
) : IEndretUtbetalingAndelForVedtaksperiode

data class EndretUtbetalingAndelForVedtaksperiodeDeltBosted(
    override val prosent: BigDecimal,
    override val søknadstidspunkt: LocalDate,
    val avtaletidspunktDeltBosted: LocalDate,
) : IEndretUtbetalingAndelForVedtaksperiode {
    override val årsak: Årsak = Årsak.DELT_BOSTED
}

fun IUtfyltEndretUtbetalingAndel.tilEndretUtbetalingAndelForVedtaksperiode(): IEndretUtbetalingAndelForVedtaksperiode =
    if (this is UtfyltEndretUtbetalingAndelDeltBosted) {
        EndretUtbetalingAndelForVedtaksperiodeDeltBosted(
            prosent = this.prosent,
            søknadstidspunkt = this.søknadstidspunkt,
            avtaletidspunktDeltBosted = this.avtaletidspunktDeltBosted,
        )
    } else {
        EndretUtbetalingAndelForVedtaksperiode(
            prosent = this.prosent,
            årsak = this.årsak,
            søknadstidspunkt = this.søknadstidspunkt,
        )
    }
