package no.nav.familie.ba.sak.integrasjoner.oppgave

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.lagArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.datagenerator.lagTestOppgaveDTO
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.DbOppgave
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.TilpassArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.hentArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.tilArbeidsfordelingsenhet
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
import no.nav.familie.kontrakter.felles.NavIdent
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
    private val mockedIntegrasjonKlient: IntegrasjonKlient = mockk()
    private val mockedArbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository = mockk()
    private val mockedBehandlingRepository: BehandlingRepository = mockk()
    private val mockedBehandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val mockedPersonidentService: PersonidentService = mockk()
    private val mockedOppgaveRepository: OppgaveRepository = mockk()
    private val mockedOpprettTaskService: OpprettTaskService = mockk()
    private val mockedLoggService: LoggService = mockk()
    private val mockedTilpassArbeidsfordelingService: TilpassArbeidsfordelingService = mockk()
    private val oppgaveService: OppgaveService =
        OppgaveService(
            integrasjonKlient = mockedIntegrasjonKlient,
            behandlingRepository = mockedBehandlingRepository,
            oppgaveRepository = mockedOppgaveRepository,
            opprettTaskService = mockedOpprettTaskService,
            loggService = mockedLoggService,
            behandlingHentOgPersisterService = mockedBehandlingHentOgPersisterService,
            tilpassArbeidsfordelingService = mockedTilpassArbeidsfordelingService,
            arbeidsfordelingPåBehandlingRepository = mockedArbeidsfordelingPåBehandlingRepository,
        )

    @Test
    fun `Opprett oppgave skal lage oppgave med enhetsnummer fra behandlingen`() {
        // Arrange
        val arbeidsfordelingPåBehandling =
            lagArbeidsfordelingPåBehandling(
                behandlingId = 1,
                behandlendeEnhetId = ENHETSNUMMER,
                behandlendeEnhetNavn = "enhet",
                manueltOverstyrt = true,
            )

        val arbeidsfordelingsenhet = arbeidsfordelingPåBehandling.tilArbeidsfordelingsenhet()
        val navIdent = NavIdent("navIdent")

        every { mockedBehandlingHentOgPersisterService.hent(BEHANDLING_ID) } returns lagTestBehandling(aktørId = AKTØR_ID_FAGSAK)
        every { mockedBehandlingHentOgPersisterService.lagreEllerOppdater(any()) } returns lagTestBehandling()
        every { mockedOppgaveRepository.save(any()) } returns lagTestOppgave()
        every { mockedOppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(any(), any()) } returns null
        every { mockedPersonidentService.hentAktør(any()) } returns Aktør(AKTØR_ID_FAGSAK)
        every { mockedArbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(any()) } returns arbeidsfordelingPåBehandling

        val opprettOppgaveRequestSlot = slot<OpprettOppgaveRequest>()
        every { mockedIntegrasjonKlient.opprettOppgave(capture(opprettOppgaveRequestSlot)) } returns OppgaveResponse(OPPGAVE_ID.toLong())

        every { mockedTilpassArbeidsfordelingService.bestemTilordnetRessursPåOppgave(arbeidsfordelingsenhet, null) } returns null

        // Act
        oppgaveService.opprettOppgave(BEHANDLING_ID, Oppgavetype.BehandleSak, FRIST_FERDIGSTILLELSE_BEH_SAK)

        // Assert
        assertThat(opprettOppgaveRequestSlot.captured.enhetsnummer).isEqualTo(ENHETSNUMMER)
        assertThat(opprettOppgaveRequestSlot.captured.saksId).isEqualTo(FAGSAK_ID.toString())
        assertThat(opprettOppgaveRequestSlot.captured.ident).isEqualTo(
            OppgaveIdentV2(
                ident = AKTØR_ID_FAGSAK,
                gruppe = IdentGruppe.AKTOERID,
            ),
        )
        assertThat(opprettOppgaveRequestSlot.captured.behandlingstema).isEqualTo(Behandlingstema.OrdinærBarnetrygd.value)
        assertThat(opprettOppgaveRequestSlot.captured.fristFerdigstillelse).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(opprettOppgaveRequestSlot.captured.aktivFra).isEqualTo(LocalDate.now())
        assertThat(opprettOppgaveRequestSlot.captured.tema).isEqualTo(Tema.BAR)
        assertThat(opprettOppgaveRequestSlot.captured.beskrivelse).contains("https://barnetrygd.intern.nav.no/fagsak/$FAGSAK_ID")
        assertThat(opprettOppgaveRequestSlot.captured.behandlesAvApplikasjon).isEqualTo("familie-ba-sak")
        assertThat(opprettOppgaveRequestSlot.captured.tilordnetRessurs).isNull()
        verify(exactly = 0) { mockedArbeidsfordelingPåBehandlingRepository.save(any()) }
    }

    @ParameterizedTest
    @EnumSource(ManuellOppgaveType::class)
    fun `Opprett oppgave med manuell oppgavetype skal lage oppgave med behandlesAvApplikasjon satt for småbarnstillegg og åpen behandling, men ikke fødselshendelse`(manuellOppgaveType: ManuellOppgaveType) {
        // Arrange
        val arbeidsfordelingPåBehandling =
            lagArbeidsfordelingPåBehandling(
                behandlingId = 1,
                behandlendeEnhetId = ENHETSNUMMER,
                behandlendeEnhetNavn = "enhet",
                manueltOverstyrt = false,
            )

        val arbeidsfordelingsenhet = arbeidsfordelingPåBehandling.tilArbeidsfordelingsenhet()

        every { mockedBehandlingHentOgPersisterService.hent(BEHANDLING_ID) } returns lagTestBehandling(aktørId = AKTØR_ID_FAGSAK)
        every { mockedBehandlingHentOgPersisterService.lagreEllerOppdater(any()) } returns lagTestBehandling()
        every { mockedOppgaveRepository.save(any()) } returns lagTestOppgave()
        every {
            mockedOppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(
                any(),
                any(),
            )
        } returns null
        every { mockedPersonidentService.hentAktør(any()) } returns Aktør(AKTØR_ID_FAGSAK)
        every { mockedArbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(any()) } returns arbeidsfordelingPåBehandling

        val opprettOppgaveRequestSlot = slot<OpprettOppgaveRequest>()
        every { mockedIntegrasjonKlient.opprettOppgave(capture(opprettOppgaveRequestSlot)) } returns OppgaveResponse(OPPGAVE_ID.toLong())

        every { mockedTilpassArbeidsfordelingService.bestemTilordnetRessursPåOppgave(arbeidsfordelingsenhet, null) } returns null

        // Act
        oppgaveService.opprettOppgave(
            behandlingId = BEHANDLING_ID,
            oppgavetype = Oppgavetype.VurderLivshendelse,
            fristForFerdigstillelse = FRIST_FERDIGSTILLELSE_BEH_SAK,
            manuellOppgaveType = manuellOppgaveType,
        )

        // Assert
        assertThat(opprettOppgaveRequestSlot.captured.enhetsnummer).isEqualTo(ENHETSNUMMER)
        assertThat(opprettOppgaveRequestSlot.captured.saksId).isEqualTo(FAGSAK_ID.toString())
        assertThat(opprettOppgaveRequestSlot.captured.ident).isEqualTo(
            OppgaveIdentV2(
                ident = AKTØR_ID_FAGSAK,
                gruppe = IdentGruppe.AKTOERID,
            ),
        )
        assertThat(opprettOppgaveRequestSlot.captured.behandlingstema).isEqualTo(Behandlingstema.OrdinærBarnetrygd.value)
        assertThat(opprettOppgaveRequestSlot.captured.fristFerdigstillelse).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(opprettOppgaveRequestSlot.captured.aktivFra).isEqualTo(LocalDate.now())
        assertThat(opprettOppgaveRequestSlot.captured.tema).isEqualTo(Tema.BAR)
        assertThat(opprettOppgaveRequestSlot.captured.beskrivelse).contains("https://barnetrygd.intern.nav.no/fagsak/$FAGSAK_ID")
        assertThat(opprettOppgaveRequestSlot.captured.tilordnetRessurs).isNull()
        when (manuellOppgaveType) {
            ManuellOppgaveType.SMÅBARNSTILLEGG, ManuellOppgaveType.ÅPEN_BEHANDLING, ManuellOppgaveType.FINNMARKSTILLEGG, ManuellOppgaveType.SVALBARDTILLEGG -> assertThat(opprettOppgaveRequestSlot.captured.behandlesAvApplikasjon).isEqualTo("familie-ba-sak")
            ManuellOppgaveType.FØDSELSHENDELSE -> assertThat(opprettOppgaveRequestSlot.captured.behandlesAvApplikasjon).isNull()
        }
        verify(exactly = 0) { mockedArbeidsfordelingPåBehandlingRepository.save(any()) }
    }

    @Test
    fun `Ferdigstill oppgave`() {
        // Arrange
        every { mockedBehandlingHentOgPersisterService.hent(BEHANDLING_ID) } returns mockk {}
        every {
            mockedOppgaveRepository.finnOppgaverSomSkalFerdigstilles(
                any(),
                any(),
            )
        } returns listOf(lagTestOppgave())
        every { mockedOppgaveRepository.saveAndFlush(any()) } returns lagTestOppgave()
        val slot = slot<Long>()
        every { mockedIntegrasjonKlient.ferdigstillOppgave(capture(slot)) } just runs
        every { mockedIntegrasjonKlient.finnOppgaveMedId(any()) } returns lagTestOppgaveDTO(0L)

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
            mockedIntegrasjonKlient.fordelOppgave(
                capture(oppgaveSlot),
                capture(saksbehandlerSlot),
            )
        } returns OppgaveResponse(OPPGAVE_ID.toLong())
        every { mockedIntegrasjonKlient.finnOppgaveMedId(any()) } returns Oppgave()

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
            mockedIntegrasjonKlient.fordelOppgave(
                capture(oppgaveSlot),
                capture(saksbehandlerSlot),
            )
        } returns OppgaveResponse(OPPGAVE_ID.toLong())

        every {
            mockedIntegrasjonKlient.finnOppgaveMedId(
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
            mockedIntegrasjonKlient.fordelOppgave(
                capture(fordelOppgaveSlot),
                any(),
            )
        } returns OppgaveResponse(OPPGAVE_ID.toLong())
        every { mockedIntegrasjonKlient.finnOppgaveMedId(capture(finnOppgaveSlot)) } returns Oppgave()

        // Act
        oppgaveService.tilbakestillFordelingPåOppgave(OPPGAVE_ID.toLong())

        // Assert
        assertThat(OPPGAVE_ID.toLong()).isEqualTo(fordelOppgaveSlot.captured)
        assertThat(OPPGAVE_ID.toLong()).isEqualTo(finnOppgaveSlot.captured)
        verify(exactly = 1) { mockedIntegrasjonKlient.fordelOppgave(any(), null) }
    }

    @Test
    fun `hent oppgavefrister for åpne utvidtet barnetrygd behandlinger`() {
        // Arrange
        every { mockedBehandlingRepository.finnÅpneUtvidetBarnetrygdBehandlinger() } returns
            listOf(
                lagTestBehandling().copy(underkategori = BehandlingUnderkategori.UTVIDET, id = 1002602L),
                lagTestBehandling().copy(underkategori = BehandlingUnderkategori.UTVIDET, id = 1002602L),
            )
        every { mockedOppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(any(), any()) } returns lagTestOppgave()

        every { mockedIntegrasjonKlient.finnOppgaveMedId(any()) } returns Oppgave(id = 10018798L, fristFerdigstillelse = "21.01.23")

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
