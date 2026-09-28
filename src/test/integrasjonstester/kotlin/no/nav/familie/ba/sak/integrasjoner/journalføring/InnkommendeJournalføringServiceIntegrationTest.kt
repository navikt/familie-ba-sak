package no.nav.familie.ba.sak.integrasjoner.journalføring

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBarnetrygdSøknadV9
import no.nav.familie.ba.sak.datagenerator.lagMockJournalføringDto
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonDto
import no.nav.familie.ba.sak.ekstern.restDomene.NavnOgIdent
import no.nav.familie.ba.sak.ekstern.restDomene.TilknyttetBehandling
import no.nav.familie.ba.sak.fake.FakeIntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.Journalføringsbehandlingstype
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.kontrakter.ba.søknad.VersjonertBarnetrygdSøknadV9
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class InnkommendeJournalføringServiceIntegrationTest(
    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService,
    @Autowired
    private val innkommendeJournalføringService: InnkommendeJournalføringService,
    @Autowired
    private val behandlingSøknadsinfoRepository: BehandlingSøknadsinfoRepository,
    @Autowired
    private val integrasjonsKlient: FakeIntegrasjonKlient,
) : AbstractSpringIntegrationTest() {
    @BeforeEach
    fun setUp() {
        integrasjonsKlient.nullstillOppdaterteLogiskeVedlegg()
    }

    @Test
    fun `journalfør skal bulk-oppdatere logiske vedlegg kun for dokumenter der titlene er endret`() {
        // Arrange
        val journalføringDto = lagMockJournalføringDto(bruker = NavnOgIdent("Mock", randomFnr()))
        val request =
            journalføringDto.copy(
                dokumenter =
                    listOf(
                        journalføringDto.dokumenter[0].copy(
                            logiskeVedlegg =
                                listOf(
                                    LogiskVedlegg(logiskVedleggId = "0", tittel = "Oppholdstillatelse"),
                                    LogiskVedlegg(logiskVedleggId = "0", tittel = "Vigselsattest"),
                                ),
                        ),
                        journalføringDto.dokumenter[1],
                    ),
            )

        // Act
        innkommendeJournalføringService.journalfør(request, "123", "mockEnhet", "1")

        // Assert
        assertThat(integrasjonsKlient.hentOppdaterteLogiskeVedlegg())
            .containsExactlyEntriesOf(mapOf("1" to listOf("Oppholdstillatelse", "Vigselsattest")))
    }

    @Test
    fun `journalfør skal ikke oppdatere logiske vedlegg når titlene er uendret`() {
        // Arrange
        val request = lagMockJournalføringDto(bruker = NavnOgIdent("Mock", randomFnr()))

        // Act
        innkommendeJournalføringService.journalfør(request, "123", "mockEnhet", "1")

        // Assert
        assertThat(integrasjonsKlient.hentOppdaterteLogiskeVedlegg()).isEmpty()
    }

    @Test
    fun `journalfør skal opprette en førstegangsbehandling fra journalføring og lagre ned søknadsinfo`() {
        // Arrange
        val søkerFnr = randomFnr()
        val request = lagMockJournalføringDto(bruker = NavnOgIdent("Mock", søkerFnr))

        // Act
        val fagsakId = innkommendeJournalføringService.journalfør(request, "123", "mockEnhet", "1")

        // Assert
        val behandling = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId.toLong())
        assertNotNull(behandling)
        assertEquals(request.nyBehandlingstype.tilBehandingType(), behandling!!.type)
        assertEquals(request.nyBehandlingsårsak, behandling.opprettetÅrsak)

        val søknadMottattDato = behandlingSøknadsinfoService.hentSøknadMottattDato(behandling.id)
        assertNotNull(søknadMottattDato)
        assertEquals(request.datoMottatt!!.toLocalDate(), søknadMottattDato!!.toLocalDate())

        val søknadsinfo = behandlingSøknadsinfoRepository.findByBehandlingId(behandling.id).single()
        assertEquals(true, søknadsinfo.erDigital)
    }

    @Test
    fun `journalfør skal lagre ned søknadsinfo tilknyttet en tidligere behandling`() {
        // Arrange
        val søkerFnr = randomFnr()
        val førsteSøknad = lagMockJournalføringDto(bruker = NavnOgIdent("Mock", søkerFnr))
        val fagsakId = innkommendeJournalføringService.journalfør(førsteSøknad, "123", "mockEnhet", "1")

        val behandling = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId.toLong())

        val nySøknad2DagerSenere =
            førsteSøknad.copy(
                datoMottatt = førsteSøknad.datoMottatt!!.plusDays(2),
                opprettOgKnyttTilNyBehandling = false,
                tilknyttedeBehandlinger =
                    listOf(
                        TilknyttetBehandling(
                            behandlingstype = Journalføringsbehandlingstype.FØRSTEGANGSBEHANDLING,
                            behandlingId = behandling!!.id.toString(),
                        ),
                    ),
            )

        // Act
        innkommendeJournalføringService.journalfør(nySøknad2DagerSenere, "124", "mockEnhet", "2")

        // Assert
        val søknadsinfo = behandlingSøknadsinfoRepository.findByBehandlingId(behandling.id)
        assertEquals(2, søknadsinfo.size)

        val søknadMottattDato = behandlingSøknadsinfoService.hentSøknadMottattDato(behandling.id)
        assertEquals(førsteSøknad.datoMottatt!!.toLocalDate(), søknadMottattDato!!.toLocalDate())
    }

    @Test
    fun `journalfør skal opprette behandling på fagsak som har BARN som eier hvis enslig mindreårig eller institusjon`() {
        // Arrange
        val request =
            lagMockJournalføringDto(bruker = NavnOgIdent("Mock", randomFnr()))
                .copy(fagsakType = FagsakType.BARN_ENSLIG_MINDREÅRIG)

        val journalpostId = "123"
        integrasjonsKlient.leggTilVersjonertBarnetrygdSøknad(
            journalpostId,
            VersjonertBarnetrygdSøknadV9(
                lagBarnetrygdSøknadV9(
                    søkerFnr = request.bruker.id,
                    barnFnr = emptyList(),
                ),
            ),
        )

        // Act
        val fagsakId = innkommendeJournalføringService.journalfør(request, journalpostId, "mockEnhet", "1")

        // Assert
        val behandling = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId.toLong())

        assertNotNull(behandling)
        assertEquals(FagsakType.BARN_ENSLIG_MINDREÅRIG, behandling!!.fagsak.type)

        // Arrange
        val request2 =
            lagMockJournalføringDto(bruker = NavnOgIdent("Mock", randomFnr()))
                .copy(fagsakType = FagsakType.INSTITUSJON, institusjon = InstitusjonDto("orgnr", tssEksternId = "tss"))

        // Act
        val fagsakId2 = innkommendeJournalføringService.journalfør(request2, "1234", "mockEnhet", "2")

        // Assert
        val behandling2 = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId2.toLong())

        assertNotNull(behandling2)
        assertEquals(FagsakType.INSTITUSJON, behandling2!!.fagsak.type)
    }

    @Test
    fun `journalfør skal ikke opprette en førstegangsbehandling fra journalføring med manglende mottatt dato`() {
        // Arrange
        val søkerFnr = randomFnr()
        val request = lagMockJournalføringDto(bruker = NavnOgIdent("Mock", søkerFnr)).copy(datoMottatt = null)

        // Act & Assert
        val exception =
            assertThrows<RuntimeException> {
                innkommendeJournalføringService.journalfør(
                    request,
                    "123",
                    "mockEnhet",
                    "1",
                )
            }
        assertEquals("Du må sette søknads mottatt dato før du kan fortsette videre", exception.message)
    }
}
