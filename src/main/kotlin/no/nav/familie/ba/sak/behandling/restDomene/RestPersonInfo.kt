package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.pdl.internal.*
import java.time.LocalDate

data class RestPersonInfo(
        val personIdent: String,
        var fødselsdato: LocalDate,
        val navn: String? = null,
        val kjønn: Kjønn? = null,
        val familierelasjoner: List<RestFamilierelasjon>,
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

fun FamilierelasjonMaskert.toRestFamilierelasjonMaskert() = RestFamilierelasjonMaskert(
        relasjonRolle = this.relasjonsrolle,
        adressebeskyttelseGradering = this.adressebeskyttelseGradering
)

fun Familierelasjon.toRestFamilieRelasjon() = RestFamilierelasjon(
        personIdent = this.personIdent.id,
        relasjonRolle = this.relasjonsrolle,
        navn = this.navn ?: "",
        fødselsdato = this.fødselsdato,
        adressebeskyttelseGradering = this.adressebeskyttelseGradering

)

fun PersonInfo.toRestPersonInfo(personIdent: String) = RestPersonInfo(
        personIdent = personIdent,
        fødselsdato = this.fødselsdato,
        navn = this.navn,
        kjønn = this.kjønn,
        familierelasjoner = this.familierelasjoner.map { it.toRestFamilieRelasjon() },
        familierelasjonerMaskert = this.familierelasjonerMaskert.map { it.toRestFamilierelasjonMaskert() }
)