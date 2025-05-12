package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent

import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.IUtfyltEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.UtfyltEndretUtbetalingAndelDeltBosted
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

sealed interface IEndretUtbetalingAndelForVedtaksperiode {
    val prosent: BigDecimal
    val årsak: Årsak
    val søknadstidspunkt: LocalDate
}

data class EndretUtbetalingAndelForVedtaksperiode(
    override val prosent: BigDecimal,
    override val årsak: Årsak,
    override val søknadstidspunkt: LocalDate,
    val fom: YearMonth,
    val tom: YearMonth,
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
            fom = this.fom,
            tom = this.tom,
        )
    }
