package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.utbetalingsoppdrag
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.kombiner
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service

@Service
class UtbetalingsTidslinjeService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {
    fun genererUtbetalingsTidslinjerForFagsak(fagsakId: Long): List<UtbetalingsTidslinje> {
        val iverksatteUtbetalingsoppdrag =
            tilkjentYtelseRepository
                .findByFagsak(fagsakId = fagsakId)
                .mapNotNull { it.utbetalingsoppdrag() }
                .sortedBy { it.avstemmingTidspunkt }

        val utbetalingsperioderPerKjede =
            genererUtbetalingsperioderPerKjede(iverksatteUtbetalingsoppdrag = iverksatteUtbetalingsoppdrag)

        val tidslinjePerKjede = genererTidslinjePerKjede(utbetalingsperioderPerKjede = utbetalingsperioderPerKjede)

        return tidslinjePerKjede.keys.map { sistePeriodeIdIKjede ->
            val utbetalingsperioder =
                utbetalingsperioderPerKjede[sistePeriodeIdIKjede]?.flatMap { it.verdi }?.toSet()
                    ?: throw Feil("Finner ikke perioder tilknyttet periodeId: $sistePeriodeIdIKjede")
            val tidslinje =
                tidslinjePerKjede[sistePeriodeIdIKjede]
                    ?: throw Feil("Finner ikke tidslinje tilknyttet periodeId: $sistePeriodeIdIKjede")

            UtbetalingsTidslinje(
                utbetalingsperioder = utbetalingsperioder,
                tidslinje = tidslinje,
            )
        }
    }

    fun finnUtbetalingsTidslinjeForPeriodeId(
        periodeId: Long,
        utbetalingsTidslinjer: List<UtbetalingsTidslinje>,
    ): UtbetalingsTidslinje = utbetalingsTidslinjer.single { it.erTidslinjeForPeriodeId(periodeId) }

    private fun genererTidslinjePerKjede(utbetalingsperioderPerKjede: Map<Long, List<Periode<Iterable<Utbetalingsperiode>>>>): Map<Long, Tidslinje<Utbetalingsperiode>> =
        utbetalingsperioderPerKjede.mapValues { (_, perioder) ->
            perioder
                .map { periode ->
                    Periode(
                        periode.verdi.maxWith(
                            compareBy<Utbetalingsperiode> { it.periodeId }
                                .thenBy { it.opphør != null }
                                .thenBy { it.opphør?.opphørDatoFom },
                        ),
                        periode.fom,
                        periode.tom,
                    )
                }.fold(mutableListOf<Periode<Utbetalingsperiode>>()) { gjeldendePerioder, periode ->
                    // Fjerner perioder med lavere periodeId enn foregående periode
                    val forrigePeriode = gjeldendePerioder.lastOrNull()
                    if (forrigePeriode == null || periode.verdi.periodeId > forrigePeriode.verdi.periodeId) {
                        gjeldendePerioder.add(periode)
                    } else if (forrigePeriode.verdi.periodeId == periode.verdi.periodeId) {
                        // Slår sammen etterfølgende perioder med samme periodeId dersom forrige periode ikke var en opphørsperiode
                        val forrigePeriodeErOpphørsperiode = forrigePeriode.verdi.opphør != null
                        if (!forrigePeriodeErOpphørsperiode) {
                            gjeldendePerioder.remove(forrigePeriode)
                            gjeldendePerioder.add(periode.copy(fom = forrigePeriode.fom))
                        }
                    }
                    gjeldendePerioder
                }.tilTidslinje()
        }

    private fun genererUtbetalingsperioderPerKjede(iverksatteUtbetalingsoppdrag: List<Utbetalingsoppdrag>): Map<Long, List<Periode<Iterable<Utbetalingsperiode>>>> =
        iverksatteUtbetalingsoppdrag
            .fold(mutableMapOf<Long, List<Tidslinje<Utbetalingsperiode>>>()) { kjederForFagsak, utbetalingsoppdrag ->
                val kjederForUtbetalingsperioder =
                    utbetalingsoppdrag
                        .utbetalingsperiode
                        .sortedBy { it.periodeId }
                        .fold(mutableMapOf<Long, MutableList<Periode<Utbetalingsperiode>>>()) { kjeder, utbetalingsperiode ->
                            kjeder.apply {
                                // Derom kjede er opphørt forkorter vi perioden til opphørsdato. Ellers bruker vi utbetalingsperiodens vedtakdatoTom
                                val periodeTom =
                                    utbetalingsperiode.opphør?.opphørDatoFom?.minusDays(1)
                                        ?: utbetalingsperiode.vedtakdatoTom
                                val kjede =
                                    getOrDefault(utbetalingsperiode.forrigePeriodeId, mutableListOf()) +
                                        Periode(
                                            utbetalingsperiode,
                                            utbetalingsperiode.vedtakdatoFom,
                                            periodeTom,
                                        )
                                put(utbetalingsperiode.periodeId, kjede.toMutableList())
                                remove(utbetalingsperiode.forrigePeriodeId)
                            }
                        }.mapValues { (_, kjede) ->
                            val opphørPeriode = kjede.singleOrNull { it.verdi.opphør != null }
                            val forrigePeriodeId =
                                opphørPeriode?.verdi?.periodeId ?: kjede.minOfOrNull { it.verdi.forrigePeriodeId ?: -1 }
                            Pair(kjede.tilTidslinje(), forrigePeriodeId)
                        }
                kjederForFagsak.apply {
                    kjederForUtbetalingsperioder.forEach { periodeId, (tidslinje, forrigePeriodeId) ->
                        val kjedeForFagsak = kjederForFagsak.getOrDefault(forrigePeriodeId, mutableListOf()) + tidslinje
                        put(periodeId, kjedeForFagsak.toMutableList())
                        if (periodeId != forrigePeriodeId) {
                            remove(forrigePeriodeId)
                        }
                    }
                }
            }.mapValues { (_, tidslinjerForKjede) ->
                tidslinjerForKjede.kombiner().tilPerioderIkkeNull()
            }
}
