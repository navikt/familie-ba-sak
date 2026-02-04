package no.nav.familie.ba.sak.integrasjoner.pdl

import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.DødsfallData
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.GeografiskTilknytning
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdresserPerson
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlBaseResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlDødsfallResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlFalskIdentitet
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlFalskIdentitetResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlGeografiskTilknytningResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlHentPersonResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlOppholdResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonBolkRequest
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonBolkRequestVariables
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonRequest
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonRequestVariables
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlStatsborgerskapResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlUtenlandskAdressseResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlVergeResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.VergemaalEllerFremtidsfullmakt
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.filtrerKjønnPåKilde
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.filtrerNavnPåKilde
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.kontrakter.felles.personopplysning.UtenlandskAdresse
import no.nav.familie.restklient.client.AbstractRestClient
import no.nav.familie.restklient.util.UriUtil
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
class PdlRestKlient(
    @Value("\${PDL_URL}") pdlBaseUrl: URI,
    @Qualifier("jwtBearer") val restTemplate: RestOperations,
    val personidentService: PersonidentService,
) : AbstractRestClient(restTemplate, "pdl.personinfo") {
    protected val pdlUri = UriUtil.uri(pdlBaseUrl, PATH_GRAPHQL)

    @Cacheable("personopplysninger", cacheManager = "shortCache")
    fun hentPerson(
        aktør: Aktør,
        personInfoQuery: PersonInfoQuery,
    ): PersonInfo = hentPerson(aktør.aktivFødselsnummer(), personInfoQuery)

    @Cacheable("personopplysninger", cacheManager = "shortCache")
    fun hentPerson(
        fødselsnummer: String,
        personInfoQuery: PersonInfoQuery,
    ): PersonInfo {
        val pdlPersonRequest =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(fødselsnummer),
                query = personInfoQuery.graphQL,
            )
        val pdlResponse: PdlBaseResponse<PdlHentPersonResponse> =
            kallEksternTjeneste(
                tjeneste = "pdl",
                uri = pdlUri,
                formål = "Hent person med query ${personInfoQuery.name}",
            ) {
                postForEntity(
                    pdlUri,
                    pdlPersonRequest,
                    httpHeaders(),
                )
            }

        return feilsjekkOgReturnerData(
            ident = fødselsnummer,
            pdlResponse = pdlResponse,
        ) { pdlPerson ->
            pdlPerson.person!!.validerOmPersonKanBehandlesIFagsystem()

            val forelderBarnRelasjon: Set<ForelderBarnRelasjon> =
                when (personInfoQuery) {
                    PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON -> {
                        pdlPerson.person.forelderBarnRelasjon
                            .mapNotNull { relasjon ->
                                relasjon.relatertPersonsIdent
                                    ?.let { ident ->
                                        personidentService.hentAktørOrNullHvisIkkeAktivFødselsnummer(ident).run {
                                            if (this == null) {
                                                secureLogger.warn("Filtrert bort relasjon som ikke har aktivt fødselsnummer i PDL for ident $ident")
                                            }
                                            this
                                        }
                                    }?.let { aktør ->
                                        ForelderBarnRelasjon(
                                            aktør = aktør,
                                            relasjonsrolle = relasjon.relatertPersonsRolle,
                                        )
                                    }
                            }.toSet()
                    }

                    else -> {
                        emptySet()
                    }
                }

            pdlPerson.person.let {
                PersonInfo(
                    // Hvis det ikke finnes fødselsdato på person forsøker vi å bruke dato for barnets død fordi det var et dødfødt barn.
                    fødselsdato = LocalDate.parse(it.foedselsdato.firstOrNull()?.foedselsdato ?: it.doedfoedtBarn.first().dato),
                    navn = it.navn.filtrerNavnPåKilde()?.fulltNavn(),
                    kjønn = it.kjoenn.filtrerKjønnPåKilde()?.kjoenn ?: Kjønn.UKJENT,
                    forelderBarnRelasjon = forelderBarnRelasjon,
                    adressebeskyttelseGradering = it.adressebeskyttelse.firstOrNull()?.gradering,
                    bostedsadresser = it.bostedsadresse,
                    oppholdsadresser = it.oppholdsadresse,
                    deltBosted = it.deltBosted,
                    statsborgerskap = it.statsborgerskap,
                    opphold = it.opphold,
                    sivilstander = it.sivilstand,
                    dødsfall = hentDødsfallDataFraListeMedDødsfall(it.doedsfall),
                    kontaktinformasjonForDoedsbo = it.kontaktinformasjonForDoedsbo.firstOrNull(),
                )
            }
        }
    }

    private fun hentDødsfallDataFraListeMedDødsfall(dødsfall: List<PdlDødsfallResponse>): DødsfallData? {
        val dødsdato =
            dødsfall
                .filter { it.doedsdato != null }
                .map { it.doedsdato }
                .firstOrNull()

        if (dødsfall.isEmpty() || dødsdato == null) {
            return null
        }
        return DødsfallData(erDød = true, dødsdato = dødsdato)
    }

    @Cacheable("vergedata", cacheManager = "shortCache")
    fun hentVergemaalEllerFremtidsfullmakt(aktør: Aktør): List<VergemaalEllerFremtidsfullmakt> {
        val pdlPersonRequest =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(aktør.aktivFødselsnummer()),
                query = hentGraphqlQuery("verge"),
            )
        val pdlResponse: PdlBaseResponse<PdlVergeResponse> =
            kallEksternTjeneste(
                tjeneste = "pdl",
                uri = pdlUri,
                formål = "Hent vergemål eller fremtidsfullmakt",
            ) { postForEntity(pdlUri, pdlPersonRequest, httpHeaders()) }

        return feilsjekkOgReturnerData(
            ident = aktør.aktivFødselsnummer(),
            pdlResponse = pdlResponse,
        ) {
            it.person!!.vergemaalEllerFremtidsfullmakt
        }
    }

    fun hentStatsborgerskap(
        aktør: Aktør,
        historikk: Boolean = false,
    ): List<Statsborgerskap> {
        val pdlPersonRequest =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(aktør.aktivFødselsnummer(), historikk = historikk),
                query = hentGraphqlQuery("statsborgerskap"),
            )
        val pdlResponse: PdlBaseResponse<PdlStatsborgerskapResponse> =
            kallEksternTjeneste(
                tjeneste = "pdl",
                uri = pdlUri,
                formål = "Hent statsborgerskap",
            ) { postForEntity(pdlUri, pdlPersonRequest, httpHeaders()) }

        return feilsjekkOgReturnerData(
            ident = aktør.aktivFødselsnummer(),
            pdlResponse = pdlResponse,
        ) {
            it.person!!.statsborgerskap
        }
    }

    fun hentOppholdstillatelse(
        aktør: Aktør,
        historikk: Boolean = false,
    ): List<Opphold> {
        val pdlPersonRequest =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(aktør.aktivFødselsnummer(), historikk = historikk),
                query = hentGraphqlQuery("oppholdstillatelse"),
            )
        val pdlResponse: PdlBaseResponse<PdlOppholdResponse> =
            kallEksternTjeneste(
                tjeneste = "pdl",
                uri = pdlUri,
                formål = "Hent oppholdstillatelse",
            ) {
                postForEntity(pdlUri, pdlPersonRequest, httpHeaders())
            }

        return feilsjekkOgReturnerData(
            ident = aktør.aktivFødselsnummer(),
            pdlResponse = pdlResponse,
        ) {
            it.person!!.opphold
        }
    }

    fun hentUtenlandskBostedsadresse(aktør: Aktør): UtenlandskAdresse? {
        val pdlPersonRequest =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(aktør.aktivFødselsnummer()),
                query = hentGraphqlQuery("bostedsadresse-utenlandsk"),
            )
        val pdlResponse: PdlBaseResponse<PdlUtenlandskAdressseResponse> =
            kallEksternTjeneste(
                tjeneste = "pdl",
                uri = pdlUri,
                formål = "Hent utenlandsk bostedsadresse",
            ) {
                postForEntity(pdlUri, pdlPersonRequest, httpHeaders())
            }

        val bostedsadresser =
            feilsjekkOgReturnerData(
                ident = aktør.aktivFødselsnummer(),
                pdlResponse = pdlResponse,
            ) {
                it.person!!.bostedsadresse
            }
        return bostedsadresser.firstOrNull { bostedsadresse -> bostedsadresse.utenlandskAdresse != null }?.utenlandskAdresse
    }

    fun hentGeografiskTilknytning(ident: String): GeografiskTilknytning? {
        val pdlPersonRequest =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(ident),
                query = hentGraphqlQuery("geografisk-tilknytning"),
            )
        val pdlResponse: PdlBaseResponse<PdlGeografiskTilknytningResponse> =
            kallEksternTjeneste(
                tjeneste = "pdl",
                uri = pdlUri,
                formål = "Hent geografisk tilknytning",
            ) {
                postForEntity(pdlUri, pdlPersonRequest, httpHeaders())
            }

        return feilsjekkOgReturnerData(
            ident = ident,
            pdlResponse = pdlResponse,
        ) { it.geografiskTilknytning }
    }

    fun hentAdresser(identer: List<String>): Map<String, PdlAdresserPerson> {
        val pdlPersonRequest =
            PdlPersonBolkRequest(
                variables = PdlPersonBolkRequestVariables(identer),
                query = hentGraphqlQuery("bolk-kommunenr-alle-adressetyper"),
            )

        val pdlResponse: PdlBolkResponse<PdlAdresserPerson> =
            kallEksternTjeneste(
                tjeneste = "pdl",
                uri = pdlUri,
                formål = "Hent bostedsadresse, delt bosted og oppholdsadresse for personer",
            ) {
                postForEntity(pdlUri, pdlPersonRequest, httpHeaders())
            }

        return feilsjekkOgReturnerData(pdlResponse = pdlResponse)
    }

    fun hentFalskIdentitet(ident: String): PdlFalskIdentitet? {
        val pdlPersonRequest =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(ident),
                query = hentGraphqlQuery("hent-falsk-identitet"),
            )
        val pdlResponse: PdlBaseResponse<PdlFalskIdentitetResponse> =
            kallEksternTjeneste(
                tjeneste = "pdl",
                uri = pdlUri,
                formål = "Hent falsk identitet",
            ) {
                postForEntity(pdlUri, pdlPersonRequest, httpHeaders())
            }

        return feilsjekkOgReturnerData(
            ident = ident,
            pdlResponse = pdlResponse,
        ) {
            it.person
        }.falskIdentitet
    }

    fun httpHeaders(): HttpHeaders =
        HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
            add("Tema", PDL_TEMA)
            add("behandlingsnummer", Tema.BAR.behandlingsnummer)
        }

    companion object {
        private const val PATH_GRAPHQL = "graphql"
        private const val PDL_TEMA = "BAR"
    }
}

enum class PersonInfoQuery(
    val graphQL: String,
) {
    ENKEL(hentGraphqlQuery("hentperson-enkel")),
    MED_RELASJONER_OG_REGISTERINFORMASJON(hentGraphqlQuery("hentperson-med-relasjoner-og-registerinformasjon")),
    NAVN_OG_ADRESSE(hentGraphqlQuery("hentperson-navn-og-adresse")),
}

fun hentGraphqlQuery(pdlResource: String): String =
    PersonInfoQuery::class.java
        .getResource("/pdl/$pdlResource.graphql")
        .readText()
        .graphqlCompatible()

private fun String.graphqlCompatible(): String = StringUtils.normalizeSpace(this.replace("\n", ""))
