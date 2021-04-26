package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
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
        private val featureToggleService: FeatureToggleService,
        private val vedtakRepository: VedtakRepository,
) : BehandlingSteg<IverksettMotFamilieTilbakeData> {

    override fun utførStegOgAngiNeste(behandling: Behandling, data: IverksettMotFamilieTilbakeData): StegType {
        val vedtak = vedtakService.hentAktivForBehandling(behandling.id) ?: throw Feil(
                "Fant ikke vedtak for behandling ${behandling.id} ved iverksetting mot familie-tilbake."
        )

        val enableTilbakeKreving = featureToggleService.isEnabled(FeatureToggleConfig.TILBAKEKREVING)

        if (vedtak.tilbakekreving != null
            && vedtak.tilbakekreving!!.valg != Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING
            && !tilbakekrevingService.søkerHarÅpenTilbakekreving(vedtak.id)
            && enableTilbakeKreving) {

            val tilbakekrevingId = tilbakekrevingService.opprettTilbakekreving(vedtak = vedtak)
            vedtak.tilbakekreving!!.tilbakekrevingsbehandlingId = tilbakekrevingId

            logger.info("Opprettet tilbakekreving for vedtak ${vedtak.id} og tilbakekrevingsid ${tilbakekrevingId}")
            vedtakRepository.save(vedtak)
        }

        opprettTaskJournalførVedtaksbrev(vedtakId = vedtak.id, data.metadata)

        return hentNesteStegForNormalFlyt(behandling)
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