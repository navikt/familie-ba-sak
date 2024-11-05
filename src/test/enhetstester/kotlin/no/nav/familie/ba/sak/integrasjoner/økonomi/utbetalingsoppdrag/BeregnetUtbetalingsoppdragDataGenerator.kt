package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.AndelMedPeriodeIdLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.Opphør
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun lagBeregnetUtbetalingsoppdragLongId(
    utbetalingsperioder: List<Utbetalingsperiode>,
    andeler: List<AndelMedPeriodeIdLongId>,
): BeregnetUtbetalingsoppdragLongId =
    BeregnetUtbetalingsoppdragLongId(
        Utbetalingsoppdrag(
            kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
            fagSystem = "BA",
            saksnummer = "",
            aktoer = UUID.randomUUID().toString(),
            saksbehandlerId = "",
            avstemmingTidspunkt = LocalDateTime.now(),
            utbetalingsperiode = utbetalingsperioder,
        ),
        andeler = andeler,
    )

fun lagUtbetalingsperiode(
    behandlingId: Long,
    periodeId: Long,
    forrigePeriodeId: Long?,
    ytelseTypeBa: YtelsetypeBA,
    fom: LocalDate = LocalDate.now(),
    tom: LocalDate = LocalDate.now(),
    opphør: Opphør? = null,
): Utbetalingsperiode =
    Utbetalingsperiode(
        erEndringPåEksisterendePeriode = true,
        opphør = opphør,
        periodeId = periodeId,
        forrigePeriodeId = forrigePeriodeId,
        datoForVedtak = LocalDate.now(),
        klassifisering = ytelseTypeBa.klassifisering,
        vedtakdatoFom = fom,
        vedtakdatoTom = tom,
        sats = BigDecimal.valueOf(1054),
        satsType = Utbetalingsperiode.SatsType.MND,
        utbetalesTil = "",
        behandlingId = behandlingId,
    )
