package no.nav.familie.ba.sak.simulering.domene

import java.math.BigDecimal
import java.time.LocalDate

data class RestVedtakSimulering(
        val perioder: List<SimuleringsPeriode>,
        val fomDatoNestePeriode: LocalDate?,
        val etterbetaling: BigDecimal,
        val feilutbetaling: BigDecimal,
        val fom: LocalDate,
        val tomDatoNestePeriode: LocalDate?,
        val forfallsdatoNestePeriode: LocalDate?,
        val tidSimuleringHentet: LocalDate,
)

data class SimuleringsPeriode(
        val fom: LocalDate,
        val tom: LocalDate,
        val forfallsdato: LocalDate,
        val nyttBeløp: BigDecimal,
        val tidligereUtbetalt: BigDecimal,
        val resultat: BigDecimal,
        val feilutbetaling: BigDecimal,
)