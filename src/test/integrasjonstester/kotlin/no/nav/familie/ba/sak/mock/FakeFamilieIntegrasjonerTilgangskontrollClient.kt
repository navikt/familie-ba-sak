package no.nav.familie.ba.sak.mock

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestOperations
import java.net.URI

@TestConfiguration
@Primary
@Profile("dev", "postgres")
class FakeFamilieIntegrasjonerTilgangskontrollClient(
    restOperations: RestOperations,
) : FamilieIntegrasjonerTilgangskontrollClient(URI("dummyURI"), restOperations) {
    private val personIdentTilTilgang = mutableMapOf<String, Tilgang>()

    val kallMotSjekkTilgangTilPersoner: MutableList<List<String>> = mutableListOf()

    fun antallKallTilSjekkTilgangTilPersoner() = kallMotSjekkTilgangTilPersoner.size

    override fun sjekkTilgangTilPersoner(personIdenter: List<String>): List<Tilgang> {
        kallMotSjekkTilgangTilPersoner.add(personIdenter)
        return personIdenter.map { personIdent ->
            personIdentTilTilgang[personIdent] ?: Tilgang(personIdent, true)
        }
    }

    fun leggTilPersonIdentTilTilgang(
        personIdentTilHarTilgang: List<Tilgang>,
    ) {
        personIdentTilTilgang.putAll(personIdentTilHarTilgang.associate { tilgang -> tilgang.personIdent to tilgang })
    }

    fun reset() = personIdentTilTilgang.clear()
}
