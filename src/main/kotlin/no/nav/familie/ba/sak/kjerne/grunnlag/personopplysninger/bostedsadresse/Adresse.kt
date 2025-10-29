package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.kontrakter.ba.finnmarkstillegg.kommuneErIFinnmarkEllerNordTroms
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.Folkeregistermetadata
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import no.nav.familie.kontrakter.felles.svalbard.erKommunePåSvalbard
import java.time.LocalDate

data class Adresse(
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
    val vegadresse: Vegadresse? = null,
    val matrikkeladresse: Matrikkeladresse? = null,
    val ukjentBosted: UkjentBosted? = null,
    val oppholdAnnetSted: OppholdAnnetSted? = null,
    val folkeregistermetadata: Folkeregistermetadata? = null,
) {
    /**
     * Dette kan oppstå ved dårlig datakvalitet.
     */
    fun erFomOgTomNull() = gyldigFraOgMed == null && gyldigTilOgMed == null

    fun erFomOgTomSamme() = gyldigFraOgMed == gyldigTilOgMed

    fun erFomEtterTom() = gyldigFraOgMed != null && gyldigTilOgMed != null && gyldigFraOgMed.isAfter(gyldigTilOgMed)

    fun overlapperMedDato(dato: LocalDate): Boolean {
        val harGyldigFraOgMed = gyldigFraOgMed == null || gyldigFraOgMed.isSameOrBefore(dato)
        val harGyldigTilOgMed = gyldigTilOgMed == null || gyldigTilOgMed.isSameOrAfter(dato)
        return harGyldigFraOgMed && harGyldigTilOgMed
    }

    fun erIFinnmarkEllerNordTroms(): Boolean {
        val erVegadresseIFinnmarkEllerNordTroms =
            vegadresse?.kommunenummer?.let { kommuneErIFinnmarkEllerNordTroms(it) } == true
        val erMatrikkeladresseIFinnmarkEllerNordTroms =
            matrikkeladresse?.kommunenummer?.let { kommuneErIFinnmarkEllerNordTroms(it) } == true
        val erUkjentBostedIFinnmarkEllerNordTroms =
            ukjentBosted?.bostedskommune?.let { kommuneErIFinnmarkEllerNordTroms(it) } == true
        return erVegadresseIFinnmarkEllerNordTroms || erMatrikkeladresseIFinnmarkEllerNordTroms || erUkjentBostedIFinnmarkEllerNordTroms
    }

    fun erPåSvalbard(): Boolean {
        val erVegadressePåSvalbard = vegadresse?.kommunenummer?.let { erKommunePåSvalbard(it) } == true
        val erMatrikkeladressePåSvalbard = matrikkeladresse?.kommunenummer?.let { erKommunePåSvalbard(it) } == true
        val erUkjentBostedPåSvalbard = ukjentBosted?.bostedskommune?.let { erKommunePåSvalbard(it) } == true
        val erOppholdAnnetStedPåSvalbard = oppholdAnnetSted === OppholdAnnetSted.PAA_SVALBARD
        return erVegadressePåSvalbard || erMatrikkeladressePåSvalbard || erUkjentBostedPåSvalbard || erOppholdAnnetStedPåSvalbard
    }

    fun erINorge(): Boolean = vegadresse != null || matrikkeladresse != null || ukjentBosted != null

    companion object {
        fun opprettFra(bostedsadresse: Bostedsadresse) =
            Adresse(
                gyldigFraOgMed = bostedsadresse.gyldigFraOgMed,
                gyldigTilOgMed = bostedsadresse.gyldigTilOgMed,
                vegadresse = bostedsadresse.vegadresse,
                matrikkeladresse = bostedsadresse.matrikkeladresse,
                ukjentBosted = bostedsadresse.ukjentBosted,
                folkeregistermetadata = bostedsadresse.folkeregistermetadata,
            )

        fun opprettFra(deltBosted: DeltBosted) =
            Adresse(
                gyldigFraOgMed = deltBosted.startdatoForKontrakt,
                gyldigTilOgMed = deltBosted.sluttdatoForKontrakt,
                vegadresse = deltBosted.vegadresse,
                matrikkeladresse = deltBosted.matrikkeladresse,
                ukjentBosted = deltBosted.ukjentBosted,
                folkeregistermetadata = deltBosted.folkeregistermetadata,
            )

        fun opprettFra(oppholdsadresse: Oppholdsadresse): Adresse =
            Adresse(
                gyldigFraOgMed = oppholdsadresse.gyldigFraOgMed,
                gyldigTilOgMed = oppholdsadresse.gyldigTilOgMed,
                vegadresse = oppholdsadresse.vegadresse,
                matrikkeladresse = oppholdsadresse.matrikkeladresse,
                oppholdAnnetSted = OppholdAnnetSted.parse(oppholdsadresse.oppholdAnnetSted),
                folkeregistermetadata = oppholdsadresse.folkeregistermetadata,
            )
    }
}

fun List<Adresse>.hentForDato(dato: LocalDate): Adresse? = finnAdressehistorikkFraOgMedDato(this, dato).firstOrNull()

fun finnAdressehistorikkFraOgMedDato(
    adresser: List<Adresse>,
    dato: LocalDate,
): List<Adresse> {
    val sorterteAdresser = adresser.filter { it.gyldigFraOgMed != null }.sortedBy { it.gyldigFraOgMed }
    val sisteAdresseSomOverlapperDato = sorterteAdresser.lastOrNull { it.overlapperMedDato(dato) }
    return sorterteAdresser.dropWhile { it != sisteAdresseSomOverlapperDato }
}
