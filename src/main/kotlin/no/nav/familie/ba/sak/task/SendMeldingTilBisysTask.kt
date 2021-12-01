package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat

import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.statistikk.producer.KafkaProducer
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.Properties

@Service
@TaskStepBeskrivelse(taskStepType = SendMeldingTilBisysTask.TASK_STEP_TYPE, beskrivelse = "Iverksett vedtak mot oppdrag", maxAntallFeil = 3)
class SendMeldingTilBisysTask(
    private val behandlingService: BehandlingService,
    private val kafkaProducer: KafkaProducer,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(SendMeldingTilBisysTask::class.java)

    override fun doTask(task: Task) {
        val behandling = behandlingService.hent(task.payload.toLong())
        // Bisys vil kun ha rene manuelle opphør
        if (behandling.resultat == BehandlingResultat.OPPHØRT) {
            kafkaProducer.sendOpphørBarnetrygdBisys(behandling.fagsak.hentAktivIdent().ident, finnOpphørsdato(behandling.id), behandling.id.toString())
        } else {
            logger.info("Sender ikke melding til bisys siden resultat ikke er opphørt.")
        }
    }



    private fun finnOpphørsdato(behandlingId: Long): YearMonth {
        return tilkjentYtelseRepository.findByBehandlingOptional(behandlingId)?.opphørFom!!
    }

    companion object {
        const val TASK_STEP_TYPE = "sendMeldingOmOpphørTilBisys"

        fun opprettTask(behandlingsId: Long): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = behandlingsId.toString(),
                properties = Properties().apply {
                    this["behandlingsId"] = behandlingsId.toString()
                }
            )
        }
    }
}