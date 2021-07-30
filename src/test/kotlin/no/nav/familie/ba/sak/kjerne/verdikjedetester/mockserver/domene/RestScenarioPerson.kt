package no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene

import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak

data class RestScenarioPerson(
        val ident: String? = null, // Settes av mock-server
        val aktørId: String? = null, // Settes av mock-server
        val familierelasjoner: List<Familierelasjon>? = emptyList(), // Settes av mock-server
        val fødselsdato: String, //yyyy-mm-dd
        val fornavn: String,
        val etternavn: String,
        val infotrygdSaker: InfotrygdSøkResponse<Sak>? = null,
) {

    val navn = "$fornavn $etternavn"
}

data class Familierelasjon(
        val relatertPersonsIdent: String,
        val relatertPersonsRolle: String
)

