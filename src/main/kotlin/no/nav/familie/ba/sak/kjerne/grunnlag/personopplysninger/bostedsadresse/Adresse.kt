package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.kontrakter.ba.finnmarkstillegg.kommuneErIFinnmarkEllerNordTroms
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import no.nav.familie.kontrakter.felles.svalbard.erKommunePåSvalbard
import java.time.LocalDate

private val SVALBARDVERDIER_FOR_OPPHOLD_ANNET_STED = setOf("PAA_SVALBARD", "paaSvalbard", OppholdAnnetSted.PAA_SVALBARD.name)

data class Adresse(
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
    val vegadresse: Vegadresse? = null,
    val matrikkeladresse: Matrikkeladresse? = null,
    val ukjentBosted: UkjentBosted? = null,
    val oppholdAnnetSted: String? = null,
) {
    fun erGyldigPåDato(dato: LocalDate): Boolean {
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
        val erOppholdAnnetStedPåSvalbard = SVALBARDVERDIER_FOR_OPPHOLD_ANNET_STED.contains(oppholdAnnetSted)
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
            )

        fun opprettFra(deltBosted: DeltBosted) =
            Adresse(
                gyldigFraOgMed = deltBosted.startdatoForKontrakt,
                gyldigTilOgMed = deltBosted.sluttdatoForKontrakt,
                vegadresse = deltBosted.vegadresse,
                matrikkeladresse = deltBosted.matrikkeladresse,
                ukjentBosted = deltBosted.ukjentBosted,
            )

        fun opprettFra(oppholdsadresse: Oppholdsadresse) =
            Adresse(
                gyldigFraOgMed = oppholdsadresse.gyldigFraOgMed,
                gyldigTilOgMed = oppholdsadresse.gyldigTilOgMed,
                vegadresse = oppholdsadresse.vegadresse,
                matrikkeladresse = oppholdsadresse.matrikkeladresse,
                oppholdAnnetSted = oppholdsadresse.oppholdAnnetSted,
            )
    }
}
