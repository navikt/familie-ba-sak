package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import java.time.LocalDateTime
import java.util.UUID

fun sats(ytelseType: YtelseType) =
    when (ytelseType) {
        YtelseType.ORDINÆR_BARNETRYGD -> 1054
        YtelseType.UTVIDET_BARNETRYGD -> 1054
        YtelseType.SMÅBARNSTILLEGG -> 660
    }

fun lagUtbetalingsoppdrag(utbetalingsperiode: List<Utbetalingsperiode>) =
    Utbetalingsoppdrag(
        kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
        fagSystem = "BA",
        saksnummer = "",
        aktoer = UUID.randomUUID().toString(),
        saksbehandlerId = "",
        avstemmingTidspunkt = LocalDateTime.now(),
        utbetalingsperiode = utbetalingsperiode,
    )
