package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import java.time.LocalDate

data class Opphør(
    val erRentOpphør: Boolean,
    val opphørsdato: LocalDate?,
) {
    companion object Factory {
        fun opprettFor(
            utbetalingsoppdrag: Utbetalingsoppdrag,
            behandling: Behandling,
        ): Opphør {
            val erRentOpphør =
                utbetalingsoppdrag.utbetalingsperiode.isNotEmpty() && utbetalingsoppdrag.utbetalingsperiode.all { it.opphør != null }
            var opphørsdato: LocalDate? = null
            if (erRentOpphør) {
                opphørsdato = utbetalingsoppdrag.utbetalingsperiode.minOf { it.opphør!!.opphørDatoFom }
            }
            if (behandling.type == BehandlingType.REVURDERING) {
                val opphørPåRevurdering = utbetalingsoppdrag.utbetalingsperiode.filter { it.opphør != null }
                if (opphørPåRevurdering.isNotEmpty()) {
                    opphørsdato = opphørPåRevurdering.maxOfOrNull { it.opphør!!.opphørDatoFom }
                }
            }
            return Opphør(erRentOpphør = erRentOpphør, opphørsdato = opphørsdato)
        }
    }
}
