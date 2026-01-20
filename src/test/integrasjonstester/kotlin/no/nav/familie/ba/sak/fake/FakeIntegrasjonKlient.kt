package no.nav.familie.ba.sak.fake

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagBarnetrygdSøknadV9
import no.nav.familie.ba.sak.datagenerator.lagKodeverkLand
import no.nav.familie.ba.sak.datagenerator.lagTestJournalpost
import no.nav.familie.ba.sak.datagenerator.lagTestOppgaveDTO
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.RestNyAktivBrukerIModiaContext
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsforhold
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsgiver
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.ArbeidsgiverType
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.LogiskVedleggRequest
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.LogiskVedleggResponse
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.OppdaterJournalpostRequest
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.OppdaterJournalpostResponse
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.ba.sak.kjerne.modiacontext.ModiaContext
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.testfiler.Testfil
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
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.organisasjon.Gyldighetsperiode
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.kontrakter.felles.organisasjon.OrganisasjonAdresse
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import org.springframework.core.io.ClassPathResource
import org.springframework.web.client.RestOperations
import java.io.BufferedReader
import java.net.URI
import java.time.LocalDate
import java.util.UUID

class FakeIntegrasjonKlient(
    restOperations: RestOperations,
) : IntegrasjonKlient(URI("integrasjoner-url"), restOperations, mockk()) {
    private val egenansatt = mutableSetOf<String>()
    private val behandlendeEnhetForIdent = mutableMapOf<String, List<Arbeidsfordelingsenhet>>()
    private val versjonerteBarnetrygdSøknader = mutableMapOf<String, VersjonertBarnetrygdSøknad>()
    private val journalførteDokumenter = mutableListOf<ArkiverDokumentRequest>()

    override fun hentAlleEØSLand(): KodeverkDto = lagKodeverkLand()

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

    override fun hentBehandlendeEnhet(
        ident: String,
        behandlingstype: Behandlingstype?,
    ): List<Arbeidsfordelingsenhet> =
        behandlendeEnhetForIdent[ident] ?: listOf(
            Arbeidsfordelingsenhet.opprettFra(BarnetrygdEnhet.OSLO),
        )

    override fun hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent: NavIdent): List<BarnetrygdEnhet> = BarnetrygdEnhet.entries

    override fun hentArbeidsforhold(
        ident: String,
        ansettelsesperiodeFom: LocalDate,
    ): List<Arbeidsforhold> =
        listOf(
            Arbeidsforhold(
                arbeidsgiver =
                    Arbeidsgiver(
                        type = ArbeidsgiverType.Organisasjon,
                        organisasjonsnummer = "123456789",
                    ),
            ),
        )

    override fun distribuerBrev(distribuerDokumentDTO: DistribuerDokumentDTO): String = "bestillingsId"

    override fun ferdigstillOppgave(oppgaveId: Long) {
        return
    }

    override fun oppdaterOppgave(
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
    ): LogiskVedleggResponse = LogiskVedleggResponse(12345678L)

    override fun slettLogiskVedlegg(
        logiskVedleggId: String,
        dokumentinfoId: String,
    ): LogiskVedleggResponse = LogiskVedleggResponse(12345678L)

    override fun hentDokument(
        dokumentInfoId: String,
        journalpostId: String,
    ): ByteArray = Testfil.TEST_PDF

    override fun journalførDokument(arkiverDokumentRequest: ArkiverDokumentRequest): ArkiverDokumentResponse {
        journalførteDokumenter.add(arkiverDokumentRequest)
        return ArkiverDokumentResponse(ferdigstilt = true, journalpostId = "journalpostId")
    }

    fun hentJournalførteDokumenter(): List<ArkiverDokumentRequest> = journalførteDokumenter

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
            adresse =
                OrganisasjonAdresse(
                    type = "Forretningsadresse",
                    adresselinje1 = "Fyrstikkalleen 1",
                    adresselinje2 = null,
                    adresselinje3 = "Avd BAKS",
                    postnummer = "0661",
                    kommunenummer = "0301",
                    gyldighetsperiode = Gyldighetsperiode(fom = LocalDate.of(2020, 1, 1), tom = null),
                ),
        )

    override fun hentDistribusjonskanal(request: DokdistkanalRequest): Distribusjonskanal = Distribusjonskanal.UKJENT

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

    override fun hentVersjonertBarnetrygdSøknad(journalpostId: String): VersjonertBarnetrygdSøknad =
        versjonerteBarnetrygdSøknader[journalpostId] ?: VersjonertBarnetrygdSøknadV9(
            lagBarnetrygdSøknadV9(),
        )

    override fun hentAInntektUrl(personIdent: PersonIdent): String = "/test/1234"

    override fun sjekkErEgenAnsattBulk(personIdenter: List<String>): Map<String, Boolean> = personIdenter.associateWith { egenansatt.contains(it) }

    override fun hentSaksbehandler(id: String): Saksbehandler =
        Saksbehandler(
            azureId = UUID.randomUUID(),
            navIdent = id,
            fornavn = "System",
            etternavn = "",
            enhet = BarnetrygdEnhet.OSLO.enhetsnummer,
            enhetsnavn = BarnetrygdEnhet.OSLO.enhetsnavn,
        )

    fun leggTilEgenansatt(ident: String) {
        egenansatt.add(ident)
    }

    fun leggTilBehandlendeEnhet(
        ident: String,
        enheter: List<BarnetrygdEnhet>,
    ) {
        behandlendeEnhetForIdent[ident] =
            enheter.map { enhet ->
                Arbeidsfordelingsenhet.opprettFra(
                    enhet,
                )
            }
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

        return objectMapper.readValue<List<LandkodeISO2>>(landkoder).associate { it.code to it.name }
    }

    data class LandkodeISO2(
        val code: String,
        val name: String,
    )
}
