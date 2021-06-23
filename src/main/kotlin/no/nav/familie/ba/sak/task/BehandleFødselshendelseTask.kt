package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.AutomatiskVilkårsVurdering
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fødselshendelse.FødselshendelseService
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.domene.FødelshendelsePreLanseringRepository
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.domene.FødselshendelsePreLansering
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(taskStepType = BehandleFødselshendelseTask.TASK_STEP_TYPE,
                     beskrivelse = "Setter i gang behandlingsløp for fødselshendelse",
                     maxAntallFeil = 3)
class BehandleFødselshendelseTask(
        private val fødselshendelseService: FødselshendelseService,

        private val fødselshendelsePreLanseringRepository: FødelshendelsePreLanseringRepository) :
        AsyncTaskStep {

    internal fun morHarPågåendeFagsak(): Boolean {
        TODO()
    }

    override fun doTask(task: Task) {
        val behandleFødselshendelseTaskDTO = objectMapper.readValue(task.payload, BehandleFødselshendelseTaskDTO::class.java)
        logger.info("Kjører BehandleFødselshendelseTask")

        val nyBehandling = behandleFødselshendelseTaskDTO.nyBehandling
        fødselshendelseService.fødselshendelseSkalBehandlesHosInfotrygd(
                nyBehandling.morsIdent,
                nyBehandling.barnasIdenter)

        // Vi har overtatt ruting.
        // Pr. nå sender vi alle hendelser til infotrygd.
        // Koden under fjernes når vi går live.
        // fødselshendelseService.sendTilInfotrygdFeed(nyBehandling.barnasIdenter)

        // Dette er flyten, slik den skal se ut når vi går "live".
        //

        println("funskjon for å finne om mor finnes i databasen til ba-sak")

        if (fødselshendelseService.fødselshendelseSkalBehandlesHosInfotrygd(
                        nyBehandling.morsIdent,
                        nyBehandling.barnasIdenter)) {
            fødselshendelseService.sendTilInfotrygdFeed(nyBehandling.barnasIdenter)

            // Mangler instansiering av faktaFiltrering
        } else if (true) {
            println("filtrer på regler")
            println("dersom reglene ikke går gjennom må behandlingen henlegges og en oppgave må opprettes for saksbehandlerene.")
            if (fødselshendelseService.fødselshendelseBehandlesHosBA(nyBehandling)) {
                println("saken kan bli automatisk godkjent, kjør vurdering")
                //opprett behandling og vurder vilkår
                val behandling = fødselshendelseService.opprettBehandlingForAutomatisertVilkårsVurdering(nyBehandling)
                val vurdering = fødselshendelseService.vilkårsVurdering(behandling)
                if (vurdering.resultat) {
                    println("Saken er biff! og godkjent")
                } else {
                    println("Dette må behandles manuelt likevell")
                    //fødselsservice.opprettManuellBehandlingMedStatusFraAutomatiskVurdering()
                }
                printVilkårsvurdering(vurdering)
            }
        } else {
            fødselshendelseService.sendTilInfotrygdFeed(nyBehandling.barnasIdenter)
            println("Sender til Infotrygd")
        }
        //     behandleHendelseIBaSak(nyBehandling)
        // }
        //
        // Når vi går live skal ba-sak behandle saker som ikke er løpende i infotrygd.
        // Etterhvert som vi kan behandle flere typer saker, utvider vi fødselshendelseSkalBehandlesHosInfotrygd.
    }

    private fun printVilkårsvurdering(vurdering: AutomatiskVilkårsVurdering) {
        println("Saken er ${if (vurdering.resultat) "" else "IKKE"} Godkjent")
        println("mor bosatt i riket: ${vurdering.morBosattIRiket}")
        println("barn under 18: ${vurdering.barnErUnder18}")
        println("barn bor med søker: ${vurdering.barnBorMedSøker}")
        println("barn er ugift: ${vurdering.barnErUgift}")
        println("barn bosatt i riket: ${vurdering.barnErBosattIRiket}")
    }

    private fun behandleHendelseIBaSak(nyBehandling: NyBehandlingHendelse) {
        try {
            fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(nyBehandling)
        } catch (e: KontrollertRollbackException) {
            when (e.fødselshendelsePreLansering) {
                null -> logger.error("Rollback har blitt trigget, men data fra fødselshendelse mangler")
                else -> fødselshendelsePreLanseringRepository.save(e.fødselshendelsePreLansering.copy(id = 0))
            }
            logger.info("Rollback utført. Data ikke persistert.")
        } catch (e: Throwable) {
            logger.error("FødselshendelseTask kjørte med Feil=${e.message}")

            if (e is Feil) {
                secureLogger.error("FødselshendelseTask kjørte med Feil=${e.frontendFeilmelding}", e)
            } else {
                secureLogger.error("FødselshendelseTask feilet!", e)
            }
            throw e
        }
    }

    companion object {

        const val TASK_STEP_TYPE = "behandleFødselshendelseTask"
        private val logger = LoggerFactory.getLogger(BehandleFødselshendelseTask::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")

        fun opprettTask(behandleFødselshendelseTaskDTO: BehandleFødselshendelseTaskDTO): Task {
            return Task.nyTask(
                    type = TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(behandleFødselshendelseTaskDTO),
                    properties = Properties().apply {
                        this["morsIdent"] = behandleFødselshendelseTaskDTO.nyBehandling.morsIdent
                    }
            )
        }
    }
}

data class KontrollertRollbackException(val fødselshendelsePreLansering: FødselshendelsePreLansering?) : RuntimeException()