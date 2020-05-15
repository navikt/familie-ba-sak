package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.kontrakter.felles.objectMapper
import java.time.LocalDate

data class SøknadDTO(
        val versjon: String = "1",
        val kategori: BehandlingKategori,
        val underkategori: BehandlingUnderkategori,
        val typeSøker: TypeSøker = TypeSøker.ORDINÆR,
        val søkerMedOpplysninger: SøkerMedOpplysninger,
        val barnaMedOpplysninger: List<BarnMedOpplysninger>,
        val annenPartIdent: String
)

fun SøknadDTO.writeValueAsString(): String = objectMapper.writeValueAsString(this)

data class SøkerMedOpplysninger(
        val ident: String,
        val oppholderSegINorge: Boolean = true,
        val harOppholdtSegINorgeSiste12Måneder: Boolean = true,
        val komTilNorge: LocalDate? = null,
        val skalOppholdeSegINorgeNeste12Måneder: Boolean = true,
        val tilleggsopplysninger: String? = null
)

data class BarnMedOpplysninger(
        val ident: String,
        val borMedSøker: Boolean = true,
        val oppholderSegINorge: Boolean = true,
        val harOppholdtSegINorgeSiste12Måneder: Boolean = true,
        val navn: String = "",
        val inkludertISøknaden: Boolean = true,
        val fødselsdato: LocalDate? = null,
        val tilleggsopplysninger: String? = null
)

enum class TypeSøker {
    ORDINÆR, INSTITUSJON, TREDJELANDSBORGER, EØS_BORGER
}