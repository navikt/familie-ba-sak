package no.nav.familie.ba.sak.integrasjoner

import medPersonident
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.domene.FAMILIERELASJONSROLLE
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.integrasjoner.domene.Tilgang
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class IntegrasjonOnBehalfClient(@Value("\${FAMILIE_INTEGRASJONER_API_URL}") private val integrasjonUri: URI,
                                @Qualifier("jwtBearer") restOperations: RestOperations,
                                private val featureToggleService: FeatureToggleService)
    : AbstractPingableRestClient(restOperations, "integrasjon") {

    override val pingUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).path(PATH_PING).build().toUri()

    val tilgangUri = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_TILGANGER).build().toUri()
    val personinfoUri = URI.create("$integrasjonUri/personopplysning/v1/info/BAR")
    val personinfoEnkelUri = URI.create("$integrasjonUri/personopplysning/v1/infoEnkel/BAR")

    fun sjekkTilgangTilPersoner(personer: Set<Person>): List<Tilgang> {
        val identer = personer.map { it.personIdent.ident }
        return postForEntity(tilgangUri, identer)!!
    }

    fun hentPersoninfo(personident: String): Personinfo {
        return try {
            val personinfo = getForEntity<Ressurs<Personinfo>>(personinfoUri, HttpHeaders().medPersonident(personident)).data!!
            secureLogger.info("Personinfo fra $personinfoUri for {}: {}", personident, personinfo)

            val barnMedNavnOgFødselsdato =
                    personinfo.familierelasjoner
                            .filter { it.relasjonsrolle == FAMILIERELASJONSROLLE.BARN }
                            .map {
                                val barn = getForEntity<Ressurs<Personinfo>>(
                                        personinfoEnkelUri, HttpHeaders().medPersonident(it.personIdent.id)).data!!
                                it.copy(navn = barn.navn, fødselsdato = barn.fødselsdato)
                            }
                            .toSet()
            personinfo.copy(familierelasjoner = barnMedNavnOgFødselsdato)
        } catch (e: Exception) {
            throw IntegrasjonException("Kall mot integrasjon feilet ved uthenting av personinfo", e, personinfoUri, personident)
        }
    }

    companion object {
        private const val PATH_PING = "isAlive"
        private const val PATH_TILGANGER = "tilgang/personer"
    }
}