package no.nav.familie.ba.sak.beregning

import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun lagTestUtbetalingsoppdragForFGB(personIdent: String,
                                    fagsakId: String,
                                    behandlingId: Long,
                                    vedtakDato: LocalDate,
                                    datoFom: LocalDate,
                                    datoTom: LocalDate)
        : Utbetalingsoppdrag {
    return Utbetalingsoppdrag(
            Utbetalingsoppdrag.KodeEndring.NY,
            "BA",
            fagsakId,
            UUID.randomUUID().toString(),
            "SAKSBEHANDLERID",
            LocalDateTime.now(),
            listOf(Utbetalingsperiode(false,
                    null,
                    1,
                    null,
                    vedtakDato,
                    "BATR",
                    datoFom,
                    datoTom,
                    BigDecimal.ONE,
                    Utbetalingsperiode.SatsType.MND,
                    personIdent,
                    behandlingId
            ))
    )
}

fun lagTestUtbetalingsoppdragForOpphør(personIdent: String,
                                       fagsakId: String,
                                       behandlingId: Long,
                                       vedtakDato: LocalDate,
                                       datoFom: LocalDate,
                                       datoTom: LocalDate,
                                       opphørFom: LocalDate): Utbetalingsoppdrag {

    return Utbetalingsoppdrag(
            Utbetalingsoppdrag.KodeEndring.NY,
            "BA",
            fagsakId,
            UUID.randomUUID().toString(),
            "SAKSBEHANDLERID",
            LocalDateTime.now(),
            listOf(Utbetalingsperiode(true,
                    Opphør(opphørFom),
                    1,
                    null,
                    vedtakDato,
                    "BATR",
                    datoFom,
                    datoTom,
                    BigDecimal.ONE,
                    Utbetalingsperiode.SatsType.MND,
                    personIdent,
                    behandlingId
            ))
    )
}

fun lagTestUtbetalingsoppdragForRevurdering(personIdent: String,
                                            fagsakId: String,
                                            behandlingId: Long,
                                            forrigeBehandlingId: Long,
                                            vedtakDato: LocalDate,
                                            opphørFom: LocalDate,
                                            datoTom: LocalDate,
                                            revurderingFom: LocalDate): Utbetalingsoppdrag {
    return Utbetalingsoppdrag(
            Utbetalingsoppdrag.KodeEndring.NY,
            "BA",
            fagsakId,
            UUID.randomUUID().toString(),
            "SAKSBEHANDLERID",
            LocalDateTime.now(),
            listOf(Utbetalingsperiode(true,
                    Opphør(opphørFom),
                    1,
                    null,
                    vedtakDato,
                    "BATR",
                    opphørFom,
                    datoTom,
                    BigDecimal.ONE,
                    Utbetalingsperiode.SatsType.MND,
                    personIdent,
                    forrigeBehandlingId
            ), Utbetalingsperiode(false,
                    null,
                    2,
                    1,
                    vedtakDato,
                    "BATR",
                    revurderingFom,
                    datoTom,
                    BigDecimal.ONE,
                    Utbetalingsperiode.SatsType.MND,
                    personIdent,
                    behandlingId
            ))
    )
}