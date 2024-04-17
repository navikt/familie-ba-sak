package no.nav.familie.ba.sak.integrasjoner.økonomi

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class ØkonomiServiceTest {
    @MockK
    private lateinit var økonomiKlient: ØkonomiKlient

    @MockK
    private lateinit var beregningService: BeregningService

    @MockK
    private lateinit var tilkjentYtelseValideringService: TilkjentYtelseValideringService

    @MockK
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @MockK
    private lateinit var utbetalingsoppdragGeneratorService: UtbetalingsoppdragGeneratorService

    @MockK
    private lateinit var behandlingHentOgPersisterService: BehandlingHentOgPersisterService

    @InjectMockKs
    private lateinit var økonomiService: ØkonomiService

    @Test
    fun `oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett - skal bruke ny utbetalingsgenerator når toggel er på`() {
        setupMocks()

        økonomiService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
            lagVedtak(),
            "123abc",
        )

        verify(exactly = 1) {
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = any(),
                saksbehandlerId = any(),
                erSimulering = any(),
            )
        }
    }

    private fun setupMocks() {
        val utbetalingsoppdrag =
            lagUtbetalingsoppdrag(
                listOf(
                    Utbetalingsperiode(
                        erEndringPåEksisterendePeriode = false,
                        opphør = null,
                        periodeId = 1,
                        forrigePeriodeId = null,
                        datoForVedtak = LocalDate.now(),
                        klassifisering = "BATR",
                        vedtakdatoFom = inneværendeMåned().førsteDagIInneværendeMåned(),
                        vedtakdatoTom = inneværendeMåned().sisteDagIInneværendeMåned(),
                        sats = BigDecimal(1054),
                        satsType = Utbetalingsperiode.SatsType.MND,
                        utbetalesTil = "13455678910",
                        behandlingId = 1,
                    ),
                ),
            )

        every {
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = any(),
                saksbehandlerId = any(),
                erSimulering = any(),
            )
        } returns BeregnetUtbetalingsoppdragLongId(utbetalingsoppdrag = utbetalingsoppdrag, andeler = emptyList())
        every { tilkjentYtelseValideringService.validerIngenAndelerTilkjentYtelseMedSammeOffsetIBehandling(any()) } just runs
        every { økonomiKlient.iverksettOppdrag(any()) } returns ""
    }
}
