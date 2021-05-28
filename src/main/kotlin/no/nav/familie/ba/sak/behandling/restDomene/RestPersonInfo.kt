package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.pdl.internal.ForelderBarnRelasjon
import no.nav.familie.ba.sak.pdl.internal.ForelderBarnRelasjonMaskert
import no.nav.familie.ba.sak.pdl.internal.PersonInfo
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import java.time.LocalDate

data class RestPersonInfo(
        val personIdent: String,
        var fødselsdato: LocalDate? = null,
        val navn: String? = null,
        val kjønn: Kjønn? = null,
        val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
        var harTilgang: Boolean = true,
        val forelderBarnRelasjon: List<RestForelderBarnRelasjon> = emptyList(),
        val forelderBarnRelasjonMaskert: List<RestForelderBarnRelasjonnMaskert> = emptyList(),
)

data class RestForelderBarnRelasjon(
        val personIdent: String,
        val relasjonRolle: FORELDERBARNRELASJONROLLE,
        val navn: String,
        val fødselsdato: LocalDate?,
        val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null
)

data class RestForelderBarnRelasjonnMaskert(
        val relasjonRolle: FORELDERBARNRELASJONROLLE,
        val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING
)

private fun ForelderBarnRelasjonMaskert.tilRestForelderBarnRelasjonMaskert() = RestForelderBarnRelasjonnMaskert(
        relasjonRolle = this.relasjonsrolle,
        adressebeskyttelseGradering = this.adressebeskyttelseGradering
)

private fun ForelderBarnRelasjon.tilRestForelderBarnRelasjon() = RestForelderBarnRelasjon(
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
        forelderBarnRelasjon = this.forelderBarnRelasjon.map { it.tilRestForelderBarnRelasjon() },
        forelderBarnRelasjonMaskert = this.forelderBarnRelasjonMaskert.map { it.tilRestForelderBarnRelasjonMaskert() },
)