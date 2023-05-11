package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.medDifferanseberegning
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class UtbetalingsoppdragValidatorTest {

    @Test
    fun `nasjonalt utbetalingsoppdrag må ha utbetalingsperiode`() {
        val utbetalingsoppdrag = lagUtbetalingsoppdrag()
        assertThrows<FunksjonellFeil> {
            utbetalingsoppdrag.validerNullutbetaling(
                behandlingskategori = BehandlingKategori.NASJONAL,
                andelerTilkjentYtelse = listOf(
                    lagAndelTilkjentYtelse(
                        fom = inneværendeMåned().minusYears(4),
                        tom = inneværendeMåned(),
                        beløp = 1054,
                    ),
                ),
            )
        }
    }

    @Test
    fun `innvilget EØS-utbetalingsoppdrag hvor Norge er sekundærland kan mangle utbetalingsperiode`() {
        val utbetalingsoppdrag = lagUtbetalingsoppdrag()
        assertDoesNotThrow {
            utbetalingsoppdrag.validerNullutbetaling(
                behandlingskategori = BehandlingKategori.EØS,
                andelerTilkjentYtelse = listOf(
                    lagAndelTilkjentYtelse(
                        fom = inneværendeMåned().minusYears(4),
                        tom = inneværendeMåned(),
                        beløp = 0,
                    ).medDifferanseberegning(BigDecimal("10")),
                ),
            )
        }
    }

    @Test
    fun `innvilget EØS-utbetalingsoppdrag hvor Norge er Primærland kan ikke mangle utbetalingsperiode`() {
        val utbetalingsoppdrag = lagUtbetalingsoppdrag()
        assertThrows<FunksjonellFeil> {
            utbetalingsoppdrag.validerNullutbetaling(
                behandlingskategori = BehandlingKategori.EØS,
                andelerTilkjentYtelse = listOf(
                    lagAndelTilkjentYtelse(
                        fom = inneværendeMåned().minusYears(4),
                        tom = inneværendeMåned(),
                        beløp = 1054,
                    ),
                ),
            )
        }
    }

    @Test
    fun `innvilget EØS-utbetalingsoppdrag hvor Norge er sekundærland ikke kaster feil når finnes utbetalingsperiode`() {
        val utbetalingsoppdrag = lagUtbetalingsoppdrag(
            utbetalingsperioder = listOf(
                Utbetalingsperiode(
                    erEndringPåEksisterendePeriode = false,
                    periodeId = 0,
                    datoForVedtak = LocalDate.now(),
                    klassifisering = "",
                    vedtakdatoFom = inneværendeMåned().førsteDagIInneværendeMåned(),
                    vedtakdatoTom = inneværendeMåned().atEndOfMonth(),
                    sats = BigDecimal(100),
                    satsType = Utbetalingsperiode.SatsType.MND,
                    utbetalesTil = "",
                    behandlingId = 123,
                ),
            ),
        )
        assertDoesNotThrow {
            utbetalingsoppdrag.validerNullutbetaling(
                behandlingskategori = BehandlingKategori.EØS,
                andelerTilkjentYtelse = listOf(
                    lagAndelTilkjentYtelse(
                        fom = inneværendeMåned().minusYears(4),
                        tom = inneværendeMåned(),
                        beløp = 1024,
                    ).medDifferanseberegning(BigDecimal("10")),
                ),
            )
        }
    }

    private fun lagUtbetalingsoppdrag(utbetalingsperioder: List<Utbetalingsperiode> = emptyList()) = Utbetalingsoppdrag(
        kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
        fagSystem = "BA",
        saksnummer = "",
        aktoer = UUID.randomUUID().toString(),
        saksbehandlerId = "",
        avstemmingTidspunkt = LocalDateTime.now(),
        utbetalingsperiode = utbetalingsperioder,
    )
}
