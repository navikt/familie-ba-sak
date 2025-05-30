package no.nav.familie.ba.sak.kjerne.simulering.domene

import java.math.BigDecimal
import java.time.LocalDate

data class Simulering(
    val perioder: List<SimuleringsPeriode>,
    val fomDatoNestePeriode: LocalDate?,
    val etterbetaling: BigDecimal,
    val feilutbetaling: BigDecimal,
    val fom: LocalDate?,
    val tomDatoNestePeriode: LocalDate?,
    val forfallsdatoNestePeriode: LocalDate?,
    val tidSimuleringHentet: LocalDate?,
    val tomSisteUtbetaling: LocalDate?,
) {
    fun tilRestSimulering(avregningsperioder: List<AvregningPeriode>): RestSimulering =
        RestSimulering(
            perioder = perioder,
            fomDatoNestePeriode = fomDatoNestePeriode,
            etterbetaling = etterbetaling,
            feilutbetaling = feilutbetaling,
            fom = fom,
            tomDatoNestePeriode = tomDatoNestePeriode,
            forfallsdatoNestePeriode = forfallsdatoNestePeriode,
            tidSimuleringHentet = tidSimuleringHentet,
            tomSisteUtbetaling = tomSisteUtbetaling,
            avregningsperioder = avregningsperioder,
        )
}

data class SimuleringsPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val forfallsdato: LocalDate,
    val nyttBeløp: BigDecimal,
    val tidligereUtbetalt: BigDecimal,
    val manuellPostering: BigDecimal,
    val resultat: BigDecimal,
    val feilutbetaling: BigDecimal,
    val etterbetaling: BigDecimal,
)

data class RestSimulering(
    val perioder: List<SimuleringsPeriode>,
    val fomDatoNestePeriode: LocalDate?,
    val etterbetaling: BigDecimal,
    val feilutbetaling: BigDecimal,
    val fom: LocalDate?,
    val tomDatoNestePeriode: LocalDate?,
    val forfallsdatoNestePeriode: LocalDate?,
    val tidSimuleringHentet: LocalDate?,
    val tomSisteUtbetaling: LocalDate?,
    val avregningsperioder: List<AvregningPeriode>,
)

data class AvregningPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val totalEtterbetaling: BigDecimal,
    val totalFeilutbetaling: BigDecimal,
)
