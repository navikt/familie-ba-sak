package no.nav.familie.ba.sak.integrasjoner.økonomi

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.årMnd
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.steg.StatusFraOppdrag
import no.nav.familie.ba.sak.kjerne.steg.StatusFraOppdragMedTask
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HentStatusTest {

    private val økonomiKlient = mockk<ØkonomiKlient>()

    private val beregningService: BeregningService = mockk()

    lateinit var statusFraOppdrag: StatusFraOppdrag

    @BeforeEach
    fun setUp() {
        val økonomiService = ØkonomiService(
            behandlingHentOgPersisterService = mockk(),
            økonomiKlient = økonomiKlient,
            beregningService = beregningService,
            utbetalingsoppdragGenerator = mockk(),
            behandlingService = mockk(),
            featureToggleService = mockk(),
            tilkjentYtelseValideringService = mockk()
        )
        statusFraOppdrag = StatusFraOppdrag(
            økonomiService = økonomiService,
            taskRepository = mockk<TaskRepositoryWrapper>().also { every { it.save(any()) } returns mockk() }
        )
    }

    @Test
    fun `enkel test`() {
        val tilfeldigPerson = tilfeldigPerson()
        val nyBehandling = lagBehandling()
        every {
            økonomiKlient.hentStatus(
                match { it.behandlingsId == nyBehandling.id.toString() }
            )
        } throws RuntimeException()

        val opprinneligBehandlingsid = -1L

        every {
            økonomiKlient.hentStatus(
                match { it.behandlingsId == opprinneligBehandlingsid.toString() }
            )
        } returns OppdragStatus.KVITTERT_OK
        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2019-04"),
                årMnd("2020-03"),
                YtelseType.ORDINÆR_BARNETRYGD,
                10,
                behandling = nyBehandling,
                person = tilfeldigPerson,
                aktør = mockk(),
                tilkjentYtelse = mockk(),
                kildeBehandlingId = opprinneligBehandlingsid
            )
        )

        every { beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(any()) } returns andelerTilkjentYtelse

        val statusFraOppdragMedTask = StatusFraOppdragMedTask(
            statusFraOppdragDTO = StatusFraOppdragDTO(
                fagsystem = "BA",
                personIdent = tilfeldigPerson.aktør.aktivFødselsnummer(),
                aktørId = "Søker1",
                behandlingsId = nyBehandling.id,
                vedtaksId = 0L
            ),
            task = Task(
                type = StatusFraOppdragTask.TASK_STEP_TYPE,
                payload = ""
            )
        )

        val nesteSteg = statusFraOppdrag.utførStegOgAngiNeste(nyBehandling, statusFraOppdragMedTask)
        assertThat(nesteSteg).isEqualTo(StegType.IVERKSETT_MOT_FAMILIE_TILBAKE)
    }
}
