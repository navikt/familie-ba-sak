package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import io.mockk.MockKException
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.ekstern.restDomene.UtfyltStatus
import no.nav.familie.ba.sak.ekstern.restDomene.ValutakursDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilValutakurs
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.integrasjoner.norgesbank.NorgesBankService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class ValutakursControllerTest {
    private val valutakursService = mockk<ValutakursService>()
    private val personidentService = mockk<PersonidentService>()
    private val utvidetBehandlingService = mockk<UtvidetBehandlingService>()
    private val ecbService = mockk<ECBService>()
    private val norgesBankService = mockk<NorgesBankService>()
    private val tilgangService = mockk<TilgangService>()
    private val automatiskOppdaterValutakursService = mockk<AutomatiskOppdaterValutakursService>()
    private val featureToggleService = mockk<FeatureToggleService>()

    private val valutakursController =
        ValutakursController(
            tilgangService = tilgangService,
            valutakursService = valutakursService,
            personidentService = personidentService,
            utvidetBehandlingService = utvidetBehandlingService,
            ecbService = ecbService,
            norgesBankService = norgesBankService,
            automatiskOppdaterValutakursService = automatiskOppdaterValutakursService,
            featureToggleService = featureToggleService,
        )

    private val barnId = "12345678910"

    private val valutakursDto: ValutakursDto =
        ValutakursDto(id = 1, fom = YearMonth.of(2020, 1), tom = null, barnIdenter = listOf(barnId), valutakursdato = null, valutakode = null, kurs = null, vurderingsform = null, status = UtfyltStatus.OK)

    @BeforeEach
    fun setup() {
        every { personidentService.hentAktør(any()) } returns lagAktør(barnId)
        every { valutakursService.hentValutakurs(any()) } returns valutakursDto.tilValutakurs(listOf(lagAktør(barnId)))
        every { featureToggleService.isEnabled(FeatureToggle.HENT_VALUTAKURS_FRA_NORGESBANK, false) } returns true
        every { norgesBankService.hentValutakurs(any(), any()) } returns BigDecimal.valueOf(0.95)
        every { ecbService.hentValutakurs(any(), any()) } returns BigDecimal.valueOf(0.95)
        justRun { tilgangService.validerTilgangTilBehandling(any(), any()) }
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
        justRun { tilgangService.validerKanRedigereBehandling(any()) }
    }

    @Test
    fun `Test at valutakurs hentes fra ECB dersom dato og valuta er satt`() {
        // Arrange
        val valutakursDato = LocalDate.of(2022, 1, 1)
        val valuta = "SEK"

        // Act & Assert
        assertThrows<MockKException> {
            valutakursController.oppdaterValutakurs(
                1,
                valutakursDto.copy(valutakursdato = valutakursDato, valutakode = valuta),
            )
        }

        // Assert
        verify(exactly = 1) { norgesBankService.hentValutakurs("SEK", valutakursDato) }
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(any(), any()) }
    }

    @Test
    fun `Test at valutakurs ikke hentes fra ECB dersom dato ikke er satt`() {
        // Arrange
        val valutakursDato = LocalDate.of(2022, 1, 1)

        // Act & Assert
        assertThrows<MockKException> {
            valutakursController.oppdaterValutakurs(
                1,
                valutakursDto.copy(valutakode = "SEK"),
            )
        }

        // Assert
        verify(exactly = 0) { ecbService.hentValutakurs("SEK", valutakursDato) }
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(any(), any()) }
    }

    @Test
    fun `Test at valutakurs ikke hentes fra ECB dersom valuta ikke er satt`() {
        // Arrange
        val valutakursDato = LocalDate.of(2022, 1, 1)

        // Act & Assert
        assertThrows<MockKException> {
            valutakursController.oppdaterValutakurs(
                1,
                valutakursDto.copy(valutakursdato = valutakursDato),
            )
        }

        // Assert
        verify(exactly = 0) { ecbService.hentValutakurs("SEK", valutakursDato) }
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(any(), any()) }
    }

    @Test
    fun `Test at valutakurs ikke hentes fra ECB dersom ISK og før 1 feb 2018`() {
        // Arrange
        val valutakursDato = LocalDate.of(2018, 1, 31)

        // Act & Assert
        assertThrows<MockKException> {
            valutakursController.oppdaterValutakurs(
                1,
                valutakursDto.copy(valutakursdato = valutakursDato, valutakode = "ISK"),
            )
        }

        // Assert
        verify(exactly = 0) { ecbService.hentValutakurs("ISK", valutakursDato) }
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(any(), any()) }
    }

    @Test
    fun `Test at valutakurs hentes fra ECB dersom ISK og etter 1 feb 2018`() {
        // Arrange
        val valutakursDato = LocalDate.of(2018, 2, 1)

        // Act & Assert
        assertThrows<MockKException> {
            valutakursController.oppdaterValutakurs(
                1,
                valutakursDto.copy(valutakursdato = valutakursDato, valutakode = "ISK"),
            )
        }

        // Assert
        verify(exactly = 1) { norgesBankService.hentValutakurs("ISK", valutakursDato) }
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(any(), any()) }
    }
}
