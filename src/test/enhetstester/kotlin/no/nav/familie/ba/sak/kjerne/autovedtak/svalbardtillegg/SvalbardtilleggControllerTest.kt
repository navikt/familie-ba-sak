package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE
import org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE

class SvalbardtilleggControllerTest {
    private val tilgangService = mockk<TilgangService>()
    private val opprettTaskService = mockk<OpprettTaskService>()
    private val fagsakService = mockk<FagsakService>()
    private val personidentService = mockk<PersonidentService>()
    private val taskService = mockk<TaskService>()

    private val svalbardtilleggController =
        SvalbardtilleggController(
            tilgangService = tilgangService,
            opprettTaskService = opprettTaskService,
            fagsakService = fagsakService,
            personidentService = personidentService,
            taskService = taskService,
        )

    private val ident = randomFnr()
    private val personIdent = PersonIdent(ident)
    private val aktør = lagAktør(ident)

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilPersoner(any(), any()) }
        every { personidentService.hentAktør(ident) } returns aktør
    }

    @Test
    fun `vurderSvalbardtillegg skal validere tilgang til person`() {
        // Arrange
        every {
            tilgangService.validerTilgangTilPersoner(
                personIdenter = listOf(ident),
                event = AuditLoggerEvent.UPDATE,
            )
        } throws RolleTilgangskontrollFeil("Ikke tilgang")

        // Act
        val feilmelding =
            assertThrows<RuntimeException> {
                svalbardtilleggController.vurderSvalbardtillegg(personIdent)
            }

        // Assert
        assertThat(feilmelding.message).isEqualTo("Ikke tilgang")
    }

    @Test
    fun `vurderSvalbardtillegg skal kaste exception for ugyldig fødselsnummer`() {
        // Arrange
        val ugyldigPersonIdent = PersonIdent("123456789")

        // Act & Assert
        val feilmelding =
            assertThrows<IllegalStateException> {
                svalbardtilleggController.vurderSvalbardtillegg(ugyldigPersonIdent)
            }

        assertThat(feilmelding.message).isEqualTo(ugyldigPersonIdent.ident)
    }

    @Test
    fun `vurderSvalbardtillegg skal opprette task for fagsaker med riktig type når ingen eksiterende task finnes`() {
        // Arrange
        val fagsak = lagFagsak(id = 1L, aktør = aktør, type = FagsakType.NORMAL)

        every { fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(aktør) } returns listOf(fagsak)
        every { taskService.finnAlleTaskerMedPayloadOgType(fagsak.id.toString(), AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE) } returns emptyList()

        justRun { opprettTaskService.opprettAutovedtakSvalbardtilleggTask(any()) }

        // Act
        svalbardtilleggController.vurderSvalbardtillegg(personIdent)

        // Assert
        verify(exactly = 1) { opprettTaskService.opprettAutovedtakSvalbardtilleggTask(fagsak.id) }
    }

    @ParameterizedTest
    @EnumSource(Status::class, names = ["UBEHANDLET", "KLAR_TIL_PLUKK"], mode = INCLUDE)
    fun `vurderSvalbardtillegg skal ikke opprette task når det eksisterende task ikke har kjørt`(
        taskStatus: Status,
    ) {
        // Arrange
        val fagsak = lagFagsak(id = 1L, aktør = aktør, type = FagsakType.NORMAL)
        val eksisterendeTask = Task(type = AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE, payload = fagsak.id.toString(), status = taskStatus)

        every { fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(aktør) } returns listOf(fagsak)
        every { taskService.finnAlleTaskerMedPayloadOgType(fagsak.id.toString(), AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE) } returns listOf(eksisterendeTask)

        // Act
        svalbardtilleggController.vurderSvalbardtillegg(personIdent)

        // Assert
        verify(exactly = 0) { opprettTaskService.opprettAutovedtakSvalbardtilleggTask(any()) }
    }

    @ParameterizedTest
    @EnumSource(Status::class, names = ["UBEHANDLET", "KLAR_TIL_PLUKK"], mode = EXCLUDE)
    fun `vurderSvalbardtillegg skal opprette task når eksisterende task har kjørt`(
        taskStatus: Status,
    ) {
        // Arrange
        val fagsak = lagFagsak(id = 1L, aktør = aktør, type = FagsakType.NORMAL)
        val eksisterendeTask = Task(type = AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE, payload = fagsak.id.toString(), status = taskStatus)

        every { fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(aktør) } returns listOf(fagsak)
        every { taskService.finnAlleTaskerMedPayloadOgType(fagsak.id.toString(), AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE) } returns listOf(eksisterendeTask)
        justRun { opprettTaskService.opprettAutovedtakSvalbardtilleggTask(any()) }

        // Act
        svalbardtilleggController.vurderSvalbardtillegg(personIdent)

        // Assert
        verify(exactly = 1) { opprettTaskService.opprettAutovedtakSvalbardtilleggTask(fagsak.id) }
    }

    @Test
    fun `vurderSvalbardtillegg skal ikke gjøre noe når person ikke har fagsaker`() {
        // Arrange
        every { fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(aktør) } returns emptyList()

        // Act
        svalbardtilleggController.vurderSvalbardtillegg(personIdent)

        // Assert
        verify(exactly = 0) { opprettTaskService.opprettAutovedtakSvalbardtilleggTask(any()) }
        verify(exactly = 0) { taskService.finnAlleTaskerMedPayloadOgType(any(), any()) }
    }

    @Test
    fun `vurderSvaldbardtillegg skal håndtere flere fagsaker med ulike typer og task-statuser`() {
        // Arrange
        val normalFagsak1 = lagFagsak(id = 1L, aktør = aktør, type = FagsakType.NORMAL)
        val normalFagsak2 = lagFagsak(id = 2L, aktør = aktør, type = FagsakType.NORMAL)
        val barnEnsligFagsak = lagFagsak(id = 3L, aktør = aktør, type = FagsakType.BARN_ENSLIG_MINDREÅRIG)
        val institusjonFagsak = lagFagsak(id = 4L, aktør = aktør, type = FagsakType.INSTITUSJON)
        val skjermetBarnFagsak = lagFagsak(id = 5L, aktør = aktør, type = FagsakType.SKJERMET_BARN)

        val eksisterendeTask =
            Task(
                type = AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE,
                payload = normalFagsak1.id.toString(),
                status = Status.UBEHANDLET,
            )

        every { fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(aktør) } returns
            listOf(normalFagsak1, normalFagsak2, barnEnsligFagsak, institusjonFagsak)

        every { taskService.finnAlleTaskerMedPayloadOgType(normalFagsak1.id.toString(), AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE) } returns listOf(eksisterendeTask)
        every { taskService.finnAlleTaskerMedPayloadOgType(normalFagsak2.id.toString(), AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE) } returns emptyList()
        every { taskService.finnAlleTaskerMedPayloadOgType(barnEnsligFagsak.id.toString(), AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE) } returns emptyList()
        every { taskService.finnAlleTaskerMedPayloadOgType(institusjonFagsak.id.toString(), AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE) } returns emptyList()
        every { taskService.finnAlleTaskerMedPayloadOgType(skjermetBarnFagsak.id.toString(), AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE) } returns emptyList()

        justRun { opprettTaskService.opprettAutovedtakSvalbardtilleggTask(any()) }

        // Act
        svalbardtilleggController.vurderSvalbardtillegg(personIdent)

        // Assert
        verify(exactly = 0) { opprettTaskService.opprettAutovedtakSvalbardtilleggTask(normalFagsak1.id) } // Har allerede UBEHANDLET task
        verify(exactly = 1) { opprettTaskService.opprettAutovedtakSvalbardtilleggTask(normalFagsak2.id) } // NORMAL type, ingen task
        verify(exactly = 1) { opprettTaskService.opprettAutovedtakSvalbardtilleggTask(barnEnsligFagsak.id) } // BARN_ENSLIG_MINDREÅRIG type, ingen task
        verify(exactly = 0) { opprettTaskService.opprettAutovedtakSvalbardtilleggTask(institusjonFagsak.id) } // INSTITUSJON type filtreres bort
        verify(exactly = 0) { opprettTaskService.opprettAutovedtakSvalbardtilleggTask(skjermetBarnFagsak.id) } // SKJERMET_BARN type filtreres bort
    }
}
