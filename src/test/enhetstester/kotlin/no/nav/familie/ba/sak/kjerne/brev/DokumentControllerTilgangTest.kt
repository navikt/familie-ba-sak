package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DokumentControllerTilgangTest {
    private val fagsakService = mockk<FagsakService>()
    private val dokumentService = mockk<DokumentService>()
    private val dokumentGenereringService = mockk<DokumentGenereringService>()
    private val tilgangService = mockk<TilgangService>()

    private val dokumentController =
        DokumentController(
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = mockk(),
            dokumentService = dokumentService,
            dokumentGenereringService = dokumentGenereringService,
            vedtakService = mockk(),
            tilgangService = tilgangService,
            utvidetBehandlingService = mockk(),
            dokumentDistribueringService = mockk(),
        )

    private val fagsak = lagFagsak(id = 1L)
    private val manueltBrevRequest = ManueltBrevRequest(brevmal = Brevmal.INNHENTE_OPPLYSNINGER)

    @BeforeEach
    fun setUp() {
        every { tilgangService.validerTilgangTilFagsak(any(), any()) } just runs
        every { tilgangService.verifiserHarTilgangTilHandling(any(), any()) } just runs
        every { fagsakService.hentPåFagsakId(fagsak.id) } returns fagsak
        every { fagsakService.lagMinimalFagsakDto(fagsakId = fagsak.id) } returns mockk()
        every { dokumentService.byggMottakerdataFraFagsak(fagsak, manueltBrevRequest) } returns manueltBrevRequest
        every { dokumentService.sendManueltBrev(manueltBrevRequest = manueltBrevRequest, fagsakId = fagsak.id) } just runs
        every { dokumentGenereringService.genererManueltBrev(any(), any(), any(), any()) } returns ByteArray(0)
    }

    @Nested
    inner class ForhåndsvisBrevPåFagsak {
        @Test
        fun `skal validere tilgang til fagsaken før brevet genereres`() {
            // Act
            dokumentController.hentForhåndsvisningPåFagsak(fagsakId = fagsak.id, manueltBrevRequest = manueltBrevRequest)

            // Assert
            verifyOrder {
                tilgangService.validerTilgangTilFagsak(fagsakId = fagsak.id, event = AuditLoggerEvent.ACCESS)
                dokumentGenereringService.genererManueltBrev(any(), any(), any(), any())
            }
        }

        @Test
        fun `skal ikke generere brev når saksbehandler mangler tilgang til fagsaken`() {
            // Arrange
            every {
                tilgangService.validerTilgangTilFagsak(fagsakId = fagsak.id, event = AuditLoggerEvent.ACCESS)
            } throws RolleTilgangskontrollFeil("Mangler tilgang til fagsaken")

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    dokumentController.hentForhåndsvisningPåFagsak(fagsakId = fagsak.id, manueltBrevRequest = manueltBrevRequest)
                }.message

            assertThat(feilmelding).isEqualTo("Mangler tilgang til fagsaken")
            verify(exactly = 0) { dokumentGenereringService.genererManueltBrev(any(), any(), any(), any()) }
        }
    }

    @Nested
    inner class SendBrevPåFagsak {
        @Test
        fun `skal validere tilgang til fagsaken før brevet sendes`() {
            // Act
            dokumentController.sendBrevPåFagsak(fagsakId = fagsak.id, manueltBrevRequest = manueltBrevRequest)

            // Assert
            verifyOrder {
                tilgangService.validerTilgangTilFagsak(fagsakId = fagsak.id, event = AuditLoggerEvent.UPDATE)
                dokumentService.sendManueltBrev(manueltBrevRequest = manueltBrevRequest, fagsakId = fagsak.id)
            }
        }

        @Test
        fun `skal ikke sende brev når saksbehandler mangler tilgang til fagsaken`() {
            // Arrange
            every {
                tilgangService.validerTilgangTilFagsak(fagsakId = fagsak.id, event = AuditLoggerEvent.UPDATE)
            } throws RolleTilgangskontrollFeil("Mangler tilgang til fagsaken")

            // Act & Assert
            assertThrows<RolleTilgangskontrollFeil> {
                dokumentController.sendBrevPåFagsak(fagsakId = fagsak.id, manueltBrevRequest = manueltBrevRequest)
            }

            verify(exactly = 0) { dokumentService.sendManueltBrev(any(), any(), any()) }
            verify(exactly = 0) { fagsakService.lagMinimalFagsakDto(any()) }
        }
    }
}
