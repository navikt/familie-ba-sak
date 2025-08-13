package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.kontrakter.ba.finnmarkstillegg.kommuneErIFinnmarkEllerNordTroms
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import java.time.LocalDate

private val FØRSTE_RELEVANTE_ADRESSEDATO_FOR_FINNMARKSTILLEGG = LocalDate.of(2025, 9, 30)

data class BostedsadresserOgDelteBosteder(
    val bostedsadresser: List<Adresse>,
    val delteBosteder: List<Adresse>,
) {
    fun harBostedsadresseEllerDeltBostedSomErRelevantForFinnmarkstillegg(): Boolean =
        bostedsadresser.filtrerUtAdresserSomErRelevanteForFinnmarkstillegg().any { it.erIFinnmarkEllerNordTroms() } ||
            delteBosteder.filtrerUtAdresserSomErRelevanteForFinnmarkstillegg().any { it.erIFinnmarkEllerNordTroms() }

    private fun List<Adresse>.filtrerUtAdresserSomErRelevanteForFinnmarkstillegg(): List<Adresse> =
        filter { it.gyldigFraOgMed != null }
            .sortedBy { it.gyldigFraOgMed }
            .let { sorterteAdresser ->
                val adressePer30Sept2025 =
                    sorterteAdresser.lastOrNull {
                        it.gyldigFraOgMed!!.isSameOrBefore(FØRSTE_RELEVANTE_ADRESSEDATO_FOR_FINNMARKSTILLEGG) &&
                            (it.gyldigTilOgMed == null || it.gyldigTilOgMed.isSameOrAfter(FØRSTE_RELEVANTE_ADRESSEDATO_FOR_FINNMARKSTILLEGG))
                    }
                sorterteAdresser.dropWhile { it != adressePer30Sept2025 }
            }
}

data class Adresse(
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
    val vegadresse: Vegadresse? = null,
    val matrikkeladresse: Matrikkeladresse? = null,
    val ukjentBosted: UkjentBosted? = null,
)

fun Bostedsadresse.tilAdresse(): Adresse =
    Adresse(
        gyldigFraOgMed = gyldigFraOgMed,
        gyldigTilOgMed = gyldigTilOgMed,
        vegadresse = vegadresse,
        matrikkeladresse = matrikkeladresse,
        ukjentBosted = ukjentBosted,
    )

fun DeltBosted.tilAdresse(): Adresse =
    Adresse(
        gyldigFraOgMed = startdatoForKontrakt,
        gyldigTilOgMed = startdatoForKontrakt,
        vegadresse = vegadresse,
        matrikkeladresse = matrikkeladresse,
        ukjentBosted = ukjentBosted,
    )

fun Adresse?.erIFinnmarkEllerNordTroms(): Boolean =
    this?.vegadresse?.kommunenummer?.let { kommuneErIFinnmarkEllerNordTroms(it) }
        ?: this?.matrikkeladresse?.kommunenummer?.let { kommuneErIFinnmarkEllerNordTroms(it) }
        ?: this?.ukjentBosted?.bostedskommune?.let { kommuneErIFinnmarkEllerNordTroms(it) }
        ?: false
