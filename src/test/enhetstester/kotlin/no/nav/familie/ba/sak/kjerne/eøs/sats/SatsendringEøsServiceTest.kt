package no.nav.familie.ba.sak.kjerne.eøs.sats

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.AutovedtakSkalIkkeGjennomføresFeil
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.SatsendringEøsKjøringService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.domene.SatsendringEøsKjøring
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class SatsendringEøsServiceTest {
    private val utenlandskPeriodebeløpService = mockk<UtenlandskPeriodebeløpService>()
    private val satsendringEøsKjøringService = mockk<SatsendringEøsKjøringService>()
    private val satsendringEøsService = SatsendringEøsService(utenlandskPeriodebeløpService, satsendringEøsKjøringService)
    private val behandlingId = BehandlingId(42L)
    private val barnAktører = setOf(randomAktør())

    private val forrigeSats =
        EøsSats(
            land = "PL",
            valuta = "PLN",
            intervall = Intervall.MÅNEDLIG,
            beløp = BigDecimal("800"),
            fom = YearMonth.of(2024, 1),
            tom = YearMonth.of(2024, 12),
        )

    private val nySats =
        EøsSats(
            land = "PL",
            valuta = "PLN",
            intervall = Intervall.MÅNEDLIG,
            beløp = BigDecimal("900"),
            fom = YearMonth.of(2025, 1),
            tom = null,
        )

    @BeforeEach
    fun setup() {
        mockkObject(EøsSatserRegister)
        every { EøsSatserRegister.satser } returns listOf(forrigeSats, nySats)

        every { satsendringEøsKjøringService.hentSatsendringEøsKjøring(behandlingId.id) } returns
            SatsendringEøsKjøring(fagsakId = 1L, utbetalingsland = "PL", satsTidspunkt = YearMonth.of(2025, 1))

        every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId) } returns listOf(lagUtenlandskPeriodebeløp())
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Nested
    inner class OppdaterUtenlandskPeriodebeløpMedSisteSats {
        @Test
        fun `Utenlandsk periodebeløp oppdateres til ny sats`() {
            // Arrange
            val lagretSkjema = slot<UtenlandskPeriodebeløp>()
            justRun { utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(any(), capture(lagretSkjema)) }

            // Act
            satsendringEøsService.oppdaterUtenlandskPeriodebeløpMedSisteSats(behandlingId)

            // Assert
            verify(exactly = 1) { utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(any(), any()) }
            assertThat(lagretSkjema.captured.beløp).isEqualByComparingTo(nySats.beløp)
            assertThat(lagretSkjema.captured.fom).isEqualTo(nySats.fom)
            assertThat(lagretSkjema.captured.tom).isNull()
            assertThat(lagretSkjema.captured.valutakode).isEqualTo("PLN")
            assertThat(lagretSkjema.captured.intervall).isEqualTo(Intervall.MÅNEDLIG)
            assertThat(lagretSkjema.captured.utbetalingsland).isEqualTo("PL")
            assertThat(lagretSkjema.captured.kalkulertMånedligBeløp).isEqualByComparingTo(nySats.beløp)
        }

        @Test
        fun `sats-fom midt i utenlandsk periodebeløp-periode gir ny fom lik sats-fom`() {
            // Arrange
            every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId) } returns
                listOf(lagUtenlandskPeriodebeløp(fom = YearMonth.of(2024, 6)))

            val lagretSkjema = slot<UtenlandskPeriodebeløp>()
            justRun { utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(any(), capture(lagretSkjema)) }

            // Act
            satsendringEøsService.oppdaterUtenlandskPeriodebeløpMedSisteSats(behandlingId)

            // Assert
            assertThat(lagretSkjema.captured.fom).isEqualTo(nySats.fom)
        }

        @Test
        fun `Utenlandsk periodebeløp-fom etter sats-fom gir ny fom lik utenlandsk periodebeløp-fom`() {
            // Arrange
            every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId) } returns
                listOf(lagUtenlandskPeriodebeløp(fom = YearMonth.of(2025, 6)))

            val lagretSkjema = slot<UtenlandskPeriodebeløp>()
            justRun { utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(any(), capture(lagretSkjema)) }

            // Act
            satsendringEøsService.oppdaterUtenlandskPeriodebeløpMedSisteSats(behandlingId)

            // Assert
            assertThat(lagretSkjema.captured.fom).isEqualTo(YearMonth.of(2025, 6))
        }

        @Test
        fun `Kun utenlandsk periodebeløp for gjeldende utbetalingsland oppdateres`() {
            // Arrange
            val utenlandskPeriodebeløpForLandUtenSats = lagUtenlandskPeriodebeløp(utbetalingsland = "DE", barnAktører = setOf(randomAktør()))
            val utenlandskPeriodebeløpForLandMedSats = lagUtenlandskPeriodebeløp()

            every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId) } returns
                listOf(utenlandskPeriodebeløpForLandUtenSats, utenlandskPeriodebeløpForLandMedSats)

            justRun { utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(any(), any()) }

            // Act
            satsendringEøsService.oppdaterUtenlandskPeriodebeløpMedSisteSats(behandlingId)

            // Assert
            verify(exactly = 1) { utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(any(), any()) }
        }

        @Test
        fun `Flere utenlandsk periodebeløp for samme land oppdateres`() {
            // Arrange
            every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId) } returns
                listOf(lagUtenlandskPeriodebeløp(), lagUtenlandskPeriodebeløp(fom = YearMonth.of(2024, 1), barnAktører = setOf(randomAktør())))
            justRun { utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(any(), any()) }

            // Act
            satsendringEøsService.oppdaterUtenlandskPeriodebeløpMedSisteSats(behandlingId)

            // Assert
            verify(exactly = 2) { utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(any(), any()) }
        }

        @Nested
        inner class KasterFeil {
            @Test
            fun `Ingen sats registrert for landet`() {
                // Arrange
                every { EøsSatserRegister.satser } returns emptyList()

                // Act & Assert
                assertThatThrownBy { satsendringEøsService.oppdaterUtenlandskPeriodebeløpMedSisteSats(behandlingId) }
                    .isInstanceOf(Feil::class.java)
                    .hasMessageContaining("Ingen EØS-sats registrert for land PL i måned 2025-01")
            }

            @Test
            fun `Ingen forrige sats registrert for landet`() {
                // Arrange
                every { EøsSatserRegister.satser } returns listOf(nySats)

                // Act & Assert
                assertThatThrownBy { satsendringEøsService.oppdaterUtenlandskPeriodebeløpMedSisteSats(behandlingId) }
                    .isInstanceOf(Feil::class.java)
                    .hasMessageContaining("Ingen EØS-sats registrert for land PL i måned 2024-12")
            }
        }

        @Nested
        inner class KasterAutovedtakSkalIkkeGjennomføresFeil {
            @Test
            fun `Utenlandsk periodebeløp som ikke er utfylt filtreres bort`() {
                // Arrange
                every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId) } returns
                    listOf(lagUtenlandskPeriodebeløp(fom = null, beløp = null, utbetalingsland = null))

                // Act & Assert
                assertThatThrownBy { satsendringEøsService.oppdaterUtenlandskPeriodebeløpMedSisteSats(behandlingId) }
                    .isInstanceOf(AutovedtakSkalIkkeGjennomføresFeil::class.java)
                    .hasMessageContaining("Ingen UtenlandskPeriodebeløp trenger oppdatering")
            }

            @Test
            fun `Utenlandsk periodebeløp som allerede har ny sats oppdateres ikke`() {
                // Arrange
                every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId) } returns
                    listOf(lagUtenlandskPeriodebeløp(fom = YearMonth.of(2025, 1), beløp = nySats.beløp))

                // Act & Assert
                assertThatThrownBy { satsendringEøsService.oppdaterUtenlandskPeriodebeløpMedSisteSats(behandlingId) }
                    .isInstanceOf(AutovedtakSkalIkkeGjennomføresFeil::class.java)
                    .hasMessageContaining("Ingen UtenlandskPeriodebeløp trenger oppdatering")

                verify(exactly = 0) { utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(any(), any()) }
            }

            @Test
            fun `Utenlandsk periodebeløp som ikke overlapper sats oppdateres ikke`() {
                // Arrange
                every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId) } returns
                    listOf(lagUtenlandskPeriodebeløp(tom = YearMonth.of(2024, 6)))

                // Act & Assert
                assertThatThrownBy { satsendringEøsService.oppdaterUtenlandskPeriodebeløpMedSisteSats(behandlingId) }
                    .isInstanceOf(AutovedtakSkalIkkeGjennomføresFeil::class.java)
                    .hasMessageContaining("Ingen UtenlandskPeriodebeløp trenger oppdatering")
            }

            @Test
            fun `Tom liste med utenlandsk periodebeløp`() {
                // Arrange
                every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId) } returns emptyList()

                // Act & Assert
                assertThatThrownBy { satsendringEøsService.oppdaterUtenlandskPeriodebeløpMedSisteSats(behandlingId) }
                    .isInstanceOf(AutovedtakSkalIkkeGjennomføresFeil::class.java)
                    .hasMessageContaining("Ingen UtenlandskPeriodebeløp trenger oppdatering")
            }
        }

        @Nested
        inner class KasterAutovedtakMåBehandlesManueltFeil {
            @Test
            fun `Beløp-mismatch mot sats`() {
                // Arrange
                every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId) } returns
                    listOf(lagUtenlandskPeriodebeløp(beløp = BigDecimal("500")))

                // Act & Assert
                assertThatThrownBy { satsendringEøsService.oppdaterUtenlandskPeriodebeløpMedSisteSats(behandlingId) }
                    .isInstanceOf(AutovedtakMåBehandlesManueltFeil::class.java)
                    .hasMessageContaining("UtenlandskPeriodebeløp for behandling ${behandlingId.id} og land PL har beløp 500 PLN")
            }

            @Test
            fun `Valutakode-mismatch mot sats`() {
                // Arrange
                every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId) } returns
                    listOf(lagUtenlandskPeriodebeløp(valutakode = "EUR"))

                // Act & Assert
                assertThatThrownBy { satsendringEøsService.oppdaterUtenlandskPeriodebeløpMedSisteSats(behandlingId) }
                    .isInstanceOf(AutovedtakMåBehandlesManueltFeil::class.java)
                    .hasMessageContaining("UtenlandskPeriodebeløp for behandling ${behandlingId.id} og land PL har valuta EUR")
            }

            @Test
            fun `Intervall-mismatch mot sats`() {
                // Arrange
                every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId) } returns
                    listOf(lagUtenlandskPeriodebeløp(intervall = Intervall.ÅRLIG))

                // Act & Assert
                assertThatThrownBy { satsendringEøsService.oppdaterUtenlandskPeriodebeløpMedSisteSats(behandlingId) }
                    .isInstanceOf(AutovedtakMåBehandlesManueltFeil::class.java)
                    .hasMessageContaining("UtenlandskPeriodebeløp for behandling ${behandlingId.id} og land PL har intervall ÅRLIG")
            }
        }
    }

    private fun lagUtenlandskPeriodebeløp(
        fom: YearMonth? = YearMonth.of(2023, 1),
        tom: YearMonth? = null,
        beløp: BigDecimal? = forrigeSats.beløp,
        valutakode: String = "PLN",
        intervall: Intervall = Intervall.MÅNEDLIG,
        utbetalingsland: String? = "PL",
        barnAktører: Set<Aktør> = this.barnAktører,
    ) = UtenlandskPeriodebeløp(
        fom = fom,
        tom = tom,
        barnAktører = barnAktører,
        beløp = beløp,
        valutakode = valutakode,
        intervall = intervall,
        utbetalingsland = utbetalingsland,
        kalkulertMånedligBeløp = beløp,
    ).also {
        it.behandlingId = behandlingId.id
    }
}
