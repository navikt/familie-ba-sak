package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.kontrakter.felles.objectMapper
import java.time.LocalDate

data class RestRegistrerSøknad(
        val søknad: SøknadDTO,
        val bekreftEndringerViaFrontend: Boolean
)

data class SøknadDTO(
        val underkategori: BehandlingUnderkategori,
        val søkerMedOpplysninger: SøkerMedOpplysninger,
        val barnaMedOpplysninger: List<BarnMedOpplysninger>,
        val endringAvOpplysningerBegrunnelse: String
)

fun SøknadDTO.writeValueAsString(): String = objectMapper.writeValueAsString(this)

data class SøkerMedOpplysninger(
        val ident: String,
        val målform: Målform = Målform.NB
)

data class BarnMedOpplysninger(
        val ident: String,
        val navn: String = "",
        val fødselsdato: LocalDate? = null,
        val inkludertISøknaden: Boolean = true,
        val manueltRegistrert: Boolean = false
)