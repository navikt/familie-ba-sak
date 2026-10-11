package no.nav.familie.ba.sak.integrasjoner.tilgangsmaskin

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.ba.sak.sikkerhet.PersonTilgang
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.tilgangsmaskin.Avvisningskode
import no.nav.familie.tilgangsmaskin.Regeltype
import no.nav.familie.tilgangsmaskin.TilgangsmaskinKlient
import no.nav.familie.tilgangsmaskin.TilgangsmaskinResultat
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.net.URI

@Component
class TilgangsmaskinTilgangskontrollKlient(
    @Value("\${TILGANGSMASKIN_API_URL}") private val tilgangsmaskinUri: URI,
    private val tilgangsmaskinKlient: TilgangsmaskinKlient,
) {
    fun sjekkTilgangTilPersoner(personIdenter: Set<String>): List<PersonTilgang> {
        if (SikkerhetContext.erSystemKontekst()) {
            return personIdenter.map { PersonTilgang.medTilgang(it) }
        }

        if (personIdenter.any { it.isBlank() }) {
            throw Feil(message = "Kan ikke sjekke tilgang til en person uten ident", httpStatus = HttpStatus.INTERNAL_SERVER_ERROR)
        }

        val resultatPerIdent =
            kallEksternTjeneste<List<TilgangsmaskinResultat>>(
                tjeneste = "tilgangsmaskin",
                uri = tilgangsmaskinUri,
                formål = "Sjekk tilgang til personer",
            ) {
                tilgangsmaskinKlient.sjekkTilgangTilPersoner(personIdenter, Regeltype.KJERNE_REGELTYPE)
            }.associateBy { it.personIdent }

        // Tilgangene caches per saksbehandler, og kallere forventer ett svar per ident. Vi feiler derfor heller enn å
        // returnere et ufullstendig svar eller cache et avslag som bare skyldes at Tilgangsmaskinen ikke ga oss et svar.
        val identerUtenGyldigSvar = personIdenter.filter { resultatPerIdent[it]?.erGyldigSvar() != true }
        if (identerUtenGyldigSvar.isNotEmpty()) {
            val traceIder = identerUtenGyldigSvar.mapNotNull { resultatPerIdent[it]?.traceId }
            throw Feil(
                message =
                    "Fikk ikke gyldig svar fra Tilgangsmaskinen for ${identerUtenGyldigSvar.size} av ${personIdenter.size} identer. " +
                        "traceIder=$traceIder",
                frontendFeilmelding = "Klarte ikke å sjekke tilgang til personene. Prøv igjen senere.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            )
        }

        val resultater = personIdenter.map { resultatPerIdent.getValue(it) }
        loggAvvisninger(resultater)
        return resultater.map { it.tilPersonTilgang() }
    }

    // Tilgangsmaskinen gir ingen tilgangsbeslutning når den svarer med en serverfeil for en ident.
    // Det gjelder også svaret klienten fyller inn for identer Tilgangsmaskinen ikke svarte for.
    private fun TilgangsmaskinResultat.erGyldigSvar(): Boolean = httpStatus !in SERVERFEIL

    private fun loggAvvisninger(resultater: List<TilgangsmaskinResultat>) {
        val avvisninger = resultater.filterNot { it.harTilgang }
        if (avvisninger.isEmpty()) return

        logger.info(
            "Tilgangsmaskinen avviste tilgang til ${avvisninger.size} av ${resultater.size} personer. " +
                "avvisningskoder=${avvisninger.map { it.avvisningskode }}, traceIder=${avvisninger.mapNotNull { it.traceId }}",
        )
    }

    private fun TilgangsmaskinResultat.tilPersonTilgang(): PersonTilgang =
        if (harTilgang) {
            PersonTilgang.medTilgang(personIdent)
        } else {
            PersonTilgang.avvist(
                personIdent = personIdent,
                avvisningskode = avvisningskode ?: Avvisningskode.UKJENT,
                begrunnelse = begrunnelse ?: "Tilgangsmaskinen ga ikke tilgang til personen (HTTP $httpStatus)",
            )
        }

    companion object {
        private val logger = LoggerFactory.getLogger(TilgangsmaskinTilgangskontrollKlient::class.java)
        private val SERVERFEIL = 500..599
    }
}
