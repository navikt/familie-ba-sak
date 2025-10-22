package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed

fun lagErBostedsadresseIFinnmarkEllerNordTromsTidslinje(
    adresser: Adresser,
    personResultat: PersonResultat,
): Tidslinje<Boolean> {
    val filtrerteAdresser = filtrereUgyldigeAdresser(adresser.bostedsadresser)
    return lagTidslinjeForAdresser(filtrerteAdresser, personResultat, "Bostedadresse") { it.erIFinnmarkEllerNordTroms() }
}

fun lagErDeltBostedIFinnmarkEllerNordTromsTidslinje(
    adresser: Adresser,
    personResultat: PersonResultat,
): Tidslinje<Boolean> {
    val filtrerteAdresser = filtrereUgyldigeAdresser(adresser.delteBosteder)
    val tidslinjer =
        filtrerteAdresser.map { adresse ->
            lagTidslinjeForAdresser(listOf(adresse), personResultat, "Delt bostedadresse") { it.erIFinnmarkEllerNordTroms() }
        }

    val deltBostedTidslinje =
        tidslinjer.fold(tomTidslinje<Boolean>()) { kombinertTidslinje, nesteTidslinje ->
            kombinertTidslinje.kombinerMed(nesteTidslinje) { kombinertVerdi, nesteVerdi ->
                (kombinertVerdi == true) || (nesteVerdi == true)
            }
        }
    return deltBostedTidslinje
}

fun filtrereUgyldigeAdresser(adresser: List<Adresse>): List<Adresse> {
    val filtrert =
        adresser
            .filterNot { it.erFomOgTomNull() || it.erFomOgTomSamme() || it.erFomEtterTom() }
            .groupBy { it.gyldigFraOgMed to it.gyldigTilOgMed }
            .values
            .map { likePerioder ->
                likePerioder.find { it.erIFinnmarkEllerNordTroms() } ?: likePerioder.first()
            }.sortedBy { it.gyldigFraOgMed }

    return forskyvTilOgMedHvisDenErLikNesteFraOgMed(filtrert)
}

fun lagErOppholdsadresserPåSvalbardTidslinje(
    adresser: Adresser,
    personResultat: PersonResultat,
): Tidslinje<Boolean> {
    val adresserPåSvalbard = adresser.oppholdsadresse.filter { it.erPåSvalbard() }

    if (adresserPåSvalbard.isEmpty()) {
        return tomTidslinje()
    }

    val filtrerteAdresser = filtrereUgyldigeOppholdsadresser(adresserPåSvalbard)

    return lagTidslinjeForAdresser(filtrerteAdresser, personResultat, "Oppholdsadresse") { it.erPåSvalbard() }
}

private fun filtrereUgyldigeOppholdsadresser(adresser: List<Adresse>): List<Adresse> {
    val filtrert =
        adresser
            .filterNot { it.erFomOgTomNull() || it.erFomOgTomSamme() || it.erFomEtterTom() }
            .groupBy { it.gyldigFraOgMed to it.gyldigTilOgMed }
            .values
            .map { likePerioder ->
                likePerioder.find { it.erPåSvalbard() } ?: likePerioder.first()
            }.sortedBy { it.gyldigFraOgMed }

    return forskyvTilOgMedHvisDenErLikNesteFraOgMed(filtrert)
}

fun forskyvTilOgMedHvisDenErLikNesteFraOgMed(adresser: List<Adresse>): List<Adresse> =
    adresser
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

fun lagTidslinjeForAdresser(
    adresser: List<Adresse>,
    personResultat: PersonResultat,
    adressetype: String,
    operator: (Adresse) -> Boolean,
): Tidslinje<Boolean> {
    try {
        return adresser
            .windowed(size = 2, step = 1, partialWindows = true) {
                val denne = it.first()
                val neste = it.getOrNull(1)

                Periode(
                    verdi = operator(denne),
                    fom = denne.gyldigFraOgMed,
                    tom = denne.gyldigTilOgMed ?: neste?.gyldigFraOgMed?.minusDays(1),
                )
            }.tilTidslinje()
    } catch (e: IllegalStateException) {
        secureLogger.error("Feil ved oppretting av tidslinjer for $adressetype med adresser $adresser for person med aktørId ${personResultat.aktør.aktørId}", e)
        throw e
    } catch (e: IllegalArgumentException) {
        secureLogger.error("Feil ved oppretting av tidslinjer for $adressetype med adresser $adresser for person med aktørId ${personResultat.aktør.aktørId}", e)
        throw e
    }
}
