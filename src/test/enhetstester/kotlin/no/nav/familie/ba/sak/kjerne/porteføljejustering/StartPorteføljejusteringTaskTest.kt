package no.nav.familie.ba.sak.kjerne.porteføljejustering

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.internal.TaskService
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class StartPorteføljejusteringTaskTest {
    private val integrasjonKlient: IntegrasjonKlient = mockk()
    private val taskService: TaskService = mockk()

    private val startPorteføljejusteringTask = StartPorteføljejusteringTask(integrasjonKlient, taskService)

    @Test
    fun `Skal hente barnetrygd oppgaver hos enhet Steinkjer og opprette task på flytting av enhet`() {
        // Arrange
        val finnOppgaveRequestForBarSteinkjer =
            FinnOppgaveRequest(
                tema = Tema.BAR,
                enhet = BarnetrygdEnhet.STEINKJER.enhetsnummer,
            )
        every { integrasjonKlient.hentOppgaver(finnOppgaveRequestForBarSteinkjer) } returns
            FinnOppgaveResponseDto(
                antallTreffTotalt = 2,
                oppgaver =
                    listOf(
                        Oppgave(id = 1, tildeltEnhetsnr = BarnetrygdEnhet.STEINKJER.enhetsnummer, mappeId = 50),
                        Oppgave(id = 2, tildeltEnhetsnr = BarnetrygdEnhet.STEINKJER.enhetsnummer, mappeId = 50),
                    ),
            )

        every { taskService.finnAlleTaskerMedType(PorteføljejusteringFlyttOppgaveTask.TASK_STEP_TYPE) } returns emptyList()

        every { taskService.save(any()) } returns mockk()

        val task = StartPorteføljejusteringTask.opprettTask(dryRun = false)

        // Act
        startPorteføljejusteringTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.hentOppgaver(finnOppgaveRequestForBarSteinkjer) }
        verify(exactly = 1) {
            taskService.save(
                match { task ->
                    val test = objectMapper.readValue<PorteføljejusteringFlyttOppgaveDto>(task.payload)
                    test.oppgaveId == 1L && test.originalEnhet == BarnetrygdEnhet.STEINKJER.enhetsnummer && test.originalMappeId == 50L
                },
            )
        }
        verify(exactly = 1) {
            taskService.save(
                match { task ->
                    val test = objectMapper.readValue<PorteføljejusteringFlyttOppgaveDto>(task.payload)
                    test.oppgaveId == 2L && test.originalEnhet == BarnetrygdEnhet.STEINKJER.enhetsnummer && test.originalMappeId == 50L
                },
            )
        }
    }

    @Test
    fun `Skal ikke opprette opprette tasks hvis dryrun er satt til true`() {
        // Arrange
        val finnOppgaveRequestForBarSteinkjer =
            FinnOppgaveRequest(
                tema = Tema.BAR,
                enhet = BarnetrygdEnhet.STEINKJER.enhetsnummer,
            )
        every { integrasjonKlient.hentOppgaver(finnOppgaveRequestForBarSteinkjer) } returns
            FinnOppgaveResponseDto(
                antallTreffTotalt = 2,
                oppgaver =
                    listOf(
                        Oppgave(id = 1, tildeltEnhetsnr = BarnetrygdEnhet.STEINKJER.enhetsnummer, mappeId = 50),
                        Oppgave(id = 2, tildeltEnhetsnr = BarnetrygdEnhet.STEINKJER.enhetsnummer, mappeId = 50),
                    ),
            )

        every { taskService.finnAlleTaskerMedType(PorteføljejusteringFlyttOppgaveTask.TASK_STEP_TYPE) } returns emptyList()

        every { taskService.save(any()) } returns mockk()

        val task = StartPorteføljejusteringTask.opprettTask(dryRun = true)

        // Act
        startPorteføljejusteringTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.hentOppgaver(finnOppgaveRequestForBarSteinkjer) }
        verify(exactly = 0) {
            taskService.save(any())
        }
    }

    @Test
    fun `Skal ikke opprette flytte task på oppgaver som har infotrygd sak i saksreferanse`() {
        // Arrange
        val finnOppgaveRequestForBarSteinkjer =
            FinnOppgaveRequest(
                tema = Tema.BAR,
                enhet = BarnetrygdEnhet.STEINKJER.enhetsnummer,
            )
        every { integrasjonKlient.hentOppgaver(finnOppgaveRequestForBarSteinkjer) } returns
            FinnOppgaveResponseDto(
                antallTreffTotalt = 2,
                oppgaver =
                    listOf(
                        Oppgave(id = 1, tildeltEnhetsnr = BarnetrygdEnhet.STEINKJER.enhetsnummer, mappeId = 50, saksreferanse = "12B34"),
                        Oppgave(id = 2, tildeltEnhetsnr = BarnetrygdEnhet.STEINKJER.enhetsnummer, mappeId = 50, saksreferanse = "IT01"),
                    ),
            )

        every { taskService.finnAlleTaskerMedType(PorteføljejusteringFlyttOppgaveTask.TASK_STEP_TYPE) } returns emptyList()

        every { taskService.save(any()) } returns mockk()

        val task = StartPorteføljejusteringTask.opprettTask(dryRun = false)

        // Act
        startPorteføljejusteringTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.hentOppgaver(finnOppgaveRequestForBarSteinkjer) }
        verify(exactly = 0) {
            taskService.save(
                match { task ->
                    val test = objectMapper.readValue<PorteføljejusteringFlyttOppgaveDto>(task.payload)
                    test.oppgaveId == 1L && test.originalEnhet == BarnetrygdEnhet.STEINKJER.enhetsnummer && test.originalMappeId == 50L
                },
            )
        }
        verify(exactly = 1) {
            taskService.save(
                match { task ->
                    val test = objectMapper.readValue<PorteføljejusteringFlyttOppgaveDto>(task.payload)
                    test.oppgaveId == 2L && test.originalEnhet == BarnetrygdEnhet.STEINKJER.enhetsnummer && test.originalMappeId == 50L
                },
            )
        }
    }

    @ParameterizedTest
    @EnumSource(Oppgavetype::class, names = ["BehandleSak", "BehandleSED", "Journalføring"], mode = EnumSource.Mode.INCLUDE)
    fun `Skal opprette flytte task på oppgaver som ikke har mappeid når det er av spesifikk oppgavetype`(oppgavetype: Oppgavetype) {
        // Arrange
        val finnOppgaveRequestForBarSteinkjer =
            FinnOppgaveRequest(
                tema = Tema.BAR,
                enhet = BarnetrygdEnhet.STEINKJER.enhetsnummer,
            )
        every { integrasjonKlient.hentOppgaver(finnOppgaveRequestForBarSteinkjer) } returns
            FinnOppgaveResponseDto(
                antallTreffTotalt = 1,
                oppgaver =
                    listOf(
                        Oppgave(
                            id = 1,
                            tildeltEnhetsnr = BarnetrygdEnhet.STEINKJER.enhetsnummer,
                            mappeId = null,
                            saksreferanse = "12345",
                            oppgavetype = oppgavetype.value,
                        ),
                    ),
            )

        every { taskService.finnAlleTaskerMedType(PorteføljejusteringFlyttOppgaveTask.TASK_STEP_TYPE) } returns emptyList()

        every { taskService.save(any()) } returns mockk()

        val task = StartPorteføljejusteringTask.opprettTask(dryRun = false)

        // Act
        startPorteføljejusteringTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.hentOppgaver(finnOppgaveRequestForBarSteinkjer) }
        verify(exactly = 1) {
            taskService.save(
                match { task ->
                    val test = objectMapper.readValue<PorteføljejusteringFlyttOppgaveDto>(task.payload)
                    test.oppgaveId == 1L && test.originalEnhet == BarnetrygdEnhet.STEINKJER.enhetsnummer && test.originalMappeId == null
                },
            )
        }
    }

    @ParameterizedTest
    @EnumSource(Oppgavetype::class, names = ["BehandleSak", "BehandleSED", "Journalføring"], mode = EnumSource.Mode.EXCLUDE)
    fun `Skal ikke opprette flytte task på oppgaver som ikke har mappeid hvis det ikke er av spesifikk oppgavetype, `(oppgavetype: Oppgavetype) {
        // Arrange
        val finnOppgaveRequestForBarSteinkjer =
            FinnOppgaveRequest(
                tema = Tema.BAR,
                enhet = BarnetrygdEnhet.STEINKJER.enhetsnummer,
            )
        every { integrasjonKlient.hentOppgaver(finnOppgaveRequestForBarSteinkjer) } returns
            FinnOppgaveResponseDto(
                antallTreffTotalt = 1,
                oppgaver =
                    listOf(
                        Oppgave(
                            id = 1,
                            tildeltEnhetsnr = BarnetrygdEnhet.STEINKJER.enhetsnummer,
                            mappeId = null,
                            saksreferanse = "12345",
                            oppgavetype = oppgavetype.value,
                        ),
                    ),
            )

        every { taskService.finnAlleTaskerMedType(PorteføljejusteringFlyttOppgaveTask.TASK_STEP_TYPE) } returns emptyList()

        every { taskService.save(any()) } returns mockk()

        val task = StartPorteføljejusteringTask.opprettTask(dryRun = false)

        // Act
        startPorteføljejusteringTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.hentOppgaver(finnOppgaveRequestForBarSteinkjer) }
        verify(exactly = 0) { taskService.save(any()) }
    }

    @Test
    fun `Skal hente barnetrygd oppgaver hos enhet Steinkjer og opprette task dersom task for oppgave ikke allerede eksisterer`() {
        // Arrange
        val finnOppgaveRequestForBarSteinkjer =
            FinnOppgaveRequest(
                tema = Tema.BAR,
                enhet = BarnetrygdEnhet.STEINKJER.enhetsnummer,
            )
        every { integrasjonKlient.hentOppgaver(finnOppgaveRequestForBarSteinkjer) } returns
            FinnOppgaveResponseDto(
                antallTreffTotalt = 2,
                oppgaver =
                    listOf(
                        Oppgave(id = 1, tildeltEnhetsnr = BarnetrygdEnhet.STEINKJER.enhetsnummer, mappeId = 50),
                        Oppgave(id = 2, tildeltEnhetsnr = BarnetrygdEnhet.STEINKJER.enhetsnummer, mappeId = 50),
                    ),
            )

        every { taskService.finnAlleTaskerMedType(PorteføljejusteringFlyttOppgaveTask.TASK_STEP_TYPE) } returns listOf(PorteføljejusteringFlyttOppgaveTask.opprettTask(1, BarnetrygdEnhet.STEINKJER.enhetsnummer, 50L))

        every { taskService.save(any()) } returns mockk()

        val task = StartPorteføljejusteringTask.opprettTask(dryRun = false)

        // Act
        startPorteføljejusteringTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.hentOppgaver(finnOppgaveRequestForBarSteinkjer) }
        verify(exactly = 0) {
            taskService.save(
                match { task ->
                    val test = objectMapper.readValue<PorteføljejusteringFlyttOppgaveDto>(task.payload)
                    test.oppgaveId == 1L && test.originalEnhet == BarnetrygdEnhet.STEINKJER.enhetsnummer && test.originalMappeId == 50L
                },
            )
        }
        verify(exactly = 1) {
            taskService.save(
                match { task ->
                    val test = objectMapper.readValue<PorteføljejusteringFlyttOppgaveDto>(task.payload)
                    test.oppgaveId == 2L && test.originalEnhet == BarnetrygdEnhet.STEINKJER.enhetsnummer && test.originalMappeId == 50L
                },
            )
        }
    }
}
