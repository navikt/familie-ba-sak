package no.nav.familie.ba.sak.integrasjoner

import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient.Companion.VEDLEGG_DOKUMENT_TYPE
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient.Companion.VEDTAK_DOKUMENT_TYPE
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient.Companion.VEDTAK_VEDLEGG_FILNAVN
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient.Companion.VEDTAK_VEDLEGG_TITTEL
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient.Companion.hentVedlegg
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.oppgave.FinnOppgaveRequest
import no.nav.familie.ba.sak.oppgave.OppgaverOgAntall
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Companion.failure
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.familie.kontrakter.felles.arkivering.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.arkivering.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.arkivering.Dokument
import no.nav.familie.kontrakter.felles.arkivering.FilType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.log.NavHttpHeaders
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDate


@SpringBootTest(classes = [ApplicationConfig::class], properties = ["FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085/api"])
@ActiveProfiles("dev", "integrasjonstest", "mock-oauth")
@AutoConfigureWireMock(port = 28085)
@TestInstance(Lifecycle.PER_CLASS)
class IntergrasjonTjenesteTest {

    @Autowired
    @Qualifier("integrasjonClient")
    lateinit var integrasjonClient: IntegrasjonClient

    @AfterEach
    fun cleanUp() {
        MDC.clear()
        resetAllRequests()
    }


    @Test
    @Tag("integration")
    fun `Opprett oppgave skal returnere oppgave id`() {
        MDC.put("callId", "opprettOppgave")
        stubFor(post("/api/oppgave/").willReturn(
                okJson(objectMapper.writeValueAsString(success(OppgaveResponse(oppgaveId = 1234))))))

        val request = lagTestOppgave()

        val opprettOppgaveResponse = integrasjonClient.opprettOppgave(request)

        assertThat(opprettOppgaveResponse).isEqualTo("1234")
        verify(anyRequestedFor(anyUrl())
                       .withHeader(NavHttpHeaders.NAV_CALL_ID.asString(), equalTo("opprettOppgave"))
                       .withHeader(NavHttpHeaders.NAV_CONSUMER_ID.asString(), equalTo("familie-ba-sak"))
                       .withRequestBody(equalToJson(objectMapper.writeValueAsString(request))))
    }

    @Test
    @Tag("integration")
    fun `Hent indenter skal returnere liste av identer`() {
        stubFor(post("/api/identer/BAR/historikk").willReturn(
                okJson(objectMapper.writeValueAsString(success(listOf(IdentInformasjon("1234", false, "AKTORID")))))))

        val identerResponse = integrasjonClient.hentIdenter("12345678910")

        assertThat(identerResponse!!.first().ident).isEqualTo("1234")
        verify(anyRequestedFor(anyUrl())
                       .withRequestBody(equalTo("\"12345678910\"")))
    }

    @Test
    @Tag("integration")
    fun `Hent indenter skal feile ved kall uten ident`() {
        assertThatThrownBy {
            integrasjonClient.hentIdenter("")
        }.isInstanceOf(IntegrasjonException::class.java)
                .hasMessageContaining("Ved henting av identer er ident null eller tom")
    }

    @Test
    @Tag("integration")
    fun `Opprett oppgave skal kaste feil hvis response er ugyldig`() {
        stubFor(post("/api/oppgave/").willReturn(aResponse()
                                                         .withStatus(500)
                                                         .withBody(objectMapper.writeValueAsString(failure<String>("test")))))

        assertThatThrownBy {
            integrasjonClient.opprettOppgave(lagTestOppgave())
        }.isInstanceOf(IntegrasjonException::class.java)
                .hasMessageContaining("Kall mot integrasjon feilet ved opprett oppgave")


    }

    @Test
    @Tag("integration")
    fun `hentOppgaver skal returnere en liste av oppgaver og antallet oppgaver`() {
        val oppgave = Oppgave()
        stubFor(post("/api/oppgave/v2").willReturn(okJson(objectMapper.writeValueAsString(
                success(OppgaverOgAntall(1, listOf(oppgave)))))))

        val oppgaverOgAntall = integrasjonClient.hentOppgaver(FinnOppgaveRequest())
        assertThat(oppgaverOgAntall.oppgaver).hasSize(1)
    }

    @Test
    @Tag("integration")
    fun `Journalfør vedtaksbrev skal journalføre dokument, returnere 201 og journalpostId`() {
        MDC.put("callId", "journalfør")
        stubFor(post("/api/arkiv/v2")
                        .withHeader("Accept", containing("json"))
                        .willReturn(aResponse()
                                            .withStatus(201)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(journalpostOkResponse()))))

        val vedtak = lagVedtak(lagBehandling())
        vedtak.stønadBrevPdF = mockPdf
        vedtak.ansvarligEnhet = "1"

        val journalPostId = integrasjonClient.lagJournalpostForVedtaksbrev(mockFnr, mockFagsakId, vedtak)

        assertThat(journalPostId).isEqualTo(mockJournalpostForVedtakId)
        verify(anyRequestedFor(anyUrl())
                       .withHeader(NavHttpHeaders.NAV_CALL_ID.asString(), equalTo("journalfør"))
                       .withHeader(NavHttpHeaders.NAV_CONSUMER_ID.asString(), equalTo("familie-ba-sak"))
                       .withRequestBody(equalToJson(objectMapper.writeValueAsString(forventetRequestArkiverDokument()))))
    }


    @Test
    @Tag("integration")
    fun `distribuerVedtaksbrev returnerer normalt ved vellykket integrasjonskall`() {
        MDC.put("callId", "distribuerVedtaksbrev")
        stubFor(post("/api/dist/v1")
                        .withHeader("Accept", containing("json"))
                        .willReturn(okJson(objectMapper.writeValueAsString(success("1234567")))))



        assertDoesNotThrow { integrasjonClient.distribuerVedtaksbrev("123456789") }
        verify(postRequestedFor(anyUrl())
                       .withHeader(NavHttpHeaders.NAV_CALL_ID.asString(), equalTo("distribuerVedtaksbrev"))
                       .withHeader(NavHttpHeaders.NAV_CONSUMER_ID.asString(), equalTo("familie-ba-sak"))
                       .withRequestBody(equalToJson("{\"journalpostId\":\"123456789\"," +
                                                    "\"bestillendeFagsystem\":\"BA\"," +
                                                    "\"dokumentProdApp\":\"FAMILIE_BA_SAK\"}")))
    }

    @Test
    @Tag("integration")
    fun `distribuerVedtaksbrev kaster exception hvis integrasjoner gir blank response`() {
        stubFor(post("/api/dist/v1")
                        .withHeader("Accept", containing("json"))
                        .willReturn(okJson(objectMapper.writeValueAsString(success("")))))

        assertThrows<IllegalStateException> { integrasjonClient.distribuerVedtaksbrev("123456789") }
    }

    @Test
    @Tag("integration")
    fun `distribuerVedtaksbrev kaster exception hvis integrasjoner gir failure response`() {
        stubFor(post("/api/dist/v1")
                        .withHeader("Accept", containing("json"))
                        .willReturn(okJson(objectMapper.writeValueAsString(failure<Any>("")))))

        assertThrows<IllegalStateException> { integrasjonClient.distribuerVedtaksbrev("123456789") }
    }

    @Test
    @Tag("integration")
    fun `distribuerVedtaksbrev kaster exception hvis responsekoden ikke er 2xx`() {
        stubFor(post("/api/dist/v1")
                        .withHeader("Accept", containing("json"))
                        .willReturn(aResponse()
                                            .withStatus(400)
                                            .withHeader("Content-Type", "application/json")))

        assertThrows<IntegrasjonException> { integrasjonClient.distribuerVedtaksbrev("123456789") }
    }


    @Test
    @Tag("integration")
    fun `Ferdigstill oppgave returnerer OK`() {
        MDC.put("callId", "ferdigstillOppgave")
        stubFor(patch(urlEqualTo("/api/oppgave/123/ferdigstill"))
                        .withHeader("Accept", containing("json"))
                        .willReturn(okJson(objectMapper.writeValueAsString(success(OppgaveResponse(1))))))

        integrasjonClient.ferdigstillOppgave(123)

        verify(patchRequestedFor(urlEqualTo("/api/oppgave/123/ferdigstill"))
                       .withHeader(NavHttpHeaders.NAV_CALL_ID.asString(), equalTo("ferdigstillOppgave"))
                       .withHeader(NavHttpHeaders.NAV_CONSUMER_ID.asString(), equalTo("familie-ba-sak")))
    }

    @Test
    @Tag("integration")
    fun `Ferdigstill oppgave returnerer feil `() {
        MDC.put("callId", "ferdigstillOppgave")
        stubFor(patch(urlEqualTo("/api/oppgave/123/ferdigstill"))
                        .withHeader("Accept", containing("json"))
                        .willReturn(aResponse()
                                            .withStatus(400)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(objectMapper.writeValueAsString(failure<String>("test")))))



        assertThatThrownBy {
            integrasjonClient.ferdigstillOppgave(123)
        }.isInstanceOf(IntegrasjonException::class.java)
                .hasMessageContaining("Kan ikke ferdigstille 123")
    }

    @Test
    @Tag("integration")
    fun `hentBehandlendeEnhet returnerer OK`() {
        stubFor(get("/api/arbeidsfordeling/enhet/BAR")
                        .withHeader("Accept", containing("json"))
                        .willReturn(okJson(objectMapper.writeValueAsString(success(listOf(
                                Arbeidsfordelingsenhet("2", "foo")))))))

        val enhet = integrasjonClient.hentBehandlendeEnhet("1")
        assertThat(enhet).isNotEmpty
        assertThat(enhet.first().enhetId).isEqualTo("2")
    }

    @Test
    @Tag("integration")
    fun `hentAktør returnerer OK`() {
        stubFor(get("/api/aktoer/v1").willReturn(okJson(objectMapper.writeValueAsString(success(mapOf("aktørId" to 1L))))))

        val aktørId = integrasjonClient.hentAktørId("12")
        assertThat(aktørId.id).isEqualTo("1")

        verify(getRequestedFor(urlEqualTo("/api/aktoer/v1"))
                       .withHeader("Nav-Personident", equalTo("12")))
    }

    @Test
    @Tag("integration")
    fun `hentPersonIdent returnerer OK`() {
        stubFor(get("/api/aktoer/v1/fraaktorid").willReturn(okJson(objectMapper.writeValueAsString(success(mapOf("personIdent" to 1L))))))

        val personIdent = integrasjonClient.hentPersonIdent("12")
        assertThat(personIdent?.ident).isEqualTo("1")

        verify(getRequestedFor(urlEqualTo("/api/aktoer/v1/fraaktorid"))
                       .withHeader("Nav-Aktorid", equalTo("12")))
    }

    @Test
    @Tag("integration")
    fun `finnOppgaveMedId returnerer OK`() {
        val oppgaveId = 1234L
        stubFor(get("/api/oppgave/$oppgaveId").willReturn(okJson(objectMapper.writeValueAsString(success(lagTestOppgaveDTO(
                oppgaveId))))))

        val oppgave = integrasjonClient.finnOppgaveMedId(oppgaveId)
        assertThat(oppgave.data).isNotNull
        assertThat(oppgave.data?.id).isEqualTo(oppgaveId)

        verify(getRequestedFor(urlEqualTo("/api/oppgave/$oppgaveId")))
    }

    @Test
    @Tag("integration")
    fun `hentJournalpost returnerer OK`() {
        val journalpostId = "1234"
        val fnr = randomFnr()
        stubFor(get("/api/journalpost?journalpostId=$journalpostId").willReturn(okJson(objectMapper.writeValueAsString(success(
                lagTestJournalpost(fnr, journalpostId))))))

        val oppgave = integrasjonClient.hentJournalpost(journalpostId)
        assertThat(oppgave.data).isNotNull
        assertThat(oppgave.data?.journalpostId).isEqualTo(journalpostId)
        assertThat(oppgave.data?.bruker?.id).isEqualTo(fnr)

        verify(getRequestedFor(urlEqualTo("/api/journalpost?journalpostId=$journalpostId")))
    }


    @Test
    @Tag("integration")
    fun `hentPerson returnerer OK`() {
        stubFor(get(urlMatching("/api/personopplysning/v1/info")).willReturn(
                okJson(objectMapper.writeValueAsString(success(Personinfo(fødselsdato = LocalDate.now()))))))
        stubFor(get(urlMatching("/api/personopplysning/v1/info/BAR")).willReturn(
                okJson(objectMapper.writeValueAsString(success(Personinfo(fødselsdato = LocalDate.now()))))))

        val personinfo = integrasjonClient.hentPersoninfoFor("12")
        assertThat(personinfo.fødselsdato).isEqualTo(LocalDate.now())

        verify(getRequestedFor(urlEqualTo("/api/personopplysning/v1/info/BAR"))
                       .withHeader("Nav-Personident", equalTo("12")))
    }

    @Test
    @Tag("integration")
    fun `hentPerson ikke funet`() {
        stubFor(get(urlMatching("/api/personopplysning/v1/info")).willReturn(aResponse().withStatus(404)))
        stubFor(get(urlMatching("/api/personopplysning/v1/info/BAR")).willReturn(aResponse().withStatus(404)))

        assertThrows<HttpClientErrorException> {
            integrasjonClient.hentPersoninfoFor("12345678910")
        }
    }

    @Test
    @Tag("integration")
    fun `hentPerson feil`() {
        stubFor(get(urlMatching("/api/personopplysning/v1/info")).willReturn(aResponse().withStatus(400)))
        stubFor(get(urlMatching("/api/personopplysning/v1/info/BAR")).willReturn(aResponse().withStatus(400)))

        assertThrows<IntegrasjonException> {
            integrasjonClient.hentPersoninfoFor("12345678910")
        }
    }

    private fun journalpostOkResponse(): Ressurs<ArkiverDokumentResponse> {
        return success(ArkiverDokumentResponse(mockJournalpostForVedtakId, true))
    }

    private fun forventetRequestArkiverDokument(): ArkiverDokumentRequest {
        val vedleggPdf = hentVedlegg(VEDTAK_VEDLEGG_FILNAVN)
        return ArkiverDokumentRequest(fnr = mockFnr,
                                      forsøkFerdigstill = true,
                                      fagsakId = mockFagsakId,
                                      journalførendeEnhet = "1",
                                      dokumenter = listOf(Dokument(dokument = mockPdf,
                                                                   filType = FilType.PDFA,
                                                                   dokumentType = VEDTAK_DOKUMENT_TYPE),
                                                          Dokument(dokument = vedleggPdf!!,
                                                                   filType = FilType.PDFA,
                                                                   dokumentType = VEDLEGG_DOKUMENT_TYPE,
                                                                   tittel = VEDTAK_VEDLEGG_TITTEL)))
    }

    companion object {
        const val mockJournalpostForVedtakId = "453491843"
        const val mockFnr = "12345678910"
        val mockPdf = "mock data".toByteArray()
        const val mockFagsakId = "140258931"
    }

}
