package no.nav.familie.ba.sak.pdl.internal

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.ForelderBarnRelasjon
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap

data class PdlHentPersonResponse(val data: PdlPerson,
                                 override val errors: List<PdlError>?)
    : PdlBaseResponse(errors)

data class PdlPerson(val person: PdlPersonData?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlPersonData(val foedsel: List<PdlFødselsDato>,
                         val navn: List<PdlNavn> = emptyList(),
                         val kjoenn: List<PdlKjoenn> = emptyList(),
                         val forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList(),
                         val adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
                         val sivilstand: List<Sivilstand>,
                         val bostedsadresse: List<Bostedsadresse>,
                         val opphold: List<Opphold> = emptyList(),
                         val statsborgerskap: List<Statsborgerskap> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlFødselsDato(val foedselsdato: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlError(val message: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlNavn(val fornavn: String,
                   val mellomnavn: String? = null,
                   val etternavn: String) {

    fun fulltNavn(): String {
        return when (mellomnavn) {
            null -> "$fornavn $etternavn"
            else -> "$fornavn $mellomnavn $etternavn"
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlKjoenn(val kjoenn: Kjønn)

