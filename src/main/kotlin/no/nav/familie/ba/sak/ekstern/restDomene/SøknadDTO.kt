package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.kontrakter.felles.objectMapper
import java.time.LocalDate

data class RegistrerSøknadDto(
    val søknad: SøknadDTO,
    val bekreftEndringerViaFrontend: Boolean,
)

data class SøknadDTO(
    val underkategori: BehandlingUnderkategoriDTO,
    val søkerMedOpplysninger: SøkerMedOpplysninger,
    val barnaMedOpplysninger: List<BarnMedOpplysninger>,
    val endringAvOpplysningerBegrunnelse: String,
    val erAutomatiskRegistrert: Boolean = false,
)

fun SøknadDTO.writeValueAsString(): String = objectMapper.writeValueAsString(this)

data class SøkerMedOpplysninger(
    val ident: String,
    val målform: Målform = Målform.NB,
) {
    // Bruker init til å validere personidenten
    init {
        Fødselsnummer(ident)
    }
}

data class BarnMedOpplysninger(
    val ident: String,
    val navn: String = "",
    val fødselsdato: LocalDate? = null,
    val inkludertISøknaden: Boolean = true,
    val manueltRegistrert: Boolean = false,
    val erFolkeregistrert: Boolean = true,
)
