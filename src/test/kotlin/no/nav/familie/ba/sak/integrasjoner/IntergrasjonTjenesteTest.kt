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
import io.mockk.mockk
import no.nav.familie.ba.sak.common.MDCOperations
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTestDev
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient.Companion.VEDTAK_VEDLEGG_FILNAVN
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient.Companion.VEDTAK_VEDLEGG_TITTEL
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient.Companion.hentVedlegg
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Ansettelsesperiode
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsforhold
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsgiver
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.ArbeidsgiverType
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidstaker
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Periode
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Skyggesak
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Companion.failure
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.log.NavHttpHeaders
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate
import kotlin.random.Random

class IntergrasjonTjenesteTest : AbstractSpringIntegrationTestDev() {

    @Autowired
    @Qualifier("jwtBearer")
    lateinit var restOperations: RestOperations

    lateinit var integrasjonClient: IntegrasjonClient

    @BeforeEach
    fun setUp() {
        integrasjonClient = IntegrasjonClient(
            URI.create(wireMockServer.baseUrl() + "/api"),
            restOperations,
            mockk(relaxed = true)
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
                okJson(objectMapper.writeValueAsString(success(OppgaveResponse(oppgaveId = 1234))))
            )
        )

        val request = lagTestOppgave()

        val opprettOppgaveResponse = integrasjonClient.opprettOppgave(request)

        assertThat(opprettOppgaveResponse).isEqualTo("1234")
        wireMockServer.verify(
            anyRequestedFor(anyUrl())
                .withHeader(NavHttpHeaders.NAV_CALL_ID.asString(), equalTo("opprettOppgave"))
                .withHeader(NavHttpHeaders.NAV_CONSUMER_ID.asString(), equalTo("familie-ba-sak"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(request)))
        )
    }

    @Test
    @Tag("integration")
    fun `Opprett oppgave skal kaste feil hvis response er ugyldig`() {
        wireMockServer.stubFor(
            post("/api/oppgave/opprett").willReturn(
                aResponse()
                    .withStatus(500)
                    .withBody(objectMapper.writeValueAsString(failure<String>("test")))
            )
        )

        assertThatThrownBy {
            integrasjonClient.opprettOppgave(lagTestOppgave())
        }.isInstanceOf(IntegrasjonException::class.java)
            .hasMessageContaining("Kall mot integrasjon feilet ved opprett oppgave")
    }

    @Test
    @Tag("integration")
    fun `hentOppgaver skal returnere en liste av oppgaver og antallet oppgaver`() {
        val oppgave = Oppgave()
        wireMockServer.stubFor(
            post("/api/oppgave/v4").willReturn(
                okJson(
                    objectMapper.writeValueAsString(
                        success(FinnOppgaveResponseDto(1, listOf(oppgave)))
                    )
                )
            )
        )

        val oppgaverOgAntall = integrasjonClient.hentOppgaver(FinnOppgaveRequest(tema = Tema.BAR))
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
                        .withBody(objectMapper.writeValueAsString(journalpostOkResponse()))
                )
        )

        val vedtak = lagVedtak(lagBehandling())
        vedtak.stønadBrevPdF = mockPdf

        val journalPostId =
            integrasjonClient.journalførVedtaksbrev(MOCK_FNR, vedtak.behandling.fagsak.id.toString(), vedtak, "1")

        assertThat(journalPostId).isEqualTo(MOCK_JOURNALPOST_FOR_VEDTAK_ID)
        wireMockServer.verify(
            anyRequestedFor(anyUrl())
                .withHeader(NavHttpHeaders.NAV_CALL_ID.asString(), equalTo("journalfør"))
                .withHeader(NavHttpHeaders.NAV_CONSUMER_ID.asString(), equalTo("familie-ba-sak"))
                .withRequestBody(
                    equalToJson(
                        objectMapper.writeValueAsString(
                            forventetRequestArkiverDokument(
                                fagsakId = vedtak.behandling.fagsak.id,
                                behandlingId = vedtak.behandling.id
                            )
                        )
                    )
                )
        )
    }

    @Test
    @Tag("integration")
    fun `distribuerVedtaksbrev returnerer normalt ved vellykket integrasjonskall`() {
        MDC.put("callId", "distribuerVedtaksbrev")
        wireMockServer.stubFor(
            post("/api/dist/v1")
                .withHeader("Accept", containing("json"))
                .willReturn(okJson(objectMapper.writeValueAsString(success("1234567"))))
        )

        assertDoesNotThrow { integrasjonClient.distribuerBrev("123456789") }
        wireMockServer.verify(
            postRequestedFor(anyUrl())
                .withHeader(NavHttpHeaders.NAV_CALL_ID.asString(), equalTo("distribuerVedtaksbrev"))
                .withHeader(NavHttpHeaders.NAV_CONSUMER_ID.asString(), equalTo("familie-ba-sak"))
                .withRequestBody(
                    equalToJson(
                        "{\"journalpostId\":\"123456789\"," +
                            "\"bestillendeFagsystem\":\"BA\"," +
                            "\"dokumentProdApp\":\"FAMILIE_BA_SAK\"}"
                    )
                )
        )
    }

    @Test
    @Tag("integration")
    fun `distribuerVedtaksbrev kaster exception hvis integrasjoner gir blank response`() {
        wireMockServer.stubFor(
            post("/api/dist/v1")
                .withHeader("Accept", containing("json"))
                .willReturn(okJson(objectMapper.writeValueAsString(success(""))))
        )

        assertThrows<IllegalStateException> { integrasjonClient.distribuerBrev("123456789") }
    }

    @Test
    @Tag("integration")
    fun `distribuerVedtaksbrev kaster exception hvis integrasjoner gir failure response`() {
        wireMockServer.stubFor(
            post("/api/dist/v1")
                .withHeader("Accept", containing("json"))
                .willReturn(okJson(objectMapper.writeValueAsString(failure<Any>(""))))
        )

        assertThrows<IllegalStateException> { integrasjonClient.distribuerBrev("123456789") }
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
                        .withHeader("Content-Type", "application/json")
                )
        )

        assertThrows<IntegrasjonException> { integrasjonClient.distribuerBrev("123456789") }
    }

    @Test
    @Tag("integration")
    fun `Ferdigstill oppgave returnerer OK`() {
        MDC.put("callId", "ferdigstillOppgave")
        wireMockServer.stubFor(
            patch(urlEqualTo("/api/oppgave/123/ferdigstill"))
                .withHeader("Accept", containing("json"))
                .willReturn(okJson(objectMapper.writeValueAsString(success(OppgaveResponse(1)))))
        )

        integrasjonClient.ferdigstillOppgave(123)

        wireMockServer.verify(
            patchRequestedFor(urlEqualTo("/api/oppgave/123/ferdigstill"))
                .withHeader(NavHttpHeaders.NAV_CALL_ID.asString(), equalTo("ferdigstillOppgave"))
                .withHeader(NavHttpHeaders.NAV_CONSUMER_ID.asString(), equalTo("familie-ba-sak"))
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
                        .withBody(objectMapper.writeValueAsString(failure<String>("test")))
                )
        )

        assertThatThrownBy {
            integrasjonClient.ferdigstillOppgave(123)
        }.isInstanceOf(IntegrasjonException::class.java)
            .hasMessageContaining("Kan ikke ferdigstille 123")
    }

    @Test
    @Tag("integration")
    fun `hentBehandlendeEnhet returnerer OK`() {
        wireMockServer.stubFor(
            post("/api/arbeidsfordeling/enhet/BAR")
                .withHeader("Accept", containing("json"))
                .willReturn(
                    okJson(
                        objectMapper.writeValueAsString(
                            success(
                                listOf(
                                    Arbeidsfordelingsenhet("2", "foo")
                                )
                            )
                        )
                    )
                )
        )

        val enhet = integrasjonClient.hentBehandlendeEnhet("1")
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
                    objectMapper.writeValueAsString(
                        success(
                            lagTestOppgaveDTO(
                                oppgaveId
                            )
                        )
                    )
                )
            )
        )

        val oppgave = integrasjonClient.finnOppgaveMedId(oppgaveId)
        assertThat(oppgave.id).isEqualTo(oppgaveId)

        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/oppgave/$oppgaveId")))
    }

    @Test
    @Tag("integration")
    fun `hentJournalpost returnerer OK`() {
        val journalpostId = "1234"
        val fnr = randomFnr()
        wireMockServer.stubFor(
            get("/api/journalpost?journalpostId=$journalpostId").willReturn(
                okJson(
                    objectMapper.writeValueAsString(
                        success(
                            lagTestJournalpost(fnr, journalpostId)
                        )
                    )
                )
            )
        )

        val oppgave = integrasjonClient.hentJournalpost(journalpostId)
        assertThat(oppgave.data).isNotNull
        assertThat(oppgave.data?.journalpostId).isEqualTo(journalpostId)
        assertThat(oppgave.data?.bruker?.id).isEqualTo(fnr)

        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/journalpost?journalpostId=$journalpostId")))
    }

    @Test
    @Tag("integration")
    fun `skal hente arbeidsforhold for person`() {
        val fnr = randomFnr()

        val arbeidsforhold = listOf(
            Arbeidsforhold(
                navArbeidsforholdId = Random.nextLong(),
                arbeidstaker = Arbeidstaker("Person", fnr),
                arbeidsgiver = Arbeidsgiver(ArbeidsgiverType.Organisasjon, "998877665"),
                ansettelsesperiode = Ansettelsesperiode(Periode(fom = LocalDate.now().minusYears(1)))
            )
        )

        wireMockServer.stubFor(
            post("/api/aareg/arbeidsforhold").willReturn(
                okJson(
                    objectMapper.writeValueAsString(
                        success(
                            arbeidsforhold
                        )
                    )
                )
            )
        )

        val response = integrasjonClient.hentArbeidsforhold(fnr, LocalDate.now())

        assertThat(response).hasSize(1)
        assertThat(response.first().arbeidstaker?.offentligIdent).isEqualTo(fnr)
        assertThat(response.first().arbeidsgiver?.organisasjonsnummer).isEqualTo("998877665")
        assertThat(response.first().ansettelsesperiode?.periode?.fom).isEqualTo(LocalDate.now().minusYears(1))
    }

    @Test
    @Tag("integration")
    fun `skal kaste integrasjonsfeil mot arbeidsforhold`() {
        val fnr = randomFnr()

        wireMockServer.stubFor(post("/api/aareg/arbeidsforhold").willReturn(status(500)))

        assertThatThrownBy {
            integrasjonClient.hentArbeidsforhold(fnr, LocalDate.now())
        }.isInstanceOf(IntegrasjonException::class.java)
            .hasMessageContaining("Kall mot integrasjon feilet ved henting av arbeidsforhold.")
    }

    @Test
    @Tag("integration")
    fun `skal opprette skyggesak for Sak`() {
        val aktørId = randomAktørId()

        wireMockServer.stubFor(post("/api/skyggesak/v1").willReturn(okJson(objectMapper.writeValueAsString(success(null)))))

        integrasjonClient.opprettSkyggesak(aktørId, MOCK_FAGSAK_ID.toLong())

        wireMockServer.verify(
            postRequestedFor(urlEqualTo("/api/skyggesak/v1"))
                .withRequestBody(
                    equalToJson(
                        objectMapper.writeValueAsString(
                            Skyggesak(
                                aktoerId = aktørId.aktørId,
                                MOCK_FAGSAK_ID,
                                "BAR",
                                "BA"
                            )
                        )
                    )
                )
        )
    }

    @Test
    @Tag("integration")
    fun `skal kaste integrasjonsfeil ved oppretting av skyggesak`() {
        val aktørId = randomAktørId()

        wireMockServer.stubFor(post("/api/skyggesak/v1").willReturn(status(500)))

        assertThatThrownBy {
            integrasjonClient.opprettSkyggesak(aktørId, MOCK_FAGSAK_ID.toLong())
        }.isInstanceOf(IntegrasjonException::class.java)
            .hasMessageContaining("Kall mot integrasjon feilet ved oppretting av skyggesak i Sak for fagsak=$MOCK_FAGSAK_ID")
    }

    private fun journalpostOkResponse(): Ressurs<ArkiverDokumentResponse> {
        return success(ArkiverDokumentResponse(MOCK_JOURNALPOST_FOR_VEDTAK_ID, true))
    }

    private fun forventetRequestArkiverDokument(fagsakId: Long, behandlingId: Long): ArkiverDokumentRequest {
        val vedleggPdf = hentVedlegg(VEDTAK_VEDLEGG_FILNAVN)
        val brev = listOf(
            Dokument(
                dokument = mockPdf,
                filtype = Filtype.PDFA,
                dokumenttype = Dokumenttype.BARNETRYGD_VEDTAK_INNVILGELSE
            )
        )
        val vedlegg = listOf(
            Dokument(
                dokument = vedleggPdf!!,
                filtype = Filtype.PDFA,
                dokumenttype = Dokumenttype.BARNETRYGD_VEDLEGG,
                tittel = VEDTAK_VEDLEGG_TITTEL
            )
        )

        return ArkiverDokumentRequest(
            fnr = MOCK_FNR,
            forsøkFerdigstill = true,
            fagsakId = fagsakId.toString(),
            journalførendeEnhet = "1",
            hoveddokumentvarianter = brev,
            vedleggsdokumenter = vedlegg,
            eksternReferanseId = "${fagsakId}_${behandlingId}_${MDCOperations.getCallId()}"
        )
    }

    companion object {

        const val MOCK_JOURNALPOST_FOR_VEDTAK_ID = "453491843"
        const val MOCK_FNR = "12345678910"
        val mockPdf = "mock data".toByteArray()
        const val MOCK_FAGSAK_ID = "140258931"
    }
}
