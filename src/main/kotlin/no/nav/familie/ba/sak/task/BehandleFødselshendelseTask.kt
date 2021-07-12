package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FagsystemRegelVurdering
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FiltreringsreglerResultat
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FødselshendelseServiceNy
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fødselshendelse.FødselshendelseServiceGammel
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.domene.FødselshendelsePreLansering
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties


@Service
@TaskStepBeskrivelse(
        taskStepType = BehandleFødselshendelseTask.TASK_STEP_TYPE,
        beskrivelse = "Setter i gang behandlingsløp for fødselshendelse",
        maxAntallFeil = 3
)
class BehandleFødselshendelseTask(
        private val fødselshendelseServiceGammel: FødselshendelseServiceGammel,
        private val fødselshendelseServiceNy: FødselshendelseServiceNy,
        private val featureToggleService: FeatureToggleService,
        private val stegService: StegService,
        private val vedtakService: VedtakService,
        private val infotrygdFeedService: InfotrygdFeedService,
        private val vedtaksperiodeService: VedtaksperiodeService
) :
        AsyncTaskStep {


    override fun doTask(task: Task) {
        val behandleFødselshendelseTaskDTO =
                objectMapper.readValue(task.payload, BehandleFødselshendelseTaskDTO::class.java)
        logger.info("Kjører BehandleFødselshendelseTask")

        val nyBehandling = behandleFødselshendelseTaskDTO.nyBehandling

        // Vi har overtatt ruting.
        // Pr. nå sender vi alle hendelser til infotrygd.
        // Koden under fjernes når vi går live.
        // fødselshendelseService.sendTilInfotrygdFeed(nyBehandling.barnasIdenter)

        // Dette er flyten, slik den skal se ut når vi går "live".
        //


        if (featureToggleService.isEnabled(FeatureToggleConfig.AUTOMATISK_FØDSELSHENDELSE)) {
            when (fødselshendelseServiceNy.hentFagsystemForFødselshendelse(nyBehandling)) {
                FagsystemRegelVurdering.SEND_TIL_BA -> behandleHendelseIBaSak(nyBehandling = nyBehandling)
                FagsystemRegelVurdering.SEND_TIL_INFOTRYGD -> infotrygdFeedService.sendTilInfotrygdFeed(
                        barnsIdenter = nyBehandling.barnasIdenter)
            }
        } else {
            fødselshendelseServiceGammel.fødselshendelseSkalBehandlesHosInfotrygd(
                    nyBehandling.morsIdent,
                    nyBehandling.barnasIdenter
            )
            fødselshendelseServiceGammel.sendTilInfotrygdFeed(nyBehandling.barnasIdenter)
        }
        // Når vi går live skal ba-sak behandle saker som ikke er løpende i infotrygd.
        // Etterhvert som vi kan behandle flere typer saker, utvider vi fødselshendelseSkalBehandlesHosInfotrygd.
    }

    private fun behandleHendelseIBaSak(nyBehandling: NyBehandlingHendelse) {
        val morHarÅpenBehandling = fødselshendelseServiceNy.harMorÅpenBehandlingIBASAK(nyBehandling = nyBehandling)
        val behandling = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
        val filtreringsResultat = fødselshendelseServiceNy.kjørFiltreringsregler(behandling, nyBehandling)

        when {
            morHarÅpenBehandling -> fødselshendelseServiceNy.opprettOppgaveForManuellBehandling(
                    behandlingId = behandling.id,
                    beskrivelse = "Fødselshendelse: Bruker har åpen behandling",
            )
            filtreringsResultat != FiltreringsreglerResultat.GODKJENT -> fødselshendelseServiceNy.opprettOppgaveForManuellBehandling(
                    behandlingId = behandling.id,
                    beskrivelse = filtreringsResultat.beskrivelse
            )
        }
        val behandlingEtterVilkårsVurdering = stegService.håndterVilkårsvurdering(behandling)
        val vedtak = vedtakService.hentAktivForBehandling(behandling.id) ?: throw Feil(
                "Fant ikke vedtak for behandling ${behandling.id} før lagring av vedtaksperiode."
        )
        
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

data class KontrollertRollbackException(val fødselshendelsePreLansering: FødselshendelsePreLansering?) :
        RuntimeException()