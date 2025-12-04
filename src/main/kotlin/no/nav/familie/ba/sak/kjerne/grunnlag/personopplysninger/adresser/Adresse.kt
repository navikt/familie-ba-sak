package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.kontrakter.ba.finnmarkstillegg.kommuneErIFinnmarkEllerNordTroms
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.Folkeregistermetadata
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import no.nav.familie.kontrakter.felles.personopplysning.UtenlandskAdresse
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import no.nav.familie.kontrakter.felles.svalbard.erKommunePåSvalbard
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import java.time.LocalDate

data class Adresse(
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
    val vegadresse: Vegadresse? = null,
    val matrikkeladresse: Matrikkeladresse? = null,
    val ukjentBosted: UkjentBosted? = null,
    val oppholdAnnetSted: OppholdAnnetSted? = null,
    val folkeregistermetadata: Folkeregistermetadata? = null,
    val utenlandskAdresse: UtenlandskAdresse? = null,
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
                gyldigFraOgMed = bostedsadresse.angittFlyttedato ?: bostedsadresse.gyldigFraOgMed,
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

fun Adresse.erSammeAdresse(annen: Adresse?): Boolean =
    when {
        annen == null -> false
        this.vegadresse != null -> this.vegadresse == annen.vegadresse
        this.matrikkeladresse != null -> this.matrikkeladresse == annen.matrikkeladresse
        else -> false
    }

fun List<Adresse>.hentForDato(dato: LocalDate): Adresse? = finnAdressehistorikkFraOgMedDato(dato).firstOrNull()

fun List<Adresse>.finnAdressehistorikkFraOgMedDato(
    dato: LocalDate,
): List<Adresse> {
    val sorterteAdresser = filter { it.gyldigFraOgMed != null }.sortedBy { it.gyldigFraOgMed }
    val sisteAdresseSomOverlapperDato = sorterteAdresser.lastOrNull { it.overlapperMedDato(dato) }
    return if (sisteAdresseSomOverlapperDato == null) {
        this
    } else {
        sorterteAdresser.dropWhile { it != sisteAdresseSomOverlapperDato }
    }
}

fun List<Adresse>.lagTidslinjeForAdresser(
    adressetype: String,
    operator: (Adresse) -> Boolean,
): Tidslinje<Boolean> {
    try {
        return windowed(size = 2, step = 1, partialWindows = true) {
            val denne = it.first()
            val neste = it.getOrNull(1)

            Periode(
                verdi = operator(denne),
                fom = denne.gyldigFraOgMed,
                tom = denne.gyldigTilOgMed ?: neste?.gyldigFraOgMed?.minusDays(1),
            )
        }.tilTidslinje()
    } catch (e: IllegalStateException) {
        secureLogger.error("Feil ved oppretting av tidslinjer for $adressetype med adresser $this", e)
        throw e
    } catch (e: IllegalArgumentException) {
        secureLogger.error("Feil ved oppretting av tidslinjer for $adressetype med adresser $this", e)
        throw e
    }
}

/**
 * Filtrerer ut adresser med ugyldige kombinasjoner av fom og tom
 * Distinkte perioder med lik fom og tom (prioriterer de som er i Finnmark eller NordTroms)
 **/
fun List<Adresse>.filtrereUgyldigeAdresser(): List<Adresse> {
    val filtrert =
        filterNot { it.erFomOgTomNull() || it.erFomOgTomSamme() || it.erFomEtterTom() }
            .groupBy { it.gyldigFraOgMed to it.gyldigTilOgMed }
            .values
            .map { likePerioder ->
                likePerioder.find { it.erIFinnmarkEllerNordTroms() } ?: likePerioder.first()
            }

    return filtrert.forskyvTilOgMedHvisDenErLikNesteFraOgMed()
}

/**
 * Filtrerer ut adresser med ugyldige kombinasjoner av fom og tom
 * Distinkte perioder med lik fom og tom (prioriterer de som er på Svalbard)
 **/
fun List<Adresse>.filtrereUgyldigeOppholdsadresser(): List<Adresse> {
    val filtrert =
        filterNot { it.erFomOgTomNull() || it.erFomOgTomSamme() || it.erFomEtterTom() }
            .groupBy { it.gyldigFraOgMed to it.gyldigTilOgMed }
            .values
            .map { likePerioder ->
                likePerioder.find { it.erPåSvalbard() } ?: likePerioder.first()
            }

    return filtrert.forskyvTilOgMedHvisDenErLikNesteFraOgMed()
}

private fun List<Adresse>.forskyvTilOgMedHvisDenErLikNesteFraOgMed(): List<Adresse> =
    sortedBy { it.gyldigFraOgMed }
        .windowed(size = 2, step = 1, partialWindows = true)
        .map { adresser ->
            val denne = adresser.first()
            val neste = adresser.getOrNull(1)

            if (denne.gyldigTilOgMed != null &&
                neste != null &&
                denne.gyldigTilOgMed == neste.gyldigFraOgMed
            ) {
                denne.copy(gyldigTilOgMed = denne.gyldigTilOgMed.minusDays(1))
            } else {
                denne
            }
        }
