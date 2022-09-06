package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class UtbetalingsoppdragServiceTest {

    val økonomiKlient = mockk<ØkonomiKlient> {
        every { iverksettOppdrag(any()) } returns ""
    }
    val beregningService = mockk<BeregningService> {
        every { hentTilkjentYtelseForBehandling(any()) } returns mockk()
        every { hentTilkjentYtelseForBehandlingerIverksattMotØkonomi(any()) } returns mockk()
        every { lagreTilkjentYtelseMedOppdaterteAndeler(any()) } returns mockk()
        every { populerTilkjentYtelse(any(), any()) } returns mockk()
    }
    val service = spyk(UtbetalingsoppdragService(mockk(), økonomiKlient, beregningService, mockk(), mockk()))

    @Test
    fun `skal ikke sende til oppdrag hvis det ikke fins utbetalingsperioder`() {
        val utbetalingsoppdrag = lagUtbetalingsoppdrag(listOf())
        every {
            service.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                any(),
                any()
            )
        } returns lagTilkjentYtelse(utbetalingsoppdrag)
        val vedtak = mockk<Vedtak> {
            every { behandling } returns mockk {
                every { id } returns 1L
            }
        }
        service.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(vedtak, "")
        verify { økonomiKlient wasNot Called }
    }

    @Test
    fun `skal sende til oppdrag hvis det fins utbetalingsperioder`() {
        val utbetalingsoppdrag = lagUtbetalingsoppdrag(listOf(lagUtbetalingsperiode()))
        every {
            service.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                any(),
                any()
            )
        } returns lagTilkjentYtelse(utbetalingsoppdrag)
        val vedtak = mockk<Vedtak> {
            every { behandling } returns mockk {
                every { id } returns 1L
            }
        }
        service.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(vedtak, "")
        verify { økonomiKlient.iverksettOppdrag(utbetalingsoppdrag) }
    }

    private fun lagUtbetalingsoppdrag(utbetalingsperiode: List<Utbetalingsperiode>) = Utbetalingsoppdrag(
        kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
        fagSystem = "BA",
        saksnummer = "",
        aktoer = UUID.randomUUID().toString(),
        saksbehandlerId = "",
        avstemmingTidspunkt = LocalDateTime.now(),
        utbetalingsperiode = utbetalingsperiode
    )

    private fun lagUtbetalingsperiode() = Utbetalingsperiode(
        erEndringPåEksisterendePeriode = false,
        opphør = null,
        periodeId = 0,
        datoForVedtak = LocalDate.now(),
        klassifisering = "BATR",
        vedtakdatoFom = LocalDate.now().minusMonths(2).førsteDagIInneværendeMåned(),
        vedtakdatoTom = LocalDate.now().minusMonths(1).sisteDagIMåned(),
        sats = BigDecimal("1054"),
        satsType = Utbetalingsperiode.SatsType.MND,
        utbetalesTil = "",
        behandlingId = 0
    )

    private fun lagTilkjentYtelse(utbetalingsoppdrag: Utbetalingsoppdrag) =
        TilkjentYtelse(
            utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag),
            behandling = lagBehandling(),
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now()
        )
}
