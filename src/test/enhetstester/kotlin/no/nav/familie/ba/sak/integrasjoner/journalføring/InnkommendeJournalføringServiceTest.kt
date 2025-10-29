package no.nav.familie.ba.sak.integrasjoner.journalføring

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagTilgangsstyrtJournalpost
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.klage.KlageService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import org.assertj.core.api.Assertions.assertThat
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
}
