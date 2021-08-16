package no.nav.familie.ba.sak.integrasjoner.oppgave

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.DbOppgave
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.FØRSTE_STEG
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OppgaveServiceTest {

    @MockK
    lateinit var integrasjonClient: IntegrasjonClient

    @MockK
    lateinit var personopplysningerService: PersonopplysningerService

    @MockK
    lateinit var arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository

    @MockK
    lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockK
    lateinit var behandlingRepository: BehandlingRepository

    @MockK
    lateinit var oppgaveRepository: OppgaveRepository

    @InjectMockKs
    lateinit var oppgaveService: OppgaveService

    @Test
    fun `Opprett oppgave skal lage oppgave med enhetsnummer fra behandlingen`() {
        every { behandlingRepository.finnBehandling(BEHANDLING_ID) } returns lagTestBehandling()
        every { behandlingRepository.save(any<Behandling>()) } returns lagTestBehandling()
        every { oppgaveRepository.save(any<DbOppgave>()) } returns lagTestOppgave()
        every {
            oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(any<Oppgavetype>(),
                                                                               any<Behandling>())
        } returns null
        every { personopplysningerService.hentAktivAktørId(any()) } returns AktørId(AKTØR_ID_FAGSAK)

        every { arbeidsfordelingService.hentAbeidsfordelingPåBehandling(any()) } returns ArbeidsfordelingPåBehandling(behandlingId = 1,
                                                                                                                      behandlendeEnhetId = ENHETSNUMMER,
                                                                                                                      behandlendeEnhetNavn = "enhet")

        every { arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(any()) } returns ArbeidsfordelingPåBehandling(
                behandlingId = 1,
                behandlendeEnhetId = ENHETSNUMMER,
                behandlendeEnhetNavn = "enhet")

        val slot = slot<OpprettOppgaveRequest>()
        every { integrasjonClient.opprettOppgave(capture(slot)) } returns OPPGAVE_ID

        oppgaveService.opprettOppgave(BEHANDLING_ID, Oppgavetype.BehandleSak, FRIST_FERDIGSTILLELSE_BEH_SAK)

        assertThat(slot.captured.enhetsnummer).isEqualTo(ENHETSNUMMER)
        assertThat(slot.captured.saksId).isEqualTo(FAGSAK_ID.toString())
        assertThat(slot.captured.ident).isEqualTo(OppgaveIdentV2(ident = AKTØR_ID_FAGSAK, gruppe = IdentGruppe.AKTOERID))
        assertThat(slot.captured.behandlingstema).isEqualTo(Behandlingstema.OrdinærBarnetrygd.value)
        assertThat(slot.captured.fristFerdigstillelse).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(slot.captured.aktivFra).isEqualTo(LocalDate.now())
        assertThat(slot.captured.tema).isEqualTo(Tema.BAR)
        assertThat(slot.captured.beskrivelse).contains("https://barnetrygd.nais.adeo.no/fagsak/$FAGSAK_ID")
    }

    @Test
    fun `Ferdigstill oppgave`() {
        every { behandlingRepository.finnBehandling(BEHANDLING_ID) } returns mockk {}
        every {
            oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(any<Oppgavetype>(),
                                                                               any<Behandling>())
        } returns lagTestOppgave()
        every { oppgaveRepository.save(any<DbOppgave>()) } returns lagTestOppgave()
        val slot = slot<Long>()
        every { integrasjonClient.ferdigstillOppgave(capture(slot)) } just runs

        oppgaveService.ferdigstillOppgave(BEHANDLING_ID, Oppgavetype.BehandleSak)
        assertThat(slot.captured).isEqualTo(OPPGAVE_ID.toLong())
    }

    @Test
    fun `Ferdigstill oppgave feiler fordi den ikke finner oppgave på behandlingen`() {
        every {
            oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(any<Oppgavetype>(),
                                                                               any<Behandling>())
        } returns null
        every { oppgaveRepository.save(any<DbOppgave>()) } returns lagTestOppgave()
        every { behandlingRepository.finnBehandling(BEHANDLING_ID) } returns mockk {}

        assertThatThrownBy { oppgaveService.ferdigstillOppgave(BEHANDLING_ID, Oppgavetype.BehandleSak) }
                .hasMessage("Finner ikke oppgave for behandling $BEHANDLING_ID")
                .isInstanceOf(java.lang.IllegalStateException::class.java)
    }

    @Test
    fun `Fordel oppgave skal tildele oppgave til saksbehandler`() {
        val oppgaveSlot = slot<Long>()
        val saksbehandlerSlot = slot<String>()
        every { integrasjonClient.fordelOppgave(capture(oppgaveSlot), capture(saksbehandlerSlot)) } returns OPPGAVE_ID
        every { integrasjonClient.finnOppgaveMedId(any()) } returns Oppgave()

        oppgaveService.fordelOppgave(OPPGAVE_ID.toLong(), SAKSBEHANDLER_ID)

        assertEquals(OPPGAVE_ID.toLong(), oppgaveSlot.captured)
        assertEquals(SAKSBEHANDLER_ID, saksbehandlerSlot.captured)
    }

    @Test
    fun `Fordel oppgave skal feile når oppgave allerede er tildelt`() {
        val oppgaveSlot = slot<Long>()
        val saksbehandlerSlot = slot<String>()
        val saksbehandler = "Test Testersen"
        every { integrasjonClient.fordelOppgave(capture(oppgaveSlot), capture(saksbehandlerSlot)) } returns OPPGAVE_ID
        every { integrasjonClient.finnOppgaveMedId(any()) } returns Oppgave(tilordnetRessurs = saksbehandler)

        val funksjonellFeil =
                assertThrows<FunksjonellFeil> { oppgaveService.fordelOppgave(OPPGAVE_ID.toLong(), SAKSBEHANDLER_ID) }

        assertEquals("Oppgaven er allerede fordelt til $saksbehandler", funksjonellFeil.frontendFeilmelding)
    }

    @Test
    fun `Tilbakestill oppgave skal nullstille tildeling på oppgave`() {
        val fordelOppgaveSlot = slot<Long>()
        val finnOppgaveSlot = slot<Long>()
        every { integrasjonClient.fordelOppgave(capture(fordelOppgaveSlot), any()) } returns OPPGAVE_ID
        every { integrasjonClient.finnOppgaveMedId(capture(finnOppgaveSlot)) } returns Oppgave()

        oppgaveService.tilbakestillFordelingPåOppgave(OPPGAVE_ID.toLong())

        assertEquals(OPPGAVE_ID.toLong(), fordelOppgaveSlot.captured)
        assertEquals(OPPGAVE_ID.toLong(), finnOppgaveSlot.captured)
        verify(exactly = 1) { integrasjonClient.fordelOppgave(any(), null) }
    }

    private fun lagTestBehandling(): Behandling {
        return Behandling(
                fagsak = Fagsak(id = FAGSAK_ID).also {
                    it.søkerIdenter = setOf(FagsakPerson(personIdent = PersonIdent(ident = FNR), fagsak = it))
                },
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                opprettetÅrsak = BehandlingÅrsak.SØKNAD).also {
            it.behandlingStegTilstand.add(BehandlingStegTilstand(0, it, FØRSTE_STEG))
        }
    }

    private fun lagTestOppgave(): DbOppgave {
        return DbOppgave(behandling = lagTestBehandling(), type = Oppgavetype.BehandleSak, gsakId = OPPGAVE_ID)
    }

    companion object {

        private const val FAGSAK_ID = 10000000L
        private const val BEHANDLING_ID = 20000000L
        private const val OPPGAVE_ID = "42"
        private const val FNR = "fnr"
        private const val ENHETSNUMMER = "enhet"
        private const val AKTØR_ID_FAGSAK = "0123456789"
        private const val SAKSBEHANDLER_ID = "Z999999"
        private val FRIST_FERDIGSTILLELSE_BEH_SAK = LocalDate.now().plusDays(1)
    }
}