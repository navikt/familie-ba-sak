package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.BostedsadresserOgDelteBosteder
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.tilAdresse
import no.nav.familie.kontrakter.ba.finnmarkstillegg.kommuneErIFinnmarkEllerNordTroms
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import java.time.LocalDate

private val FØRSTE_RELEVANTE_ADRESSEDATO_FOR_FINNMARKSTILLEGG = LocalDate.of(2025, 9, 30)

data class PdlBostedsadresseOgDeltBostedPerson(
    val bostedsadresse: List<Bostedsadresse>,
    val deltBosted: List<DeltBosted>,
) {
    fun harBostedsadresseEllerDeltBostedSomErRelevantForFinnmarkstillegg(): Boolean =
        bostedsadresserSomErRelevanteForFinnmarkstillegg().any { it.erIFinnmarkEllerNordTroms() } ||
            deltBostedSomErRelevanteForFinnmarkstillegg().any { it.erIFinnmarkEllerNordTroms() }

    private fun bostedsadresserSomErRelevanteForFinnmarkstillegg(): List<Bostedsadresse> =
        bostedsadresse.sortedBy { it.gyldigFraOgMed }.let { sorterteAdresser ->
            val adressePer30Sept2025 =
                sorterteAdresser.lastOrNull {
                    it.gyldigFraOgMed!!.isSameOrBefore(FØRSTE_RELEVANTE_ADRESSEDATO_FOR_FINNMARKSTILLEGG) &&
                        (it.gyldigTilOgMed == null || it.gyldigTilOgMed!!.isSameOrAfter(FØRSTE_RELEVANTE_ADRESSEDATO_FOR_FINNMARKSTILLEGG))
                }
            sorterteAdresser.dropWhile { it != adressePer30Sept2025 }
        }

    private fun deltBostedSomErRelevanteForFinnmarkstillegg(): List<DeltBosted> =
        deltBosted.sortedBy { it.startdatoForKontrakt }.let { sorterteDeltBosted ->
            val deltBostedPer30Sept2025 =
                sorterteDeltBosted.lastOrNull {
                    it.startdatoForKontrakt!!.isSameOrBefore(FØRSTE_RELEVANTE_ADRESSEDATO_FOR_FINNMARKSTILLEGG) &&
                        (it.sluttdatoForKontrakt == null || it.sluttdatoForKontrakt!!.isSameOrAfter(FØRSTE_RELEVANTE_ADRESSEDATO_FOR_FINNMARKSTILLEGG))
                }
            sorterteDeltBosted.dropWhile { it != deltBostedPer30Sept2025 }
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

fun PdlBostedsadresseOgDeltBostedPerson?.tilAdresser(): BostedsadresserOgDelteBosteder =
    BostedsadresserOgDelteBosteder(
        bostedsadresser = this?.let { bostedsadresse.map { it.tilAdresse() } } ?: emptyList(),
        delteBosteder = this?.let { deltBosted.map { it.tilAdresse() } } ?: emptyList(),
    )
