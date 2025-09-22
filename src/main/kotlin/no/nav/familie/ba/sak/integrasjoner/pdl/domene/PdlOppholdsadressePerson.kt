package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import java.time.LocalDate

internal fun Oppholdsadresse.erPåSvalbard(): Boolean = oppholdAnnetSted in setOf(OppholdAnnetSted.PAA_SVALBARD.kode, OppholdAnnetSted.PAA_SVALBARD.name)

fun List<Oppholdsadresse>.oppholdsadresseErPåSvalbardPåDato(dato: LocalDate): Boolean = hentForDato(dato)?.erPåSvalbard() ?: false

private fun List<Oppholdsadresse>.hentForDato(dato: LocalDate): Oppholdsadresse? =
    filter { it.gyldigFraOgMed != null }
        .sortedBy { it.gyldigFraOgMed }
        .lastOrNull {
            it.gyldigFraOgMed!!.isSameOrBefore(dato) &&
                (it.gyldigTilOgMed == null || it.gyldigTilOgMed!!.isSameOrAfter(dato))
        }
