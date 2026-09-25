package no.nav.familie.ba.sak.integrasjoner.journalføring

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagMockJournalføringDto
import no.nav.familie.ba.sak.datagenerator.lagTestJournalpost
import no.nav.familie.ba.sak.datagenerator.lagTilgangsstyrtJournalpost
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.JournalføringDto
import no.nav.familie.ba.sak.ekstern.restDomene.NavnOgIdent
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.OppdaterJournalpostResponse
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.klage.KlageService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.BulkOppdaterLogiskVedleggRequest
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class InnkommendeJournalføringServiceTest {
    private val mockedIntegrasjonKlient: IntegrasjonKlient = mockk()
    private val mockedFagsakService: FagsakService = mockk()
    private val mockedBehandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val mockedLoggService: LoggService = mockk()
    private val mockedStegService: StegService = mockk()
    private val mockedJournalføringMetrikk: JournalføringMetrikk = mockk()
    private val mockedBehandlingSøknadsinfoService: BehandlingSøknadsinfoService = mockk()
    private val klageService: KlageService = mockk()
    private val innkommendeJournalføringService: InnkommendeJournalføringService =
        InnkommendeJournalføringService(
            integrasjonKlient = mockedIntegrasjonKlient,
            fagsakService = mockedFagsakService,
            behandlingHentOgPersisterService = mockedBehandlingHentOgPersisterService,
            loggService = mockedLoggService,
            stegService = mockedStegService,
            journalføringMetrikk = mockedJournalføringMetrikk,
            behandlingSøknadsinfoService = mockedBehandlingSøknadsinfoService,
            klageService = klageService,
        )

    @Test
    fun `skal hente og returnere tilgangsstyrte journalposter`() {
        // Arrange
        val brukerId = "12345678910"
        val journalpostId = "123"
        val journalposter =
            listOf(
                lagTilgangsstyrtJournalpost(
                    personIdent = brukerId,
                    journalpostId = journalpostId,
                    harTilgang = true,
                ),
            )

        every {
            mockedIntegrasjonKlient.hentTilgangsstyrteJournalposterForBruker(
                JournalposterForBrukerRequest(
                    antall = 1000,
                    brukerId = Bruker(id = brukerId, type = BrukerIdType.FNR),
                    tema = listOf(Tema.BAR),
                ),
            )
        } returns journalposter

        // Act
        val journalposterForBruker = innkommendeJournalføringService.hentJournalposterForBruker(brukerId)

        // Assert
        assertThat(journalposterForBruker.first { it.journalpost.journalpostId === journalpostId }.journalpostTilgang.harTilgang).isTrue()
    }

    @Nested
    inner class OppdaterLogiskeVedlegg {
        private val journalpostId = "123"
        private val journalpost =
            lagTestJournalpost(
                personIdent = randomFnr(),
                journalpostId = journalpostId,
                avsenderMottakerIdType = AvsenderMottakerIdType.FNR,
                kanal = "SKAN_IM",
            )

        @BeforeEach
        fun setUp() {
            every { mockedFagsakService.hentEllerOpprettFagsak(any(), any(), any(), any(), any()) } returns lagFagsak()
            every { mockedIntegrasjonKlient.hentJournalpost(journalpostId) } returns journalpost
            every { mockedIntegrasjonKlient.oppdaterLogiskeVedlegg(any(), any()) } just runs
            every { mockedIntegrasjonKlient.oppdaterJournalpost(any(), any()) } returns OppdaterJournalpostResponse(journalpostId)
            every { mockedIntegrasjonKlient.ferdigstillJournalpost(any(), any()) } just runs
            every { mockedIntegrasjonKlient.ferdigstillOppgave(any()) } just runs
            every { mockedJournalføringMetrikk.tellManuellJournalføringsmetrikker(any(), any()) } just runs
        }

        @Test
        fun `skal bulk-oppdatere logiske vedlegg kun for dokumenter der titlene er endret`() {
            // Arrange
            val nyeTitler = listOf("Oppholdstillatelse", "Pass", "Vigselsattest")
            val request =
                lagJournalføringDto(
                    logiskeVedleggDokument1 = nyeTitler.map { LogiskVedlegg(logiskVedleggId = "0", tittel = it) },
                    logiskeVedleggDokument2 = listOf(LogiskVedlegg(logiskVedleggId = "0", tittel = "Pass")),
                )

            // Act
            innkommendeJournalføringService.journalfør(request, journalpostId, "4820", "1")

            // Assert
            verify(exactly = 1) { mockedIntegrasjonKlient.oppdaterLogiskeVedlegg("1", BulkOppdaterLogiskVedleggRequest(titler = nyeTitler)) }
            verify(exactly = 0) { mockedIntegrasjonKlient.oppdaterLogiskeVedlegg("2", any()) }
        }

        @Test
        fun `skal fjerne alle logiske vedlegg når tom liste sendes inn`() {
            // Arrange
            val request =
                lagJournalføringDto(
                    logiskeVedleggDokument1 = emptyList(),
                    logiskeVedleggDokument2 = listOf(LogiskVedlegg(logiskVedleggId = "123", tittel = "Pass")),
                )

            // Act
            innkommendeJournalføringService.journalfør(request, journalpostId, "4820", "1")

            // Assert
            verify(exactly = 1) { mockedIntegrasjonKlient.oppdaterLogiskeVedlegg("1", BulkOppdaterLogiskVedleggRequest(titler = emptyList())) }
            verify(exactly = 0) { mockedIntegrasjonKlient.oppdaterLogiskeVedlegg("2", any()) }
        }

        @Test
        fun `skal ikke oppdatere logiske vedlegg når logiske vedlegg ikke er sendt inn`() {
            // Arrange
            val request = lagJournalføringDto(logiskeVedleggDokument1 = null, logiskeVedleggDokument2 = null)

            // Act
            innkommendeJournalføringService.journalfør(request, journalpostId, "4820", "1")

            // Assert
            verify(exactly = 0) { mockedIntegrasjonKlient.oppdaterLogiskeVedlegg(any(), any()) }
        }

        @Test
        fun `skal oppdatere logiske vedlegg før journalposten oppdateres og ferdigstilles`() {
            // Arrange
            val request =
                lagJournalføringDto(
                    logiskeVedleggDokument1 = listOf(LogiskVedlegg(logiskVedleggId = "0", tittel = "Vigselsattest")),
                    logiskeVedleggDokument2 = null,
                )

            // Act
            innkommendeJournalføringService.journalfør(request, journalpostId, "4820", "1")

            // Assert
            verifyOrder {
                mockedIntegrasjonKlient.oppdaterLogiskeVedlegg("1", BulkOppdaterLogiskVedleggRequest(titler = listOf("Vigselsattest")))
                mockedIntegrasjonKlient.oppdaterJournalpost(any(), journalpostId)
                mockedIntegrasjonKlient.ferdigstillJournalpost(journalpostId, "4820")
            }
        }

        private fun lagJournalføringDto(
            logiskeVedleggDokument1: List<LogiskVedlegg>?,
            logiskeVedleggDokument2: List<LogiskVedlegg>?,
        ): JournalføringDto {
            val journalføringDto = lagMockJournalføringDto(bruker = NavnOgIdent("Mock", randomFnr()))
            return journalføringDto.copy(
                opprettOgKnyttTilNyBehandling = false,
                dokumenter =
                    listOf(
                        journalføringDto.dokumenter[0].copy(logiskeVedlegg = logiskeVedleggDokument1),
                        journalføringDto.dokumenter[1].copy(logiskeVedlegg = logiskeVedleggDokument2),
                    ),
            )
        }
    }
}
