package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient.Companion.VEDTAK_VEDLEGG_FILNAVN
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient.Companion.VEDTAK_VEDLEGG_TITTEL
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.tilDokumenttype
import no.nav.familie.ba.sak.integrasjoner.journalføring.UtgåendeJournalføringService
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.hentBrevmal
import no.nav.familie.ba.sak.kjerne.brev.hentOverstyrtDokumenttittel
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.domene.JournalførVedtaksbrevDTO
import no.nav.familie.ba.sak.kjerne.steg.domene.MottakerInfo
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.DistribuerDokumentTask
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevTilInstitusjonVergeEllerManuellBrevMottakerTask
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
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
    private val personidentService: PersonidentService,
    private val personopplysningerService: PersonopplysningerService,
    private val brevmottakerService: BrevmottakerService
) : BehandlingSteg<JournalførVedtaksbrevDTO> {

    override fun utførStegOgAngiNeste(behandling: Behandling, data: JournalførVedtaksbrevDTO): StegType {
        val vedtak = vedtakService.hent(vedtakId = data.vedtakId)
        val fagsakId = "${vedtak.behandling.fagsak.id}"
        val fagsak = fagsakRepository.finnFagsak(vedtak.behandling.fagsak.id)
        val søkersident = vedtak.behandling.fagsak.aktør.aktivFødselsnummer()

        if (fagsak == null || fagsak.type == FagsakType.INSTITUSJON && fagsak.institusjon == null) {
            error("Journalfør vedtaksbrev feil: fagsak er null eller institusjon fagsak har ikke institusjonsinformasjon")
        }

        val behanlendeEnhet =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id).behandlendeEnhetId

        val mottakere = mutableListOf<MottakerInfo>()

        if (fagsak.type == FagsakType.INSTITUSJON) {
            mottakere += MottakerInfo(fagsak.institusjon!!.orgNummer, BrukerIdType.ORGNR, false)
        } else {
            val brevMottakere = brevmottakerService.hentBrevmottagere(behandling.id)
            if (brevMottakere.isNotEmpty()) {
                mottakere += brevmottakerService.lagMottakereFraBrevMottakere(
                    brevMottakere,
                    søkersident,
                    hentMottakerNavn(søkersident)
                )
            } else {
                mottakere += MottakerInfo(søkersident, BrukerIdType.FNR, false)
            }
        }
        if (vedtak.behandling.verge?.ident != null) { // brukes kun i institusjon
            mottakere += MottakerInfo(vedtak.behandling.verge.ident, BrukerIdType.FNR, true)
        }

        val journalposterTilDistribusjon = mutableMapOf<String, MottakerInfo>()
        mottakere.forEach { mottakerInfo ->
            journalførVedtaksbrev(
                fnr = fagsak.aktør.aktivFødselsnummer(),
                fagsakId = fagsakId,
                vedtak = vedtak,
                journalførendeEnhet = behanlendeEnhet,
                mottakerInfo = mottakerInfo,
                tilManuellMottakerEllerVerge = if (vedtak.behandling.verge?.ident != null) {
                    mottakerInfo.erInstitusjonVerge
                } else {
                    (mottakerInfo.navn != null && mottakerInfo.navn != hentMottakerNavn(søkersident))
                } // mottakersnavn fyller ut kun når manuell mottaker finnes
            ).also { journalposterTilDistribusjon[it] = mottakerInfo }
        }

        lagTaskForÅDistribuereVedtaksbrev(journalposterTilDistribusjon, data, behandling)

        return hentNesteStegForNormalFlyt(behandling)
    }

    private fun lagTaskForÅDistribuereVedtaksbrev(
        journalposterTilDistribusjon: MutableMap<String, MottakerInfo>,
        data: JournalførVedtaksbrevDTO,
        behandling: Behandling
    ) {
        journalposterTilDistribusjon.forEach {
            val finnesBrevMottaker = it.value.navn != null &&
                it.value.navn != hentMottakerNavn(behandling.fagsak.aktør.aktivFødselsnummer())
            if (it.value.erInstitusjonVerge || finnesBrevMottaker) { // Denne tasken sender kun vedtaksbrev
                val distribuerTilVergeTask =
                    DistribuerVedtaksbrevTilInstitusjonVergeEllerManuellBrevMottakerTask
                        .opprettDistribuerVedtaksbrevTilInstitusjonVergeEllerManuellBrevMottakerTask(
                            distribuerDokumentDTO = lagDistribuerDokumentDto(behandling, it.key, it.value),
                            properties = data.task.metadata
                        )
                taskRepository.save(distribuerTilVergeTask)
            } else { // Denne tasken sender vedtaksbrev og håndterer steg videre
                val distribuerTilSøkerTask = DistribuerDokumentTask.opprettDistribuerDokumentTask(
                    distribuerDokumentDTO = lagDistribuerDokumentDto(behandling, it.key, it.value),
                    properties = data.task.metadata
                )
                taskRepository.save(distribuerTilSøkerTask)
            }
        }
    }

    fun journalførVedtaksbrev(
        fnr: String,
        fagsakId: String,
        vedtak: Vedtak,
        journalførendeEnhet: String,
        mottakerInfo: MottakerInfo,
        tilManuellMottakerEllerVerge: Boolean
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
            fnr = fnr,
            fagsakId = fagsakId,
            journalførendeEnhet = journalførendeEnhet,
            brev = brev,
            vedlegg = vedlegg,
            behandlingId = vedtak.behandling.id,
            avsenderMottaker = utledAvsenderMottaker(mottakerInfo),
            tilVerge = tilManuellMottakerEllerVerge
        )
    }

    private fun utledAvsenderMottaker(mottakerInfo: MottakerInfo): AvsenderMottaker? {
        return when {
            mottakerInfo.brukerIdType == BrukerIdType.ORGNR -> {
                AvsenderMottaker(
                    idType = mottakerInfo.brukerIdType,
                    id = mottakerInfo.brukerId,
                    navn = organisasjonService.hentOrganisasjon(mottakerInfo.brukerId).navn
                )
            }
            mottakerInfo.erInstitusjonVerge -> {
                AvsenderMottaker(
                    idType = mottakerInfo.brukerIdType,
                    id = mottakerInfo.brukerId,
                    navn = hentMottakerNavn(mottakerInfo.brukerId)
                )
            }
            mottakerInfo.brukerIdType == BrukerIdType.FNR && mottakerInfo.navn != null -> {
                AvsenderMottaker(
                    idType = mottakerInfo.brukerIdType,
                    id = mottakerInfo.brukerId,
                    navn = mottakerInfo.navn
                )
            }
            else -> {
                null
            }
        }
    }

    private fun hentMottakerNavn(personIdent: String): String {
        val aktør = personidentService.hentAktør(personIdent)
        return personopplysningerService.hentPersoninfoNavnOgAdresse(aktør).let {
            it.navn!!
        }
    }

    private fun lagDistribuerDokumentDto(behandling: Behandling, journalPostId: String, mottakerInfo: MottakerInfo) =
        DistribuerDokumentDTO(
            personEllerInstitusjonIdent = mottakerInfo.brukerId,
            behandlingId = behandling.id,
            journalpostId = journalPostId,
            brevmal = hentBrevmal(behandling),
            erManueltSendt = false,
            manuellAdresseInfo = mottakerInfo.manuellAdresseInfo
        )

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
