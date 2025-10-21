package no.nav.familie.ba.sak.kjerne.simulering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.kjerne.simulering.domene.OverlappendePerioderMedAndreFagsaker
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder

fun finnOverlappendePerioder(
    økonomiSimuleringMottakere: List<ØkonomiSimuleringMottaker>,
    fagsakId: Long,
): List<OverlappendePerioderMedAndreFagsaker> {
    val fagsakPerioder =
        økonomiSimuleringMottakere.flatMap { mottaker ->
            mottaker.økonomiSimuleringPostering.mapNotNull { postering ->
                postering.fagsakId?.let {
                    Periode(it, postering.fom, postering.tom)
                }
            }
        }
    val perioderForForskjelligeFagsaker = fagsakPerioder.groupBy { it.verdi }
    val fagsakMedPosteringTidslinjer = perioderForForskjelligeFagsaker.mapValues { (_, perioder) -> perioder.tilTidslinje() }

    val kombinertTidslinje =
        fagsakMedPosteringTidslinjer
            .values
            .fold(tomTidslinje<List<Long>>()) { kombinertTidslinje, tidslinje ->
                kombinertTidslinje.kombinerMed(tidslinje) { kombinert, annen ->
                    kombinert.orEmpty() + listOfNotNull(annen)
                }
            }
    return kombinertTidslinje
        .tilPerioder()
        .filter { (it.verdi?.size ?: 0) > 1 } // Må være overlappende perioder
        .filter { it.verdi?.contains(fagsakId) == true } // Må overlappe med fagsakIden som er sendt inn
        .map { periode ->
            OverlappendePerioderMedAndreFagsaker(
                fom = periode.fom ?: throw Feil("ØkonomiSimuleringPostering for fagsaker ${periode.verdi} skal ikke kunne ha null som fom-verdi"),
                tom = periode.tom ?: throw Feil("ØkonomiSimuleringPostering for fagsaker ${periode.verdi} skal ikke kunne ha null som fom-verdi"),
                fagsaker = periode.verdi!!.filterNot { it == fagsakId },
            )
        }
}
