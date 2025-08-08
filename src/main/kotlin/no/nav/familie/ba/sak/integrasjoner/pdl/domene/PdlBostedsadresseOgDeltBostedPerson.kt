package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import no.nav.familie.kontrakter.ba.finnmarkstillegg.kommuneErIFinnmarkEllerNordTroms
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted

data class PdlBostedsadresseOgDeltBostedPerson(
    val bostedsadresse: List<Bostedsadresse>,
    val deltBosted: List<DeltBosted>,
) {
    fun nåværendeBostedEllerDeltBostedErIFinnmarkEllerNordTroms(): Boolean {
        val sisteBostedsadresse = bostedsadresse.filter { it.gyldigTilOgMed == null }.sortedBy { it.gyldigFraOgMed }.lastOrNull()
        val sisteDeltBosted = deltBosted.filter { it.sluttdatoForKontrakt == null }.sortedBy { it.startdatoForKontrakt }.lastOrNull()
        return sisteBostedsadresse.erIFinnmarkEllerNordTroms() || sisteDeltBosted.erIFinnmarkEllerNordTroms()
    }

    fun sisteFlyttingVarInnEllerUtAvFinnmarkEllerNordTroms(): Boolean {
        val bostedsadresseSortert = bostedsadresse.sortedByDescending { it.gyldigFraOgMed }
        val deltBostedSortert = deltBosted.sortedByDescending { it.startdatoForKontrakt }

        val sisteBostedsadresseErIFinnmarkEllerNordTroms = bostedsadresseSortert.getOrNull(0).erIFinnmarkEllerNordTroms()
        val nestSisteBostedsadresseErIFinnmarkEllerNordTroms = bostedsadresseSortert.getOrNull(1).erIFinnmarkEllerNordTroms()

        val sisteDeltBostedErIFinnmarkEllerNordTroms = deltBostedSortert.getOrNull(0).erIFinnmarkEllerNordTroms()
        val nestSisteDeltBostedErIFinnmarkEllerNordTroms = deltBostedSortert.getOrNull(1).erIFinnmarkEllerNordTroms()

        val bostedsadresseErEndretInnEllerUtAvFinnmarkEllerNordTroms =
            (sisteBostedsadresseErIFinnmarkEllerNordTroms || nestSisteBostedsadresseErIFinnmarkEllerNordTroms) &&
                sisteBostedsadresseErIFinnmarkEllerNordTroms != nestSisteBostedsadresseErIFinnmarkEllerNordTroms

        val deltBostedErEndretInnEllerUtAvFinnmarkEllerNordTroms =
            (sisteDeltBostedErIFinnmarkEllerNordTroms || nestSisteDeltBostedErIFinnmarkEllerNordTroms) &&
                sisteDeltBostedErIFinnmarkEllerNordTroms != nestSisteDeltBostedErIFinnmarkEllerNordTroms

        return bostedsadresseErEndretInnEllerUtAvFinnmarkEllerNordTroms || deltBostedErEndretInnEllerUtAvFinnmarkEllerNordTroms
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
