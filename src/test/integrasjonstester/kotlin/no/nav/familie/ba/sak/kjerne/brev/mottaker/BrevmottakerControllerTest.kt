package no.nav.familie.ba.sak.kjerne.brev.mottaker

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagBrevmottakerDb
import no.nav.familie.ba.sak.datagenerator.lagBrevmottakerDto
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BrevmottakerControllerTest(
    @Autowired private val brevmottakerService: BrevmottakerService,
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
) : AbstractSpringIntegrationTest() {
    private val tilgangService = mockk<TilgangService>(relaxed = true)
    private val brevmottakerController =
        BrevmottakerController(
            brevmottakerService = brevmottakerService,
            tilgangService = tilgangService,
            utvidetBehandlingService = mockk(relaxed = true),
        )

    private val brevmottakerDto = lagBrevmottakerDto()

    @BeforeEach
    fun setUp() {
        clearMocks(tilgangService)
    }

    @Nested
    inner class LeggTilBrevmottaker {
        @Test
        fun `skal lagre brevmottakere på behandlingen`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

            // Act
            brevmottakerController.leggTilBrevmottaker(behandling.id, brevmottakerDto)
            brevmottakerController.leggTilBrevmottaker(behandling.id, brevmottakerDto.copy(type = MottakerType.VERGE))

            // Assert
            val brevmottakere = brevmottakerService.hentBrevmottakere(behandling.id)
            assertThat(brevmottakere.map { it.type }).containsExactlyInAnyOrder(MottakerType.FULLMEKTIG, MottakerType.VERGE)
        }

        @Test
        fun `skal validere tilgang når brevmottaker legges til`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

            // Act
            brevmottakerController.leggTilBrevmottaker(behandling.id, brevmottakerDto)

            // Assert
            verify(exactly = 1) { tilgangService.validerTilgangTilBehandling(behandlingId = behandling.id, event = AuditLoggerEvent.UPDATE) }
            verify(exactly = 1) { tilgangService.verifiserHarTilgangTilHandling(BehandlerRolle.SAKSBEHANDLER, "legge til brevmottaker") }
            verify(exactly = 1) { tilgangService.validerKanRedigereBehandling(behandling.id) }
        }

        @Test
        fun `skal ikke lagre brevmottaker når tilgangskontrollen feiler`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

            every { tilgangService.validerKanRedigereBehandling(behandling.id) } throws RolleTilgangskontrollFeil("Ingen tilgang")

            // Act & Assert
            assertThatThrownBy { brevmottakerController.leggTilBrevmottaker(behandling.id, brevmottakerDto) }
                .isInstanceOf(RolleTilgangskontrollFeil::class.java)

            verify(exactly = 1) { tilgangService.validerTilgangTilBehandling(behandlingId = behandling.id, event = AuditLoggerEvent.UPDATE) }
            verify(exactly = 1) { tilgangService.verifiserHarTilgangTilHandling(BehandlerRolle.SAKSBEHANDLER, "legge til brevmottaker") }
            assertThat(brevmottakerService.hentBrevmottakere(behandling.id)).isEmpty()
        }
    }

    @Nested
    inner class SlettBrevmottaker {
        @Test
        fun `skal slette brevmottaker fra behandlingen`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

            brevmottakerController.leggTilBrevmottaker(behandling.id, brevmottakerDto)
            brevmottakerController.leggTilBrevmottaker(behandling.id, brevmottakerDto.copy(type = MottakerType.VERGE))

            val brevmottakerSomSkalSlettes = brevmottakerService.hentBrevmottakere(behandling.id).first()

            // Act
            brevmottakerController.slettBrevmottaker(brevmottakerSomSkalSlettes.id, behandling.id)

            // Assert
            val gjenværendeBrevmottakere = brevmottakerService.hentBrevmottakere(behandling.id)
            assertThat(gjenværendeBrevmottakere).hasSize(1)
            assertThat(gjenværendeBrevmottakere.map { it.id }).doesNotContain(brevmottakerSomSkalSlettes.id)
        }

        @Test
        fun `skal kaste feil og ikke slette brevmottaker som tilhører en annen behandling`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

            val annenAktør = aktørIdRepository.save(randomAktør())
            val annenFagsak = fagsakRepository.save(lagFagsakUtenId(aktør = annenAktør))
            val annenBehandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = annenFagsak))

            val brevmottakerPåAnnenBehandling = brevmottakerService.opprettBrevmottaker(lagBrevmottakerDb(behandlingId = annenBehandling.id))

            // Act & Assert
            assertThatThrownBy { brevmottakerController.slettBrevmottaker(brevmottakerPåAnnenBehandling.id, behandling.id) }
                .isInstanceOf(Feil::class.java)
                .hasMessage("Finner ikke brevmottaker med id=${brevmottakerPåAnnenBehandling.id} på behandling med id=${behandling.id}")

            assertThat(brevmottakerService.hentBrevmottakere(annenBehandling.id).map { it.id })
                .containsExactly(brevmottakerPåAnnenBehandling.id)
        }

        @Test
        fun `skal validere tilgang når brevmottaker slettes`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))
            val brevmottaker = brevmottakerService.opprettBrevmottaker(lagBrevmottakerDb(behandlingId = behandling.id))

            // Act
            brevmottakerController.slettBrevmottaker(brevmottaker.id, behandling.id)

            // Assert
            verify(exactly = 1) { tilgangService.validerTilgangTilBehandling(behandlingId = behandling.id, event = AuditLoggerEvent.DELETE) }
            verify(exactly = 1) { tilgangService.verifiserHarTilgangTilHandling(BehandlerRolle.SAKSBEHANDLER, "fjerne brevmottaker") }
            verify(exactly = 1) { tilgangService.validerKanRedigereBehandling(behandling.id) }
        }

        @Test
        fun `skal ikke slette brevmottaker når tilgangskontrollen feiler`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))
            val brevmottaker = brevmottakerService.opprettBrevmottaker(lagBrevmottakerDb(behandlingId = behandling.id))

            every { tilgangService.validerKanRedigereBehandling(behandling.id) } throws RolleTilgangskontrollFeil("Ingen tilgang")

            // Act & Assert
            assertThatThrownBy { brevmottakerController.slettBrevmottaker(brevmottaker.id, behandling.id) }
                .isInstanceOf(RolleTilgangskontrollFeil::class.java)

            verify(exactly = 1) { tilgangService.validerTilgangTilBehandling(behandlingId = behandling.id, event = AuditLoggerEvent.DELETE) }
            verify(exactly = 1) { tilgangService.verifiserHarTilgangTilHandling(BehandlerRolle.SAKSBEHANDLER, "fjerne brevmottaker") }
            assertThat(brevmottakerService.hentBrevmottakere(behandling.id).map { it.id }).containsExactly(brevmottaker.id)
        }
    }

    @Nested
    inner class HentBrevmottakere {
        @Test
        fun `skal hente brevmottakere for behandlingen`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

            brevmottakerController.leggTilBrevmottaker(behandling.id, brevmottakerDto)

            // Act
            val brevmottakere = brevmottakerController.hentBrevmottakere(behandling.id).body?.data

            // Assert
            assertThat(brevmottakere).hasSize(1)
            assertThat(brevmottakere!!.single())
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(brevmottakerDto)
        }

        @Test
        fun `skal returnere tom liste når behandlingen ikke har brevmottakere`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

            // Act
            val brevmottakere = brevmottakerController.hentBrevmottakere(behandling.id).body?.data

            // Assert
            assertThat(brevmottakere).isEmpty()
        }

        @Test
        fun `skal validere tilgang når brevmottakere hentes`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

            // Act
            brevmottakerController.hentBrevmottakere(behandling.id)

            // Assert
            verify(exactly = 1) { tilgangService.validerTilgangTilBehandling(behandlingId = behandling.id, event = AuditLoggerEvent.ACCESS) }
            verify(exactly = 1) { tilgangService.verifiserHarTilgangTilHandling(BehandlerRolle.VEILEDER, "hente brevmottakere") }
        }
    }
}
