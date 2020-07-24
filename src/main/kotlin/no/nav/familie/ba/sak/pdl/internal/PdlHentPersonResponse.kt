package no.nav.familie.ba.sak.pdl.internal

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.personinfo.Bostedsadresse
import no.nav.familie.kontrakter.felles.personinfo.SIVILSTAND
import java.time.LocalDate

data class PdlHentPersonResponse(val data: PdlPerson,
                                 val errors: List<PdlError>?) {

    fun harFeil(): Boolean {
        return errors != null && errors.isNotEmpty()
    }

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }
}

data class PdlPerson(val person: PdlPersonData?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlPersonData(val foedsel: List<PdlFødselsDato>,
                         val navn: List<PdlNavn>,
                         val kjoenn: List<PdlKjoenn>,
                         val familierelasjoner: List<PdlFamilierelasjon> = emptyList(),
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

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlKjoenn(val kjoenn: Kjønn)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlFamilierelasjon(val relatertPersonsIdent: String,
                              val relatertPersonsRolle: FAMILIERELASJONSROLLE)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Adressebeskyttelse(
        val gradering: ADRESSEBESKYTTELSEGRADERING
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Sivilstand(
        val type: SIVILSTAND
)

enum class FAMILIERELASJONSROLLE {
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
