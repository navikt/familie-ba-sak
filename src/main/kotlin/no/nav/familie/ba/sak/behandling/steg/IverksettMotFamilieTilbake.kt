package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.tilbake.TilbakekrevingService
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties


data class IverksettMotFamilieTilbakeData(
        val metadata: Properties
)

@Service
class IverksettMotFamilieTilbake(
        private val vedtakService: VedtakService,
        private val tilbakekrevingService: TilbakekrevingService,
        private val taskRepository: TaskRepository,
        private val persongrunnlagService: PersongrunnlagService,
        private val featureToggleService: FeatureToggleService,
) : BehandlingSteg<IverksettMotFamilieTilbakeData> {

    override fun utførStegOgAngiNeste(behandling: Behandling, data: IverksettMotFamilieTilbakeData): StegType {
        val vedtak = vedtakService.hentAktivForBehandling(behandling.id) ?: throw Feil(
                "Fant ikke vedtak for behandling ${behandling.id} ved iverksetting mot familie-tilbake."
        )

        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id) ?: throw Feil(
                message = "Finner ikke personopplysningsgrunnlag på vedtak ${vedtak.id} " +
                          "ved iverksetting av tilbakekreving mot familie-tilbake",
        )

        // Hent tilbakekrevingsvalg + tilbakekreving-tekster og feilutbetaling

        val skipTilbakeKrevingSteg =
                featureToggleService.isEnabled(FeatureToggleConfig.SKIP_TILBAKE_KREVING_STEG, true)

        // dersom det er en feilutbetaling og tilbakekrevingsvalget ikke er avvent tilbakekreving
        if (tilbakekrevingService.søkerHarÅpenTilbakekreving(vedtak.id) && !skipTilbakeKrevingSteg) {
            val tilbakekrevingId = tilbakekrevingService.opprettTilbakekreving(vedtak = vedtak)
            //TODO: Persister tilbakekrevingId når db-modellen for tilbakebetaling er klar.
        }

        if (behandling.sendVedtaksbrev()) {
            opprettTaskJournalførVedtaksbrev(vedtakId = vedtak.id,
                                             data.metadata)
        } else {

            opprettFerdigstillBehandling(personIdent = personopplysningGrunnlag.søker.personIdent, vedtak.behandling.id)
        }

        return hentNesteStegForNormalFlyt(behandling)
    }

    private fun opprettFerdigstillBehandling(personIdent: PersonIdent, behandlingId: Long) {
        val ferdigstillBehandling = FerdigstillBehandlingTask.opprettTask(personIdent = personIdent.ident,
                                                                          behandlingsId = behandlingId)
        taskRepository.save(ferdigstillBehandling)
    }

    private fun opprettTaskJournalførVedtaksbrev(vedtakId: Long, metadata: Properties) {
        val task = Task.nyTask(JournalførVedtaksbrevTask.TASK_STEP_TYPE,
                               "$vedtakId",
                               metadata)
        taskRepository.save(task)
    }

    override fun stegType(): StegType {
        return StegType.IVERKSETT_MOT_FAMILIE_TILBAKE
    }

    companion object {

        private val logger = LoggerFactory.getLogger(StatusFraOppdrag::class.java)
    }
}