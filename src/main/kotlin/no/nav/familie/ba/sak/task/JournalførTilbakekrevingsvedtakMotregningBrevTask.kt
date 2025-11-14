package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.journalføring.UtgåendeJournalføringService
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.DokumentService.Companion.genererEksternReferanseIdForJournalpost
import no.nav.familie.ba.sak.kjerne.brev.domene.ManuellBrevmottaker
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.Bruker
import no.nav.familie.ba.sak.kjerne.brev.mottaker.Institusjon
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerInfo
import no.nav.familie.ba.sak.kjerne.brev.mottaker.tilAvsenderMottaker
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.steg.JournalførVedtaksbrev.Companion.logger
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregning
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregningService
import no.nav.familie.ba.sak.task.JournalførTilbakekrevingsvedtakMotregningBrevTask.Companion.TASK_STEP_TYPE
import no.nav.familie.ba.sak.task.OpprettTaskService.Companion.overstyrTaskMedNyCallId
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.log.IdUtils
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Journalfør tilbakekrevingsvedtak ved motregning brev i Joark", maxAntallFeil = 3)
class JournalførTilbakekrevingsvedtakMotregningBrevTask(
    private val tilbakekrevingsvedtakMotregningService: TilbakekrevingsvedtakMotregningService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val utgåendeJournalføringService: UtgåendeJournalføringService,
    private val taskRepository: TaskRepositoryWrapper,
    private val organisasjonService: OrganisasjonService,
    private val brevmottakerService: BrevmottakerService,
    private val behandlingService: BehandlingHentOgPersisterService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val behandling = behandlingService.hent(task.payload.toLong())
        val fagsak = behandling.fagsak
        val tilbakekrevingsvedtakMotregning = tilbakekrevingsvedtakMotregningService.hentTilbakekrevingsvedtakMotregningEllerKastFunksjonellFeil(behandling.id)
        val behandlendeEnhet = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id).behandlendeEnhetId

        val mottakere = hentAlleMottakereAvBrevet(fagsak, behandling)

        val journalposterTilDistribusjon =
            mottakere.associate { mottakerInfo ->
                val journalpostId =
                    journalførTilbakekrevingsvedtakMotregningsbrev(
                        fnr = fagsak.aktør.aktivFødselsnummer(),
                        fagsakId = fagsak.id,
                        journalførendeEnhet = behandlendeEnhet,
                        mottakerInfo = mottakerInfo,
                        eksternReferanseId = genererEksternReferanseIdForJournalpost(fagsak.id, behandling.id, mottakerInfo),
                        tilbakekrevingsvedtakMotregning = tilbakekrevingsvedtakMotregning,
                    )
                journalpostId to mottakerInfo
            }

        journalposterTilDistribusjon.forEach {
            val distribueringTask =
                DistribuerDokumentTask.opprettDistribuerDokumentTask(
                    distribuerDokumentDTO =
                        lagDistribuerDokumentDto(
                            behandling = behandling,
                            journalPostId = it.key,
                            mottakerInfo = it.value,
                        ),
                    properties = task.metadata,
                )

            taskRepository.save(distribueringTask)
        }
    }

    private fun hentAlleMottakereAvBrevet(
        fagsak: Fagsak,
        behandling: Behandling,
    ) = if (fagsak.type == FagsakType.INSTITUSJON) {
        listOf(
            Institusjon(
                orgNummer = fagsak.institusjon!!.orgNummer,
                navn = organisasjonService.hentOrganisasjon(fagsak.institusjon!!.orgNummer).navn,
            ),
        )
    } else {
        brevmottakerService
            .hentBrevmottakere(behandling.id)
            .takeIf { it.isNotEmpty() }
            ?.map(::ManuellBrevmottaker)
            ?.let(brevmottakerService::lagMottakereFraBrevMottakere)
            ?: listOf(Bruker)
    }

    private fun journalførTilbakekrevingsvedtakMotregningsbrev(
        fnr: String,
        fagsakId: Long,
        tilbakekrevingsvedtakMotregning: TilbakekrevingsvedtakMotregning,
        journalførendeEnhet: String,
        mottakerInfo: MottakerInfo,
        eksternReferanseId: String,
    ): String {
        val behandling = tilbakekrevingsvedtakMotregning.behandling
        val brev =
            listOf(
                Dokument(
                    tilbakekrevingsvedtakMotregning.vedtakPdf!!,
                    filtype = Filtype.PDFA,
                    dokumenttype = Dokumenttype.BARNETRYGD_TILBAKEKREVINGSVEDTAK_MOTREGNING,
                ),
            )

        logger.info("Journalfører brev for tilbakekrevingsvedtak ved motregning for behandling ${behandling.id}")

        return utgåendeJournalføringService.journalførDokument(
            fnr = fnr,
            fagsakId = fagsakId.toString(),
            journalførendeEnhet = journalførendeEnhet,
            brev = brev,
            avsenderMottaker = mottakerInfo.tilAvsenderMottaker(),
            eksternReferanseId = eksternReferanseId,
        )
    }

    private fun lagDistribuerDokumentDto(
        behandling: Behandling,
        journalPostId: String,
        mottakerInfo: MottakerInfo,
    ) = DistribuerDokumentDTO(
        fagsakId = behandling.fagsak.id,
        behandlingId = behandling.id,
        journalpostId = journalPostId,
        brevmal = Brevmal.TILBAKEKREVINGSVEDTAK_MOTREGNING,
        erManueltSendt = false,
        manuellAdresseInfo = mottakerInfo.manuellAdresseInfo,
    )

    companion object {
        const val TASK_STEP_TYPE = "journalførTilbakekrevingsvedtakMotregningBrev"

        fun opprettTask(
            behandlingId: Long,
        ): Task =
            overstyrTaskMedNyCallId(IdUtils.generateId()) {
                Task(
                    TASK_STEP_TYPE,
                    "$behandlingId",
                )
            }
    }
}
