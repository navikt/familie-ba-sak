package no.nav.familie.ba.sak.integrasjoner

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.arkivering.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.arkivering.ArkiverDokumentResponse
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestTemplate
import java.net.URI

@Tag("integration")
class IntegrasjonTjenesteTest{

    @MockK
    lateinit var restTemplateMock: RestTemplate

    @MockK
    lateinit var restTemplateBuilderMock: RestTemplateBuilder

    @MockK
    lateinit var clientConfigurationProfilesMock: ClientConfigurationProperties

    @MockK
    lateinit var oAuth2AccessTokenServiceMock: OAuth2AccessTokenService

    @MockK
    lateinit var clientProperties: ClientProperties

    lateinit private var integrasjonTjeneste: IntegrasjonTjeneste

    val mockServiceUri= "mock.integrasjoner.uri"
    val mockFnr= "12345678910"
    val mockPdf= "pdf data".toByteArray()

    @BeforeEach
    fun setUp()= MockKAnnotations.init(this)

    @Test
    fun `test å lager journalpost for vedtaksbrev`(){
        every{restTemplateBuilderMock.additionalInterceptors(any<ClientHttpRequestInterceptor>())}.answers{restTemplateBuilderMock}
        every{restTemplateBuilderMock.build()}.answers{restTemplateMock}

        every{clientConfigurationProfilesMock.getRegistration()}.answers{
            mapOf(IntegrasjonTjeneste.OAUTH2_CLIENT_CONFIG_KEY to clientProperties) }

        integrasjonTjeneste= IntegrasjonTjeneste(URI.create(mockServiceUri), restTemplateBuilderMock,
                clientConfigurationProfilesMock, oAuth2AccessTokenServiceMock)

        val endpointSlot= slot<URI>()
        val methodSlot= slot<HttpMethod>()
        val entitySlot= slot<HttpEntity<Any>>()

        val mockJournalpostForVedtakId= "453491843"
        every{restTemplateMock.exchange(capture(endpointSlot), capture(methodSlot), capture(entitySlot), any<Class<*>>())}
                .answers{ResponseEntity<Ressurs<ArkiverDokumentResponse>>(
                        Ressurs<ArkiverDokumentResponse>(
                                data= ArkiverDokumentResponse(mockJournalpostForVedtakId, true),
                                status= Ressurs.Status.SUKSESS,
                                melding = "",
                                stacktrace = ""
                                ),
                        HttpStatus.CREATED)}

        val journpostForVedtakId= integrasjonTjeneste.lagerJournalpostForVedtaksbrev(mockFnr, mockPdf)

        val expectedURI= URI.create("$mockServiceUri/arkiv/v1")

        assert(endpointSlot.captured.equals(expectedURI))
        assert(methodSlot.captured.equals(HttpMethod.POST))
        val arkiverDokumentRequest= entitySlot.captured.body as ArkiverDokumentRequest
        assert(arkiverDokumentRequest.fnr.equals(mockFnr))
        assert(arkiverDokumentRequest.dokumenter.size.equals(1))
        assert(arkiverDokumentRequest.dokumenter[0].dokumentType.equals(IntegrasjonTjeneste.VEDTAK_DOKUMENT_TYPE))
        assert(arkiverDokumentRequest.dokumenter[0].filType.equals(IntegrasjonTjeneste.VEDTAK_FILTYPE))
        assert(arkiverDokumentRequest.dokumenter[0].filnavn.equals(IntegrasjonTjeneste.VEDTAK_FILNAVN))
        assert(arkiverDokumentRequest.dokumenter[0].dokument.equals(mockPdf))
        assert(journpostForVedtakId.equals(mockJournalpostForVedtakId))
    }
 }