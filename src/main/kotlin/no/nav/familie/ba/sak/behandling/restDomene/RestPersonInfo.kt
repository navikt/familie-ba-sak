package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.pdl.internal.*
import java.time.LocalDate

data class RestPersonInfo(
        val personIdent: String,
        var fødselsdato: LocalDate? = null,
        val navn: String? = null,
        val kjønn: Kjønn? = null,
        val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
        var harTilgang: Boolean = true,
        val familierelasjoner: List<RestFamilierelasjon> = emptyList(),
        val familierelasjonerMaskert: List<RestFamilierelasjonMaskert> = emptyList()
)

data class RestFamilierelasjon(
        val personIdent: String,
        val relasjonRolle: FAMILIERELASJONSROLLE,
        val navn: String,
        val fødselsdato: LocalDate?,
        val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null
)

data class RestFamilierelasjonMaskert(
        val relasjonRolle: FAMILIERELASJONSROLLE,
        val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING
)

private fun FamilierelasjonMaskert.tilRestFamilierelasjonMaskert() = RestFamilierelasjonMaskert(
        relasjonRolle = this.relasjonsrolle,
        adressebeskyttelseGradering = this.adressebeskyttelseGradering
)

private fun Familierelasjon.tilRestFamilieRelasjon() = RestFamilierelasjon(
        personIdent = this.personIdent.id,
        relasjonRolle = this.relasjonsrolle,
        navn = this.navn ?: "",
        fødselsdato = this.fødselsdato,
        adressebeskyttelseGradering = this.adressebeskyttelseGradering

)

fun PersonInfo.tilRestPersonInfo(personIdent: String) = RestPersonInfo(
        personIdent = personIdent,
        fødselsdato = this.fødselsdato,
        navn = this.navn,
        kjønn = this.kjønn,
        adressebeskyttelseGradering = this.adressebeskyttelseGradering,
        familierelasjoner = this.familierelasjoner.map { it.tilRestFamilieRelasjon() },
        familierelasjonerMaskert = this.familierelasjonerMaskert.map { it.tilRestFamilierelasjonMaskert() }
)