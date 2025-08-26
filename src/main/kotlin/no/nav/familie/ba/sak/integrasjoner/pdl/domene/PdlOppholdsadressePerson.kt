package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.OppholdAnnetSted.PAA_SVALBARD
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import java.time.LocalDate

data class Oppholdsadresse(
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
    val oppholdAnnetSted: String? = null,
    val vegadresse: Vegadresse? = null,
    val matrikkeladresse: Matrikkeladresse? = null,
    val utenlandskAdresse: PdlUtenlandskAdresssePersonUtenlandskAdresse? = null,
) {
    internal fun erPåSvalbard(): Boolean = oppholdAnnetSted in setOf(PAA_SVALBARD.kode, PAA_SVALBARD.name)
}

enum class OppholdAnnetSted(
    val kode: String,
) {
    MILITAER("militaer"),
    PENDLER("pendler"),
    UTENRIKS("utenriks"),
    PAA_SVALBARD("paaSvalbard"),
    ;

    override fun toString(): String =
        when (this) {
            MILITAER -> "militær"
            PENDLER -> "pendler"
            UTENRIKS -> "utenriks"
            PAA_SVALBARD -> "Svalbard"
        }

    companion object {
        fun parse(verdi: String?): OppholdAnnetSted? = entries.firstOrNull { it.kode == verdi || it.name == verdi }
    }
}

fun List<Oppholdsadresse>.oppholdsadresseErPåSvalbardPåDato(dato: LocalDate): Boolean = hentForDato(dato)?.erPåSvalbard() ?: false

private fun List<Oppholdsadresse>.hentForDato(dato: LocalDate): Oppholdsadresse? =
    filter { it.gyldigFraOgMed != null }
        .sortedBy { it.gyldigFraOgMed }
        .lastOrNull {
            it.gyldigFraOgMed!!.isSameOrBefore(dato) &&
                (it.gyldigTilOgMed == null || it.gyldigTilOgMed.isSameOrAfter(dato))
        }
