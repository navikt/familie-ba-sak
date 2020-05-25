package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevTask
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevDTO
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
        private val taskRepository: TaskRepository) : BehandlingSteg<JournalførVedtaksbrevDTO> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: JournalførVedtaksbrevDTO,
                                      stegService: StegService?): StegType {
        val vedtak = vedtakService.hent(vedtakId = data.vedtakId)

        val fnr = vedtak.behandling.fagsak.hentAktivIdent().ident
        val fagsakId = "${vedtak.behandling.fagsak.id}"

        val journalpostId = integrasjonClient.journalFørVedtaksbrev(fnr, fagsakId, vedtak)

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