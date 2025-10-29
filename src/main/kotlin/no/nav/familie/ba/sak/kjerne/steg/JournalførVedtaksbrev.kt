package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient.Companion.VEDTAK_VEDLEGG_FILNAVN
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient.Companion.VEDTAK_VEDLEGG_TITTEL
import no.nav.familie.ba.sak.integrasjoner.journalføring.UtgåendeJournalføringService
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.brev.DokumentService.Companion.genererEksternReferanseIdForJournalpost
import no.nav.familie.ba.sak.kjerne.brev.domene.ManuellBrevmottaker
import no.nav.familie.ba.sak.kjerne.brev.hentOverstyrtDokumenttittel
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.Bruker
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrukerMedUtenlandskAdresse
import no.nav.familie.ba.sak.kjerne.brev.mottaker.Dødsbo
import no.nav.familie.ba.sak.kjerne.brev.mottaker.FullmektigEllerVerge
import no.nav.familie.ba.sak.kjerne.brev.mottaker.Institusjon
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerInfo
import no.nav.familie.ba.sak.kjerne.brev.mottaker.tilAvsenderMottaker
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.steg.domene.JournalførVedtaksbrevDTO
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.DistribuerDokumentTask
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevTilFullmektigEllerVergeTask
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JournalførVedtaksbrev(
    private val vedtakService: VedtakService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val utgåendeJournalføringService: UtgåendeJournalføringService,
    private val taskRepository: TaskRepositoryWrapper,
    private val fagsakRepository: FagsakRepository,
    private val organisasjonService: OrganisasjonService,
    private val brevmottakerService: BrevmottakerService,
    private val brevmalService: BrevmalService,
) : BehandlingSteg<JournalførVedtaksbrevDTO> {
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: JournalførVedtaksbrevDTO,
    ): StegType {
        val vedtak = vedtakService.hent(vedtakId = data.vedtakId)
        val fagsakId = "${vedtak.behandling.fagsak.id}"
        val fagsak = fagsakRepository.finnFagsak(vedtak.behandling.fagsak.id)

        if (fagsak == null || fagsak.type == FagsakType.INSTITUSJON && fagsak.institusjon == null) {
            throw Feil("Journalfør vedtaksbrev feil: fagsak er null eller institusjon fagsak har ikke institusjonsinformasjon")
        }

        val behandlendeEnhet =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id).behandlendeEnhetId

        val mottakere = mutableListOf<MottakerInfo>()

        if (fagsak.type == FagsakType.INSTITUSJON) {
            val orgNummer = fagsak.institusjon!!.orgNummer
            mottakere +=
                Institusjon(
                    orgNummer = orgNummer,
                    navn = organisasjonService.hentOrganisasjon(orgNummer).navn,
                )
        } else {
            val brevMottakere = brevmottakerService.hentBrevmottakere(behandling.id)
            if (brevMottakere.isNotEmpty()) {
                mottakere +=
                    brevmottakerService.lagMottakereFraBrevMottakere(
                        brevMottakere.map { ManuellBrevmottaker(it) },
                    )
            } else {
                mottakere += Bruker
            }
        }

        val journalposterTilDistribusjon = mutableMapOf<String, MottakerInfo>()

        mottakere.forEach { mottakerInfo ->
            journalførVedtaksbrev(
                fnr = fagsak.aktør.aktivFødselsnummer(),
                fagsakId = fagsakId,
                vedtak = vedtak,
                journalførendeEnhet = behandlendeEnhet,
                mottakerInfo = mottakerInfo,
                eksternReferanseId = genererEksternReferanseIdForJournalpost(fagsak.id, behandling.id, mottakerInfo),
            ).also { journalposterTilDistribusjon[it] = mottakerInfo }
        }

        lagTaskForÅDistribuereVedtaksbrev(journalposterTilDistribusjon, data, behandling)

        return hentNesteStegForNormalFlyt(behandling)
    }

    private fun lagTaskForÅDistribuereVedtaksbrev(
        journalposterTilDistribusjon: Map<String, MottakerInfo>,
        data: JournalførVedtaksbrevDTO,
        behandling: Behandling,
    ) {
        journalposterTilDistribusjon.forEach { (journalpostId, mottaker) ->
            when (mottaker) {
                is FullmektigEllerVerge,
                -> { // Denne tasken sender kun vedtaksbrev
                    val distribuerTilVergeTask =
                        DistribuerVedtaksbrevTilFullmektigEllerVergeTask
                            .opprettDistribuerVedtaksbrevTilFullmektigEllerVergeTask(
                                distribuerDokumentDTO =
                                    lagDistribuerDokumentDto(
                                        behandling = behandling,
                                        journalPostId = journalpostId,
                                        mottakerInfo = mottaker,
                                    ),
                                properties = data.task.metadata,
                            )
                    taskRepository.save(distribuerTilVergeTask)
                }

                is Bruker,
                is BrukerMedUtenlandskAdresse,
                is Dødsbo,
                is Institusjon,
                -> {
                    // Denne tasken sender vedtaksbrev og håndterer steg videre
                    val distribuerTilSøkerTask =
                        DistribuerDokumentTask.opprettDistribuerDokumentTask(
                            distribuerDokumentDTO =
                                lagDistribuerDokumentDto(
                                    behandling = behandling,
                                    journalPostId = journalpostId,
                                    mottakerInfo = mottaker,
                                ),
                            properties = data.task.metadata,
                        )
                    taskRepository.save(distribuerTilSøkerTask)
                }
            }
        }
    }

    fun journalførVedtaksbrev(
        fnr: String,
        fagsakId: String,
        vedtak: Vedtak,
        journalførendeEnhet: String,
        mottakerInfo: MottakerInfo,
        eksternReferanseId: String,
    ): String {
        val vedleggPdf =
            hentVedlegg(VEDTAK_VEDLEGG_FILNAVN) ?: throw Feil("Klarte ikke hente vedlegg $VEDTAK_VEDLEGG_FILNAVN")

        val brev =
            listOf(
                Dokument(
                    vedtak.stønadBrevPdF!!,
                    filtype = Filtype.PDFA,
                    dokumenttype = vedtak.behandling.resultat.tilDokumenttype(),
                    tittel = hentOverstyrtDokumenttittel(vedtak.behandling),
                ),
            )
        logger.info(
            "Journalfører vedtaksbrev for behandling ${vedtak.behandling.id} med tittel ${
                hentOverstyrtDokumenttittel(vedtak.behandling)
            }",
        )
        val vedlegg =
            listOf(
                Dokument(
                    vedleggPdf,
                    filtype = Filtype.PDFA,
                    dokumenttype = Dokumenttype.BARNETRYGD_VEDLEGG,
                    tittel = VEDTAK_VEDLEGG_TITTEL,
                ),
            )
        return utgåendeJournalføringService.journalførDokument(
            fnr = fnr,
            fagsakId = fagsakId,
            journalførendeEnhet = journalførendeEnhet,
            brev = brev,
            vedlegg = vedlegg,
            behandlingId = vedtak.behandling.id,
            avsenderMottaker = mottakerInfo.tilAvsenderMottaker(),
            eksternReferanseId = eksternReferanseId,
        )
    }

    private fun Behandlingsresultat.tilDokumenttype() =
        when (this) {
            Behandlingsresultat.AVSLÅTT -> Dokumenttype.BARNETRYGD_VEDTAK_AVSLAG
            Behandlingsresultat.OPPHØRT -> Dokumenttype.BARNETRYGD_OPPHØR
            else -> Dokumenttype.BARNETRYGD_VEDTAK_INNVILGELSE
        }

    private fun lagDistribuerDokumentDto(
        behandling: Behandling,
        journalPostId: String,
        mottakerInfo: MottakerInfo,
    ) = DistribuerDokumentDTO(
        fagsakId = behandling.fagsak.id,
        behandlingId = behandling.id,
        journalpostId = journalPostId,
        brevmal = brevmalService.hentBrevmal(behandling),
        erManueltSendt = false,
        manuellAdresseInfo = mottakerInfo.manuellAdresseInfo,
    )

    override fun stegType(): StegType = StegType.JOURNALFØR_VEDTAKSBREV

    companion object {
        val logger = LoggerFactory.getLogger(JournalførVedtaksbrev::class.java)

        fun hentVedlegg(vedleggsnavn: String): ByteArray? {
            val inputStream = this::class.java.classLoader.getResourceAsStream("dokumenter/$vedleggsnavn")
            return inputStream?.readAllBytes()
        }
    }
}
