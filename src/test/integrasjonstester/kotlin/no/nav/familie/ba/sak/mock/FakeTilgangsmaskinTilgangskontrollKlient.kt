package no.nav.familie.ba.sak.mock

import io.mockk.mockk
import no.nav.familie.ba.sak.integrasjoner.tilgangsmaskin.TilgangsmaskinTilgangskontrollKlient
import no.nav.familie.ba.sak.sikkerhet.PersonTilgang
import no.nav.familie.tilgangsmaskin.Avvisningskode
import no.nav.familie.tilgangsmaskin.Regeltype
import no.nav.familie.tilgangsmaskin.TilgangsmaskinKlient
import no.nav.familie.tilgangsmaskin.TilgangsmaskinResultat
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.net.URI

/**
 * Fake som bare erstatter kallet mot Tilgangsmaskinen. Resten av [TilgangsmaskinTilgangskontrollKlient] kjøres som i
 * produksjon, slik at systemkontekst, validering og mapping av svar også gjelder i testene.
 */
@TestConfiguration
@Primary
@Profile("dev", "postgres")
class FakeTilgangsmaskinTilgangskontrollKlient(
    private val tilgangsmaskin: StyrbarTilgangsmaskinKlient = StyrbarTilgangsmaskinKlient(),
) : TilgangsmaskinTilgangskontrollKlient(URI("dummyURI"), tilgangsmaskin) {
    /**
     * Henter antall ganger Tilgangsmaskinen er blitt kalt.
     * Erstatter mockk sin verify() {}
     */
    fun antallKallTilSjekkTilgangTilPersoner(): Int = tilgangsmaskin.kall.size

    /**
     * Henter hvilke identer som det har blitt sjekket tilgang for i hvert kall mot Tilgangsmaskinen.
     * Erstatter mockk sin slot()-funksjonalitet
     */
    fun hentKallMotSjekkTilgangTilPersoner(): List<Set<String>> = tilgangsmaskin.kall.toList()

    /**
     * Legger til tilgang for testIdenter og setter defaulten for godkjenning til false
     *
     * VIKTIG at man resetter godkjennDefault tilbake til true i etterkant, hvis ikke feiler påfølgende tester som trenger at den er satt til true
     */
    fun leggTilTilganger(
        personIdentTilHarTilgang: List<PersonTilgang>,
        godkjennDefault: Boolean = false,
    ) {
        tilgangsmaskin.tilganger.putAll(personIdentTilHarTilgang.associateBy { it.personIdent })
        tilgangsmaskin.godkjennByDefault = godkjennDefault
    }

    fun reset() {
        tilgangsmaskin.tilganger.clear()
        tilgangsmaskin.kall.clear()
        tilgangsmaskin.godkjennByDefault = true
    }

    class StyrbarTilgangsmaskinKlient : TilgangsmaskinKlient(URI("dummyURI"), mockk(relaxed = true)) {
        val tilganger = mutableMapOf<String, PersonTilgang>()
        val kall = mutableListOf<Set<String>>()
        var godkjennByDefault = true

        override fun sjekkTilgangTilPersoner(
            personIdenter: Set<String>,
            regeltype: Regeltype,
        ): List<TilgangsmaskinResultat> {
            kall.add(personIdenter)
            return personIdenter.map { personIdent ->
                tilganger[personIdent]?.tilTilgangsmaskinResultat() ?: standardsvar(personIdent)
            }
        }

        private fun standardsvar(personIdent: String) =
            if (godkjennByDefault) {
                TilgangsmaskinResultat(personIdent = personIdent, harTilgang = true, httpStatus = 204)
            } else {
                TilgangsmaskinResultat(personIdent = personIdent, harTilgang = false, httpStatus = 403, avvisningskode = Avvisningskode.UKJENT)
            }

        private fun PersonTilgang.tilTilgangsmaskinResultat() =
            TilgangsmaskinResultat(
                personIdent = personIdent,
                harTilgang = harTilgang,
                httpStatus = if (harTilgang) 204 else 403,
                avvisningskode = avvisning?.avvisningskode,
                begrunnelse = avvisning?.begrunnelse,
            )
    }
}
