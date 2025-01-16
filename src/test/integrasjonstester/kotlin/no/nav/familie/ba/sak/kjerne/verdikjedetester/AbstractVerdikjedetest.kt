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
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlStatsborgerskapPerson
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlStatsborgerskapResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlVergePerson
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlVergeResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.hentGraphqlQuery
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.ForelderBarnRelasjon
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
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

class VerdikjedetesterPropertyOverrideContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext?> {
    override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            configurableApplicationContext,
            "PDL_URL: http://localhost:1338/rest/api/pdl",
        )
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

    fun stubScenario(scenario: RestScenario) {
        val alleIdenter = scenario.barna.map { it } + scenario.søker
        alleIdenter.forEach {
            stubHentIdenter(it.ident)
            stubHentPersonStatsborgerskap(it)
            stubHentPersonVergemaalEllerFretidfullmakt(it)
        }
        stubHentPerson(scenario)
    }

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
                                            ident = personIdent,
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
                                            relatertPersonsIdent = barn.ident,
                                            relatertPersonsRolle = FORELDERBARNRELASJONROLLE.BARN,
                                        )
                                    },
                                statsborgerskap = scenario.søker.statsborgerskap,
                            ),
                    ),
                errors = null,
                extensions = null,
            )

        val pdlRequestBody =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(ident = scenario.søker.ident),
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
                                            relatertPersonsIdent = søker.ident,
                                            relatertPersonsRolle = FORELDERBARNRELASJONROLLE.MOR,
                                        ),
                                    ),
                                statsborgerskap = søker.statsborgerskap,
                            ),
                    ),
                errors = null,
                extensions = null,
            )

        val pdlRequestBody =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(ident = barn.ident),
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
            bostedsadresse = scenarioPerson.bostedsadresser,
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
                variables = PdlPersonRequestVariables(ident = scenarioPerson.ident),
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

    private fun stubHentPersonStatsborgerskap(scenarioPerson: RestScenarioPerson) {
        val response =
            PdlBaseResponse(
                data =
                    PdlStatsborgerskapResponse(
                        person =
                            PdlStatsborgerskapPerson(
                                statsborgerskap = scenarioPerson.statsborgerskap,
                            ),
                    ),
                errors = null,
                extensions = null,
            )

        val pdlRequestBody =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(ident = scenarioPerson.ident),
                query = hentGraphqlQuery("statsborgerskap-uten-historikk"),
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

    private fun stubHentPersonVergemaalEllerFretidfullmakt(scenarioPerson: RestScenarioPerson) {
        val response =
            PdlBaseResponse(
                data =
                    PdlVergeResponse(
                        person =
                            PdlVergePerson(
                                vergemaalEllerFremtidsfullmakt = emptyList(),
                            ),
                    ),
                errors = null,
                extensions = null,
            )

        val pdlRequestBody =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(ident = scenarioPerson.ident),
                query = hentGraphqlQuery("verge"),
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
