package no.nav.familie.ba.sak.tilbakekreving

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.simulering.domene.RestVedtakSimulering
import no.nav.familie.ba.sak.simulering.domene.SimuleringsPeriode
import no.nav.familie.kontrakter.felles.tilbakekreving.Periode
import java.math.BigDecimal
import java.time.LocalDate

fun validerVerdierPåRestTilbakekreving(restTilbakekreving: RestTilbakekreving?, feilutbetaling: BigDecimal) {
    if (feilutbetaling != BigDecimal.ZERO && restTilbakekreving == null) {
        throw FunksjonellFeil("Simuleringen har en feilutbetaling, men restTilbakekreving var null",
                              frontendFeilmelding = "Du må velge en tilbakekrevingsstrategi siden det er en feilutbetaling.")
    }
    if (feilutbetaling == BigDecimal.ZERO && restTilbakekreving != null) {
        throw FunksjonellFeil(
                "Simuleringen har ikke en feilutbetaling, men restTilbakekreving var ikke null",
                frontendFeilmelding = "Du kan ikke opprette en tilbakekreving når det ikke er en feilutbetaling."
        )
    }
}

fun slåsammenNærliggendeFeilutbtalingPerioder(simuleringsPerioder: List<SimuleringsPeriode>): List<Periode> {
    val perioder: MutableList<Periode> = mutableListOf()

    val sortedSimuleringsPerioder = simuleringsPerioder.sortedBy { it.fom }.filter { it.feilutbetaling != BigDecimal.ZERO }
    var aktuellFom: LocalDate = sortedSimuleringsPerioder.first().fom
    var aktuellTom: LocalDate = sortedSimuleringsPerioder.first().tom

    sortedSimuleringsPerioder.forEach { periode ->
        if (aktuellTom.toYearMonth().plusMonths(1) < periode.fom.toYearMonth()) {

            perioder.add(Periode(fom = aktuellFom, tom = aktuellTom))
            aktuellFom = periode.fom
        }
        aktuellTom = periode.tom
    }
    perioder.add(Periode(fom = aktuellFom, tom = aktuellTom))
    return perioder
}