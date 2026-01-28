package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ba.sak.integrasjoner.journalføring.UtgåendeJournalføringService
import no.nav.familie.ba.sak.kjerne.brev.DokumentGenereringService
import no.nav.familie.ba.sak.kjerne.brev.DokumentService.Companion.genererEksternReferanseIdForJournalpost
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerInfo
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.task.JournalførManueltBrevTask.Companion.TASK_STEP_TYPE
import no.nav.familie.ba.sak.task.dto.JournalførManueltBrevDTO
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Førsteside
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
    val dokumentGenereringService: DokumentGenereringService,
    val taskRepository: TaskRepositoryWrapper,
    val fagsakService: FagsakService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val journalførManueltBrevDTO = jsonMapper.readValue(task.payload, JournalførManueltBrevDTO::class.java)
        logger.info("Journalfører manuelt brev for fagsak=${journalførManueltBrevDTO.fagsakId} og behandling=${journalførManueltBrevDTO.behandlingId}")

        val fagsak = fagsakService.hentPåFagsakId(journalførManueltBrevDTO.fagsakId)
        val saksbehandlerSignaturTilBrev = journalførManueltBrevDTO.saksbehandlerSignaturTilBrev

        val generertBrev =
            dokumentGenereringService.genererManueltBrev(
                manueltBrevRequest = journalførManueltBrevDTO.manuellBrevRequest,
                fagsak = fagsak,
                saksbehandlerSignaturTilBrev = saksbehandlerSignaturTilBrev,
            )

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
                .journalførDokument(
                    fnr = fagsak.aktør.aktivFødselsnummer(),
                    fagsakId = fagsak.id.toString(),
                    journalførendeEnhet = journalførManueltBrevDTO.manuellBrevRequest.enhet?.enhetId ?: DEFAULT_JOURNALFØRENDE_ENHET,
                    brev =
                        listOf(
                            Dokument(
                                dokument = generertBrev,
                                filtype = Filtype.PDFA,
                                dokumenttype = journalførManueltBrevDTO.manuellBrevRequest.brevmal.tilFamilieKontrakterDokumentType(),
                            ),
                        ),
                    førsteside = førsteside,
                    eksternReferanseId = journalførManueltBrevDTO.eksternReferanseId,
                    avsenderMottaker = journalførManueltBrevDTO.mottaker.avsenderMottaker,
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
                        manuellAdresseInfo = journalførManueltBrevDTO.mottaker.manuellAdresseInfo,
                    ),
                properties =
                    Properties().apply
                        {
                            this["fagsakIdent"] = fagsak.aktør.aktivFødselsnummer()
                            this["mottakerType"] = task.metadata["mottakerType"]
                            this["journalpostId"] = journalpostId
                            this["behandlingId"] = journalførManueltBrevDTO.behandlingId.toString()
                            this["fagsakId"] = fagsak.id.toString()
                        },
            ).also { taskRepository.save(it) }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(JournalførManueltBrevTask::class.java)
        const val TASK_STEP_TYPE = "journalførManueltBrev"

        fun opprettTask(
            behandlingId: Long?,
            fagsakId: Long,
            manuellBrevRequest: ManueltBrevRequest,
            mottakerInfo: MottakerInfo,
            saksbehandlerSignaturTilBrev: String,
        ): Task =
            Task(
                TASK_STEP_TYPE,
                jsonMapper.writeValueAsString(
                    JournalførManueltBrevDTO(
                        fagsakId = fagsakId,
                        behandlingId = behandlingId,
                        manuellBrevRequest = manuellBrevRequest,
                        mottaker = JournalførManueltBrevDTO.Mottaker.opprettFra(mottakerInfo),
                        eksternReferanseId = genererEksternReferanseIdForJournalpost(fagsakId, behandlingId, mottakerInfo),
                        saksbehandlerSignaturTilBrev = saksbehandlerSignaturTilBrev,
                    ),
                ),
                properties =
                    Properties().apply
                        {
                            this["behandlingId"] = behandlingId.toString()
                            this["fagsakId"] = fagsakId.toString()
                            this["brevmal"] = manuellBrevRequest.brevmal.name
                            this["mottakerType"] = mottakerInfo.javaClass.simpleName
                        },
            )
    }
}
