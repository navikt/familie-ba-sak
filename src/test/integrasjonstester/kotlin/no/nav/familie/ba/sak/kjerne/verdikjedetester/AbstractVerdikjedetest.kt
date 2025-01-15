package no.nav.familie.ba.sak.kjerne.verdikjedetester

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonInfoQuery
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.FolkeregisteridentifikatorStatus
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.FolkeregisteridentifikatorType
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlBaseResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlFolkeregisteridentifikator
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlFødselsDato
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlHentIdenterResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlHentPersonResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlIdenter
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlKjoenn
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlNavn
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonData
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonRequest
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonRequestVariables
import no.nav.familie.ba.sak.integrasjoner.pdl.hentGraphqlQuery
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.MockserverKlient
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.ForelderBarnRelasjon
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.support.TestPropertySourceUtils
import org.springframework.web.client.RestOperations
import org.testcontainers.containers.FixedHostPortGenericContainer
import org.testcontainers.images.PullPolicy

val MOCK_SERVER_IMAGE = "europe-north1-docker.pkg.dev/nais-management-233d/teamfamilie/familie-mock-server:latest"

class VerdikjedetesterPropertyOverrideContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext?> {
    override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            configurableApplicationContext,
            "PDL_URL: http://localhost:1338/rest/api/pdl",
        )
        val brukLokalMockserver = System.getProperty("brukLokalMockserver")?.toBoolean() ?: false
        if (!brukLokalMockserver) {
            mockServer.start()
        }
    }

    companion object {
        // Lazy because we only want it to be initialized when accessed
        val mockServer: KMockServerContainer by lazy {
            val mockServer = KMockServerContainer(MOCK_SERVER_IMAGE)
            mockServer.withExposedPorts(1337)
            mockServer.withFixedExposedPort(1337, 1337)
            mockServer.withImagePullPolicy(PullPolicy.alwaysPull())
            mockServer
        }
    }
}

@ActiveProfiles(
    "postgres",
    "integrasjonstest",
    "mock-oauth",
    "mock-localdate-service",
    "mock-tilbakekreving-klient",
    "mock-brev-klient",
    "mock-økonomi",
    "mock-infotrygd-feed",
    "mock-rest-template-config",
    "mock-task-repository",
    "mock-task-service",
    "mock-sanity-client",
    "mock-unleash",
    "mock-infotrygd-barnetrygd",
)
@ContextConfiguration(initializers = [VerdikjedetesterPropertyOverrideContextInitializer::class])
@Tag("verdikjedetest")
@AutoConfigureWireMock(port = 1338)
abstract class AbstractVerdikjedetest : WebSpringAuthTestRunner() {
    @AfterAll
    fun tearDownSuper() {
        WireMock.reset()
    }

    @Autowired
    lateinit var restOperations: RestOperations

    fun familieBaSakKlient(): FamilieBaSakKlient =
        FamilieBaSakKlient(
            baSakUrl = hentUrl(""),
            restOperations = restOperations,
            headers = hentHeadersForSystembruker(),
        )

    fun mockServerKlient(): MockserverKlient =
        MockserverKlient(
            mockServerUrl = "http://localhost:1337",
            restOperations = restOperations,
        )

    fun stubHentIdenter(personIdent: String) {
        val response =
            PdlBaseResponse(
                data =
                    PdlHentIdenterResponse(
                        pdlIdenter =
                            PdlIdenter(
                                identer =
                                    listOf(
                                        IdentInformasjon(
                                            ident = "$personIdent",
                                            historisk = false,
                                            gruppe = "FOLKEREGISTERIDENT",
                                        ),
                                        IdentInformasjon(
                                            ident = "${personIdent}99",
                                            historisk = false,
                                            gruppe = "AKTORID",
                                        ),
                                    ),
                            ),
                    ),
                errors = null,
                extensions = null,
            )

        val pdlRequestBody =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(personIdent),
                query = hentGraphqlQuery("hentIdenter"),
            )

        stubFor(
            post(urlEqualTo("/rest/api/pdl/graphql"))
                .withRequestBody(WireMock.equalToJson(objectMapper.writeValueAsString(pdlRequestBody)))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(response),
                        ),
                ),
        )
    }

    fun stubHentPerson(scenario: RestScenario) {
        stubHentPersonEnkel(scenario.søker)
        scenario.barna.forEach { stubHentPersonEnkel(it) }
        stubHentPersonMedRelasjonSøker(scenario)
        scenario.barna.forEach { barn -> stubHentPersonMedRelasjonBarn(barn, scenario.søker) }
    }

    private fun stubHentPersonMedRelasjonSøker(scenario: RestScenario) {
        val response =
            PdlBaseResponse(
                data =
                    PdlHentPersonResponse(
                        person =
                            enkelPdlHentPersonResponse(scenario.søker).copy(
                                forelderBarnRelasjon =
                                    scenario.barna.map { barn ->
                                        ForelderBarnRelasjon(
                                            relatertPersonsIdent = barn.ident!!,
                                            relatertPersonsRolle = FORELDERBARNRELASJONROLLE.BARN,
                                        )
                                    },
                                statsborgerskap =
                                    listOf(
                                        Statsborgerskap(
                                            land = "NOR",
                                            gyldigFraOgMed = null,
                                            gyldigTilOgMed = null,
                                            bekreftelsesdato = null,
                                        ),
                                    ),
                            ),
                    ),
                errors = null,
                extensions = null,
            )

        val pdlRequestBody =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(ident = scenario.søker.ident!!),
                query = PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON.graphQL,
            )

        stubFor(
            post(urlEqualTo("/rest/api/pdl/graphql"))
                .withRequestBody(WireMock.equalToJson(objectMapper.writeValueAsString(pdlRequestBody)))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(response),
                        ),
                ),
        )
    }

    private fun stubHentPersonMedRelasjonBarn(
        barn: RestScenarioPerson,
        søker: RestScenarioPerson,
    ) {
        val response =
            PdlBaseResponse(
                data =
                    PdlHentPersonResponse(
                        person =
                            enkelPdlHentPersonResponse(barn).copy(
                                forelderBarnRelasjon =
                                    listOf(
                                        ForelderBarnRelasjon(
                                            relatertPersonsIdent = søker.ident!!,
                                            relatertPersonsRolle = FORELDERBARNRELASJONROLLE.MOR,
                                        ),
                                    ),
                                statsborgerskap =
                                    listOf(
                                        Statsborgerskap(
                                            land = "NOR",
                                            gyldigFraOgMed = null,
                                            gyldigTilOgMed = null,
                                            bekreftelsesdato = null,
                                        ),
                                    ),
                            ),
                    ),
                errors = null,
                extensions = null,
            )

        val pdlRequestBody =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(ident = barn.ident!!),
                query = PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON.graphQL,
            )

        stubFor(
            post(urlEqualTo("/rest/api/pdl/graphql"))
                .withRequestBody(WireMock.equalToJson(objectMapper.writeValueAsString(pdlRequestBody)))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(response),
                        ),
                ),
        )
    }

    private fun enkelPdlHentPersonResponse(scenarioPerson: RestScenarioPerson): PdlPersonData =
        PdlPersonData(
            folkeregisteridentifikator =
                listOf(
                    PdlFolkeregisteridentifikator(
                        identifikasjonsnummer = scenarioPerson.ident,
                        status = FolkeregisteridentifikatorStatus.I_BRUK,
                        type = FolkeregisteridentifikatorType.FNR,
                    ),
                ),
            foedselsdato =
                listOf(
                    PdlFødselsDato(
                        foedselsdato = scenarioPerson.fødselsdato,
                    ),
                ),
            navn =
                listOf(
                    PdlNavn(
                        fornavn = scenarioPerson.fornavn,
                        mellomnavn = null,
                        etternavn = scenarioPerson.etternavn,
                    ),
                ),
            kjoenn = listOf(PdlKjoenn(kjoenn = Kjønn.KVINNE)),
            adressebeskyttelse = emptyList(),
            sivilstand = listOf(Sivilstand(type = SIVILSTANDTYPE.UGIFT)),
            bostedsadresse =
                listOf(
                    Bostedsadresse(
                        angittFlyttedato = null,
                        gyldigTilOgMed = null,
                        vegadresse =
                            Vegadresse(
                                matrikkelId = 100L,
                                husnummer = "3",
                                husbokstav = null,
                                bruksenhetsnummer = "H111",
                                adressenavn = "OTTO SVERDRUPS VEG",
                                kommunenummer = "1566",
                                postnummer = "6650",
                                tilleggsnavn = null,
                            ),
                    ),
                ),
            doedsfall = emptyList(),
            kontaktinformasjonForDoedsbo = emptyList(),
        )

    private fun stubHentPersonEnkel(scenarioPerson: RestScenarioPerson) {
        val response =
            PdlBaseResponse(
                data =
                    PdlHentPersonResponse(
                        person =
                            enkelPdlHentPersonResponse(scenarioPerson),
                    ),
                errors = null,
                extensions = null,
            )

        val pdlRequestBody =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(ident = scenarioPerson.ident!!),
                query = PersonInfoQuery.ENKEL.graphQL,
            )

        stubFor(
            post(urlEqualTo("/rest/api/pdl/graphql"))
                .withRequestBody(WireMock.equalToJson(objectMapper.writeValueAsString(pdlRequestBody)))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(response),
                        ),
                ),
        )
    }
}

/**
 * Hack needed because testcontainers use of generics confuses Kotlin.
 * Må bruke fixed host port for at klientene våres kan konfigureres med fast port.
 */
class KMockServerContainer(
    imageName: String,
) : FixedHostPortGenericContainer<KMockServerContainer>(imageName)
