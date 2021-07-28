package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FagsystemRegelVurdering
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FødselshendelseServiceNy
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.FødselshendelseServiceGammel
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.domene.FødselshendelsePreLansering
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.erOppfylt
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*


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
        private val vedtaksperiodeService: VedtaksperiodeService,
        private val taskRepository: TaskRepository,
) : AsyncTaskStep {


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
        val morsÅpneBehandling = fødselshendelseServiceNy.hentÅpenBehandling(ident = nyBehandling.morsIdent)
        if (morsÅpneBehandling != null) {
            fødselshendelseServiceNy.opprettOppgaveForManuellBehandling(
                    morsÅpneBehandling.id,
                    "Fødselshendelse: Bruker har åpen behandling"
            )
            return
        }

        val behandling = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
        val evalueringer = fødselshendelseServiceNy.kjørFiltreringsregler(behandling, nyBehandling)

        if (!evalueringer.erOppfylt()) henleggBehandlingOgOpprettManuellOppgave(
                behandling = behandling,
                beskrivelse = evalueringer.first { it.resultat == Resultat.IKKE_OPPFYLT }.beskrivelse,
        )
        else vurderVilkår(behandling = behandling)
    }

    private fun vurderVilkår(behandling: Behandling) {
        val behandlingEtterVilkårsVurdering = stegService.håndterVilkårsvurdering(behandling = behandling)
        if (behandlingEtterVilkårsVurdering.resultat == BehandlingResultat.INNVILGET) {
            val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id)
            val tidligstePeriodeForVedtak =
                    vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak).sortedBy { it.fom }.first()
            vedtaksperiodeService.oppdaterVedtaksperioderForNyfødtBarn(tidligstePeriodeForVedtak,
                                                                       vedtak.behandling.fagsak.status)
            val vedtakEtterToTrinn =
                    vedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandling = behandlingEtterVilkårsVurdering)

            val task = IverksettMotOppdragTask.opprettTask(behandling, vedtakEtterToTrinn, SikkerhetContext.hentSaksbehandler())
            taskRepository.save(task)


            //TODO vet ikke hvilken fødselsdato som skal sendes med. Det kan være flere barn
        } else {
            henleggBehandlingOgOpprettManuellOppgave(behandling = behandlingEtterVilkårsVurdering)
        }
    }

    private fun henleggBehandlingOgOpprettManuellOppgave(
            behandling: Behandling,
            beskrivelse: String = "",
    ) {

        val begrunnelseForManuellOppgave = if (beskrivelse == "") {
            fødselshendelseServiceNy.hentBegrunnelseFraVilkårsvurdering(behandlingId = behandling.id)
        } else {
            beskrivelse
        }

        logger.info("Henlegger behandling ${behandling.id} automatisk på grunn av ugyldig resultat")
        secureLogger.info("Henlegger behandling ${behandling.id} automatisk på grunn av ugyldig resultat. Beskrivelse: $beskrivelse")

        stegService.håndterHenleggBehandling(behandling = behandling, henleggBehandlingInfo = RestHenleggBehandlingInfo(
                årsak = HenleggÅrsak.FØDSELSHENDELSE_UGYLDIG_UTFALL,
                begrunnelse = "Automatisk henlagt: $begrunnelseForManuellOppgave" // TODO: avklar denne meldingen med fag
        ))

        fødselshendelseServiceNy.opprettOppgaveForManuellBehandling(
                behandlingId = behandling.id,
                beskrivelse = begrunnelseForManuellOppgave
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