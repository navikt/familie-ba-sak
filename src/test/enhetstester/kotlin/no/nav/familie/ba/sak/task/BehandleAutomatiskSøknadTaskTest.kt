package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.Søknadsinfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.task.dto.BehandleAutomatiskSøknadTaskDTO
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class BehandleAutomatiskSøknadTaskTest {
    private val autovedtakStegService = mockk<AutovedtakStegService>()
    private val fagsakService = mockk<FagsakService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val stegService = mockk<StegService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()

    private val behandleAutomatiskSøknadTask =
        BehandleAutomatiskSøknadTask(
            autovedtakStegService = autovedtakStegService,
            oppgaveService = oppgaveService,
            stegService = stegService,
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        )

    private val søkersIdent = randomFnr()
    private val søkersAktør = randomAktør(søkersIdent)
    private val fagsak = lagFagsak(aktør = søkersAktør)

    private val nyBehandling =
        NyBehandling(
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            kategori = BehandlingKategori.EØS,
            underkategori = BehandlingUnderkategori.UTVIDET,
            fagsakId = 1L,
            barnasIdenter = listOf(randomFnr()),
            søknadMottattDato = LocalDate.of(2026, 1, 1),
            søknadsinfo =
                Søknadsinfo(
                    journalpostId = "123456789",
                    brevkode = "NAV 33-00.07",
                    erDigital = true,
                ),
        )

    @Nested
    inner class DoTask {
        @Test
        fun `skal kjøre automatisk behandling av søknad`() {
            // Arrange
            val task = BehandleAutomatiskSøknadTask.opprettTask(BehandleAutomatiskSøknadTaskDTO(nyBehandling))

            every { fagsakService.hentPåFagsakId(nyBehandling.fagsakId) } returns fagsak
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(nyBehandling.fagsakId) } returns false
            every { autovedtakStegService.kjørAutomatiskBehandlingSøknad(any(), any(), any()) } returns "KJØRT OK"

            // Act
            behandleAutomatiskSøknadTask.doTask(task)

            // Assert
            verify(exactly = 1) {
                autovedtakStegService.kjørAutomatiskBehandlingSøknad(
                    mottakersAktør = søkersAktør,
                    nyBehandling = nyBehandling,
                    førstegangKjørt = any(),
                )
            }
            verify(exactly = 0) { stegService.håndterNyBehandlingOgSendInfotrygdFeed(any()) }
            verify(exactly = 0) { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any(), any()) }
        }

        @Test
        fun `skal håndtere ny behandling og opprette oppgave for manuell behandling hvis automatisk behandling må gjøres manuelt`() {
            // Arrange
            val task = BehandleAutomatiskSøknadTask.opprettTask(BehandleAutomatiskSøknadTaskDTO(nyBehandling))
            val behandling = lagBehandling()

            every { fagsakService.hentPåFagsakId(nyBehandling.fagsakId) } returns fagsak
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(nyBehandling.fagsakId) } returns false
            every {
                autovedtakStegService.kjørAutomatiskBehandlingSøknad(any(), any(), any())
            } throws AutovedtakMåBehandlesManueltFeil("Ikke kandidat for automatisk behandling")
            every { stegService.håndterNyBehandlingOgSendInfotrygdFeed(nyBehandling) } returns behandling
            every {
                oppgaveService.opprettOppgaveForManuellBehandling(
                    behandlingId = behandling.id,
                    begrunnelse = any(),
                    manuellOppgaveType = ManuellOppgaveType.SØKNAD,
                    oppgavetype = Oppgavetype.BehandleSak,
                )
            } returns "oppgaveId"

            // Act
            behandleAutomatiskSøknadTask.doTask(task)

            // Assert
            verify(exactly = 1) { stegService.håndterNyBehandlingOgSendInfotrygdFeed(nyBehandling) }
            verify(exactly = 1) {
                oppgaveService.opprettOppgaveForManuellBehandling(
                    behandlingId = behandling.id,
                    begrunnelse = "Ikke kandidat for automatisk behandling. Må behandles manuelt.",
                    manuellOppgaveType = ManuellOppgaveType.SØKNAD,
                    oppgavetype = Oppgavetype.BehandleSak,
                )
            }
        }

        @Test
        fun `skal opprette den manuelle behandlingen med årsak SØKNAD når baks-mottak sendte årsak AUTOMATISK_BEHANDLING_AV_SØKNAD`() {
            // Arrange
            val nyBehandlingFraMottak = nyBehandling.copy(behandlingÅrsak = BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD)
            val task = BehandleAutomatiskSøknadTask.opprettTask(BehandleAutomatiskSøknadTaskDTO(nyBehandlingFraMottak))
            val behandling = lagBehandling()
            val nyBehandlingSlot = slot<NyBehandling>()

            every { fagsakService.hentPåFagsakId(nyBehandlingFraMottak.fagsakId) } returns fagsak
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(nyBehandlingFraMottak.fagsakId) } returns false
            every {
                autovedtakStegService.kjørAutomatiskBehandlingSøknad(any(), any(), any())
            } throws AutovedtakMåBehandlesManueltFeil("Ikke kandidat for automatisk behandling")
            every { stegService.håndterNyBehandlingOgSendInfotrygdFeed(capture(nyBehandlingSlot)) } returns behandling
            every { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any(), any()) } returns "oppgaveId"

            // Act
            behandleAutomatiskSøknadTask.doTask(task)

            // Assert
            assertThat(nyBehandlingSlot.captured.behandlingÅrsak).isEqualTo(BehandlingÅrsak.SØKNAD)
        }

        @Test
        fun `skal kaste feil og ikke behandle søknad hvis det finnes en åpen behandling på fagsak`() {
            // Arrange
            val task = BehandleAutomatiskSøknadTask.opprettTask(BehandleAutomatiskSøknadTaskDTO(nyBehandling))

            every { fagsakService.hentPåFagsakId(nyBehandling.fagsakId) } returns fagsak
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(nyBehandling.fagsakId) } returns true

            // Act & Assert
            val exception = assertThrows<Feil> { behandleAutomatiskSøknadTask.doTask(task) }
            assertThat(exception.message).isEqualTo("Det er ikke mulig å behandle en søknad automatisk hvis fagsak=${nyBehandling.fagsakId} har en åpen behandling.")

            verify(exactly = 0) { autovedtakStegService.kjørAutomatiskBehandlingSøknad(any(), any(), any()) }
            verify(exactly = 0) { stegService.håndterNyBehandlingOgSendInfotrygdFeed(any()) }
            verify(exactly = 0) { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any(), any()) }
        }
    }

    @Nested
    inner class OpprettTask {
        @Test
        fun `skal sette riktig type, metadata, og trigger tid`() {
            // Act
            val nåtidspunkt = LocalDateTime.of(2026, 9, 14, 21, 1, 0)
            val task = BehandleAutomatiskSøknadTask.opprettTask(BehandleAutomatiskSøknadTaskDTO(nyBehandling), nåtidspunkt)

            // Assert
            assertThat(task.type).isEqualTo(BehandleAutomatiskSøknadTask.TASK_STEP_TYPE)
            assertThat(task.triggerTid).isEqualTo(LocalDateTime.of(2026, 9, 15, 6, 0, 0))
            assertThat(task.metadata["fagsakId"]).isEqualTo(nyBehandling.fagsakId.toString())
        }
    }
}
