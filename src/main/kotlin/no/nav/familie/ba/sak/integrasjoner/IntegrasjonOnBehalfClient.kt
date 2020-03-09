package no.nav.familie.ba.sak.integrasjoner

import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.domene.Tilgang
import no.nav.familie.http.client.AbstractPingableRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
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

    fun sjekkTilgangTilPersoner(personer: Set<Person>): List<Tilgang> {
        val identer = personer.map { it.personIdent.ident }
        return postForEntity(tilgangUri, identer)!!
    }

    companion object {
        private const val PATH_PING = "isAlive"
        private const val PATH_TILGANGER = "tilgang/personer"
    }
}