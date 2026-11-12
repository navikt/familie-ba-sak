package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import io.mockk.MockKException
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.ekstern.restDomene.UtfyltStatus
import no.nav.familie.ba.sak.ekstern.restDomene.ValutakursDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilValutakurs
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
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
    private val tilgangService = mockk<TilgangService>()
    private val automatiskOppdaterValutakursService = mockk<AutomatiskOppdaterValutakursService>()

    private val valutakursController =
        ValutakursController(
            tilgangService = tilgangService,
            valutakursService = valutakursService,
            personidentService = personidentService,
            utvidetBehandlingService = utvidetBehandlingService,
            ecbService = ecbService,
            automatiskOppdaterValutakursService = automatiskOppdaterValutakursService,
        )

    private val barnId = "12345678910"

    private val valutakursDto: ValutakursDto =
        ValutakursDto(id = 1, fom = YearMonth.of(2020, 1), tom = null, barnIdenter = listOf(barnId), valutakursdato = null, valutakode = null, kurs = null, vurderingsform = null, status = UtfyltStatus.OK)

    @BeforeEach
    fun setup() {
        every { personidentService.hentAktør(any()) } returns lagAktør(barnId)
        every { valutakursService.hentValutakurs(any()) } returns valutakursDto.tilValutakurs(listOf(lagAktør(barnId)))
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
        verify(exactly = 1) { ecbService.hentValutakurs("SEK", valutakursDato) }
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
        verify(exactly = 1) { ecbService.hentValutakurs("ISK", valutakursDato) }
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(any(), any()) }
    }

    @Test
    fun `skal kreve saksbehandlerrolle og at behandlingen kan redigeres for å endre vurderingsstrategi`() {
        // Arrange
        every { automatiskOppdaterValutakursService.endreVurderingsstrategiForValutakurser(any(), any()) } returns mockk()
        every { utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = 1) } returns mockk()

        // Act
        valutakursController.endreVurderingsstrategiForValutakurser(1, VurderingsstrategiForValutakurser.MANUELL)

        // Assert
        verify(exactly = 1) {
            tilgangService.verifiserHarTilgangTilHandling(
                minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                handling = "Endrer vurderingsstrategi for valutakurser",
            )
        }
        verify(exactly = 1) { tilgangService.validerKanRedigereBehandling(1) }
        verify(exactly = 1) { automatiskOppdaterValutakursService.endreVurderingsstrategiForValutakurser(BehandlingId(1), VurderingsstrategiForValutakurser.MANUELL) }
    }

    @Test
    fun `skal ikke endre vurderingsstrategi når kaller ikke er saksbehandler`() {
        // Arrange
        every {
            tilgangService.verifiserHarTilgangTilHandling(
                minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                handling = "Endrer vurderingsstrategi for valutakurser",
            )
        } throws RolleTilgangskontrollFeil("Krever SAKSBEHANDLER")

        // Act & Assert
        val feilmelding =
            assertThrows<RolleTilgangskontrollFeil> {
                valutakursController.endreVurderingsstrategiForValutakurser(1, VurderingsstrategiForValutakurser.MANUELL)
            }.message

        assertThat(feilmelding).isEqualTo("Krever SAKSBEHANDLER")
        verify(exactly = 0) { automatiskOppdaterValutakursService.endreVurderingsstrategiForValutakurser(any(), any()) }
    }

    @Test
    fun `skal ikke endre vurderingsstrategi når behandlingen ikke kan redigeres`() {
        // Arrange
        every { tilgangService.validerKanRedigereBehandling(1) } throws FunksjonellFeil("Behandlingen er låst for videre redigering")

        // Act & Assert
        assertThrows<FunksjonellFeil> {
            valutakursController.endreVurderingsstrategiForValutakurser(1, VurderingsstrategiForValutakurser.MANUELL)
        }

        verify(exactly = 0) { automatiskOppdaterValutakursService.endreVurderingsstrategiForValutakurser(any(), any()) }
    }
}
