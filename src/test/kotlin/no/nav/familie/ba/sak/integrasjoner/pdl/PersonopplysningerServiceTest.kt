package no.nav.familie.ba.sak.integrasjoner.pdl

import com.github.tomakehurst.wiremock.client.WireMock
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

internal class PersonopplysningerServiceTest(
    @Autowired
    @Qualifier("jwtBearer")
    private val restTemplate: RestOperations,

    @Autowired
    private val mockIntegrasjonClient: IntegrasjonClient
) : AbstractSpringIntegrationTest() {

    lateinit var personopplysningerService: PersonopplysningerService

    @BeforeEach
    fun setUp() {
        personopplysningerService =
            PersonopplysningerService(
                PdlRestClient(URI.create(wireMockServer.baseUrl() + "/api"), restTemplate),
                SystemOnlyPdlRestClient(URI.create(wireMockServer.baseUrl() + "/api"), restTemplate, mockk()),
                mockIntegrasjonClient
            )
    }

    @Test
    fun `hentPersoninfoMedRelasjonerOgRegisterinformasjon() skal return riktig personinfo`() {

        every {
            mockIntegrasjonClient.sjekkTilgangTilPersoner(listOf(ID_BARN_1))
        } returns listOf(Tilgang(true, null))
        every {
            mockIntegrasjonClient.sjekkTilgangTilPersoner(listOf(ID_BARN_2))
        } returns listOf(Tilgang(false, null))

        val personInfo = personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(ID_MOR)

        assert(LocalDate.of(1955, 9, 13) == personInfo.fødselsdato)
        assertThat(personInfo.adressebeskyttelseGradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.UGRADERT)
        assertThat(personInfo.forelderBarnRelasjon.size).isEqualTo(1)
        assertThat(personInfo.forelderBarnRelasjonMaskert.size).isEqualTo(1)
    }

    @Test
    fun `hentStatsborgerskap() skal return riktig statsborgerskap`() {
        val statsborgerskap = personopplysningerService.hentGjeldendeStatsborgerskap(Ident(ID_MOR))
        assert(statsborgerskap.land == "XXX")
    }

    @Test
    fun `hentOpphold() skal returnere riktig opphold`() {
        val opphold = personopplysningerService.hentGjeldendeOpphold(ID_MOR)
        assert(opphold.type == OPPHOLDSTILLATELSE.MIDLERTIDIG)
    }

    @Test
    fun `hentLandkodeUtenlandskAdresse() skal returnere landkode `() {
        val landkode = personopplysningerService.hentLandkodeUtenlandskBostedsadresse(ID_MOR)
        assertThat(landkode).isEqualTo("DK")
    }

    @Test
    fun `hentLandkodeUtenlandskAdresse() skal returnere ZZ hvis ingen landkode `() {
        val landkode = personopplysningerService.hentLandkodeUtenlandskBostedsadresse(ID_BARN_1)
        assertThat(landkode).isEqualTo("ZZ")
    }

    @Test
    fun `hentLandkodeUtenlandskAdresse() skal returnere ZZ hvis ingen bostedsadresse `() {
        val landkode = personopplysningerService.hentLandkodeUtenlandskBostedsadresse(ID_MOR_MED_TOM_BOSTEDSADRESSE)
        assertThat(landkode).isEqualTo("ZZ")
    }

    @Test
    fun `hentadressebeskyttelse skal returnere gradering`() {
        val gradering = personopplysningerService.hentAdressebeskyttelseSomSystembruker(ID_BARN_1)
        assertThat(gradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG)
    }

    @Test
    fun `hentadressebeskyttelse skal returnere ugradert ved tom liste fra pdl`() {
        val gradering = personopplysningerService.hentAdressebeskyttelseSomSystembruker(ID_UGRADERT_PERSON)
        assertThat(gradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.UGRADERT)
    }

    @Test
    fun `hentadressebeskyttelse feiler`() {
        assertThrows<Feil> { personopplysningerService.hentAdressebeskyttelseSomSystembruker(ID_MOR) }
    }

    companion object {

        const val ID_MOR = "22345678901"
        const val ID_MOR_MED_TOM_BOSTEDSADRESSE = "22345678903"
        const val ID_BARN_1 = "32345678901"
        const val ID_BARN_2 = "32345678902"
        const val ID_UGRADERT_PERSON = "32345678903"
    }

    private fun gyldigRequest(queryFilnavn: String, requestFilnavn: String): String {
        return readfile(requestFilnavn)
            .replace(
                "GRAPHQL-PLACEHOLDER",
                readfile(queryFilnavn).graphqlCompatible()
            )
    }

    private fun readfile(filnavn: String): String {
        return this::class.java.getResource("/pdl/$filnavn")!!.readText()
    }

    private fun String.graphqlCompatible(): String {
        return StringUtils.normalizeSpace(this.replace("\n", ""))
    }

    private fun lagMockForPdl(graphqlQueryFilnavn: String, requestFilnavn: String, mockResponse: String) {
        wireMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/api/graphql"))
                .withRequestBody(WireMock.equalToJson(gyldigRequest(graphqlQueryFilnavn, requestFilnavn)))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)
                )
        )
    }

    @BeforeAll
    fun lagMockForPersoner() {
        lagMockForPdl(
            "hentperson-med-relasjoner-og-registerinformasjon.graphql",
            "PdlIntegrasjon/gyldigRequestForMorMedXXXStatsborgerskap.json",
            readfile("PdlIntegrasjon/personinfoResponseForMorMedXXXStatsborgerskap.json")
        )

        lagMockForPdl(
            "hentperson-enkel.graphql", "PdlIntegrasjon/gyldigRequestForBarn.json",
            readfile("PdlIntegrasjon/personinfoResponseForBarn.json")
        )

        lagMockForPdl(
            "hentperson-enkel.graphql", "PdlIntegrasjon/gyldigRequestForBarn2.json",
            readfile("PdlIntegrasjon/personinfoResponseForBarnMedAdressebeskyttelse.json")
        )

        lagMockForPdl(
            "statsborgerskap-uten-historikk.graphql", "PdlIntegrasjon/gyldigRequestForMorMedXXXStatsborgerskap.json",
            readfile("PdlIntegrasjon/personinfoResponseForMorMedXXXStatsborgerskap.json")
        )

        lagMockForPdl(
            "opphold-uten-historikk.graphql", "PdlIntegrasjon/gyldigRequestForMorMedXXXStatsborgerskap.json",
            readfile("PdlIntegrasjon/personinfoResponseForMorMedXXXStatsborgerskap.json")
        )

        lagMockForPdl(
            "bostedsadresse-utenlandsk.graphql", "PdlIntegrasjon/gyldigRequestForBostedsadresseperioder.json",
            readfile("PdlIntegrasjon/utenlandskAdresseResponse.json")
        )

        lagMockForPdl(
            "bostedsadresse-utenlandsk.graphql", "PdlIntegrasjon/gyldigRequestForBarn.json",
            readfile("PdlIntegrasjon/personinfoResponseForBarn.json")
        )

        lagMockForPdl(
            "bostedsadresse-utenlandsk.graphql", "PdlIntegrasjon/gyldigRequestForMorMedTomBostedsadresse.json",
            readfile("PdlIntegrasjon/tomBostedsadresseResponse.json")
        )

        lagMockForPdl(
            "hent-adressebeskyttelse.graphql", "PdlIntegrasjon/gyldigRequestForAdressebeskyttelse.json",
            readfile("pdlAdressebeskyttelseResponse.json")
        )

        lagMockForPdl(
            "hent-adressebeskyttelse.graphql", "PdlIntegrasjon/gyldigRequestForAdressebeskyttelse2.json",
            readfile("pdlAdressebeskyttelseResponse.json")
        )

        lagMockForPdl(
            "hent-adressebeskyttelse.graphql", "PdlIntegrasjon/gyldigRequestForAdressebeskyttelse3.json",
            readfile("PdlIntegrasjon/pdlAdressebeskyttelseMedTomListeResponse.json")
        )
    }
}
