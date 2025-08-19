package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import java.time.LocalDate

data class PdlOppholdsadressePerson(
    val oppholdsadresse: List<Oppholdsadresse>,
)

data class Oppholdsadresse(
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
    val oppholdAnnetSted: String? = null,
) {
    internal fun erPåSvalbard(): Boolean = oppholdAnnetSted == "PAA_SVALBARD"
}

fun List<Oppholdsadresse>.oppholdsadresseErPåSvalbardPåDato(dato: LocalDate): Boolean = hentForDato(dato)?.erPåSvalbard() ?: false

private fun List<Oppholdsadresse>.hentForDato(dato: LocalDate): Oppholdsadresse? =
    filter { it.gyldigFraOgMed != null }
        .sortedBy { it.gyldigFraOgMed }
        .lastOrNull {
            it.gyldigFraOgMed!!.isSameOrBefore(dato) &&
                (it.gyldigTilOgMed == null || it.gyldigTilOgMed.isSameOrAfter(dato))
        }
