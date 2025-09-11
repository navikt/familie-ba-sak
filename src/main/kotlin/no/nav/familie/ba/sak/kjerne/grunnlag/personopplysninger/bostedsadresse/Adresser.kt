package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse

import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlBostedsadresseDeltBostedOppholdsadressePerson
import java.time.LocalDate

private val FØRSTE_RELEVANTE_ADRESSEDATO_FOR_FINNMARKSTILLEGG = LocalDate.of(2025, 9, 30)
private val FØRSTE_RELEVANTE_ADRESSEDATO_FOR_SVALBARDSTILLEGG = LocalDate.of(2025, 9, 30)

data class Adresser(
    val bostedsadresser: List<Adresse>,
    val delteBosteder: List<Adresse>,
    val oppholdsadresse: List<Adresse>,
) {
    fun harAdresserSomErRelevantForFinnmarkstillegg(): Boolean {
        val harBostedsadresserRelevantForFinnmarkstillegg = finnAdresserGyldigPåDato(bostedsadresser, FØRSTE_RELEVANTE_ADRESSEDATO_FOR_FINNMARKSTILLEGG).any { it.erIFinnmarkEllerNordTroms() }
        val harDeltBostederRelevantForFinnmarkstillegg = finnAdresserGyldigPåDato(delteBosteder, FØRSTE_RELEVANTE_ADRESSEDATO_FOR_FINNMARKSTILLEGG).any { it.erIFinnmarkEllerNordTroms() }
        return harBostedsadresserRelevantForFinnmarkstillegg || harDeltBostederRelevantForFinnmarkstillegg
    }

    fun harAdresserSomErRelevantForSvalbardstillegg(): Boolean = finnAdresserGyldigPåDato(oppholdsadresse, FØRSTE_RELEVANTE_ADRESSEDATO_FOR_SVALBARDSTILLEGG).any { it.erPåSvalbard() }

    companion object {
        fun opprettFra(pdlAdresser: PdlBostedsadresseDeltBostedOppholdsadressePerson?): Adresser =
            Adresser(
                bostedsadresser = pdlAdresser?.bostedsadresse?.map { Adresse.opprettFra(it) } ?: emptyList(),
                delteBosteder = pdlAdresser?.deltBosted?.map { Adresse.opprettFra(it) } ?: emptyList(),
                oppholdsadresse = pdlAdresser?.oppholdsadresse?.map { Adresse.opprettFra(it) } ?: emptyList(),
            )
    }
}

private fun finnAdresserGyldigPåDato(
    adresseer: List<Adresse>,
    dato: LocalDate,
): List<Adresse> {
    val sorterteAdresser = adresseer.filter { it.gyldigFraOgMed != null }.sortedBy { it.gyldigFraOgMed }
    val sisteGyldigeAdressePåDato = sorterteAdresser.lastOrNull { it.erGyldigPåDato(dato) }
    return sorterteAdresser.dropWhile { it != sisteGyldigeAdressePåDato }
}
