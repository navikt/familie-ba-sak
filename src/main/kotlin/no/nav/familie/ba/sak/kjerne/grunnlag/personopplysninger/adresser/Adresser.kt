package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser

import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdresserPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.time.LocalDate

private val FØRSTE_RELEVANTE_ADRESSEDATO_FOR_FINNMARKSTILLEGG = LocalDate.of(2025, 9, 30)
private val FØRSTE_RELEVANTE_ADRESSEDATO_FOR_SVALBARDSTILLEGG = LocalDate.of(2025, 9, 30)

data class Adresser(
    val bostedsadresser: List<Adresse>,
    val delteBosteder: List<Adresse>,
    val oppholdsadresse: List<Adresse>,
) {
    fun harAdresserSomErRelevantForFinnmarkstillegg(): Boolean {
        val harBostedsadresserRelevantForFinnmarkstillegg = bostedsadresser.finnAdressehistorikkFraOgMedDato(FØRSTE_RELEVANTE_ADRESSEDATO_FOR_FINNMARKSTILLEGG).any { it.erIFinnmarkEllerNordTroms() }
        val harDeltBostederRelevantForFinnmarkstillegg = delteBosteder.finnAdressehistorikkFraOgMedDato(FØRSTE_RELEVANTE_ADRESSEDATO_FOR_FINNMARKSTILLEGG).any { it.erIFinnmarkEllerNordTroms() }
        return harBostedsadresserRelevantForFinnmarkstillegg || harDeltBostederRelevantForFinnmarkstillegg
    }

    fun harAdresserSomErRelevantForSvalbardtillegg(): Boolean = oppholdsadresse.finnAdressehistorikkFraOgMedDato(FØRSTE_RELEVANTE_ADRESSEDATO_FOR_SVALBARDSTILLEGG).any { it.erPåSvalbard() }

    fun lagErOppholdsadresserPåSvalbardTidslinje(): Tidslinje<Boolean> = oppholdsadresse.filtrereUgyldigeOppholdsadresser().lagTidslinjeForAdresser("Oppholdsadresse") { it.erPåSvalbard() }

    fun lagErDeltBostedIFinnmarkEllerNordTromsTidslinje(): Tidslinje<Boolean> {
        val filtrerteAdresser = delteBosteder.filtrereUgyldigeAdresser()
        val tidslinjer =
            filtrerteAdresser.map { adresse ->
                listOf(adresse).lagTidslinjeForAdresser("Delt bostedadresse") { it.erIFinnmarkEllerNordTroms() }
            }

        val deltBostedTidslinje =
            tidslinjer.fold(tomTidslinje<Boolean>()) { kombinertTidslinje, nesteTidslinje ->
                kombinertTidslinje.kombinerMed(nesteTidslinje) { kombinertVerdi, nesteVerdi ->
                    (kombinertVerdi == true) || (nesteVerdi == true)
                }
            }
        return deltBostedTidslinje
    }

    fun lagErBostedsadresseIFinnmarkEllerNordTromsTidslinje(): Tidslinje<Boolean> {
        val filtrerteAdresser = bostedsadresser.filtrereUgyldigeAdresser()
        return filtrerteAdresser.lagTidslinjeForAdresser("Bostedadresse") { it.erIFinnmarkEllerNordTroms() }
    }

    fun lagErBosattINorgeTidslinje(): Tidslinje<Boolean> =
        bostedsadresser
            .filtrereUgyldigeAdresser()
            .lagTidslinjeForAdresser("Bostedadresse") { adresse -> adresse.erINorge() }

    fun lagErBosattINorgeEllerSvalbardTidslinje(): Tidslinje<Boolean> {
        val bostedsadresseINorgeTidslinje = lagErBosattINorgeTidslinje()
        val oppholdsadressePåSvalbardTidslinje = lagErOppholdsadresserPåSvalbardTidslinje()
        return bostedsadresseINorgeTidslinje
            .kombinerMed(oppholdsadressePåSvalbardTidslinje) { erBosattINorge, erBosattPåSvalbard ->
                when {
                    erBosattINorge == true || erBosattPåSvalbard == true -> true
                    erBosattINorge == false && erBosattPåSvalbard == false -> false
                    else -> null
                }
            }.tilPerioderIkkeNull()
            .tilTidslinje()
    }

    fun lagErBosattIFinnmarkEllerNordTromsTidslinje(): Tidslinje<Boolean> {
        val bostedsadresseIFinnmarkEllerNordTromsTidslinje = lagErBostedsadresseIFinnmarkEllerNordTromsTidslinje()
        val deltBostedIFinnmarkEllerNordTromsTidslinje = lagErDeltBostedIFinnmarkEllerNordTromsTidslinje()
        return bostedsadresseIFinnmarkEllerNordTromsTidslinje
            .kombinerMed(deltBostedIFinnmarkEllerNordTromsTidslinje) { erBostedsadresseIFinnmarkEllerNordTroms, erDeltBostedIFinnmarkEllerNordTroms ->
                erBostedsadresseIFinnmarkEllerNordTroms == true || erDeltBostedIFinnmarkEllerNordTroms == true
            }
    }

    companion object {
        fun opprettFra(pdlAdresser: PdlAdresserPerson?): Adresser =
            Adresser(
                bostedsadresser = pdlAdresser?.bostedsadresse?.map { Adresse.opprettFra(it) } ?: emptyList(),
                delteBosteder = pdlAdresser?.deltBosted?.map { Adresse.opprettFra(it) } ?: emptyList(),
                oppholdsadresse = pdlAdresser?.oppholdsadresse?.map { Adresse.opprettFra(it) } ?: emptyList(),
            )

        fun opprettFra(person: Person): Adresser =
            Adresser(
                bostedsadresser = person.bostedsadresser.map { it.tilAdresse() },
                delteBosteder = person.deltBosted.map { it.tilAdresse() },
                oppholdsadresse = person.oppholdsadresser.map { it.tilAdresse() },
            )
    }
}
