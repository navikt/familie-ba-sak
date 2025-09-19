package no.nav.familie.ba.sak.config

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.datagenerator.lagBarnetrygdSøknadV9
import no.nav.familie.ba.sak.datagenerator.lagTestJournalpost
import no.nav.familie.ba.sak.datagenerator.lagTestOppgaveDTO
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.RestNyAktivBrukerIModiaContext
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsforhold
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.LogiskVedleggRequest
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.LogiskVedleggResponse
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.OppdaterJournalpostRequest
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.OppdaterJournalpostResponse
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.ba.sak.kjerne.modiacontext.ModiaContext
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.mock.IntegrasjonClientMock
import no.nav.familie.ba.sak.mock.IntegrasjonClientMock.Companion.FOM_1900
import no.nav.familie.ba.sak.mock.IntegrasjonClientMock.Companion.FOM_1990
import no.nav.familie.ba.sak.mock.IntegrasjonClientMock.Companion.FOM_2004
import no.nav.familie.ba.sak.mock.IntegrasjonClientMock.Companion.TOM_2010
import no.nav.familie.ba.sak.mock.IntegrasjonClientMock.Companion.TOM_9999
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.testfiler.Testfil.TEST_PDF
import no.nav.familie.kontrakter.ba.søknad.VersjonertBarnetrygdSøknad
import no.nav.familie.kontrakter.ba.søknad.VersjonertBarnetrygdSøknadV9
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokdistkanal.Distribusjonskanal
import no.nav.familie.kontrakter.felles.dokdistkanal.DokdistkanalRequest
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.TilgangsstyrtJournalpost
import no.nav.familie.kontrakter.felles.kodeverk.BeskrivelseDto
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkSpråk
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.io.BufferedReader
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@Service
@Profile("mock-integrasjon-client")
@Primary
class FakeIntegrasjonClient(
    restOperations: RestOperations,
) : IntegrasjonClient(URI("integrasjoner-url"), restOperations) {
    private val egenansatt = mutableSetOf<String>()
    private val behandlendeEnhetForIdent = mutableMapOf<String, List<Arbeidsfordelingsenhet>>()
    private val versjonerteBarnetrygdSøknader = mutableMapOf<String, VersjonertBarnetrygdSøknad>()

    override fun hentAlleEØSLand(): KodeverkDto = hentKodeverkLand()

    override fun hentLand(landkode: String): String = "Testland"

    override fun hentPoststeder(): KodeverkDto =
        KodeverkDto(
            betydninger =
                (0..9999).associate {
                    it.toString().padStart(4, '0') to
                        listOf(
                            BetydningDto(
                                gyldigFra = LocalDate.now().minusYears(1),
                                gyldigTil = LocalDate.now().plusYears(1),
                                beskrivelser = mapOf(KodeverkSpråk.BOKMÅL.kode to BeskrivelseDto("Oslo", "Oslo")),
                            ),
                        )
                },
        )

    override fun hentBehandlendeEnhet(ident: String): List<Arbeidsfordelingsenhet> =
        behandlendeEnhetForIdent[ident] ?: listOf(
            Arbeidsfordelingsenhet(
                BarnetrygdEnhet.OSLO.enhetsnummer,
                BarnetrygdEnhet.OSLO.enhetsnavn,
            ),
        )

    override fun hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent: NavIdent): List<BarnetrygdEnhet> = BarnetrygdEnhet.entries

    override fun hentArbeidsforhold(
        ident: String,
        ansettelsesperiodeFom: LocalDate,
    ): List<Arbeidsforhold> = emptyList()

    override fun distribuerBrev(distribuerDokumentDTO: DistribuerDokumentDTO): String = "bestillingsId"

    override fun ferdigstillOppgave(oppgaveId: Long) {
        return
    }

    override fun oppdaterOppgave(
        oppgaveId: Long,
        oppdatertOppgave: Oppgave,
    ) {
        return
    }

    override fun hentEnhet(enhetId: String): NavKontorEnhet =
        NavKontorEnhet(
            enhetId.toInt(),
            BarnetrygdEnhet.entries.single { it.enhetsnummer == enhetId }.enhetsnavn,
            enhetId,
            "",
        )

    override fun opprettOppgave(opprettOppgave: OpprettOppgaveRequest): OppgaveResponse = OppgaveResponse(12345678L)

    override fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse = OppgaveResponse(12345678L)

    override fun fordelOppgave(
        oppgaveId: Long,
        saksbehandler: String?,
    ): OppgaveResponse = OppgaveResponse(12345678L)

    override fun tilordneEnhetOgRessursForOppgave(
        oppgaveId: Long,
        nyEnhet: String,
    ): OppgaveResponse = OppgaveResponse(12345678L)

    override fun finnOppgaveMedId(oppgaveId: Long): Oppgave = lagTestOppgaveDTO(oppgaveId)

    override fun hentJournalpost(journalpostId: String): Journalpost =
        lagTestJournalpost(
            journalpostId = journalpostId,
            personIdent = randomFnr(),
            avsenderMottakerIdType = AvsenderMottakerIdType.FNR,
            kanal = "NAV_NO",
        )

    override fun hentJournalposterForBruker(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<Journalpost> {
        val søkerFnr = randomFnr()
        return listOf(
            lagTestJournalpost(
                personIdent = søkerFnr,
                journalpostId = UUID.randomUUID().toString(),
                avsenderMottakerIdType = AvsenderMottakerIdType.FNR,
                kanal = "NAV_NO",
            ),
            lagTestJournalpost(
                personIdent = søkerFnr,
                journalpostId = UUID.randomUUID().toString(),
                avsenderMottakerIdType = AvsenderMottakerIdType.FNR,
                kanal = "NAV_NO",
            ),
        )
    }

    override fun hentTilgangsstyrteJournalposterForBruker(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<TilgangsstyrtJournalpost> = emptyList()

    override fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto =
        FinnOppgaveResponseDto(
            2,
            listOf(lagTestOppgaveDTO(1L), lagTestOppgaveDTO(2L, Oppgavetype.BehandleSak, "Z999999")),
        )

    override fun ferdigstillJournalpost(
        journalpostId: String,
        journalførendeEnhet: String,
    ) {
        return
    }

    override fun oppdaterJournalpost(
        request: OppdaterJournalpostRequest,
        journalpostId: String,
    ): OppdaterJournalpostResponse = OppdaterJournalpostResponse("1234567")

    override fun leggTilLogiskVedlegg(
        request: LogiskVedleggRequest,
        dokumentinfoId: String,
    ): LogiskVedleggResponse = LogiskVedleggResponse(12345678)

    override fun slettLogiskVedlegg(
        logiskVedleggId: String,
        dokumentinfoId: String,
    ): LogiskVedleggResponse = super.slettLogiskVedlegg(logiskVedleggId, dokumentinfoId)

    override fun hentDokument(
        dokumentInfoId: String,
        journalpostId: String,
    ): ByteArray = TEST_PDF

    override fun journalførDokument(arkiverDokumentRequest: ArkiverDokumentRequest): ArkiverDokumentResponse = ArkiverDokumentResponse(ferdigstilt = true, journalpostId = "journalpostId")

    override fun opprettSkyggesak(
        aktør: Aktør,
        fagsakId: Long,
    ) {
        return
    }

    override fun hentLandkoderISO2(): Map<String, String> = hentLandkoder()

    override fun hentOrganisasjon(organisasjonsnummer: String): Organisasjon =
        Organisasjon(
            "998765432",
            "Testinstitusjon",
        )

    override fun hentDistribusjonskanal(request: DokdistkanalRequest): Distribusjonskanal = super.hentDistribusjonskanal(request)

    override fun settNyAktivBrukerIModiaContext(nyAktivBruker: RestNyAktivBrukerIModiaContext): ModiaContext =
        ModiaContext(
            aktivBruker = nyAktivBruker.personIdent,
            aktivEnhet = "0000",
        )

    override fun hentModiaContext(): ModiaContext =
        ModiaContext(
            aktivBruker = "13025514402",
            aktivEnhet = "0000",
        )

    override fun hentVersjonertBarnetrygdSøknad(journalpostId: String): VersjonertBarnetrygdSøknad = versjonerteBarnetrygdSøknader[journalpostId] ?: VersjonertBarnetrygdSøknadV9(lagBarnetrygdSøknadV9())

    override fun hentAInntektUrl(personIdent: PersonIdent): String = "/test/1234"

    override fun sjekkErEgenAnsattBulk(personIdenter: List<String>): Map<String, Boolean> = personIdenter.associateWith { egenansatt.contains(it) }

    fun leggTilEgenansatt(ident: String) {
        egenansatt.add(ident)
    }

    fun leggTilBehandlendeEnhet(
        ident: String,
        enheter: List<BarnetrygdEnhet>,
    ) {
        behandlendeEnhetForIdent[ident] = enheter.map { enhet -> Arbeidsfordelingsenhet(enhet.enhetsnummer, enhet.enhetsnavn) }
    }

    fun leggTilVersjonertBarnetrygdSøknad(
        journalpostId: String,
        versjonertBarnetrygdSøknad: VersjonertBarnetrygdSøknad,
    ) {
        versjonerteBarnetrygdSøknader[journalpostId] = versjonertBarnetrygdSøknad
    }

    private fun hentLandkoder(): Map<String, String> {
        val landkoder =
            ClassPathResource("landkoder/landkoder.json").inputStream.bufferedReader().use(BufferedReader::readText)

        return objectMapper.readValue<List<IntegrasjonClientMock.Companion.LandkodeISO2>>(landkoder).associate { it.code to it.name }
    }

    private fun hentKodeverkLand(): KodeverkDto {
        val beskrivelsePolen = BeskrivelseDto("POL", "")
        val betydningPolen = BetydningDto(FOM_2004, TOM_9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelsePolen))
        val beskrivelseTyskland = BeskrivelseDto("DEU", "")
        val betydningTyskland =
            BetydningDto(FOM_1900, TOM_9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseTyskland))
        val beskrivelseDanmark = BeskrivelseDto("DNK", "")
        val betydningDanmark =
            BetydningDto(FOM_1990, TOM_9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseDanmark))
        val beskrivelseUK = BeskrivelseDto("GBR", "")
        val betydningUK = BetydningDto(FOM_1900, TOM_2010, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseUK))

        return KodeverkDto(
            betydninger =
                mapOf(
                    "POL" to listOf(betydningPolen),
                    "DEU" to listOf(betydningTyskland),
                    "DNK" to listOf(betydningDanmark),
                    "GBR" to listOf(betydningUK),
                ),
        )
    }
}
