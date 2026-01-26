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
        YtelseType.FINNMARKSTILLEGG, YtelseType.SVALBARDTILLEGG -> 500
    }

fun lagUtbetalingsoppdrag(
    utbetalingsperiode: List<Utbetalingsperiode>,
    avstemmingTidspunkt: LocalDateTime = LocalDateTime.now(),
) = Utbetalingsoppdrag(
    kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
    fagSystem = "BA",
    saksnummer = "saksnummer",
    aktoer = UUID.randomUUID().toString(),
    saksbehandlerId = "saksbehandler",
    avstemmingTidspunkt = avstemmingTidspunkt,
    utbetalingsperiode = utbetalingsperiode,
)
