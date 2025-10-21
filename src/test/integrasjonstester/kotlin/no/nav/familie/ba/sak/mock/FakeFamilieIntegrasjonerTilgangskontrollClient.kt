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

    private val kallMotSjekkTilgangTilPersoner: MutableList<List<String>> = mutableListOf()
    private var godkjennByDefault: Boolean = true

    /**
     * Henter antall ganger sjekkTilgangTilPersoner er blitt kalt.
     * Erstatter mockk sin verify() {}
     *
     * OBS! Reseter kall i etterkant så hvis man vil hente ut flere ganger på rad vil denne feile.
     */
    fun antallKallTilSjekkTilgangTilPersoner(): Int = kallMotSjekkTilgangTilPersoner.size.also { this.reset() } // Reseter i etterkant for å gjøre det mindre skummelt å glemme å resete selv

    /**
     * Henter hvilke identer som det har blitt sjekket tilgang for.
     * Erstatter mockk sin slot()-funksjonalitet
     *
     * OBS! Reseter kall i etterkant så hvis man vil hente ut flere ganger på rad vil denne feile.
     */
    fun hentKallMotSjekkTilgangTilPersoner(): MutableList<List<String>> = kallMotSjekkTilgangTilPersoner.also { this.reset() } // Reseter i etterkant for å gjøre det mindre skummelt å glemme å resete selv

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
    fun leggTilPersonIdentTilTilgang(
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
