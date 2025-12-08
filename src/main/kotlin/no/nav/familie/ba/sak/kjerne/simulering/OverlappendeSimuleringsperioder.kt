package no.nav.familie.ba.sak.kjerne.simulering

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.kjerne.simulering.domene.OverlappendePerioderMedAndreFagsaker
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringPostering
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.math.BigDecimal
import java.time.LocalDate

fun finnOverlappendePerioder(
    økonomiSimuleringMottakere: List<ØkonomiSimuleringMottaker>,
    fagsakId: Long,
): List<OverlappendePerioderMedAndreFagsaker> {
    val posteringerMedFagsakId = økonomiSimuleringMottakere.flatMap { it.økonomiSimuleringPostering }.filter { it.fagsakId != null }
    val posteringerPerFagsak = posteringerMedFagsakId.groupBy { it.fagsakId!! }
    val fagsakTilTidslinje =
        posteringerPerFagsak.mapValues { (fagsakId, posteringerForFagsak) ->
            val perioderForFagsak = posteringerForFagsak.groupBy { it.fom to it.tom }
            val tidslinjeMedFeilOgEtterbetalingForFagsak =
                perioderForFagsak
                    .map { (fomOgTom, posteringForFagsakIPeriode) ->
                        posteringForFagsakIPeriode.lagPerioderMedFeilOgEtterbetalinger(
                            tidSimuleringHentet = økonomiSimuleringMottakere.first().opprettetTidspunkt.toLocalDate(),
                            fagsakId = fagsakId,
                            fomOgTom = fomOgTom,
                        )
                    }.tilTidslinje()
            tidslinjeMedFeilOgEtterbetalingForFagsak
        }

    val originalFagsakUtenVerdierForFeilOgEtterbetaling =
        fagsakTilTidslinje[fagsakId]
            ?.mapVerdi {
                FagsakerMedFeilOgEtterbetalinger(emptyList(), emptyList())
            }?.filtrerIkkeNull() ?: tomTidslinje()

    val feilOgEtterbetalingerForAndreFagsakerOverTid =
        fagsakTilTidslinje
            .filterNot { it.key == fagsakId }
            .values
            .fold(
                originalFagsakUtenVerdierForFeilOgEtterbetaling,
            ) { kombinerteTidslinjer, nyTidslinje ->
                kombinerteTidslinjer.kombinerMed(nyTidslinje) { eksisterendePeriode, nyPeriode ->
                    eksisterendePeriode?.copy(
                        fagsakerMedFeilutbetaling = eksisterendePeriode.fagsakerMedFeilutbetaling + (nyPeriode?.fagsakerMedFeilutbetaling ?: emptyList()),
                        fagsakerMedEtterbetaling = eksisterendePeriode.fagsakerMedEtterbetaling + (nyPeriode?.fagsakerMedEtterbetaling ?: emptyList()),
                    )
                }
            }

    return feilOgEtterbetalingerForAndreFagsakerOverTid
        .tilPerioder()
        .filtrerIkkeNull()
        .map { periode ->
            OverlappendePerioderMedAndreFagsaker(
                fom = periode.fom ?: TIDENES_MORGEN,
                tom = periode.tom ?: TIDENES_ENDE,
                fagsakerMedFeilutbetaling = periode.verdi.fagsakerMedFeilutbetaling,
                fagsakerMedEtterbetaling = periode.verdi.fagsakerMedEtterbetaling,
            )
        }.filterNot {
            it.fagsakerMedFeilutbetaling.isEmpty() && it.fagsakerMedEtterbetaling.isEmpty()
        }
}

private fun List<ØkonomiSimuleringPostering>.lagPerioderMedFeilOgEtterbetalinger(
    tidSimuleringHentet: LocalDate,
    fagsakId: Long,
    fomOgTom: Pair<LocalDate, LocalDate>,
): Periode<FagsakerMedFeilOgEtterbetalinger> {
    val feilutbetalinger = hentPositivFeilbetalingIPeriode(this)
    val etterbetalinger = hentEtterbetalingIPeriode(this, tidSimuleringHentet)

    val fagsakerMedFeilOgEtterbetalinger =
        FagsakerMedFeilOgEtterbetalinger(
            fagsakerMedFeilutbetaling = listOfNotNull(fagsakId.takeIf { feilutbetalinger != BigDecimal.ZERO }),
            fagsakerMedEtterbetaling = listOfNotNull(fagsakId.takeIf { etterbetalinger != BigDecimal.ZERO }),
        )

    return Periode(
        fom = fomOgTom.first,
        tom = fomOgTom.second,
        verdi = fagsakerMedFeilOgEtterbetalinger,
    )
}

private data class FagsakerMedFeilOgEtterbetalinger(
    val fagsakerMedFeilutbetaling: List<Long>,
    val fagsakerMedEtterbetaling: List<Long>,
)
