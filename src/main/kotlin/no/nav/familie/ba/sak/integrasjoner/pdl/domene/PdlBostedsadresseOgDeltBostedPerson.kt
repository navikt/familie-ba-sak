package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import no.nav.familie.kontrakter.ba.finnmarkstillegg.kommuneErIFinnmarkEllerNordTroms
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted

data class PdlBostedsadresseOgDeltBostedPerson(
    val bostedsadresse: List<Bostedsadresse>,
    val deltBosted: List<DeltBosted>,
) {
    fun nåværendeBostedEllerDeltBostedErIFinnmarkEllerNordTroms(): Boolean {
        val sisteBostedsadresse = bostedsadresse.filter { it.gyldigFraOgMed != null && it.gyldigTilOgMed == null }.maxByOrNull { it.gyldigFraOgMed!! }
        val sisteDeltBosted = deltBosted.filter { it.startdatoForKontrakt != null && it.sluttdatoForKontrakt == null }.maxByOrNull { it.sluttdatoForKontrakt!! }
        return sisteBostedsadresse.erIFinnmarkEllerNordTroms() || sisteDeltBosted.erIFinnmarkEllerNordTroms()
    }
}

fun Bostedsadresse?.erIFinnmarkEllerNordTroms(): Boolean =
    this?.vegadresse?.kommunenummer?.let { kommuneErIFinnmarkEllerNordTroms(it) }
        ?: this?.matrikkeladresse?.kommunenummer?.let { kommuneErIFinnmarkEllerNordTroms(it) }
        ?: this?.ukjentBosted?.bostedskommune?.let { kommuneErIFinnmarkEllerNordTroms(it) }
        ?: false

fun DeltBosted?.erIFinnmarkEllerNordTroms(): Boolean =
    this?.vegadresse?.kommunenummer?.let { kommuneErIFinnmarkEllerNordTroms(it) }
        ?: this?.matrikkeladresse?.kommunenummer?.let { kommuneErIFinnmarkEllerNordTroms(it) }
        ?: this?.ukjentBosted?.bostedskommune?.let { kommuneErIFinnmarkEllerNordTroms(it) }
        ?: false
