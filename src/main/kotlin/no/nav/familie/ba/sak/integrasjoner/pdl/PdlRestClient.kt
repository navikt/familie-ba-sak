package no.nav.familie.ba.sak.integrasjoner.pdl

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.Doedsfall
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PdlDødsfallResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PdlHentIdenterResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PdlHentPersonRelasjonerResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PdlHentPersonResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PdlOppholdResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PdlPersonRequest
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PdlPersonRequestVariables
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PdlStatsborgerskapResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PdlUtenlandskAdressseResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PdlVergeResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.Personident
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.VergemaalEllerFremtidsfullmakt
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.http.util.UriUtil
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.NestedExceptionUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

@Service
class PdlRestClient(
    @Value("\${PDL_URL}") pdlBaseUrl: URI,
    @Qualifier("jwtBearer") val restTemplate: RestOperations
) :
    AbstractRestClient(restTemplate, "pdl.personinfo") {

    protected val pdlUri = UriUtil.uri(pdlBaseUrl, PATH_GRAPHQL)

    private val hentIdenterQuery = hentGraphqlQuery("hentIdenter")

    fun hentPerson(personIdent: String, personInfoQuery: PersonInfoQuery): PersonInfo {

        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(personIdent),
            query = personInfoQuery.graphQL
        )
        try {
            val response = postForEntity<PdlHentPersonResponse>(
                pdlUri,
                pdlPersonRequest,
                httpHeaders()
            )
            if (!response.harFeil()) {
                return Result.runCatching {
                    val forelderBarnRelasjon: Set<ForelderBarnRelasjon> =
                        when (personInfoQuery) {
                            PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON -> {
                                response.data.person!!.forelderBarnRelasjon.map { relasjon ->
                                    ForelderBarnRelasjon(
                                        personIdent = Personident(id = relasjon.relatertPersonsIdent),
                                        relasjonsrolle = relasjon.relatertPersonsRolle
                                    )
                                }.toSet()
                            }
                            else -> emptySet()
                        }
                    response.data.person!!.let {
                        PersonInfo(
                            fødselsdato = LocalDate.parse(it.foedsel.first().foedselsdato!!),
                            navn = it.navn.firstOrNull()?.fulltNavn(),
                            kjønn = it.kjoenn.firstOrNull()?.kjoenn,
                            forelderBarnRelasjon = forelderBarnRelasjon,
                            adressebeskyttelseGradering = it.adressebeskyttelse.firstOrNull()?.gradering,
                            bostedsadresser = it.bostedsadresse,
                            statsborgerskap = it.statsborgerskap,
                            opphold = it.opphold,
                            sivilstander = it.sivilstand
                        )
                    }
                }.fold(
                    onSuccess = { it },
                    onFailure = {
                        throw Feil(
                            message = "Fant ikke forespurte data på person.",
                            frontendFeilmelding = "Kunne ikke slå opp data for person $personIdent",
                            httpStatus = HttpStatus.NOT_FOUND,
                            throwable = it
                        )
                    }
                )
            } else {
                throw Feil(
                    message = "Feil ved oppslag på person: ${response.errorMessages()}",
                    frontendFeilmelding = "Feil ved oppslag på person $personIdent: ${response.errorMessages()}",
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR
                )
            }
        } catch (e: Exception) {
            val mostSpecificThrowable = NestedExceptionUtils.getMostSpecificCause(e)

            when (e) {
                is Feil -> throw e
                else -> throw Feil(
                    message = "Feil ved oppslag på person. Gav feil: ${mostSpecificThrowable.message}.",
                    frontendFeilmelding = "Feil oppsto ved oppslag på person $personIdent",
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                    throwable = mostSpecificThrowable
                )
            }
        }
    }

    fun hentIdenter(personIdent: String): PdlHentIdenterResponse {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(personIdent),
            query = hentIdenterQuery
        )
        val response = postForEntity<PdlHentIdenterResponse>(
            pdlUri,
            pdlPersonRequest,
            httpHeaders()
        )

        if (!response.harFeil()) return response
        throw Feil(
            message = "Fant ikke identer for person: ${response.errorMessages()}",
            frontendFeilmelding = "Fant ikke identer for person $personIdent: ${response.errorMessages()}",
            httpStatus = HttpStatus.NOT_FOUND
        )
    }

    fun hentDødsfall(personIdent: String): List<Doedsfall> {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(personIdent),
            query = hentGraphqlQuery("doedsfall")
        )
        val response = try {
            postForEntity<PdlDødsfallResponse>(pdlUri, pdlPersonRequest, httpHeaders())
        } catch (e: Exception) {
            throw Feil(
                message = "Feil ved oppslag på person. Gav feil: ${e.message}",
                frontendFeilmelding = "Feil oppsto ved oppslag på person $personIdent",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = e
            )
        }

        if (!response.harFeil()) return response.data.person!!.doedsfall
        throw Feil(
            message = "Fant ikke data på person: ${response.errorMessages()}",
            frontendFeilmelding = "Fant ikke identer for person $personIdent: ${response.errorMessages()}",
            httpStatus = HttpStatus.NOT_FOUND
        )
    }

    fun hentVergemaalEllerFremtidsfullmakt(personIdent: String): List<VergemaalEllerFremtidsfullmakt> {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(personIdent),
            query = hentGraphqlQuery("verge")
        )
        val response = try {
            postForEntity<PdlVergeResponse>(pdlUri, pdlPersonRequest, httpHeaders())
        } catch (e: Exception) {
            throw Feil(
                message = "Feil ved oppslag på person. Gav feil: ${e.message}",
                frontendFeilmelding = "Feil oppsto ved oppslag på person $personIdent",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = e
            )
        }

        if (!response.harFeil()) return response.data.person!!.vergemaalEllerFremtidsfullmakt
        throw Feil(
            message = "Fant ikke data på person: ${response.errorMessages()}",
            frontendFeilmelding = "Fant ikke data på person $personIdent: ${response.errorMessages()}",
            httpStatus = HttpStatus.NOT_FOUND
        )
    }

    fun hentStatsborgerskapUtenHistorikk(ident: String): List<Statsborgerskap> {

        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(ident),
            query = hentGraphqlQuery("statsborgerskap-uten-historikk")
        )
        val response = try {
            postForEntity<PdlStatsborgerskapResponse>(pdlUri, pdlPersonRequest, httpHeaders())
        } catch (e: Exception) {
            throw Feil(
                message = "Feil ved oppslag på person. Gav feil: ${e.message}",
                frontendFeilmelding = "Feil oppsto ved oppslag på person $ident",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = e
            )
        }

        if (!response.harFeil()) return response.data.person!!.statsborgerskap
        throw Feil(
            message = "Fant ikke data på person: ${response.errorMessages()}",
            frontendFeilmelding = "Fant ikke identer for person $ident: ${response.errorMessages()}",
            httpStatus = HttpStatus.NOT_FOUND
        )
    }

    fun hentOppholdUtenHistorikk(ident: String): List<Opphold> {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(ident),
            query = hentGraphqlQuery("opphold-uten-historikk")
        )
        val response = try {
            postForEntity<PdlOppholdResponse>(pdlUri, pdlPersonRequest, httpHeaders())
        } catch (e: Exception) {
            throw Feil(
                message = "Feil ved oppslag på person. Gav feil: ${e.message}",
                frontendFeilmelding = "Feil oppsto ved oppslag på person $ident",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = e
            )
        }

        if (!response.harFeil()) {
            if (response.data?.person?.opphold == null) {
                throw Feil(
                    message = "Ugyldig response (null) fra PDL ved henting av opphold.",
                    frontendFeilmelding = "Feilet ved henting av opphold for person $ident",
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR
                )
            }
            return response.data.person.opphold
        }

        throw Feil(
            message = "Fant ikke data på person: ${response.errorMessages()}",
            frontendFeilmelding = "Fant ikke opphold for person $ident: ${response.errorMessages()}",
            httpStatus = HttpStatus.NOT_FOUND
        )
    }

    fun hentUtenlandskBostedsadresse(personIdent: String): PdlUtenlandskAdressseResponse.UtenlandskAdresse? {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(personIdent),
            query = hentGraphqlQuery("bostedsadresse-utenlandsk")
        )
        val response = try {
            postForEntity<PdlUtenlandskAdressseResponse>(pdlUri, pdlPersonRequest, httpHeaders())
        } catch (e: Exception) {
            throw Feil(
                message = "Feil ved oppslag på utenlandsk bostedsadresse. Gav feil: ${
                NestedExceptionUtils.getMostSpecificCause(e).message
                }",
                frontendFeilmelding = "Feil oppsto ved oppslag på utenlandsk bostedsadresse $personIdent",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = e
            )
        }

        if (!response.harFeil()) return response.data?.person?.bostedsadresse?.firstOrNull { it.utenlandskAdresse != null }?.utenlandskAdresse
        throw Feil(
            message = "Fant ikke data på person: ${response.errorMessages()}",
            frontendFeilmelding = "Fant ikke identer for person $personIdent: ${response.errorMessages()}",
            httpStatus = HttpStatus.NOT_FOUND
        )
    }

    /**
     * Til bruk for migrering. Vurder hentPerson som gir maskerte data for personer med adressebeskyttelse.
     *
     */
    fun hentForelderBarnRelasjon(ident: String): List<no.nav.familie.kontrakter.felles.personopplysning.ForelderBarnRelasjon> {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(ident),
            query = hentGraphqlQuery("hentperson-relasjoner")
        )
        val response = try {
            postForEntity<PdlHentPersonRelasjonerResponse>(pdlUri, pdlPersonRequest, httpHeaders())
        } catch (e: Exception) {
            throw Feil(
                message = "Feil ved oppslag på person. Gav feil: ${e.message}",
                frontendFeilmelding = "Feil oppsto ved oppslag på person $ident",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = e
            )
        }

        if (!response.harFeil()) return response.data.person!!.forelderBarnRelasjon
        throw Feil(
            message = "Fant ikke data på person: ${response.errorMessages()}",
            frontendFeilmelding = "Fant ikke identer for person $ident: ${response.errorMessages()}",
            httpStatus = HttpStatus.NOT_FOUND
        )
    }

    fun httpHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
            add("Tema", PDL_TEMA)
        }
    }

    companion object {

        private const val PATH_GRAPHQL = "graphql"
        private const val PDL_TEMA = "BAR"
    }
}

enum class PersonInfoQuery(val graphQL: String) {
    ENKEL(hentGraphqlQuery("hentperson-enkel")),
    MED_RELASJONER_OG_REGISTERINFORMASJON(hentGraphqlQuery("hentperson-med-relasjoner-og-registerinformasjon")),
}

fun hentGraphqlQuery(pdlResource: String): String {
    return PersonInfoQuery::class.java.getResource("/pdl/$pdlResource.graphql").readText().graphqlCompatible()
}

private fun String.graphqlCompatible(): String {
    return StringUtils.normalizeSpace(this.replace("\n", ""))
}
