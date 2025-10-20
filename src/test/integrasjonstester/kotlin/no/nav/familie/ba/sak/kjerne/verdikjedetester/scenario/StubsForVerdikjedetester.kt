package no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Ansettelsesperiode
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsforhold
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsgiver
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Periode
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlBolkResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonBolk
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonDataBolk
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonInfoQuery
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.FolkeregisteridentifikatorStatus
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.FolkeregisteridentifikatorType
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdresserPerson
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlBaseResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlFolkeregisteridentifikator
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlFødselsDato
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlHentIdenterResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlHentPersonResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlIdenter
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlKjoenn
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlMetadata
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlNavn
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlOppholdPerson
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlOppholdResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonBolkRequest
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonBolkRequestVariables
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonData
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonRequest
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonRequestVariables
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlStatsborgerskapPerson
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlStatsborgerskapResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlVergePerson
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlVergeResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.hentGraphqlQuery
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.ForelderBarnRelasjon
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import java.time.LocalDate

fun stubScenario(scenario: RestScenario) {
    val alleIdenter = scenario.barna.map { it } + scenario.søker
    alleIdenter.forEach {
        stubHentIdenter(it.ident)
        stubHentPersonStatsborgerskap(it)
        stubHentPersonVergemaalEllerFretidfullmakt(it)
        stubHentStatsborgerskap(it)
        stubHentSøknad(it)
        stubHentOppholdstillatelse(it)
    }
    stubHentBostedsadresserOgDeltBostedForPerson(scenario)
    stubHenthentBostedsadresseDeltBostedOgOppholdsadresseForPerson(scenario)
    stubHentPerson(scenario)
    stubHentArbeidsforhold(scenario.søker.ident)
}

private fun stubHentSøknad(restScenarioPerson: RestScenarioPerson) {
    stubFor(
        post(urlEqualTo("/rest/api/"))
            .withRequestBody(WireMock.equalToJson(objectMapper.writeValueAsString("")))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        objectMapper.writeValueAsString(""),
                    ),
            ),
    )
}

private fun stubHentStatsborgerskap(restScenarioPerson: RestScenarioPerson) {
    val response =
        PdlBaseResponse(
            data =
                PdlStatsborgerskapResponse(
                    person =
                        PdlStatsborgerskapPerson(
                            statsborgerskap =
                                restScenarioPerson.statsborgerskap,
                        ),
                ),
            errors = null,
            extensions = null,
        )
    val pdlRequestBody =
        PdlPersonRequest(
            variables = PdlPersonRequestVariables(restScenarioPerson.ident, historikk = true),
            query = hentGraphqlQuery("statsborgerskap"),
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

private fun stubHentOppholdstillatelse(restScenarioPerson: RestScenarioPerson) {
    val response =
        PdlBaseResponse(
            data =
                PdlOppholdResponse(
                    person =
                        PdlOppholdPerson(
                            opphold = restScenarioPerson.oppholdstillatelse,
                        ),
                ),
            errors = null,
            extensions = null,
        )

    val pdlRequestBody =
        PdlPersonRequest(
            variables = PdlPersonRequestVariables(restScenarioPerson.ident, historikk = true),
            query = hentGraphqlQuery("oppholdstillatelse"),
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

private fun stubHentArbeidsforhold(ident: String) {
    val response = listOf(Arbeidsforhold(arbeidsgiver = Arbeidsgiver(organisasjonsnummer = "123456789"), ansettelsesperiode = Ansettelsesperiode(Periode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1)))))

    val ressursResponse = Ressurs.success(response)

    stubFor(
        post(urlEqualTo("/rest/api/integrasjoner/aareg/arbeidsforhold"))
            .withRequestBody(WireMock.matchingJsonPath("$.personIdent", WireMock.equalTo(ident)))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        objectMapper.writeValueAsString(ressursResponse),
                    ),
            ),
    )
}

private fun stubHentBostedsadresserOgDeltBostedForPerson(restScenario: RestScenario) {
    genererAlleKombinasjoner(restScenario.barna + restScenario.søker).forEach { personer ->
        val pdlRequestBody =
            PdlPersonBolkRequest(
                variables = PdlPersonBolkRequestVariables(personer.map { it.ident }),
                query = hentGraphqlQuery("bostedsadresse-og-delt-bosted"),
            )

        val response =
            PdlBolkResponse(
                data =
                    PersonBolk(
                        personBolk =
                            personer.map { person ->
                                PersonDataBolk(
                                    ident = person.ident,
                                    code = "ok",
                                    person =
                                        PdlAdresserPerson(
                                            bostedsadresse = person.bostedsadresser,
                                            deltBosted = emptyList(),
                                        ),
                                )
                            },
                    ),
                errors = null,
                extensions = null,
            )

        stubFor(
            post(urlEqualTo("/rest/api/pdl/graphql"))
                .withRequestBody(WireMock.equalToJson(objectMapper.writeValueAsString(pdlRequestBody), true, true))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(response)),
                ),
        )
    }
}

private fun stubHenthentBostedsadresseDeltBostedOgOppholdsadresseForPerson(restScenario: RestScenario) {
    genererAlleKombinasjoner(restScenario.barna + restScenario.søker).forEach { personer ->
        val pdlRequestBody =
            PdlPersonBolkRequest(
                variables = PdlPersonBolkRequestVariables(personer.map { it.ident }),
                query = hentGraphqlQuery("bostedsadresse-delt-bosted-oppholdsadresse"),
            )

        val response =
            PdlBolkResponse(
                data =
                    PersonBolk(
                        personBolk =
                            personer.map { person ->
                                PersonDataBolk(
                                    ident = person.ident,
                                    code = "ok",
                                    person =
                                        PdlAdresserPerson(
                                            bostedsadresse = person.bostedsadresser,
                                            deltBosted = emptyList(),
                                        ),
                                )
                            },
                    ),
                errors = null,
                extensions = null,
            )

        stubFor(
            post(urlEqualTo("/rest/api/pdl/graphql"))
                .withRequestBody(WireMock.equalToJson(objectMapper.writeValueAsString(pdlRequestBody), true, true))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(response)),
                ),
        )
    }
}

private fun stubHentIdenter(personIdent: String) {
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

private fun stubHentPerson(scenario: RestScenario) {
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
                    metadata = PdlMetadata(master = "kilde", historisk = false),
                ),
            ),
        kjoenn = listOf(PdlKjoenn(kjoenn = Kjønn.KVINNE, metadata = PdlMetadata(master = "kilde", historisk = false))),
        adressebeskyttelse = emptyList(),
        sivilstand = listOf(Sivilstand(type = SIVILSTANDTYPE.UGIFT)),
        bostedsadresse = scenarioPerson.bostedsadresser,
        oppholdsadresse = emptyList(),
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
            query = hentGraphqlQuery("statsborgerskap"),
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

private fun <T> genererAlleKombinasjoner(elementer: List<T>): List<List<T>> =
    (1 until (1 shl elementer.size)).map { i ->
        elementer.filterIndexed { index, _ ->
            (i shr index) and 1 == 1
        }
    }
