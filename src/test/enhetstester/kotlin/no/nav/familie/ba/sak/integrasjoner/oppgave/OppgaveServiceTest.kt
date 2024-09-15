package no.nav.familie.ba.sak.integrasjoner.oppgave

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.lagTestOppgaveDTO
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.DbOppgave
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.FØRSTE_STEG
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class OppgaveServiceTest {
    private val integrasjonClient: IntegrasjonClient = mockk()
    private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository = mockk()
    private val arbeidsfordelingService: ArbeidsfordelingService = mockk()
    private val behandlingRepository: BehandlingRepository = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val personidentService: PersonidentService = mockk()
    private val oppgaveRepository: OppgaveRepository = mockk()
    private val opprettTaskService: OpprettTaskService = mockk()
    private val loggService: LoggService = mockk()
    private val navIdentOgEnhetsnummerService: NavIdentOgEnhetsnummerService = mockk()
    private val oppgaveService: OppgaveService =
        OppgaveService(
            integrasjonClient = integrasjonClient,
            behandlingRepository = behandlingRepository,
            oppgaveRepository = oppgaveRepository,
            opprettTaskService = opprettTaskService,
            loggService = loggService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            navIdentOgEnhetsnummerService = navIdentOgEnhetsnummerService,
        )

    @Test
    fun `Opprett oppgave skal lage oppgave med enhetsnummer fra behandlingen`() {
        // Arrange
        every { behandlingHentOgPersisterService.hent(BEHANDLING_ID) } returns lagTestBehandling(aktørId = AKTØR_ID_FAGSAK)
        every { behandlingHentOgPersisterService.lagreEllerOppdater(any()) } returns lagTestBehandling()
        every { oppgaveRepository.save(any()) } returns lagTestOppgave()
        every { oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(any(), any()) } returns null
        every { personidentService.hentAktør(any()) } returns Aktør(AKTØR_ID_FAGSAK)

        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(any()) } returns
            ArbeidsfordelingPåBehandling(
                behandlingId = 1,
                behandlendeEnhetId = ENHETSNUMMER,
                behandlendeEnhetNavn = "enhet",
            )

        every { arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(any()) } returns
            ArbeidsfordelingPåBehandling(
                behandlingId = 1,
                behandlendeEnhetId = ENHETSNUMMER,
                behandlendeEnhetNavn = "enhet",
            )

        val slot = slot<OpprettOppgaveRequest>()
        every { integrasjonClient.opprettOppgave(capture(slot)) } returns OppgaveResponse(OPPGAVE_ID.toLong())

        every {
            navIdentOgEnhetsnummerService.hentNavIdentOgEnhetsnummer(BEHANDLING_ID, null)
        } returns NavIdentOgEnhetsnummer(null, ENHETSNUMMER)

        // Act
        oppgaveService.opprettOppgave(BEHANDLING_ID, Oppgavetype.BehandleSak, FRIST_FERDIGSTILLELSE_BEH_SAK)

        // Assert
        assertThat(slot.captured.enhetsnummer).isEqualTo(ENHETSNUMMER)
        assertThat(slot.captured.saksId).isEqualTo(FAGSAK_ID.toString())
        assertThat(slot.captured.ident).isEqualTo(
            OppgaveIdentV2(
                ident = AKTØR_ID_FAGSAK,
                gruppe = IdentGruppe.AKTOERID,
            ),
        )
        assertThat(slot.captured.behandlingstema).isEqualTo(Behandlingstema.OrdinærBarnetrygd.value)
        assertThat(slot.captured.fristFerdigstillelse).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(slot.captured.aktivFra).isEqualTo(LocalDate.now())
        assertThat(slot.captured.tema).isEqualTo(Tema.BAR)
        assertThat(slot.captured.beskrivelse).contains("https://barnetrygd.intern.nav.no/fagsak/$FAGSAK_ID")
        assertThat(slot.captured.behandlesAvApplikasjon).isEqualTo("familie-ba-sak")
    }

    @ParameterizedTest
    @EnumSource(ManuellOppgaveType::class)
    fun `Opprett oppgave med manuell oppgavetype skal lage oppgave med behandlesAvApplikasjon satt for småbarnstillegg og åpen behandling, men ikke fødselshendelse`(manuellOppgaveType: ManuellOppgaveType) {
        // Arrange
        every { behandlingHentOgPersisterService.hent(BEHANDLING_ID) } returns lagTestBehandling(aktørId = AKTØR_ID_FAGSAK)
        every { behandlingHentOgPersisterService.lagreEllerOppdater(any()) } returns lagTestBehandling()
        every { oppgaveRepository.save(any()) } returns lagTestOppgave()
        every {
            oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(
                any(),
                any(),
            )
        } returns null
        every { personidentService.hentAktør(any()) } returns Aktør(AKTØR_ID_FAGSAK)

        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(any()) } returns
            ArbeidsfordelingPåBehandling(
                behandlingId = 1,
                behandlendeEnhetId = ENHETSNUMMER,
                behandlendeEnhetNavn = "enhet",
            )

        every { arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(any()) } returns
            ArbeidsfordelingPåBehandling(
                behandlingId = 1,
                behandlendeEnhetId = ENHETSNUMMER,
                behandlendeEnhetNavn = "enhet",
            )

        val slot = slot<OpprettOppgaveRequest>()
        every { integrasjonClient.opprettOppgave(capture(slot)) } returns OppgaveResponse(OPPGAVE_ID.toLong())

        every {
            navIdentOgEnhetsnummerService.hentNavIdentOgEnhetsnummer(BEHANDLING_ID, null)
        } returns NavIdentOgEnhetsnummer(null, ENHETSNUMMER)

        // Act
        oppgaveService.opprettOppgave(
            behandlingId = BEHANDLING_ID,
            oppgavetype = Oppgavetype.VurderLivshendelse,
            fristForFerdigstillelse = FRIST_FERDIGSTILLELSE_BEH_SAK,
            manuellOppgaveType = manuellOppgaveType,
        )

        // Assert
        assertThat(slot.captured.enhetsnummer).isEqualTo(ENHETSNUMMER)
        assertThat(slot.captured.saksId).isEqualTo(FAGSAK_ID.toString())
        assertThat(slot.captured.ident).isEqualTo(
            OppgaveIdentV2(
                ident = AKTØR_ID_FAGSAK,
                gruppe = IdentGruppe.AKTOERID,
            ),
        )
        assertThat(slot.captured.behandlingstema).isEqualTo(Behandlingstema.OrdinærBarnetrygd.value)
        assertThat(slot.captured.fristFerdigstillelse).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(slot.captured.aktivFra).isEqualTo(LocalDate.now())
        assertThat(slot.captured.tema).isEqualTo(Tema.BAR)
        assertThat(slot.captured.beskrivelse).contains("https://barnetrygd.intern.nav.no/fagsak/$FAGSAK_ID")

        when (manuellOppgaveType) {
            ManuellOppgaveType.SMÅBARNSTILLEGG, ManuellOppgaveType.ÅPEN_BEHANDLING -> assertThat(slot.captured.behandlesAvApplikasjon).isEqualTo("familie-ba-sak")
            ManuellOppgaveType.FØDSELSHENDELSE -> assertThat(slot.captured.behandlesAvApplikasjon).isNull()
        }
    }

    @Test
    fun `Ferdigstill oppgave`() {
        // Arrange
        every { behandlingHentOgPersisterService.hent(BEHANDLING_ID) } returns mockk {}
        every {
            oppgaveRepository.finnOppgaverSomSkalFerdigstilles(
                any(),
                any(),
            )
        } returns listOf(lagTestOppgave())
        every { oppgaveRepository.saveAndFlush(any()) } returns lagTestOppgave()
        val slot = slot<Long>()
        every { integrasjonClient.ferdigstillOppgave(capture(slot)) } just runs
        every { integrasjonClient.finnOppgaveMedId(any()) } returns lagTestOppgaveDTO(0L)

        // Act
        oppgaveService.ferdigstillOppgaver(BEHANDLING_ID, Oppgavetype.BehandleSak)

        // Assert
        assertThat(slot.captured).isEqualTo(OPPGAVE_ID.toLong())
    }

    @Test
    fun `Fordel oppgave skal tildele oppgave til saksbehandler`() {
        // Arrange
        val oppgaveSlot = slot<Long>()
        val saksbehandlerSlot = slot<String>()
        every {
            integrasjonClient.fordelOppgave(
                capture(oppgaveSlot),
                capture(saksbehandlerSlot),
            )
        } returns OppgaveResponse(OPPGAVE_ID.toLong())
        every { integrasjonClient.finnOppgaveMedId(any()) } returns Oppgave()

        // Act
        oppgaveService.fordelOppgave(OPPGAVE_ID.toLong(), SAKSBEHANDLER_ID)

        // Assert
        assertThat(OPPGAVE_ID.toLong()).isEqualTo(oppgaveSlot.captured)
        assertThat(SAKSBEHANDLER_ID).isEqualTo(saksbehandlerSlot.captured)
    }

    @Test
    fun `Fordel oppgave skal feile når oppgave allerede er tildelt`() {
        // Arrange
        val oppgaveSlot = slot<Long>()
        val saksbehandlerSlot = slot<String>()
        val saksbehandler = "Test Testersen"

        every {
            integrasjonClient.fordelOppgave(
                capture(oppgaveSlot),
                capture(saksbehandlerSlot),
            )
        } returns OppgaveResponse(OPPGAVE_ID.toLong())

        every {
            integrasjonClient.finnOppgaveMedId(
                any(),
            )
        } returns Oppgave(tilordnetRessurs = saksbehandler)

        // Act & assert
        val funksjonellFeil =
            assertThrows<FunksjonellFeil> {
                oppgaveService.fordelOppgave(OPPGAVE_ID.toLong(), SAKSBEHANDLER_ID)
            }
        assertThat("Oppgaven er allerede fordelt til $saksbehandler").isEqualTo(funksjonellFeil.frontendFeilmelding)
    }

    @Test
    fun `Tilbakestill oppgave skal nullstille tildeling på oppgave`() {
        // Arrange
        val fordelOppgaveSlot = slot<Long>()
        val finnOppgaveSlot = slot<Long>()
        every {
            integrasjonClient.fordelOppgave(
                capture(fordelOppgaveSlot),
                any(),
            )
        } returns OppgaveResponse(OPPGAVE_ID.toLong())
        every { integrasjonClient.finnOppgaveMedId(capture(finnOppgaveSlot)) } returns Oppgave()

        // Act
        oppgaveService.tilbakestillFordelingPåOppgave(OPPGAVE_ID.toLong())

        // Assert
        assertThat(OPPGAVE_ID.toLong()).isEqualTo(fordelOppgaveSlot.captured)
        assertThat(OPPGAVE_ID.toLong()).isEqualTo(finnOppgaveSlot.captured)
        verify(exactly = 1) { integrasjonClient.fordelOppgave(any(), null) }
    }

    @Test
    fun `hent oppgavefrister for åpne utvidtet barnetrygd behandlinger`() {
        // Arrange
        every { behandlingRepository.finnÅpneUtvidetBarnetrygdBehandlinger() } returns
            listOf(
                lagTestBehandling().copy(underkategori = BehandlingUnderkategori.UTVIDET, id = 1002602L),
                lagTestBehandling().copy(underkategori = BehandlingUnderkategori.UTVIDET, id = 1002602L),
            )
        every { oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(any(), any()) } returns lagTestOppgave()

        every { integrasjonClient.finnOppgaveMedId(any()) } returns Oppgave(id = 10018798L, fristFerdigstillelse = "21.01.23")

        // Act
        val frister = oppgaveService.hentFristerForÅpneUtvidetBarnetrygdBehandlinger()

        // Assert
        assertThat(
            "behandlingId;oppgaveId;frist\n" +
                "1002602;10018798;21.01.23\n" +
                "1002602;10018798;21.01.23\n",
        ).isEqualTo(frister)
    }

    private fun lagTestBehandling(aktørId: String = "1234567891000"): Behandling =
        Behandling(
            fagsak = Fagsak(id = FAGSAK_ID, aktør = Aktør(aktørId)),
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            kategori = BehandlingKategori.NASJONAL,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            opprettetÅrsak = BehandlingÅrsak.SØKNAD,
        ).also {
            it.behandlingStegTilstand.add(BehandlingStegTilstand(0, it, FØRSTE_STEG))
        }

    private fun lagTestOppgave(): DbOppgave = DbOppgave(behandling = lagTestBehandling(), type = Oppgavetype.BehandleSak, gsakId = OPPGAVE_ID)

    companion object {
        private const val FAGSAK_ID = 10000000L
        private const val BEHANDLING_ID = 20000000L
        private const val OPPGAVE_ID = "42"
        private const val ENHETSNUMMER = "9999"
        private const val AKTØR_ID_FAGSAK = "1234567891000"
        private const val SAKSBEHANDLER_ID = "Z999999"
        private val FRIST_FERDIGSTILLELSE_BEH_SAK = LocalDate.now().plusDays(1)
    }
}
