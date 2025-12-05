package no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Vurderingsform
import no.nav.familie.util.VirkedagerProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random.Default.nextLong

class MånedligValutajusteringServiceTest {
    private val inneværendeMåned = YearMonth.of(2024, 12)
    private val sisteVirkedagForrigeMåned = VirkedagerProvider.senesteVirkedagFørEllerMed(inneværendeMåned.minusMonths(1).atEndOfMonth())
    private val barnAktør = randomAktør()

    private val ecbService = mockk<ECBService>()
    private val valutakursService = mockk<ValutakursService>(relaxed = true)
    private val clockProvider = TestClockProvider.lagClockProviderMedFastTidspunkt(inneværendeMåned)

    private val månedligValutajusteringService =
        MånedligValutajusteringService(
            ecbService = ecbService,
            valutakursService = valutakursService,
            clockProvider = clockProvider,
        )

    @Test
    fun `oppdaterValutakurserFraOgMedInneværendeMåned skal oppdatere valutakurser fra og med inneværende måned`() {
        // Arrange
        val behandlingId = BehandlingId(nextLong(1000000000))

        val eksisterendeValutakurs =
            Valutakurs(
                fom = YearMonth.of(2024, 1),
                tom = null,
                barnAktører = setOf(barnAktør),
                valutakursdato = LocalDate.of(2024, 11, 30),
                valutakode = "SEK",
                kurs = BigDecimal("1.05"),
                vurderingsform = Vurderingsform.MANUELL,
            )

        every { valutakursService.hentValutakurser(behandlingId) } returns listOf(eksisterendeValutakurs)
        every { ecbService.hentValutakurs("SEK", sisteVirkedagForrigeMåned) } returns BigDecimal("1.10")

        // Act
        månedligValutajusteringService.oppdaterValutakurserFraOgMedInneværendeMåned(behandlingId)

        // Assert
        val valutakursSlot = slot<Valutakurs>()
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(behandlingId, capture(valutakursSlot)) }

        val oppdatertValutakurs = valutakursSlot.captured
        assertThat(oppdatertValutakurs.fom).isEqualTo(inneværendeMåned)
        assertThat(oppdatertValutakurs.tom).isNull()
        assertThat(oppdatertValutakurs.valutakode).isEqualTo("SEK")
        assertThat(oppdatertValutakurs.kurs).isEqualTo(BigDecimal("1.10"))
        assertThat(oppdatertValutakurs.valutakursdato).isEqualTo(sisteVirkedagForrigeMåned)
        assertThat(oppdatertValutakurs.vurderingsform).isEqualTo(Vurderingsform.AUTOMATISK)
        assertThat(oppdatertValutakurs.barnAktører).isEqualTo(setOf(barnAktør))
    }

    @Test
    fun `oppdaterValutakurserFraOgMedInneværendeMåned skal ikke oppdatere valutakurser som slutter før inneværende måned`() {
        // Arrange
        val behandlingId = BehandlingId(nextLong(1000000000))

        val eksisterendeValutakurs =
            Valutakurs(
                fom = YearMonth.of(2024, 1),
                tom = YearMonth.of(2024, 11),
                barnAktører = setOf(barnAktør),
                valutakursdato = LocalDate.of(2024, 11, 30),
                valutakode = "SEK",
                kurs = BigDecimal("1.05"),
                vurderingsform = Vurderingsform.MANUELL,
            )

        every { valutakursService.hentValutakurser(behandlingId) } returns listOf(eksisterendeValutakurs)

        // Act
        månedligValutajusteringService.oppdaterValutakurserFraOgMedInneværendeMåned(behandlingId)

        // Assert
        verify(exactly = 0) { valutakursService.oppdaterValutakurs(any(), any()) }
        verify(exactly = 0) { ecbService.hentValutakurs(any(), any()) }
    }

    @Test
    fun `oppdaterValutakurserFraOgMedInneværendeMåned skal oppdatere valutakurs som slutter etter inneværende måned`() {
        // Arrange
        val behandlingId = BehandlingId(nextLong(1000000000))

        val eksisterendeValutakurs =
            Valutakurs(
                fom = YearMonth.of(2024, 1),
                tom = YearMonth.of(2025, 6),
                barnAktører = setOf(barnAktør),
                valutakursdato = LocalDate.of(2024, 11, 30),
                valutakode = "SEK",
                kurs = BigDecimal("1.05"),
                vurderingsform = Vurderingsform.MANUELL,
            )

        every { valutakursService.hentValutakurser(behandlingId) } returns listOf(eksisterendeValutakurs)
        every { ecbService.hentValutakurs("SEK", sisteVirkedagForrigeMåned) } returns BigDecimal("1.10")

        // Act
        månedligValutajusteringService.oppdaterValutakurserFraOgMedInneværendeMåned(behandlingId)

        // Assert
        val valutakursSlot = slot<Valutakurs>()
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(behandlingId, capture(valutakursSlot)) }

        val oppdatertValutakurs = valutakursSlot.captured
        assertThat(oppdatertValutakurs.fom).isEqualTo(inneværendeMåned)
        assertThat(oppdatertValutakurs.tom).isEqualTo(YearMonth.of(2025, 6))
        assertThat(oppdatertValutakurs.kurs).isEqualTo(BigDecimal("1.10"))
    }

    @Test
    fun `oppdaterValutakurserFraOgMedInneværendeMåned skal oppdatere flere valutakurser`() {
        // Arrange
        val behandlingId = BehandlingId(nextLong(1000000000))
        val barnAktør1 = randomAktør()
        val barnAktør2 = randomAktør()

        val valutakurs1 =
            Valutakurs(
                fom = YearMonth.of(2024, 1),
                tom = null,
                barnAktører = setOf(barnAktør1),
                valutakursdato = LocalDate.of(2024, 11, 30),
                valutakode = "SEK",
                kurs = BigDecimal("1.05"),
                vurderingsform = Vurderingsform.MANUELL,
            )

        val valutakurs2 =
            Valutakurs(
                fom = YearMonth.of(2024, 1),
                tom = null,
                barnAktører = setOf(barnAktør2),
                valutakursdato = LocalDate.of(2024, 11, 30),
                valutakode = "EUR",
                kurs = BigDecimal("11.50"),
                vurderingsform = Vurderingsform.MANUELL,
            )

        every { valutakursService.hentValutakurser(behandlingId) } returns listOf(valutakurs1, valutakurs2)
        every { ecbService.hentValutakurs("SEK", sisteVirkedagForrigeMåned) } returns BigDecimal("1.10")
        every { ecbService.hentValutakurs("EUR", sisteVirkedagForrigeMåned) } returns BigDecimal("11.75")

        // Act
        månedligValutajusteringService.oppdaterValutakurserFraOgMedInneværendeMåned(behandlingId)

        // Assert
        verify(exactly = 2) { valutakursService.oppdaterValutakurs(behandlingId, any()) }
        verify(exactly = 1) { ecbService.hentValutakurs("SEK", sisteVirkedagForrigeMåned) }
        verify(exactly = 1) { ecbService.hentValutakurs("EUR", sisteVirkedagForrigeMåned) }
    }

    @Test
    fun `oppdaterValutakurserFraOgMedInneværendeMåned skal bruke maxOf for fom når valutakurs starter senere enn inneværende måned`() {
        // Arrange
        val behandlingId = BehandlingId(nextLong(1000000000))

        val eksisterendeValutakurs =
            Valutakurs(
                fom = YearMonth.of(2025, 1),
                tom = null,
                barnAktører = setOf(barnAktør),
                valutakursdato = LocalDate.of(2024, 11, 30),
                valutakode = "SEK",
                kurs = BigDecimal("1.05"),
                vurderingsform = Vurderingsform.MANUELL,
            )

        every { valutakursService.hentValutakurser(behandlingId) } returns listOf(eksisterendeValutakurs)
        every { ecbService.hentValutakurs("SEK", sisteVirkedagForrigeMåned) } returns BigDecimal("1.10")

        // Act
        månedligValutajusteringService.oppdaterValutakurserFraOgMedInneværendeMåned(behandlingId)

        // Assert
        val valutakursSlot = slot<Valutakurs>()
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(behandlingId, capture(valutakursSlot)) }

        val oppdatertValutakurs = valutakursSlot.captured
        assertThat(oppdatertValutakurs.fom).isEqualTo(YearMonth.of(2025, 1))
        assertThat(oppdatertValutakurs.kurs).isEqualTo(BigDecimal("1.10"))
    }

    @Test
    fun `oppdaterValutakurserFraOgMedInneværendeMåned skal ignorere valutakurser som ikke er utfylt`() {
        // Arrange
        val behandlingId = BehandlingId(nextLong(1000000000))

        every { valutakursService.hentValutakurser(behandlingId) } returns listOf(Valutakurs.NULL)

        // Act
        månedligValutajusteringService.oppdaterValutakurserFraOgMedInneværendeMåned(behandlingId)

        // Assert
        verify(exactly = 0) { valutakursService.oppdaterValutakurs(any(), any()) }
        verify(exactly = 0) { ecbService.hentValutakurs(any(), any()) }
    }

    @Test
    fun `oppdaterValutakurserFraOgMedInneværendeMåned skal håndtere tom liste med valutakurser`() {
        // Arrange
        val behandlingId = BehandlingId(nextLong(1000000000))

        every { valutakursService.hentValutakurser(behandlingId) } returns emptyList()

        // Act
        månedligValutajusteringService.oppdaterValutakurserFraOgMedInneværendeMåned(behandlingId)

        // Assert
        verify(exactly = 0) { valutakursService.oppdaterValutakurs(any(), any()) }
        verify(exactly = 0) { ecbService.hentValutakurs(any(), any()) }
    }

    @Test
    fun `oppdaterValutakurserFraOgMedInneværendeMåned skal oppdatere valutakurs som starter i inneværende måned`() {
        // Arrange
        val behandlingId = BehandlingId(nextLong(1000000000))

        val eksisterendeValutakurs =
            Valutakurs(
                fom = inneværendeMåned,
                tom = null,
                barnAktører = setOf(barnAktør),
                valutakursdato = LocalDate.of(2024, 11, 30),
                valutakode = "SEK",
                kurs = BigDecimal("1.05"),
                vurderingsform = Vurderingsform.MANUELL,
            )

        every { valutakursService.hentValutakurser(behandlingId) } returns listOf(eksisterendeValutakurs)
        every { ecbService.hentValutakurs("SEK", sisteVirkedagForrigeMåned) } returns BigDecimal("1.10")

        // Act
        månedligValutajusteringService.oppdaterValutakurserFraOgMedInneværendeMåned(behandlingId)

        // Assert
        val valutakursSlot = slot<Valutakurs>()
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(behandlingId, capture(valutakursSlot)) }

        val oppdatertValutakurs = valutakursSlot.captured
        assertThat(oppdatertValutakurs.fom).isEqualTo(inneværendeMåned)
        assertThat(oppdatertValutakurs.kurs).isEqualTo(BigDecimal("1.10"))
    }

    @Test
    fun `oppdaterValutakurserFraOgMedInneværendeMåned skal oppdatere valutakurs som slutter i inneværende måned`() {
        // Arrange
        val behandlingId = BehandlingId(nextLong(1000000000))

        val eksisterendeValutakurs =
            Valutakurs(
                fom = YearMonth.of(2024, 1),
                tom = inneværendeMåned,
                barnAktører = setOf(barnAktør),
                valutakursdato = LocalDate.of(2024, 11, 30),
                valutakode = "SEK",
                kurs = BigDecimal("1.05"),
                vurderingsform = Vurderingsform.MANUELL,
            )

        every { valutakursService.hentValutakurser(behandlingId) } returns listOf(eksisterendeValutakurs)
        every { ecbService.hentValutakurs("SEK", sisteVirkedagForrigeMåned) } returns BigDecimal("1.10")

        // Act
        månedligValutajusteringService.oppdaterValutakurserFraOgMedInneværendeMåned(behandlingId)

        // Assert
        val valutakursSlot = slot<Valutakurs>()
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(behandlingId, capture(valutakursSlot)) }

        val oppdatertValutakurs = valutakursSlot.captured
        assertThat(oppdatertValutakurs.fom).isEqualTo(inneværendeMåned)
        assertThat(oppdatertValutakurs.tom).isEqualTo(inneværendeMåned)
        assertThat(oppdatertValutakurs.kurs).isEqualTo(BigDecimal("1.10"))
    }
}
