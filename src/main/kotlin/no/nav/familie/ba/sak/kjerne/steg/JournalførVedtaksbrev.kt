package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient.Companion.VEDTAK_VEDLEGG_FILNAVN
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient.Companion.VEDTAK_VEDLEGG_TITTEL
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.tilDokumenttype
import no.nav.familie.ba.sak.integrasjoner.journalføring.UtgåendeJournalføringService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.hentBrevmal
import no.nav.familie.ba.sak.kjerne.brev.hentOverstyrtDokumenttittel
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.DistribuerDokumentTask
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevTilVergeDTO
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevTilVergeTask
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class JournalførVedtaksbrevDTO(
    val vedtakId: Long,
    val task: Task
)

@Service
class JournalførVedtaksbrev(
    private val vedtakService: VedtakService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val utgåendeJournalføringService: UtgåendeJournalføringService,
    private val taskRepository: TaskRepositoryWrapper,
    private val fagsakRepository: FagsakRepository
) : BehandlingSteg<JournalførVedtaksbrevDTO> {

    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: JournalførVedtaksbrevDTO
    ): StegType {
        val vedtak = vedtakService.hent(vedtakId = data.vedtakId)
        val fagsakId = "${vedtak.behandling.fagsak.id}"
        val fagsak = fagsakRepository.finnFagsak(vedtak.behandling.fagsak.id)
        if (fagsak == null || fagsak.type == FagsakType.INSTITUSJON && fagsak.institusjon == null) {
            error("Journalfør vedtaksbrev feil: fagsak er null eller institusjon fagsak har ikke institusjonsinformasjon")
        }

        val behanlendeEnhet =
            arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id).behandlendeEnhetId

        val mottaker = if (fagsak?.type == FagsakType.INSTITUSJON) {
            mutableListOf(MottakerInfo(fagsak.institusjon!!.orgNummer!!, BrukerIdType.ORGNR, false))
        } else {
            mutableListOf(MottakerInfo(vedtak.behandling.fagsak.aktør.aktivFødselsnummer(), BrukerIdType.FNR, false))
        }
        if (vedtak.behandling.verge != null && vedtak.behandling.verge?.ident != null) {
            mottaker.add(MottakerInfo(vedtak.behandling.verge!!.ident!!, BrukerIdType.FNR, true))
        }

        val journalposterTilDistribusjon = mutableMapOf<String, MottakerInfo>()
        mottaker.forEach { mottakerInfo ->
            journalførVedtaksbrev(
                brukersId = mottakerInfo.brukerId,
                fagsakId = fagsakId,
                vedtak = vedtak,
                journalførendeEnhet = behanlendeEnhet,
                brukersType = mottakerInfo.brukerIdType,
                tilVerge = mottakerInfo.erVerge
            ).also { journalposterTilDistribusjon[it] = mottakerInfo }
        }
        Lagt inn
            journalposterTilDistribusjon.forEach {
                if (it.value.erVerge) {
                    val distribuerTilVergeTask = DistribuerVedtaksbrevTilVergeTask.opprettDistribuerVedtaksbrevTilVergeTask(
                        distribuerVedtaksbrevTilVergeDTO = DistribuerVedtaksbrevTilVergeDTO(
                            behandlingId = vedtak.behandling.id,
                            personIdent = it.value.brukerId,
                            journalpostId = it.key
                        ),
                        properties = data.task.metadata
                    )
                    taskRepository.save(distribuerTilVergeTask)
                } else {
                    val distribuerTilSøkerTask = DistribuerDokumentTask.opprettDistribuerDokumentTask(
                        distribuerDokumentDTO = DistribuerDokumentDTO(
                            personEllerInstitusjonIdent = it.value.brukerId,
                            behandlingId = vedtak.behandling.id,
                            journalpostId = it.key,
                            brevmal = hentBrevmal(behandling),
                            erManueltSendt = false
                        ),
                        properties = data.task.metadata
                    )
                    taskRepository.save(distribuerTilSøkerTask)
                }
            }

        return hentNesteStegForNormalFlyt(behandling)
    }

    fun journalførVedtaksbrev(
        brukersId: String,
        brukersType: BrukerIdType,
        fagsakId: String,
        vedtak: Vedtak,
        journalførendeEnhet: String,
        tilVerge: Boolean = false
    ): String {
        val vedleggPdf =
            hentVedlegg(VEDTAK_VEDLEGG_FILNAVN) ?: error("Klarte ikke hente vedlegg $VEDTAK_VEDLEGG_FILNAVN")

        val brev = listOf(
            Dokument(
                vedtak.stønadBrevPdF!!,
                filtype = Filtype.PDFA,
                dokumenttype = vedtak.behandling.resultat.tilDokumenttype(),
                tittel = hentOverstyrtDokumenttittel(vedtak.behandling)
            )
        )
        logger.info(
            "Journalfører vedtaksbrev for behandling ${vedtak.behandling.id} med tittel ${
            hentOverstyrtDokumenttittel(vedtak.behandling)
            }"
        )
        val vedlegg = listOf(
            Dokument(
                vedleggPdf,
                filtype = Filtype.PDFA,
                dokumenttype = Dokumenttype.BARNETRYGD_VEDLEGG,
                tittel = VEDTAK_VEDLEGG_TITTEL
            )
        )
        return utgåendeJournalføringService.journalførDokument(
            brukerId = brukersId,
            fagsakId = fagsakId,
            journalførendeEnhet = journalførendeEnhet,
            brev = brev,
            vedlegg = vedlegg,
            behandlingId = vedtak.behandling.id,
            brukersType = brukersType,
            tilVerge = tilVerge
        )
    }

    override fun stegType(): StegType {
        return StegType.JOURNALFØR_VEDTAKSBREV
    }

    companion object {

        val logger = LoggerFactory.getLogger(JournalførVedtaksbrev::class.java)

        fun hentVedlegg(vedleggsnavn: String): ByteArray? {
            val inputStream = this::class.java.classLoader.getResourceAsStream("dokumenter/$vedleggsnavn")
            return inputStream?.readAllBytes()
        }
    }
}

data class MottakerInfo(
    val brukerId: String,
    val brukerIdType: BrukerIdType,
    val erVerge: Boolean
)
