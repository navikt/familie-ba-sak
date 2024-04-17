package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import java.math.BigDecimal
import java.time.LocalDate

fun Utbetalingsoppdrag.harLøpendeUtbetaling() =
    this.utbetalingsperiode.any {
        it.opphør == null &&
            it.sats > BigDecimal.ZERO &&
            it.vedtakdatoTom > LocalDate.now().sisteDagIMåned()
    }
