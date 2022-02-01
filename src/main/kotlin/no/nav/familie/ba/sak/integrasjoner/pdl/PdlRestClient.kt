package no.nav.familie.ba.sak.integrasjoner.pdl

import no.nav.familie.ba.sak.integrasjoner.pdl.domene.Doedsfall
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.DødsfallData
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlBaseResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlDødsfallResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlHentPersonRelasjonerResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlHentPersonResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlOppholdResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonRequest
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonRequestVariables
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlStatsborgerskapResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlUtenlandskAdresssePersonUtenlandskAdresse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlUtenlandskAdressseResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlVergeResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.VergemaalEllerFremtidsfullmakt
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.http.util.UriUtil
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

@Service
class PdlRestClient(
    @Value("\${PDL_URL}") pdlBaseUrl: URI,
    @Qualifier("jwtBearer") val restTemplate: RestOperations,
    val personidentService: PersonidentService
) : AbstractRestClient(restTemplate, "pdl.personinfo") {

    protected val pdlUri = UriUtil.uri(pdlBaseUrl, PATH_GRAPHQL)

    @Cacheable("personopplysninger", cacheManager = "shortCache")
    fun hentPerson(aktør: Aktør, personInfoQuery: PersonInfoQuery): PersonInfo {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(aktør.aktivFødselsnummer()),
            query = personInfoQuery.graphQL
        )
        val pdlResponse: PdlBaseResponse<PdlHentPersonResponse> = postForEntity(
            pdlUri,
            pdlPersonRequest,
            httpHeaders()
        )

        return feilsjekkOgReturnerData(
            ident = aktør.aktivFødselsnummer(),
            pdlResponse = pdlResponse
        ) { pdlPerson ->
            val forelderBarnRelasjon: Set<ForelderBarnRelasjon> =
                when (personInfoQuery) {
                    PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON -> {
                        pdlPerson.person!!.forelderBarnRelasjon.map { relasjon ->
                            val relatertAktør =
                                personidentService.hentOgLagreAktør(relasjon.relatertPersonsIdent)
                            ForelderBarnRelasjon(
                                aktør = relatertAktør,
                                relasjonsrolle = relasjon.relatertPersonsRolle
                            )
                        }.toSet()
                    }
                    else -> emptySet()
                }

            pdlPerson.person!!.let {
                PersonInfo(
                    fødselsdato = LocalDate.parse(it.foedsel.first().foedselsdato!!),
                    navn = it.navn.firstOrNull()?.fulltNavn(),
                    kjønn = it.kjoenn.firstOrNull()?.kjoenn,
                    forelderBarnRelasjon = forelderBarnRelasjon,
                    adressebeskyttelseGradering = it.adressebeskyttelse.firstOrNull()?.gradering,
                    bostedsadresser = it.bostedsadresse,
                    statsborgerskap = it.statsborgerskap,
                    opphold = it.opphold,
                    sivilstander = it.sivilstand,
                    dødsfall = hentDødsfallDataFraListeMedDødsfall(it.doedsfall),
                    kontaktinformasjonForDoedsbo = it.kontaktinformasjonForDoedsbo.firstOrNull()
                )
            }
        }
    }

    private fun hentDødsfallDataFraListeMedDødsfall(doedsfall: List<Doedsfall>): DødsfallData {
        return DødsfallResponse(
            erDød = doedsfall.isNotEmpty(),
            dødsdato = doedsfall.filter { it.doedsdato != null }
                .map { it.doedsdato }
                .firstOrNull()
        )
            .let { DødsfallData(erDød = it.erDød, dødsdato = it.dødsdato) }
    }

    @Cacheable("dødsfall", cacheManager = "shortCache")
    fun hentDødsfall(aktør: Aktør): List<Doedsfall> {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(aktør.aktivFødselsnummer()),
            query = hentGraphqlQuery("doedsfall")
        )
        val pdlResponse: PdlBaseResponse<PdlDødsfallResponse> = postForEntity(pdlUri, pdlPersonRequest, httpHeaders())

        return feilsjekkOgReturnerData(
            ident = aktør.aktivFødselsnummer(),
            pdlResponse = pdlResponse
        ) {
            it.person!!.doedsfall
        }
    }

    @Cacheable("vergedata", cacheManager = "shortCache")
    fun hentVergemaalEllerFremtidsfullmakt(aktør: Aktør): List<VergemaalEllerFremtidsfullmakt> {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(aktør.aktivFødselsnummer()),
            query = hentGraphqlQuery("verge")
        )
        val pdlResponse: PdlBaseResponse<PdlVergeResponse> = postForEntity(pdlUri, pdlPersonRequest, httpHeaders())

        return feilsjekkOgReturnerData(
            ident = aktør.aktivFødselsnummer(),
            pdlResponse = pdlResponse
        ) {
            it.person!!.vergemaalEllerFremtidsfullmakt
        }
    }

    fun hentStatsborgerskapUtenHistorikk(aktør: Aktør): List<Statsborgerskap> {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(aktør.aktivFødselsnummer()),
            query = hentGraphqlQuery("statsborgerskap-uten-historikk")
        )
        val pdlResponse: PdlBaseResponse<PdlStatsborgerskapResponse> =
            postForEntity(pdlUri, pdlPersonRequest, httpHeaders())

        return feilsjekkOgReturnerData(
            ident = aktør.aktivFødselsnummer(),
            pdlResponse = pdlResponse
        ) {
            it.person!!.statsborgerskap
        }
    }

    fun hentOppholdUtenHistorikk(aktør: Aktør): List<Opphold> {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(aktør.aktivFødselsnummer()),
            query = hentGraphqlQuery("opphold-uten-historikk")
        )
        val pdlResponse: PdlBaseResponse<PdlOppholdResponse> = postForEntity(pdlUri, pdlPersonRequest, httpHeaders())

        return feilsjekkOgReturnerData(
            ident = aktør.aktivFødselsnummer(),
            pdlResponse = pdlResponse
        ) {
            it.person!!.opphold
        }
    }

    fun hentUtenlandskBostedsadresse(aktør: Aktør): PdlUtenlandskAdresssePersonUtenlandskAdresse? {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(aktør.aktivFødselsnummer()),
            query = hentGraphqlQuery("bostedsadresse-utenlandsk")
        )
        val pdlResponse: PdlBaseResponse<PdlUtenlandskAdressseResponse> =
            postForEntity(pdlUri, pdlPersonRequest, httpHeaders())

        val bostedsadresser = feilsjekkOgReturnerData(
            ident = aktør.aktivFødselsnummer(),
            pdlResponse = pdlResponse
        ) {
            it.person!!.bostedsadresse
        }
        return bostedsadresser.firstOrNull { bostedsadresse -> bostedsadresse.utenlandskAdresse != null }?.utenlandskAdresse
    }

    /**
     * Til bruk for migrering. Vurder hentPerson som gir maskerte data for personer med adressebeskyttelse.
     *
     */
    fun hentForelderBarnRelasjon(aktør: Aktør): List<no.nav.familie.kontrakter.felles.personopplysning.ForelderBarnRelasjon> {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(aktør.aktivFødselsnummer()),
            query = hentGraphqlQuery("hentperson-relasjoner")
        )
        val pdlResponse: PdlBaseResponse<PdlHentPersonRelasjonerResponse> =
            postForEntity(pdlUri, pdlPersonRequest, httpHeaders())

        return feilsjekkOgReturnerData(
            ident = aktør.aktivFødselsnummer(),
            pdlResponse = pdlResponse
        ) {
            it.person!!.forelderBarnRelasjon
        }
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
