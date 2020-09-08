package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.kontrakter.felles.objectMapper
import java.time.LocalDate

data class RestRegistrerSøknad(
        val søknad: SøknadDTO,
        val bekreftEndringerViaFrontend: Boolean
)

data class RestRegistrerSøknadGammel(
        val søknad: SøknadDTOGammel,
        val bekreftEndringerViaFrontend: Boolean
)

data class SøknadDTOGammel(
        val underkategori: BehandlingUnderkategori,
        val søkerMedOpplysninger: SøkerMedOpplysningerGammel,
        val barnaMedOpplysninger: List<BarnMedOpplysningerGammel>
)

fun SøknadDTOGammel.toSøknadDTO() = SøknadDTO(
        underkategori = this.underkategori,
        søkerMedOpplysninger = SøkerMedOpplysninger(ident = this.søkerMedOpplysninger.ident),
        barnaMedOpplysninger = this.barnaMedOpplysninger.map { barnMedOpplysninger ->
            BarnMedOpplysninger(ident = barnMedOpplysninger.ident,
                                inkludertISøknaden = barnMedOpplysninger.inkludertISøknaden,
                                navn = barnMedOpplysninger.navn,
                                fødselsdato = barnMedOpplysninger.fødselsdato,
                                manueltRegistrert = false
            )
        },
        endringAvOpplysningerBegrunnelse = ""
)

data class SøknadDTO(
        val underkategori: BehandlingUnderkategori,
        val søkerMedOpplysninger: SøkerMedOpplysninger,
        val barnaMedOpplysninger: List<BarnMedOpplysninger>,
        val endringAvOpplysningerBegrunnelse: String
)

fun SøknadDTO.toSøknadDTOGammel() = SøknadDTOGammel(
        underkategori = this.underkategori,
        søkerMedOpplysninger = SøkerMedOpplysningerGammel(ident = this.søkerMedOpplysninger.ident),
        barnaMedOpplysninger = this.barnaMedOpplysninger.map { barnMedOpplysninger ->
            BarnMedOpplysningerGammel(ident = barnMedOpplysninger.ident,
                                      inkludertISøknaden = barnMedOpplysninger.inkludertISøknaden,
                                      navn = barnMedOpplysninger.navn,
                                      fødselsdato = barnMedOpplysninger.fødselsdato)
        }
)

fun SøknadDTO.writeValueAsString(): String = objectMapper.writeValueAsString(this)

data class SøkerMedOpplysningerGammel(
        val ident: String
)

data class SøkerMedOpplysninger(
        val ident: String,
        val målform: Målform = Målform.NB
)

data class BarnMedOpplysningerGammel(
        val ident: String,
        val navn: String = "",
        val fødselsdato: LocalDate? = null,
        val inkludertISøknaden: Boolean = true
)

data class BarnMedOpplysninger(
        val ident: String,
        val navn: String = "",
        val fødselsdato: LocalDate? = null,
        val inkludertISøknaden: Boolean = true,
        val manueltRegistrert: Boolean = false
)