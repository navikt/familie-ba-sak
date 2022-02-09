package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.tilAdressebeskyttelse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class FamilieIntegrasjonerTilgangskontrollClient(
    @Value("\${FAMILIE_INTEGRASJONER_API_URL}") private val integrasjonUri: URI,
    @Qualifier("jwtBearer") restOperations: RestOperations,
    private val systemOnlyPdlRestClient: SystemOnlyPdlRestClient
) : AbstractRestClient(restOperations, "integrasjon-tilgangskontroll") {

    val tilgangRelasjonerUri: URI =
        UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_TILGANG_RELASJONER).build().toUri()
    val tilgangPersonUri: URI =
        UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_TILGANG_PERSON).build().toUri()

    fun sjekkTilgangTilPersoner(personIdenter: List<String>): Tilgang {
        if (SikkerhetContext.erSystemKontekst()) {
            return Tilgang(true, null)
        }

        val tilganger = postForEntity<List<Tilgang>>(
            tilgangPersonUri,
            personIdenter,
            HttpHeaders().also {
                it.set(HEADER_NAV_TEMA, HEADER_NAV_TEMA_BAR)
            }
        )

        return tilganger.firstOrNull { !it.harTilgang } ?: tilganger.first()
    }

    fun sjekkTilgangTilPersonMedRelasjoner(personIdent: String): Tilgang {
        if (SikkerhetContext.erSystemKontekst()) {
            return Tilgang(true, null)
        }

        return postForEntity(
            tilgangRelasjonerUri, PersonIdent(personIdent),
            HttpHeaders().also {
                it.set(HEADER_NAV_TEMA, HEADER_NAV_TEMA_BAR)
            }
        )
    }

    fun hentMaskertPersonInfoVedManglendeTilgang(aktør: Aktør): RestPersonInfo? {
        val harTilgang = sjekkTilgangTilPersoner(listOf(aktør.aktivFødselsnummer())).harTilgang
        return if (!harTilgang) {
            val adressebeskyttelse = systemOnlyPdlRestClient.hentAdressebeskyttelse(aktør).tilAdressebeskyttelse()
            RestPersonInfo(
                personIdent = aktør.aktivFødselsnummer(),
                adressebeskyttelseGradering = adressebeskyttelse,
                harTilgang = false
            )
        } else null
    }

    companion object {

        private const val PATH_TILGANG_RELASJONER = "tilgang/person-med-relasjoner"
        private const val PATH_TILGANG_PERSON = "tilgang/v2/personer"
        private const val HEADER_NAV_TEMA = "Nav-Tema"
        private val HEADER_NAV_TEMA_BAR = Tema.BAR.name
    }
}
