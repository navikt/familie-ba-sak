package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.dto.AutobrevOpphørOvergangsstonadDTO
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OpphørAvFullOvergangsstonadService(
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val behandlingService: BehandlingService,
    private val fagsakRepository: FagsakRepository,
    private val vedtakService: VedtakService,
    private val taskRepository: TaskRepositoryWrapper,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val autovedtakService: AutovedtakService
) {
    @Transactional
    fun opprettOmregningsoppgaveForAvsluttetOvergangsstonad(dto: AutobrevOpphørOvergangsstonadDTO) {

        val fagsakId = dto.fagsakId
        val fagsak: Fagsak = fagsakRepository.finnFagsak(fagsakId)
            ?: error("Fant ikke aktiv fagsak med id $fagsakId. Kunne ikke kjøre revurdering pga avsluttet overgangsstønad.")

        // println("Ikke kjør automatisk revurdering hvis yngste barn ble 3 år forrige måned!")
        // if (yngsteBarnBle3ÅrForrigeMåned()) {
        //     logger.info("Fagsak ${fagsakId.fagsakId} har opphør i overgangsstønad, men yngste barn ble tre år forrige måned. Denne casen håndteres av AutobrevService for å få riktig begrunnelse")
        //     return
        // }

        println("TODO: Kjør automatisk behandling")
        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                fagsak = fagsak,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG
            )

        vedtaksperiodeService.oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(
            vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingEtterBehandlingsresultat.id),
            vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD
        )

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat
            )

        val task = Task(
            JournalførVedtaksbrevTask.TASK_STEP_TYPE,
            "${opprettetVedtak.id}"
        )
        taskRepository.save(task)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OpphørAvFullOvergangsstonadService::class.java)
    }
}
