package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class UtbetalingsoppdragServiceTest {

    val økonomiKlient = mockk<ØkonomiKlient> {
        every { iverksettOppdrag(any()) } returns ""
    }
    val beregningService = mockk<BeregningService> {
        every { populerTilkjentYtelse(any(), any()) } returns mockk()
    }
    val service = spyk(UtbetalingsoppdragService(mockk(), økonomiKlient, beregningService, mockk(), mockk(), mockk()))

    @Test
    fun `skal ikke sende til oppdrag hvis det ikke fins utbetalingsperioder`() {
        val utbetalingsoppdrag = lagUtbetalingsoppdrag(listOf())
        every {
            service.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                any(),
                any()
            )
        } returns utbetalingsoppdrag
        val vedtak = mockk<Vedtak> {
            every { behandling } returns mockk {
                every { id } returns 1L
            }
        }
        service.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(vedtak, "")
        verify { økonomiKlient wasNot Called }
    }

    @Disabled(value = "Implementasjonen er per no kommentert ut.")
    @Test
    fun `skal sende til oppdrag hvis det fins utbetalingsperioder`() {
        val utbetalingsoppdrag = lagUtbetalingsoppdrag(listOf(mockk()))
        every {
            service.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                any(),
                any()
            )
        } returns utbetalingsoppdrag
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
        "BA",
        "",
        UUID.randomUUID().toString(),
        "",
        LocalDateTime.now(),
        utbetalingsperiode = utbetalingsperiode
    )
}
