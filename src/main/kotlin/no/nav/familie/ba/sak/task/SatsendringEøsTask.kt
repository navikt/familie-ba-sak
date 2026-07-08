package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.AutovedtakSkalIkkeGjennomføresFeil
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.SatsendringEøsKjøringService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
@TaskStepBeskrivelse(
    taskStepType = SatsendringEøsTask.TASK_STEP_TYPE,
    beskrivelse = "Utfør satsendring EØS",
    maxAntallFeil = 3,
)
class SatsendringEøsTask(
    private val autovedtakStegService: AutovedtakStegService,
    private val fagsakService: FagsakService,
    private val satsendringEøsKjøringService: SatsendringEøsKjøringService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val dto = jsonMapper.readValue(task.payload, SatsendringEøsTaskDto::class.java)
        val aktør = fagsakService.hentAktør(dto.fagsakId)

        val resultat =
            try {
                autovedtakStegService.kjørBehandlingSatsendringEøs(
                    mottakersAktør = aktør,
                    fagsakId = dto.fagsakId,
                    utbetalingsland = dto.utbetalingsland,
                    satsTidspunkt = dto.satsTidspunkt,
                    førstegangKjørt = task.opprettetTid,
                )
            } catch (feil: AutovedtakSkalIkkeGjennomføresFeil) {
                feil.beskrivelse
            } catch (feil: AutovedtakMåBehandlesManueltFeil) {
                satsendringEøsKjøringService.settFeiltype(dto.fagsakId, dto.utbetalingsland, dto.satsTidspunkt, feil.beskrivelse)
                feil.beskrivelse
            }

        satsendringEøsKjøringService.settFerdigTidspunkt(dto.fagsakId, dto.utbetalingsland, dto.satsTidspunkt)

        task.metadata["resultat"] = resultat
    }

    companion object {
        const val TASK_STEP_TYPE = "satsendringEøs"
    }
}

data class SatsendringEøsTaskDto(
    val fagsakId: Long,
    val utbetalingsland: String,
    val satsTidspunkt: YearMonth,
)
