package no.nav.familie.ba.sak.pdl

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.pdl.internal.*
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.http.sts.StsRestClient
import no.nav.familie.http.util.UriUtil
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

@Service
class PdlRestClient(@Value("\${PDL_URL}") pdlBaseUrl: URI,
                    @Qualifier("sts") val restTemplate: RestOperations,
                    private val stsRestClient: StsRestClient)
    : AbstractRestClient(restTemplate, "pdl.personinfo") {

    private val pdlUri = UriUtil.uri(pdlBaseUrl,
                                     PATH_GRAPHQL)

    private val hentIdenterQuery = hentGraphqlQuery("hentIdenter")

    fun hentPerson(personIdent: String, tema: String, personInfoQuery: PersonInfoQuery): PersonInfo {

        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = personInfoQuery.graphQL)
        try {
            val response = postForEntity<PdlHentPersonResponse>(pdlUri,
                                                                pdlPersonRequest,
                                                                httpHeaders(tema))
            if (!response.harFeil()) {
                return Result.runCatching {
                    val familierelasjoner: Set<Familierelasjon> =
                            when (personInfoQuery) {
                                PersonInfoQuery.ENKEL -> emptySet()
                                PersonInfoQuery.MED_RELASJONER -> {
                                    response.data.person!!.familierelasjoner.map { relasjon ->
                                        Familierelasjon(personIdent = Personident(id = relasjon.relatertPersonsIdent),
                                                        relasjonsrolle = relasjon.relatertPersonsRolle)
                                    }.toSet()
                                }
                            }
                    response.data.person!!.let {
                        PersonInfo(fødselsdato = LocalDate.parse(it.foedsel.first().foedselsdato!!),
                                   navn = it.navn.first().fulltNavn(),
                                   kjønn = it.kjoenn.first().kjoenn,
                                   familierelasjoner = familierelasjoner,
                                   adressebeskyttelseGradering = it.adressebeskyttelse.firstOrNull()?.gradering,
                                   bostedsadresse = it.bostedsadresse.firstOrNull(),
                                   sivilstand = it.sivilstand.firstOrNull()?.type)
                    }
                }.fold(
                        onSuccess = { it },
                        onFailure = {
                            throw Feil(message = "Fant ikke forespurte data på person.",
                                       frontendFeilmelding = "Kunne ikke slå opp data for person $personIdent",
                                       httpStatus = HttpStatus.NOT_FOUND,
                                       throwable = it)
                        }
                )
            } else {
                throw Feil(message = "Feil ved oppslag på person: ${response.errorMessages()}",
                           frontendFeilmelding = "Feil ved oppslag på person $personIdent: ${response.errorMessages()}",
                           httpStatus = HttpStatus.INTERNAL_SERVER_ERROR)
            }
        } catch (e: Exception) {
            when (e) {
                is Feil -> throw e
                else -> throw Feil(message = "Feil ved oppslag på person. Gav feil: ${e.message}",
                                   frontendFeilmelding = "Feil oppsto ved oppslag på person $personIdent",
                                   httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                                   throwable = e)
            }
        }
    }

    private fun httpHeaders(tema: String): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
            add("Nav-Consumer-Token", "Bearer ${stsRestClient.systemOIDCToken}")
            add("Tema", tema)
        }
    }

    fun hentIdenter(personIdent: String, tema: String): PdlHentIdenterResponse {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = hentIdenterQuery)
        val response = postForEntity<PdlHentIdenterResponse>(pdlUri,
                                                             pdlPersonRequest,
                                                             httpHeaders(tema))

        if (!response.harFeil()) return response
        throw Feil(message = "Fant ikke identer for person: ${response.errorMessages()}",
                   frontendFeilmelding = "Fant ikke identer for person $personIdent: ${response.errorMessages()}",
                   httpStatus = HttpStatus.NOT_FOUND)
    }

    fun hentDødsfall(personIdent: String, tema: String): List<Doedsfall> {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = hentGraphqlQuery("doedsfall"))
        val response = try {
            postForEntity<PdlDødsfallResponse>(pdlUri, pdlPersonRequest, httpHeaders(tema))
        } catch (e: Exception) {
            throw Feil(message = "Feil ved oppslag på person. Gav feil: ${e.message}",
                       frontendFeilmelding = "Feil oppsto ved oppslag på person $personIdent",
                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                       throwable = e)
        }

        if (!response.harFeil()) return response.data.person!!.doedsfall
        throw Feil(message = "Fant ikke data på person: ${response.errorMessages()}",
                   frontendFeilmelding = "Fant ikke identer for person $personIdent: ${response.errorMessages()}",
                   httpStatus = HttpStatus.NOT_FOUND)

    }

    fun hentVergemaalEllerFremtidsfullmakt(personIdent: String, tema: String): List<VergemaalEllerFremtidsfullmakt> {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = hentGraphqlQuery("verge"))
        val response = try {
            postForEntity<PdlVergeResponse>(pdlUri, pdlPersonRequest, httpHeaders(tema))
        } catch (e: Exception) {
            throw Feil(message = "Feil ved oppslag på person. Gav feil: ${e.message}",
                       frontendFeilmelding = "Feil oppsto ved oppslag på person $personIdent",
                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                       throwable = e)
        }

        if (!response.harFeil()) return response.data.person!!.vergemaalEllerFremtidsfullmakt
        throw Feil(message = "Fant ikke data på person: ${response.errorMessages()}",
                   frontendFeilmelding = "Fant ikke data på person $personIdent: ${response.errorMessages()}",
                   httpStatus = HttpStatus.NOT_FOUND)
    }

    fun hentStatsborgerskap(ident: String, tema: String): List<Statsborgerskap> {

        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(ident),
                                                query = hentGraphqlQuery("statsborgerskap"))
        val response = try {
            postForEntity<PdlStatsborgerskapResponse>(pdlUri, pdlPersonRequest, httpHeaders(tema))
        } catch (e: Exception) {
            throw Feil(message = "Feil ved oppslag på person. Gav feil: ${e.message}",
                       frontendFeilmelding = "Feil oppsto ved oppslag på person $ident",
                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                       throwable = e)
        }

        if (!response.harFeil()) return response.data.person!!.statsborgerskap
        throw Feil(message = "Fant ikke data på person: ${response.errorMessages()}",
                   frontendFeilmelding = "Fant ikke identer for person $ident: ${response.errorMessages()}",
                   httpStatus = HttpStatus.NOT_FOUND)
    }

    fun hentOpphold(ident: String, tema: String): List<Opphold>{
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(ident),
                                                query = hentGraphqlQuery("opphold"))
        val response = try {
            postForEntity<PdlOppholdResponse>(pdlUri, pdlPersonRequest, httpHeaders(tema))
        } catch (e: Exception) {
            throw Feil(message = "Feil ved oppslag på person. Gav feil: ${e.message}",
                       frontendFeilmelding = "Feil oppsto ved oppslag på person $ident",
                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                       throwable = e)
        }

        if (!response.harFeil()){
            if(response.data == null || response.data.person== null || response.data.person.opphold== null){
                throw Feil(message = "Ugyldig response (null) fra PDL ved henting av opphold.",
                           frontendFeilmelding = "Feilet ved henting av opphold for person $ident",
                           httpStatus = HttpStatus.INTERNAL_SERVER_ERROR)
            }
            return response.data.person.opphold
        }

        throw Feil(message = "Fant ikke data på person: ${response.errorMessages()}",
                   frontendFeilmelding = "Fant ikke opphold for person $ident: ${response.errorMessages()}",
                   httpStatus = HttpStatus.NOT_FOUND)
    }

    fun hentBostedsadresseperioder(ident : String) : List<Bostedsadresseperiode>{
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(ident),
                                                query = hentGraphqlQuery("hentBostedsadresseperioder"))
        val response = try {
            postForEntity<PdlBostedsadresseperioderResponse>(pdlUri, pdlPersonRequest, httpHeaders("BAR"))
        } catch (e: Exception) {
            throw Feil(message = "Feil ved oppslag på person. Gav feil: ${e.message}",
                       frontendFeilmelding = "Feil oppsto ved oppslag på person $ident",
                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                       throwable = e)
        }

        if (!response.harFeil()) {
            if (response.data.person == null){
                throw Feil(message = "Ugyldig response (null) fra PDL ved henting av bostedsadresseperioder.",
                           frontendFeilmelding = "Feilet ved henting av bostedsadresseperioder for person $ident",
                           httpStatus = HttpStatus.INTERNAL_SERVER_ERROR)
            }
            return response.data.person.bostedsadresse
        }

        throw Feil(message = "Fant ikke data på person: ${response.errorMessages()}",
                   frontendFeilmelding = "Fant ikke bostedsadresseperioder for person $ident: ${response.errorMessages()}",
                   httpStatus = HttpStatus.NOT_FOUND)
    }

    companion object {
        private const val PATH_GRAPHQL = "graphql"
        val LOG = LoggerFactory.getLogger(PdlRestClient::class.java)
    }
}

enum class PersonInfoQuery(val graphQL: String) {
    ENKEL(hentGraphqlQuery("hentperson-enkel")),
    MED_RELASJONER(hentGraphqlQuery("hentperson-med-relasjoner"))
}

private fun hentGraphqlQuery(pdlResource: String): String {
    return PersonInfoQuery::class.java.getResource("/pdl/$pdlResource.graphql").readText().graphqlCompatible()
}

private fun String.graphqlCompatible(): String {
    return StringUtils.normalizeSpace(this.replace("\n", ""))
}

