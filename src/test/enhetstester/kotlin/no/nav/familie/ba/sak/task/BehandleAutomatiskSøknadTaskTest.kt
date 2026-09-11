package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.Søknad
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.task.BehandleAutomatiskSøknadTask.Companion.TASK_STEP_TYPE
import no.nav.familie.ba.sak.task.dto.BehandleAutomatiskSøknadTaskDTO
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.Properties

class BehandleAutomatiskSøknadTaskTest {
    private val autovedtakStegService = mockk<AutovedtakStegService>()
    private val personidentService = mockk<PersonidentService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val stegService = mockk<StegService>()

    private val behandleAutomatiskSøknadTask =
        BehandleAutomatiskSøknadTask(
            autovedtakStegService = autovedtakStegService,
            personidentService = personidentService,
            oppgaveService = oppgaveService,
            stegService = stegService,
        )

    private val søkersIdent = randomFnr()
    private val søkersAktør = randomAktør(søkersIdent)

    private val nyBehandling =
        NyBehandling(
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            fagsakId = 1L,
            søkersIdent = søkersIdent,
            barnasIdenter = listOf(randomFnr()),
            søknadMottattDato = LocalDate.of(2026, 1, 1),
        )

    @Nested
    inner class DoTask {
        @Test
        fun `skal kaste feil hvis søkers ident er null`() {
            // Arrange
            val nyBehandlingUtenSøkersIdent = nyBehandling.copy(søkersIdent = null)

            val task =
                Task(
                    type = TASK_STEP_TYPE,
                    payload = jsonMapper.writeValueAsString(BehandleAutomatiskSøknadTaskDTO(nyBehandlingUtenSøkersIdent)),
                    properties = Properties(),
                )

            // Act & Assert
            val feilmelding =
                assertThrows<Feil> {
                    behandleAutomatiskSøknadTask.doTask(task)
                }.message

            assertThat(feilmelding).isEqualTo("Søkers ident kan ikke være null i en BehandleAutomatiskSøknadTask task.")

            verify(exactly = 0) { autovedtakStegService.kjørAutomatiskBehandlingSøknad(any(), any()) }
        }

        @Test
        fun `skal kjøre automatisk behandling av søknad`() {
            // Arrange
            val task = BehandleAutomatiskSøknadTask.opprettTask(BehandleAutomatiskSøknadTaskDTO(nyBehandling))

            every { personidentService.hentAktør(søkersIdent) } returns søkersAktør
            every { autovedtakStegService.kjørAutomatiskBehandlingSøknad(any(), any(), any()) } returns "KJØRT OK"

            // Act
            behandleAutomatiskSøknadTask.doTask(task)

            // Assert
            verify(exactly = 1) {
                autovedtakStegService.kjørAutomatiskBehandlingSøknad(
                    mottakersAktør = søkersAktør,
                    søknad =
                        Søknad(
                            fagsakId = nyBehandling.fagsakId,
                            søkersIdent = søkersIdent,
                            barnasIdenter = nyBehandling.barnasIdenter,
                        ),
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

            every { personidentService.hentAktør(søkersIdent) } returns søkersAktør
            every { autovedtakStegService.kjørAutomatiskBehandlingSøknad(any(), any(), any()) } throws
                AutovedtakMåBehandlesManueltFeil("Ikke kandidat for automatisk behandling")
            every { stegService.håndterNyBehandlingOgSendInfotrygdFeed(nyBehandling) } returns behandling
            every {
                oppgaveService.opprettOppgaveForManuellBehandling(
                    behandlingId = behandling.id,
                    begrunnelse = any(),
                    manuellOppgaveType = ManuellOppgaveType.FØDSELSHENDELSE,
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
                    manuellOppgaveType = ManuellOppgaveType.FØDSELSHENDELSE,
                    oppgavetype = Oppgavetype.BehandleSak,
                )
            }
        }
    }

    @Nested
    inner class OpprettTask {
        @Test
        fun `skal sette riktig type og metadata`() {
            // Act
            val task = BehandleAutomatiskSøknadTask.opprettTask(BehandleAutomatiskSøknadTaskDTO(nyBehandling))

            // Assert
            assertThat(task.type).isEqualTo(BehandleAutomatiskSøknadTask.TASK_STEP_TYPE)
            assertThat(task.metadata["søkersIdent"]).isEqualTo(søkersIdent)
        }

        @Test
        fun `skal kaste feil hvis søkers ident er null`() {
            // Act & assert
            val exception =
                assertThrows<Feil> {
                    BehandleAutomatiskSøknadTask.opprettTask(BehandleAutomatiskSøknadTaskDTO(nyBehandling.copy(søkersIdent = null)))
                }
            assertThat(exception.message).isEqualTo("Søkers ident kan ikke være null i en ${BehandleAutomatiskSøknadTask::class.simpleName} task.")
        }
    }
}
