package no.nav.familie.ba.sak.pdl.internal

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.pdl.internal.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import java.time.LocalDate

data class PdlHentPersonResponse(val data: PdlPerson,
                                 override val errors: List<PdlError>?)
    : PdlBaseResponse(errors)

data class PdlPerson(val person: PdlPersonData?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlPersonData(val foedsel: List<PdlFødselsDato>,
                         val navn: List<PdlNavn>,
                         val kjoenn: List<PdlKjoenn>,
                         val forelderBarnRelasjon: List<PdlForelderBarnRelasjon> = emptyList(),
                         val adressebeskyttelse: List<Adressebeskyttelse>,
                         val bostedsadresse: List<Bostedsadresse?>,
                         val sivilstand: List<Sivilstand?>)

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

// TODO: Legg over denne i familie-kontrakter
@JsonIgnoreProperties(ignoreUnknown = true)
data class Bostedsadresse(val gyldigFraOgMed: LocalDate? = null,
                          val gyldigTilOgMed: LocalDate? = null,
                          val vegadresse: Vegadresse? = null,
                          val matrikkeladresse: Matrikkeladresse? = null,
                          val ukjentBosted: UkjentBosted? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlKjoenn(val kjoenn: Kjønn)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlForelderBarnRelasjon(val relatertPersonsIdent: String,
                                   val relatertPersonsRolle: FORELDERBARNRELASJONROLLE)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Adressebeskyttelse(
        val gradering: ADRESSEBESKYTTELSEGRADERING
)

// TODO: Legg over denne i familie-kontrakter
@JsonIgnoreProperties(ignoreUnknown = true)
data class Sivilstand(
        val type: SIVILSTAND,
        val gyldigFraOgMed: LocalDate? = null
)

enum class FORELDERBARNRELASJONROLLE {
    BARN,
    FAR,
    MEDMOR,
    MOR
}

enum class ADRESSEBESKYTTELSEGRADERING {
    STRENGT_FORTROLIG_UTLAND, // Kode 19
    FORTROLIG, // Kode 7
    STRENGT_FORTROLIG, // Kode 6
    UGRADERT
}
