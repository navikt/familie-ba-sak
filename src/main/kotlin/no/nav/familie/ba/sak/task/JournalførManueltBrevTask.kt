package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ba.sak.integrasjoner.journalføring.UtgåendeJournalføringService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.brev.DokumentGenereringService
import no.nav.familie.ba.sak.kjerne.brev.DokumentService.Companion.genererEksternReferanseIdForJournalpost
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerInfo
import no.nav.familie.ba.sak.kjerne.brev.mottaker.tilAvsenderMottaker
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.task.LogJournalpostIdForFagsakTask.Companion.TASK_STEP_TYPE
import no.nav.familie.ba.sak.task.dto.JournalførManueltBrevDTO
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Førsteside
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = TASK_STEP_TYPE,
    beskrivelse = "Journalfører manuelt brev",
    maxAntallFeil = 1,
)
class JournalførManueltBrevTask(
    val utgåendeJournalføringService: UtgåendeJournalføringService,
    val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    val dokumentGenereringService: DokumentGenereringService,
    val taskRepository: TaskRepositoryWrapper,
    val fagsakService: FagsakService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val journalførManueltBrevDTO = objectMapper.readValue(task.payload, JournalførManueltBrevDTO::class.java)
        val fagsak = fagsakService.hentPåFagsakId(journalførManueltBrevDTO.fagsakId)

        val førsteside =
            if (journalførManueltBrevDTO.manuellBrevRequest.brevmal.skalGenerereForside()) {
                Førsteside(
                    språkkode = journalførManueltBrevDTO.manuellBrevRequest.mottakerMålform.tilSpråkkode(),
                    navSkjemaId = "NAV 33.00-07",
                    overskriftstittel = "Ettersendelse til søknad om barnetrygd ordinær NAV 33-00.07",
                )
            } else {
                null
            }

        val journalpostId =
            utgåendeJournalføringService
                .journalførManueltBrev(
                    fnr = fagsak.aktør.aktivFødselsnummer(),
                    fagsakId = fagsak.id.toString(),
                    journalførendeEnhet = journalførManueltBrevDTO.manuellBrevRequest.enhet?.enhetId ?: DEFAULT_JOURNALFØRENDE_ENHET,
                    brev = dokumentGenereringService.genererManueltBrev(journalførManueltBrevDTO.manuellBrevRequest, fagsak),
                    dokumenttype = journalførManueltBrevDTO.manuellBrevRequest.brevmal.tilFamilieKontrakterDokumentType(),
                    førsteside = førsteside,
                    eksternReferanseId = journalførManueltBrevDTO.eksternReferanseId,
                    avsenderMottaker = journalførManueltBrevDTO.mottakerInfo.tilAvsenderMottaker(),
                )

        DistribuerDokumentTask
            .opprettDistribuerDokumentTask(
                distribuerDokumentDTO =
                    DistribuerDokumentDTO(
                        fagsakId = fagsak.id,
                        behandlingId = journalførManueltBrevDTO.behandlingId,
                        journalpostId = journalpostId,
                        brevmal = journalførManueltBrevDTO.manuellBrevRequest.brevmal,
                        erManueltSendt = true,
                        manuellAdresseInfo = journalførManueltBrevDTO.mottakerInfo.manuellAdresseInfo,
                    ),
                properties =
                    Properties().apply
                        {
                            this["fagsakIdent"] = fagsak.aktør.aktivFødselsnummer()
                            this["mottakerType"] = journalførManueltBrevDTO.mottakerInfo::class.simpleName
                            this["journalpostId"] = journalpostId
                            this["behandlingId"] = journalførManueltBrevDTO.behandlingId.toString()
                            this["fagsakId"] = fagsak.id.toString()
                        },
            ).also { taskRepository.save(it) }
    }

    companion object {
        const val TASK_STEP_TYPE = "journalførManueltBrev"

        fun opprettTask(
            behandlingId: Long?,
            fagsakId: Long,
            manuellBrevRequest: ManueltBrevRequest,
            mottakerInfo: MottakerInfo,
        ): Task =
            Task(
                TASK_STEP_TYPE,
                objectMapper.writeValueAsString(
                    JournalførManueltBrevDTO(
                        fagsakId = fagsakId,
                        behandlingId = behandlingId,
                        manuellBrevRequest = manuellBrevRequest,
                        mottakerInfo = mottakerInfo,
                        eksternReferanseId = genererEksternReferanseIdForJournalpost(fagsakId, behandlingId, mottakerInfo),
                    ),
                ),
                properties =
                    Properties().apply
                        {
                            this["behandlingId"] = behandlingId.toString()
                            this["fagsakId"] = fagsakId.toString()
                            this["brevmal"] = manuellBrevRequest.brevmal.name
                            this["mottakerType"] = mottakerInfo::class.simpleName
                        },
            )
    }
}
