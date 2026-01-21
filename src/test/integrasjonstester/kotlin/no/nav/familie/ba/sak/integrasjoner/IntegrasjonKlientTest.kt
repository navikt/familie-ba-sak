package no.nav.familie.ba.sak.integrasjoner

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.status
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MDCOperations
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle.HENT_ARBEIDSFORDELING_MED_BEHANDLINGSTYPE
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagBarnetrygdSøknadV9
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagTestJournalpost
import no.nav.familie.ba.sak.datagenerator.lagTestOppgave
import no.nav.familie.ba.sak.datagenerator.lagTestOppgaveDTO
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.NyAktivBrukerIModiaContextDto
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient.Companion.VEDTAK_VEDLEGG_FILNAVN
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient.Companion.VEDTAK_VEDLEGG_TITTEL
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient.Companion.hentVedlegg
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Ansettelsesperiode
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsforhold
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsgiver
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.ArbeidsgiverType
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Periode
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Skyggesak
import no.nav.familie.ba.sak.integrasjoner.journalføring.UtgåendeJournalføringService
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.modiacontext.ModiaContext
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.kontrakter.ba.søknad.VersjonertBarnetrygdSøknadV9
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Companion.failure
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.restklient.client.RessursException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

class IntegrasjonKlientTest : AbstractSpringIntegrationTest() {
    @Autowired
    @Qualifier("jwtBearer")
    lateinit var restOperations: RestOperations

    lateinit var integrasjonKlient: IntegrasjonKlient
    lateinit var utgåendeJournalføringService: UtgåendeJournalføringService
    private val featureToggleService = mockk<FeatureToggleService>()

    @BeforeEach
    fun setUp() {
        integrasjonKlient =
            IntegrasjonKlient(
                URI.create(wireMockServer.baseUrl() + "/api"),
                restOperations,
                featureToggleService,
            )
        utgåendeJournalføringService =
            UtgåendeJournalføringService(
                integrasjonKlient = integrasjonKlient,
            )
    }

    @AfterEach
    fun clearTest() {
        MDC.clear()
        wireMockServer.resetAll()
    }

    @Test
    @Tag("integration")
    fun `Opprett oppgave skal returnere oppgave id`() {
        MDC.put("callId", "opprettOppgave")
        wireMockServer.stubFor(
            post("/api/oppgave/opprett").willReturn(
                okJson(jsonMapper.writeValueAsString(success(OppgaveResponse(oppgaveId = 1234)))),
            ),
        )

        val request = lagTestOppgave()

        val opprettOppgaveResponse = integrasjonKlient.opprettOppgave(request).oppgaveId.toString()

        assertThat(opprettOppgaveResponse).isEqualTo("1234")
        wireMockServer.verify(
            anyRequestedFor(anyUrl())
                .withHeader(NavHttpHeaders.NAV_CALL_ID.asString(), equalTo("opprettOppgave"))
                .withHeader(NavHttpHeaders.NAV_CONSUMER_ID.asString(), equalTo("srvfamilie-ba-sak"))
                .withRequestBody(equalToJson(jsonMapper.writeValueAsString(request))),
        )
    }

    @Test
    @Tag("integration")
    fun `Opprett oppgave skal kaste feil hvis response er ugyldig`() {
        wireMockServer.stubFor(
            post("/api/oppgave/opprett").willReturn(
                aResponse()
                    .withStatus(500)
                    .withBody(jsonMapper.writeValueAsString(failure<String>("test"))),
            ),
        )

        val feil = assertThrows<RessursException> { integrasjonKlient.opprettOppgave(lagTestOppgave()) }
        assertEquals("test", feil.ressurs.melding)
    }

    @Test
    @Tag("integration")
    fun `hentOppgaver skal returnere en liste av oppgaver og antallet oppgaver`() {
        val oppgave = Oppgave()
        wireMockServer.stubFor(
            post("/api/oppgave/v4").willReturn(
                okJson(
                    jsonMapper.writeValueAsString(
                        success(FinnOppgaveResponseDto(1, listOf(oppgave))),
                    ),
                ),
            ),
        )

        val oppgaverOgAntall = integrasjonKlient.hentOppgaver(FinnOppgaveRequest(tema = Tema.BAR))
        assertThat(oppgaverOgAntall.oppgaver).hasSize(1)
    }

    @Test
    @Tag("integration")
    fun `Journalfør vedtaksbrev skal journalføre dokument, returnere 201 og journalpostId`() {
        MDC.put("callId", "journalfør")
        wireMockServer.stubFor(
            post("/api/arkiv/v4")
                .withHeader("Accept", containing("json"))
                .willReturn(
                    aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonMapper.writeValueAsString(journalpostOkResponse())),
                ),
        )

        val vedtak = lagVedtak(lagBehandlingUtenId())
        vedtak.stønadBrevPdF = mockPdf

        val journalPostId =
            utgåendeJournalføringService.journalførDokument(
                fnr = MOCK_FNR,
                fagsakId =
                    vedtak.behandling.fagsak.id
                        .toString(),
                brev =
                    listOf(
                        Dokument(
                            dokument = mockPdf,
                            filtype = Filtype.PDFA,
                            dokumenttype = Dokumenttype.BARNETRYGD_VEDTAK_INNVILGELSE,
                        ),
                    ),
                journalførendeEnhet = "1",
                vedlegg =
                    listOf(
                        Dokument(
                            dokument = hentVedlegg(VEDTAK_VEDLEGG_FILNAVN)!!,
                            filtype = Filtype.PDFA,
                            dokumenttype = Dokumenttype.BARNETRYGD_VEDLEGG,
                            tittel = VEDTAK_VEDLEGG_TITTEL,
                        ),
                    ),
                eksternReferanseId = "0_${vedtak.behandling.id}_journalfør",
            )

        assertThat(journalPostId).isEqualTo(MOCK_JOURNALPOST_FOR_VEDTAK_ID)
        wireMockServer.verify(
            anyRequestedFor(anyUrl())
                .withHeader(NavHttpHeaders.NAV_CALL_ID.asString(), equalTo("journalfør"))
                .withHeader(NavHttpHeaders.NAV_CONSUMER_ID.asString(), equalTo("srvfamilie-ba-sak"))
                .withRequestBody(
                    equalToJson(
                        jsonMapper.writeValueAsString(
                            forventetRequestArkiverDokument(
                                fagsakId = vedtak.behandling.fagsak.id,
                                behandlingId = vedtak.behandling.id,
                            ),
                        ),
                    ),
                ),
        )
    }

    @Test
    @Tag("integration")
    fun `distribuerVedtaksbrev returnerer normalt ved vellykket integrasjonskall`() {
        MDC.put("callId", "distribuerVedtaksbrev")
        wireMockServer.stubFor(
            post("/api/dist/v1")
                .withHeader("Accept", containing("json"))
                .willReturn(okJson(jsonMapper.writeValueAsString(success("1234567")))),
        )

        assertDoesNotThrow { integrasjonKlient.distribuerBrev(lagDistribuerDokumentDTO()) }
        wireMockServer.verify(
            postRequestedFor(anyUrl())
                .withHeader(NavHttpHeaders.NAV_CALL_ID.asString(), equalTo("distribuerVedtaksbrev"))
                .withHeader(NavHttpHeaders.NAV_CONSUMER_ID.asString(), equalTo("srvfamilie-ba-sak"))
                .withRequestBody(
                    equalToJson(
                        "{\"journalpostId\":\"123456789\"," +
                            "\"bestillendeFagsystem\":\"BA\"," +
                            "\"dokumentProdApp\":\"FAMILIE_BA_SAK\"," +
                            "\"distribusjonstype\" : \"VIKTIG\"," +
                            "\"distribusjonstidspunkt\" : \"KJERNETID\"}",
                        false,
                        true,
                    ),
                ),
        )
    }

    @Test
    @Tag("integration")
    fun `distribuerVedtaksbrev kaster exception hvis integrasjoner gir blank response`() {
        wireMockServer.stubFor(
            post("/api/dist/v1")
                .withHeader("Accept", containing("json"))
                .willReturn(okJson(jsonMapper.writeValueAsString(success("")))),
        )

        assertThrows<Feil> { integrasjonKlient.distribuerBrev(lagDistribuerDokumentDTO()) }
    }

    @Test
    @Tag("integration")
    fun `distribuerVedtaksbrev kaster exception hvis integrasjoner gir failure response`() {
        wireMockServer.stubFor(
            post("/api/dist/v1")
                .withHeader("Accept", containing("json"))
                .willReturn(okJson(jsonMapper.writeValueAsString(failure<Any>("")))),
        )

        val feil = assertThrows<IntegrasjonException> { integrasjonKlient.distribuerBrev(lagDistribuerDokumentDTO()) }
        assertTrue(feil.message?.contains("dokdist") == true)
    }

    @Test
    @Tag("integration")
    fun `distribuerVedtaksbrev kaster exception hvis responsekoden ikke er 2xx`() {
        wireMockServer.stubFor(
            post("/api/dist/v1")
                .withHeader("Accept", containing("json"))
                .willReturn(
                    aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json"),
                ),
        )

        assertThrows<HttpClientErrorException.BadRequest> { integrasjonKlient.distribuerBrev(lagDistribuerDokumentDTO()) }
    }

    @Test
    @Tag("integration")
    fun `Ferdigstill oppgave returnerer OK`() {
        MDC.put("callId", "ferdigstillOppgave")
        wireMockServer.stubFor(
            patch(urlEqualTo("/api/oppgave/123/ferdigstill"))
                .withHeader("Accept", containing("json"))
                .willReturn(okJson(jsonMapper.writeValueAsString(success(OppgaveResponse(1))))),
        )

        integrasjonKlient.ferdigstillOppgave(123)

        wireMockServer.verify(
            patchRequestedFor(urlEqualTo("/api/oppgave/123/ferdigstill"))
                .withHeader(NavHttpHeaders.NAV_CALL_ID.asString(), equalTo("ferdigstillOppgave"))
                .withHeader(NavHttpHeaders.NAV_CONSUMER_ID.asString(), equalTo("srvfamilie-ba-sak")),
        )
    }

    @Test
    @Tag("integration")
    fun `Ferdigstill oppgave returnerer feil `() {
        MDC.put("callId", "ferdigstillOppgave")
        wireMockServer.stubFor(
            patch(urlEqualTo("/api/oppgave/123/ferdigstill"))
                .withHeader("Accept", containing("json"))
                .willReturn(
                    aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonMapper.writeValueAsString(failure<String>("test"))),
                ),
        )

        val feil =
            assertThrows<RessursException> { integrasjonKlient.ferdigstillOppgave(123) }
        assertEquals("test", feil.ressurs.melding)
    }

    @Test
    @Tag("integration")
    fun `hentBehandlendeEnhet returnerer OK uten behandlingstype`() {
        every { featureToggleService.isEnabled(HENT_ARBEIDSFORDELING_MED_BEHANDLINGSTYPE) } returns false
        wireMockServer.stubFor(
            post("/api/arbeidsfordeling/enhet/BAR")
                .withHeader("Accept", containing("json"))
                .willReturn(
                    okJson(
                        jsonMapper.writeValueAsString(
                            success(
                                listOf(
                                    Arbeidsfordelingsenhet("2", "foo"),
                                ),
                            ),
                        ),
                    ),
                ),
        )

        val enhet = integrasjonKlient.hentBehandlendeEnhet("1")
        assertThat(enhet).isNotEmpty
        assertThat(enhet.first().enhetId).isEqualTo("2")
    }

    @Test
    @Tag("integration")
    fun `hentBehandlendeEnhet returnerer OK med behandlingstype`() {
        every { featureToggleService.isEnabled(HENT_ARBEIDSFORDELING_MED_BEHANDLINGSTYPE) } returns true
        wireMockServer.stubFor(
            post("/api/arbeidsfordeling/enhet/BAR?behandlingstype=E%C3%98S")
                .withHeader("Accept", containing("json"))
                .willReturn(
                    okJson(
                        jsonMapper.writeValueAsString(
                            success(
                                listOf(
                                    Arbeidsfordelingsenhet("2", "foo"),
                                ),
                            ),
                        ),
                    ),
                ),
        )

        val enhet = integrasjonKlient.hentBehandlendeEnhet("1", Behandlingstype.EØS)
        assertThat(enhet).isNotEmpty
        assertThat(enhet.first().enhetId).isEqualTo("2")
    }

    @Test
    @Tag("integration")
    fun `finnOppgaveMedId returnerer OK`() {
        val oppgaveId = 1234L
        wireMockServer.stubFor(
            get("/api/oppgave/$oppgaveId").willReturn(
                okJson(
                    jsonMapper.writeValueAsString(
                        success(
                            lagTestOppgaveDTO(
                                oppgaveId,
                            ),
                        ),
                    ),
                ),
            ),
        )

        val oppgave = integrasjonKlient.finnOppgaveMedId(oppgaveId)
        assertThat(oppgave.id).isEqualTo(oppgaveId)

        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/oppgave/$oppgaveId")))
    }

    @Test
    @Tag("integration")
    fun `hentJournalpost returnerer OK`() {
        val journalpostId = "1234"
        val fnr = randomFnr()
        wireMockServer.stubFor(
            get("/api/journalpost/tilgangsstyrt/baks?journalpostId=$journalpostId").willReturn(
                okJson(
                    jsonMapper.writeValueAsString(
                        success(
                            lagTestJournalpost(fnr, journalpostId, AvsenderMottakerIdType.FNR, "NAV_NO"),
                        ),
                    ),
                ),
            ),
        )

        val journalpost = integrasjonKlient.hentJournalpost(journalpostId)
        assertThat(journalpost).isNotNull
        assertThat(journalpost.journalpostId).isEqualTo(journalpostId)
        assertThat(journalpost.bruker?.id).isEqualTo(fnr)

        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/journalpost/tilgangsstyrt/baks?journalpostId=$journalpostId")))
    }

    @Test
    fun `hentDokument returnerer OK`() {
        val journalpostId = "1234"
        val dokumentId = "5678"
        val fnr = randomFnr()
        wireMockServer.stubFor(
            get("/api/journalpost/hentdokument/tilgangsstyrt/baks/$journalpostId/$dokumentId").willReturn(
                okJson(
                    jsonMapper.writeValueAsString(
                        success(
                            "Test".toByteArray(),
                        ),
                    ),
                ),
            ),
        )

        val dokument = integrasjonKlient.hentDokument(journalpostId = journalpostId, dokumentInfoId = dokumentId)
        assertThat(dokument).isNotNull
        assertThat(dokument.decodeToString()).isEqualTo("Test")

        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/journalpost/hentdokument/tilgangsstyrt/baks/$journalpostId/$dokumentId")))
    }

    @Test
    @Tag("integration")
    fun `skal hente arbeidsforhold for person`() {
        val fnr = randomFnr()

        val arbeidsforhold =
            listOf(
                Arbeidsforhold(
                    arbeidsgiver = Arbeidsgiver(ArbeidsgiverType.Organisasjon, "998877665"),
                    ansettelsesperiode = Ansettelsesperiode(Periode(fom = LocalDate.now().minusYears(1))),
                ),
            )

        wireMockServer.stubFor(
            post("/api/aareg/arbeidsforhold").willReturn(
                okJson(
                    jsonMapper.writeValueAsString(
                        success(
                            arbeidsforhold,
                        ),
                    ),
                ),
            ),
        )

        val response = integrasjonKlient.hentArbeidsforhold(fnr, LocalDate.now())

        assertThat(response).hasSize(1)
        assertThat(response.first().arbeidsgiver?.organisasjonsnummer).isEqualTo("998877665")
        assertThat(
            response
                .first()
                .ansettelsesperiode
                ?.periode
                ?.fom,
        ).isEqualTo(LocalDate.now().minusYears(1))
    }

    @Test
    @Tag("integration")
    fun `skal kaste integrasjonsfeil mot arbeidsforhold`() {
        val fnr = randomFnr()

        wireMockServer.stubFor(post("/api/aareg/arbeidsforhold").willReturn(status(500)))

        val feil = assertThrows<IntegrasjonException> { integrasjonKlient.hentArbeidsforhold(fnr, LocalDate.now()) }
        assertTrue(feil.message?.contains("aareg") == true)
    }

    @Test
    @Tag("integration")
    fun `skal opprette skyggesak for Sak`() {
        val aktørId = randomAktør()

        wireMockServer.stubFor(post("/api/skyggesak/v1").willReturn(okJson(jsonMapper.writeValueAsString(success(null)))))

        integrasjonKlient.opprettSkyggesak(aktørId, MOCK_FAGSAK_ID.toLong())

        wireMockServer.verify(
            postRequestedFor(urlEqualTo("/api/skyggesak/v1"))
                .withRequestBody(
                    equalToJson(
                        jsonMapper.writeValueAsString(
                            Skyggesak(
                                aktoerId = aktørId.aktørId,
                                MOCK_FAGSAK_ID,
                                "BAR",
                                "BA",
                            ),
                        ),
                    ),
                ),
        )
    }

    @Test
    @Tag("integration")
    fun `skal kaste integrasjonsfeil ved oppretting av skyggesak`() {
        val aktørId = randomAktør()

        wireMockServer.stubFor(post("/api/skyggesak/v1").willReturn(status(500)))

        val feil =
            assertThrows<IntegrasjonException> { integrasjonKlient.opprettSkyggesak(aktørId, MOCK_FAGSAK_ID.toLong()) }
        assertTrue(feil.message?.contains("skyggesak") == true)
    }

    @Test
    @Tag("integration")
    fun `skal hente ModiaContext`() {
        wireMockServer
            .stubFor(
                get("/api/modia-context-holder")
                    .willReturn(okJson(modiaContextResponse { success(it) })),
            )

        val modiaContext = integrasjonKlient.hentModiaContext()

        assertThat(modiaContext.aktivBruker).isEqualTo("13025514402")
    }

    @Test
    @Tag("integration")
    fun `skal oppdatere ModiaContext`() {
        wireMockServer
            .stubFor(
                post("/api/modia-context-holder/sett-aktiv-bruker")
                    .withRequestBody(modiaContextRequest())
                    .willReturn(okJson(modiaContextResponse { success(it) })),
            )

        val modiaContext = integrasjonKlient.settNyAktivBrukerIModiaContext(NyAktivBrukerIModiaContextDto(personIdent = "13025514402"))

        assertThat(modiaContext.aktivBruker).isEqualTo("13025514402")
    }

    @Test
    @Tag("integration")
    fun `skal kaste RessursException ved henting av ModiaContext`() {
        wireMockServer
            .stubFor(
                get("/api/modia-context-holder")
                    .willReturn(
                        aResponse()
                            .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .withBody(modiaContextResponse { failure("Noe gikk galt") }),
                    ),
            )

        val exception = assertThrows<RessursException> { integrasjonKlient.hentModiaContext() }

        assertThat(exception.message).contains("modia-context-holder")
        assertThat(exception.httpStatus).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(exception.ressurs.melding).isEqualTo("Noe gikk galt")
    }

    @Test
    @Tag("integration")
    fun `skal kaste RessursException ved oppdatering av ModiaContext`() {
        wireMockServer
            .stubFor(
                post("/api/modia-context-holder/sett-aktiv-bruker")
                    .withRequestBody(modiaContextRequest())
                    .willReturn(
                        aResponse()
                            .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .withBody(modiaContextResponse { failure("Noe gikk galt") }),
                    ),
            )

        val exception =
            assertThrows<RessursException> {
                integrasjonKlient.settNyAktivBrukerIModiaContext(NyAktivBrukerIModiaContextDto(personIdent = "13025514402"))
            }

        assertThat(exception.message).contains("modia-context-holder")
        assertThat(exception.httpStatus).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(exception.ressurs.melding).isEqualTo("Noe gikk galt")
    }

    @Test
    @Tag("integration")
    fun `skal hente VersjonertBarnetrygdSøknad`() {
        val journalpostId = "1234"
        val versjonertBarnetrygdSøknadV9 =
            VersjonertBarnetrygdSøknadV9(
                barnetrygdSøknad = lagBarnetrygdSøknadV9(),
            )
        wireMockServer.stubFor(
            get("/api/baks/versjonertsoknad/ba/$journalpostId")
                .willReturn(
                    okJson(
                        jsonMapper.writeValueAsString(
                            success(
                                versjonertBarnetrygdSøknadV9,
                            ),
                        ),
                    ),
                ),
        )

        val versjonertSøknad = integrasjonKlient.hentVersjonertBarnetrygdSøknad(journalpostId)

        assertThat(versjonertSøknad).isNotNull
        assertThat(versjonertSøknad).isEqualTo(versjonertBarnetrygdSøknadV9)
    }

    @Test
    @Tag("integration")
    fun `skal hente URL mot A-Inntekt`() {
        val url = "/test/1234"
        wireMockServer.stubFor(
            post("/api/arbeid-og-inntekt/hent-url")
                .willReturn(
                    okJson(jsonMapper.writeValueAsString(success(url))),
                ),
        )

        val aInntektUrl = integrasjonKlient.hentAInntektUrl(PersonIdent(randomFnr()))

        assertThat(aInntektUrl).isEqualTo(url)
    }

    @Test
    @Tag("integration")
    fun `skal kaste feil når henting av URL mot A-Inntekt feiler`() {
        wireMockServer.stubFor(post("/api/arbeid-og-inntekt/hent-url").willReturn(status(500)))

        val feil = assertThrows<IntegrasjonException> { integrasjonKlient.hentAInntektUrl(PersonIdent(randomFnr())) }
        assertTrue(feil.message?.contains("a-inntekt-url") == true)
    }

    private fun modiaContextResponse(ressursFunksjon: (ModiaContext) -> Ressurs<ModiaContext>) =
        jsonMapper.writeValueAsString(
            ressursFunksjon(
                ModiaContext(
                    aktivBruker = "13025514402",
                    aktivEnhet = "0000",
                ),
            ),
        )

    private fun modiaContextRequest() =
        equalToJson(
            jsonMapper.writeValueAsString(
                NyAktivBrukerIModiaContextDto(personIdent = "13025514402"),
            ),
        )

    private fun journalpostOkResponse(): Ressurs<ArkiverDokumentResponse> = success(ArkiverDokumentResponse(MOCK_JOURNALPOST_FOR_VEDTAK_ID, true))

    private fun forventetRequestArkiverDokument(
        fagsakId: Long,
        behandlingId: Long,
    ): ArkiverDokumentRequest {
        val vedleggPdf = hentVedlegg(VEDTAK_VEDLEGG_FILNAVN)
        val brev =
            listOf(
                Dokument(
                    dokument = mockPdf,
                    filtype = Filtype.PDFA,
                    dokumenttype = Dokumenttype.BARNETRYGD_VEDTAK_INNVILGELSE,
                ),
            )
        val vedlegg =
            listOf(
                Dokument(
                    dokument = vedleggPdf!!,
                    filtype = Filtype.PDFA,
                    dokumenttype = Dokumenttype.BARNETRYGD_VEDLEGG,
                    tittel = VEDTAK_VEDLEGG_TITTEL,
                ),
            )

        return ArkiverDokumentRequest(
            fnr = MOCK_FNR,
            forsøkFerdigstill = true,
            fagsakId = fagsakId.toString(),
            journalførendeEnhet = "1",
            hoveddokumentvarianter = brev,
            vedleggsdokumenter = vedlegg,
            eksternReferanseId = "${fagsakId}_${behandlingId}_${MDCOperations.getCallId()}",
        )
    }

    private fun lagDistribuerDokumentDTO() =
        DistribuerDokumentDTO(
            journalpostId = "123456789",
            behandlingId = 1L,
            brevmal = Brevmal.VARSEL_OM_REVURDERING,
            fagsakId = 1L,
            erManueltSendt = true,
        )

    companion object {
        const val MOCK_JOURNALPOST_FOR_VEDTAK_ID = "453491843"
        const val MOCK_FNR = "12345678910"
        val mockPdf = "mock data".toByteArray()
        const val MOCK_FAGSAK_ID = "140258931"
    }
}
