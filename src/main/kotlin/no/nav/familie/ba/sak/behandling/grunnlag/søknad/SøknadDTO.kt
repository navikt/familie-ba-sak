package no.nav.familie.ba.sak.behandling.grunnlag.søknad

import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.kontrakter.felles.objectMapper
import java.time.LocalDate

data class SøknadDTO(
        val kategori: BehandlingKategori,
        val underkategori: BehandlingUnderkategori,
        val typeSøker: TypeSøker = TypeSøker.ORDINÆR,
        val søkerMedOpplysninger: PartMedOpplysninger,
        val barnaMedOpplysninger: List<PartMedOpplysninger>,
        val annenPartIdent: String
)

fun SøknadDTO.writeValueAsString(): String = objectMapper.writeValueAsString(this)

data class PartMedOpplysninger(
        val ident: String,
        val personType: PersonType,
        val opphold: Opphold
)

data class Opphold(
        val oppholderSegINorge: Boolean = true,
        val harOppholdtSegINorgeSiste12Måneder: Boolean = true,
        val komTilNorge: LocalDate? = null,
        val skalOppholdeSegINorgeNeste12Måneder: Boolean = true,
        val tilleggsopplysninger: String? = null
)

enum class TypeSøker {
    ORDINÆR, INSTITUSJON, TREDJELANDSBORGER, EØS_BORGER
}