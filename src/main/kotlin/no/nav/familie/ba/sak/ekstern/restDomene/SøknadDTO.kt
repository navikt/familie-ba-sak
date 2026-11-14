package no.nav.familie.ba.sak.ekstern.restDomene

import com.fasterxml.jackson.annotation.JsonAutoDetect
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.kontrakter.felles.jsonMapper
import java.time.LocalDate

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class RegistrerSøknadDto(
    val søknad: SøknadDTO,
    val bekreftEndringerViaFrontend: Boolean,
) {
    // Bruker init til å validere personidentene
    init {
        søknad.hentIdenterTilValgteBarn().forEach { Fødselsnummer(it) }
    }
}

data class SøknadDTO(
    val underkategori: BehandlingUnderkategoriDTO,
    val søkerMedOpplysninger: SøkerMedOpplysninger,
    val barnaMedOpplysninger: List<BarnMedOpplysninger>,
    val endringAvOpplysningerBegrunnelse: String = "",
    val erAutomatiskRegistrert: Boolean = false,
)

fun SøknadDTO.writeValueAsString(): String = jsonMapper.writeValueAsString(this)

fun SøknadDTO.hentIdenterTilValgteBarn(): List<String> =
    barnaMedOpplysninger
        .filter { it.inkludertISøknaden && it.erFolkeregistrert }
        .map { it.ident }

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
    val skjermesForBruker: Boolean = false,
)
