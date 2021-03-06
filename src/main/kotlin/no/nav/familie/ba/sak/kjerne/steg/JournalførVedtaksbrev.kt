package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevDTO
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevTask
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

data class JournalførVedtaksbrevDTO(
        val vedtakId: Long,
        val task: Task
)

@Service
class JournalførVedtaksbrev(
        private val vedtakService: VedtakService,
        private val integrasjonClient: IntegrasjonClient,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val taskRepository: TaskRepository) : BehandlingSteg<JournalførVedtaksbrevDTO> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: JournalførVedtaksbrevDTO): StegType {
        val vedtak = vedtakService.hent(vedtakId = data.vedtakId)

        val fnr = vedtak.behandling.fagsak.hentAktivIdent().ident
        val fagsakId = "${vedtak.behandling.fagsak.id}"

        val behanlendeEnhet =
                arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id).behandlendeEnhetId

        val journalpostId = integrasjonClient.journalførVedtaksbrev(fnr = fnr,
                                                                    fagsakId = fagsakId,
                                                                    vedtak = vedtak,
                                                                    journalførendeEnhet = behanlendeEnhet)

        val nyTask = Task.nyTask(
                type = DistribuerVedtaksbrevTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                        DistribuerVedtaksbrevDTO(
                                personIdent = vedtak.behandling.fagsak.hentAktivIdent().ident,
                                behandlingId = vedtak.behandling.id,
                                journalpostId = journalpostId
                        )),
                properties = data.task.metadata)
        taskRepository.save(nyTask)

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.JOURNALFØR_VEDTAKSBREV
    }
}