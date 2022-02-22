package no.nav.familie.ba.sak.integrasjoner.journalføring

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.ekstern.restDomene.NavnOgIdent
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpostType
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.Sakstype
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.lagMockRestJournalføring
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class JournalføringServiceTest(

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val journalføringService: JournalføringService,

    @Autowired
    private val journalføringRepository: JournalføringRepository,
) : AbstractSpringIntegrationTest() {

    @Test
    fun `lagrer journalpostreferanse til behandling og fagsak til journalpost`() {

        val søkerFnr = randomFnr()
        val søkerAktør = personidentService.hentAktør(søkerFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsak(søkerAktør.aktivFødselsnummer())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val (sak, behandlinger) = journalføringService
            .lagreJournalpostOgKnyttFagsakTilJournalpost(listOf(behandling.id.toString()), "12345")

        val journalposter = journalføringRepository.findByBehandlingId(behandlingId = behandling.id)

        assertEquals(1, journalposter.size)
        assertEquals(DbJournalpostType.I, journalposter.first().type)
        assertEquals(fagsak.id.toString(), sak.fagsakId)
        assertEquals(1, behandlinger.size)
    }

    @Test
    fun `ferdigstill skal oppdatere journalpost med GENERELL_SAKSTYPE hvis knyttTilFagsak er false`() {
        val søkerFnr = randomFnr()
        val søkerAktør = personidentService.hentAktør(søkerFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsak(søkerAktør.aktivFødselsnummer())
        behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val (sak, behandlinger) = journalføringService
            .lagreJournalpostOgKnyttFagsakTilJournalpost(listOf(), "12345")

        assertNull(sak.fagsakId)
        assertEquals(Sakstype.GENERELL_SAK.type, sak.sakstype)
        assertEquals(0, behandlinger.size)
    }

    @Test
    fun `journalfør skal opprette en førstegangsbehandling fra journalføring`() {
        val søkerFnr = randomFnr()
        val request = lagMockRestJournalføring(bruker = NavnOgIdent("Mock", søkerFnr))
        val fagsakId = journalføringService.journalfør(request, "123", "mockEnhet", "1")

        val behandling = behandlingService.hentAktivForFagsak(fagsakId.toLong())
        assertNotNull(behandling)
        assertEquals(request.nyBehandlingstype, behandling!!.type)
        assertEquals(request.nyBehandlingsårsak, behandling.opprettetÅrsak)

        val søknadMottattDato = behandlingService.hentSøknadMottattDato(behandling.id)
        assertNotNull(søknadMottattDato)
        assertEquals(request.datoMottatt!!.toLocalDate(), søknadMottattDato!!.toLocalDate())
    }

    @Test
    fun `journalfør skal ikke opprette en førstegangsbehandling fra journalføring med manglende mottatt dato`() {
        val søkerFnr = randomFnr()
        val request = lagMockRestJournalføring(bruker = NavnOgIdent("Mock", søkerFnr)).copy(datoMottatt = null)

        val exception = assertThrows<RuntimeException> {
            journalføringService.journalfør(
                request,
                "123",
                "mockEnhet",
                "1"
            )
        }
        assertEquals("Du må sette søknads mottatt dato før du kan fortsette videre", exception.message)
    }
}
