package no.nav.familie.ba.sak.pdl

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.BRUK_NAV_CONSUMER_TOKEN_PDL
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.pdl.internal.Bostedsadresseperiode
import no.nav.familie.ba.sak.pdl.internal.Doedsfall
import no.nav.familie.ba.sak.pdl.internal.Familierelasjon
import no.nav.familie.ba.sak.pdl.internal.PdlBostedsadresseperioderResponse
import no.nav.familie.ba.sak.pdl.internal.PdlDødsfallResponse
import no.nav.familie.ba.sak.pdl.internal.PdlHentIdenterResponse
import no.nav.familie.ba.sak.pdl.internal.PdlHentPersonResponse
import no.nav.familie.ba.sak.pdl.internal.PdlOppholdResponse
import no.nav.familie.ba.sak.pdl.internal.PdlPersonRequest
import no.nav.familie.ba.sak.pdl.internal.PdlPersonRequestVariables
import no.nav.familie.ba.sak.pdl.internal.PdlStatsborgerskapResponse
import no.nav.familie.ba.sak.pdl.internal.PdlUtenlandskAdressseResponse
import no.nav.familie.ba.sak.pdl.internal.PdlVergeResponse
import no.nav.familie.ba.sak.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.pdl.internal.Personident
import no.nav.familie.ba.sak.pdl.internal.VergemaalEllerFremtidsfullmakt
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.http.sts.StsRestClient
import no.nav.familie.http.util.UriUtil
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
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
class PdlRestClient(@Value("\${PDL_URL}") pdlBaseUrl: URI,
                    @Qualifier("jwt-sts") val restTemplate: RestOperations,
                    private val stsRestClient: StsRestClient,
                    private val featureToggleService: FeatureToggleService)
    : AbstractRestClient(restTemplate, "pdl.personinfo") {

    protected val pdlUri = UriUtil.uri(pdlBaseUrl, PATH_GRAPHQL)

    private val hentIdenterQuery = hentGraphqlQuery("hentIdenter")

    fun hentPerson(personIdent: String, personInfoQuery: PersonInfoQuery): PersonInfo {

        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = personInfoQuery.graphQL)
        try {
            val response = postForEntity<PdlHentPersonResponse>(pdlUri,
                                                                pdlPersonRequest,
                                                                httpHeaders())
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

    fun hentIdenter(personIdent: String): PdlHentIdenterResponse {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = hentIdenterQuery)
        val response = postForEntity<PdlHentIdenterResponse>(pdlUri,
                                                             pdlPersonRequest,
                                                             httpHeaders())

        if (!response.harFeil()) return response
        throw Feil(message = "Fant ikke identer for person: ${response.errorMessages()}",
                   frontendFeilmelding = "Fant ikke identer for person $personIdent: ${response.errorMessages()}",
                   httpStatus = HttpStatus.NOT_FOUND)
    }

    fun hentDødsfall(personIdent: String): List<Doedsfall> {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = hentGraphqlQuery("doedsfall"))
        val response = try {
            postForEntity<PdlDødsfallResponse>(pdlUri, pdlPersonRequest, httpHeaders())
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

    fun hentVergemaalEllerFremtidsfullmakt(personIdent: String): List<VergemaalEllerFremtidsfullmakt> {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = hentGraphqlQuery("verge"))
        val response = try {
            postForEntity<PdlVergeResponse>(pdlUri, pdlPersonRequest, httpHeaders())
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

    fun hentStatsborgerskap(ident: String): List<Statsborgerskap> {

        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(ident),
                                                query = hentGraphqlQuery("statsborgerskap"))
        val response = try {
            postForEntity<PdlStatsborgerskapResponse>(pdlUri, pdlPersonRequest, httpHeaders())
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

    fun hentOpphold(ident: String): List<Opphold> {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(ident),
                                                query = hentGraphqlQuery("opphold"))
        val response = try {
            postForEntity<PdlOppholdResponse>(pdlUri, pdlPersonRequest, httpHeaders())
        } catch (e: Exception) {
            throw Feil(message = "Feil ved oppslag på person. Gav feil: ${e.message}",
                       frontendFeilmelding = "Feil oppsto ved oppslag på person $ident",
                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                       throwable = e)
        }

        if (!response.harFeil()) {
            if (response.data?.person?.opphold == null) {
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

    fun hentUtenlandskBostedsadresse(personIdent: String): PdlUtenlandskAdressseResponse.UtenlandskAdresse? {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = hentGraphqlQuery("bostedsadresse-utenlandsk"))
        val response = try {
            postForEntity<PdlUtenlandskAdressseResponse>(pdlUri, pdlPersonRequest, httpHeaders())
        } catch (e: Exception) {
            throw Feil(message = "Feil ved oppslag på utenlandsk bostedsadresse. Gav feil: ${
                NestedExceptionUtils.getMostSpecificCause(e).message
            }",
                       frontendFeilmelding = "Feil oppsto ved oppslag på utenlandsk bostedsadresse $personIdent",
                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                       throwable = e)
        }

        if (!response.harFeil()) return response.data?.person?.bostedsadresse?.firstOrNull()?.utenlandskAdresse
        throw Feil(message = "Fant ikke data på person: ${response.errorMessages()}",
                   frontendFeilmelding = "Fant ikke identer for person $personIdent: ${response.errorMessages()}",
                   httpStatus = HttpStatus.NOT_FOUND)

    }

    fun hentBostedsadresseperioder(ident: String): List<Bostedsadresseperiode> {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(ident),
                                                query = hentGraphqlQuery("hentBostedsadresseperioder"))
        val response = try {
            postForEntity<PdlBostedsadresseperioderResponse>(pdlUri, pdlPersonRequest, httpHeaders())
        } catch (e: Exception) {
            throw Feil(message = "Feil ved oppslag på person. Gav feil: ${e.message}",
                       frontendFeilmelding = "Feil oppsto ved oppslag på person $ident",
                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                       throwable = e)
        }

        if (!response.harFeil()) {
            if (response.data.person == null) {
                throw Feil(message = "Ugyldig response (person=null) fra PDL ved henting av bostedsadresseperioder.",
                           frontendFeilmelding = "Feilet ved henting av bostedsadresseperioder for person $ident",
                           httpStatus = HttpStatus.INTERNAL_SERVER_ERROR)
            }
            if (response.data.person.bostedsadresse.any { it.gyldigFraOgMed == null }) {
                logger.warn("Uventet response (gyldigFraOgMed == null) fra PDL ved henting av bostedadresseperioder.")
            }
            return response.data.person.bostedsadresse
        }

        throw Feil(message = "Fant ikke data på person: ${response.errorMessages()}",
                   frontendFeilmelding = "Fant ikke bostedsadresseperioder for person $ident: ${response.errorMessages()}",
                   httpStatus = HttpStatus.NOT_FOUND)
    }

    protected fun httpHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
            if (SikkerhetContext.erSystemKontekst() || featureToggleService.isEnabled(BRUK_NAV_CONSUMER_TOKEN_PDL, false)) {
                add("Nav-Consumer-Token", "Bearer ${stsRestClient.systemOIDCToken}")
            }
            add("Tema", TEMA)
            secureLogger.info("Headere ved kall mot pdl: $this, ${this["Nav-Consumer-Token"]}, ${this["Authorization"]}")
        }
    }

    companion object {

        private const val PATH_GRAPHQL = "graphql"
        private const val TEMA = "BAR"
        private val logger = LoggerFactory.getLogger(PdlRestClient::class.java)
    }
}

enum class PersonInfoQuery(val graphQL: String) {
    ENKEL(hentGraphqlQuery("hentperson-enkel")),
    MED_RELASJONER(hentGraphqlQuery("hentperson-med-relasjoner"))
}

fun hentGraphqlQuery(pdlResource: String): String {
    return PersonInfoQuery::class.java.getResource("/pdl/$pdlResource.graphql").readText().graphqlCompatible()
}

private fun String.graphqlCompatible(): String {
    return StringUtils.normalizeSpace(this.replace("\n", ""))
}

