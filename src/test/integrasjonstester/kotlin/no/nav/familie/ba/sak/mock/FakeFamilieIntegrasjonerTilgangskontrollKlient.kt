package no.nav.familie.ba.sak.mock

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollKlient
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestOperations
import java.net.URI

@TestConfiguration
@Primary
@Profile("dev", "postgres")
class FakeFamilieIntegrasjonerTilgangskontrollKlient(
    restOperations: RestOperations,
) : FamilieIntegrasjonerTilgangskontrollKlient(URI("dummyURI"), restOperations) {
    private val personIdentTilTilgang = mutableMapOf<String, Tilgang>()

    private val kallMotSjekkTilgangTilPersoner: MutableList<List<String>> = mutableListOf()
    private var godkjennByDefault: Boolean = true

    /**
     * Henter antall ganger sjekkTilgangTilPersoner er blitt kalt.
     * Erstatter mockk sin verify() {}
     */
    fun antallKallTilSjekkTilgangTilPersoner(): Int = kallMotSjekkTilgangTilPersoner.size

    /**
     * Henter hvilke identer som det har blitt sjekket tilgang for.
     * Erstatter mockk sin slot()-funksjonalitet
     */
    fun hentKallMotSjekkTilgangTilPersoner(): MutableList<List<String>> = kallMotSjekkTilgangTilPersoner

    override fun sjekkTilgangTilPersoner(personIdenter: List<String>): List<Tilgang> {
        kallMotSjekkTilgangTilPersoner.add(personIdenter)
        return personIdenter.map { personIdent ->
            personIdentTilTilgang[personIdent] ?: Tilgang(personIdent, godkjennByDefault)
        }
    }

    /**
     * Legger til tilgang for testIdenter og setter defaulten for godkjenning til false
     *
     * VIKTIG at man resetter godkjennDefault tilbake til true i etterkant, hvis ikke feiler påfølgende tester som trenger at den er satt til true
     */
    fun leggTilTilganger(
        personIdentTilHarTilgang: List<Tilgang>,
        godkjennDefault: Boolean = false,
    ) {
        personIdentTilTilgang.putAll(personIdentTilHarTilgang.associate { tilgang -> tilgang.personIdent to tilgang })
        godkjennByDefault = godkjennDefault
    }

    fun reset() {
        personIdentTilTilgang.clear()
        kallMotSjekkTilgangTilPersoner.clear()
        godkjennByDefault = true
    }
}
