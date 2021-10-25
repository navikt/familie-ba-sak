package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.dokument.hentBrevtype
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.DistribuerDokumentTask
import no.nav.familie.prosessering.domene.Task
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
    private val taskRepository: TaskRepositoryWrapper,
    private val envService: EnvService
) : BehandlingSteg<JournalførVedtaksbrevDTO> {

    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: JournalførVedtaksbrevDTO
    ): StegType {
        val vedtak = vedtakService.hent(vedtakId = data.vedtakId)

        val fnr = vedtak.behandling.fagsak.hentAktivIdent().ident
        val fagsakId = "${vedtak.behandling.fagsak.id}"

        val behanlendeEnhet =
            arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id).behandlendeEnhetId

        val journalpostId = integrasjonClient.journalførVedtaksbrev(
            fnr = fnr,
            fagsakId = fagsakId,
            vedtak = vedtak,
            journalførendeEnhet = behanlendeEnhet
        )

        val nyTask = DistribuerDokumentTask.opprettDistribuerDokumentTask(
            distribuerDokumentDTO = DistribuerDokumentDTO(
                personIdent = vedtak.behandling.fagsak.hentAktivIdent().ident,
                behandlingId = vedtak.behandling.id,
                journalpostId = journalpostId,
                brevmal = hentBrevtype(behandling),
                erManueltSendt = false
            ),
            properties = data.task.metadata,
            envService
        )
        taskRepository.save(nyTask)

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.JOURNALFØR_VEDTAKSBREV
    }
}
