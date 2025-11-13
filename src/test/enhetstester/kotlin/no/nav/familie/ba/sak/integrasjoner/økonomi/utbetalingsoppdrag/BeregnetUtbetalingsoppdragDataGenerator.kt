package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.AndelMedPeriodeIdLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.Opphør
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode
import no.nav.familie.kontrakter.felles.objectMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun lagBeregnetUtbetalingsoppdragLongId(
    utbetalingsperioder: List<Utbetalingsperiode>,
    andeler: List<AndelMedPeriodeIdLongId>,
): BeregnetUtbetalingsoppdragLongId =
    BeregnetUtbetalingsoppdragLongId(
        lagUtbetalingsoppdrag(utbetalingsperioder),
        andeler = andeler,
    )

fun lagMinimalUtbetalingsoppdragString(
    behandlingId: Long,
    ytelseTypeBa: YtelsetypeBA? = YtelsetypeBA.ORDINÆR_BARNETRYGD,
): String =
    objectMapper.writeValueAsString(
        Utbetalingsoppdrag(
            kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
            fagSystem = "BA",
            saksnummer = "",
            aktoer = UUID.randomUUID().toString(),
            saksbehandlerId = "",
            avstemmingTidspunkt = LocalDateTime.now(),
            utbetalingsperiode =
                listOf(
                    lagUtbetalingsperiode(behandlingId = behandlingId, periodeId = 0, forrigePeriodeId = null, ytelseTypeBa = ytelseTypeBa ?: YtelsetypeBA.ORDINÆR_BARNETRYGD),
                ),
        ),
    )

fun lagUtbetalingsoppdrag(utbetalingsperioder: List<Utbetalingsperiode>): Utbetalingsoppdrag =
    Utbetalingsoppdrag(
        kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
        fagSystem = "BA",
        saksnummer = "",
        aktoer = UUID.randomUUID().toString(),
        saksbehandlerId = "",
        avstemmingTidspunkt = LocalDateTime.now(),
        utbetalingsperiode = utbetalingsperioder,
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

fun lagAndelMedPeriodeIdLong(
    id: Long,
    periodeId: Long,
    forrigePeriodeId: Long?,
    kildeBehandlingId: Long,
): AndelMedPeriodeIdLongId =
    AndelMedPeriodeIdLongId(
        id = id,
        periodeId = periodeId,
        forrigePeriodeId = forrigePeriodeId,
        kildeBehandlingId = kildeBehandlingId,
    )
