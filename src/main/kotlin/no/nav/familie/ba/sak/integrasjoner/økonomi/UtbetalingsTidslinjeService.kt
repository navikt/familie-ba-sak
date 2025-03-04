package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.utbetalingsoppdrag
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon.beskjærTilOgMed
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon.beskjærTilOgMedEtter
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UtbetalingsTidslinjeService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val behandlingRepository: BehandlingRepository,
) {
    fun genererUtbetalingsperioderForBehandlingerEtterDato(
        behandlinger: List<Long>,
        dato: LocalDate,
    ): List<Periode<Utbetalingsperiode>> {
        val fagsakerIder = behandlingRepository.finnFagsakIderForBehandlinger(behandlinger).toSet()
        return fagsakerIder
            .flatMap { fagsakId ->
                try {
                    genererUtbetalingstidslinjerForFagsak(fagsakId)
                        .flatMap { utbetalingstidslinje ->
                            utbetalingstidslinje.tidslinje.beskjærFraOgMed(dato.førsteDagIInneværendeMåned()).tilPerioderIkkeNull()
                        }
                } catch (e: Exception) {
                    secureLogger.error("Feil ved generering av utbetalingsperioder for fagsak=$fagsakId", e)
                    throw e
                }
            }
    }

    fun genererUtbetalingstidslinjerForFagsak(fagsakId: Long): List<Utbetalingstidslinje> {
        val iverksatteUtbetalingsoppdrag =
            tilkjentYtelseRepository
                .findByFagsak(fagsakId = fagsakId)
                .mapNotNull { it.utbetalingsoppdrag() }
                .sortedBy { it.avstemmingTidspunkt }

        val utbetalingsperioderPerKjede =
            utbetalingsperioderPerKjede(iverksatteUtbetalingsoppdrag = iverksatteUtbetalingsoppdrag)

        val tidslinjePerKjede = genererTidslinjePerKjede(iverksatteUtbetalingsoppdrag = iverksatteUtbetalingsoppdrag)

        return tidslinjePerKjede.keys.map { sistePeriodeIdIKjede ->
            Utbetalingstidslinje(
                utbetalingsperioder = utbetalingsperioderPerKjede.values.single { it.any { utbetalingsperiode -> utbetalingsperiode.periodeId == sistePeriodeIdIKjede } },
                tidslinje = tidslinjePerKjede[sistePeriodeIdIKjede]!!,
            )
        }
    }

    fun finnUtbetalingsTidslinjeForPeriodeId(
        periodeId: Long,
        utbetalingstidslinjer: List<Utbetalingstidslinje>,
    ): Utbetalingstidslinje = utbetalingstidslinjer.single { it.erTidslinjeForPeriodeId(periodeId) }

    private fun utbetalingsperioderPerKjede(iverksatteUtbetalingsoppdrag: List<Utbetalingsoppdrag>): Map<Long, Set<Utbetalingsperiode>> =
        iverksatteUtbetalingsoppdrag
            .fold(mutableMapOf()) { kjederForFagsak, utbetalingsoppdrag ->
                val utbetalingsperidoerPerKjede =
                    utbetalingsoppdrag.tilUtbetalingsperioderPerKjede().mapValues { (_, utbetalingsperioder) ->
                        val opphørsPeriode = utbetalingsperioder.singleOrNull { it.opphør != null }
                        val forrigePeriodeId = opphørsPeriode?.periodeId ?: utbetalingsperioder.minOfOrNull { it.forrigePeriodeId ?: -1 }
                        Pair(utbetalingsperioder, forrigePeriodeId)
                    }
                kjederForFagsak.apply {
                    utbetalingsperidoerPerKjede.forEach { periodeId, (utbetalingsperioder, forrigePeriodeId) ->
                        put(periodeId, getOrDefault(forrigePeriodeId, emptySet()).plus(utbetalingsperioder))
                        if (periodeId != forrigePeriodeId) {
                            remove(forrigePeriodeId)
                        }
                    }
                }
            }

    private fun Utbetalingsoppdrag.tilUtbetalingsperioderPerKjede(): Map<Long, List<Utbetalingsperiode>> =
        this.utbetalingsperiode
            .sortedBy { it.periodeId }
            .fold(mutableMapOf()) { kjeder, utbetalingsperiode ->
                kjeder.apply {
                    val kjede =
                        getOrDefault(utbetalingsperiode.forrigePeriodeId, emptyList()) +
                            utbetalingsperiode

                    put(utbetalingsperiode.periodeId, kjede.toMutableList())
                    remove(utbetalingsperiode.forrigePeriodeId)
                }
            }

    private fun genererTidslinjePerKjede(iverksatteUtbetalingsoppdrag: List<Utbetalingsoppdrag>): Map<Long, Tidslinje<Utbetalingsperiode>> =
        iverksatteUtbetalingsoppdrag
            .fold(mutableMapOf()) { kjederForFagsak, utbetalingsoppdrag ->
                val kjederForUtbetalingsperioder =
                    utbetalingsoppdrag.tilUtbetalingsperioderPerKjede().mapValues { (_, kjede) ->
                        val opphørsperiode = kjede.singleOrNull { it.opphør != null }
                        val nyePerioder = kjede.filter { it.opphør == null }.map { Periode(it, fom = it.vedtakdatoFom, tom = it.vedtakdatoTom) }
                        val forrigePeriodeId = opphørsperiode?.periodeId ?: nyePerioder.minOfOrNull { it.verdi.forrigePeriodeId ?: -1 }
                        Triple(nyePerioder.tilTidslinje(), forrigePeriodeId, opphørsperiode)
                    }
                kjederForFagsak.apply {
                    kjederForUtbetalingsperioder.forEach { periodeId, (tidslinje, forrigePeriodeId, opphørsperiode) ->
                        val gjeldendeTidslinje =
                            kjederForFagsak
                                .getOrDefault(forrigePeriodeId, tomTidslinje())
                                .beskjærOgKorrigerPerioderVedOpphør(opphørsperiode)
                                .beskjærTilOgMedEtterIkkeTomTidslinje(tidslinje)

                        // Sørger for at vi alltid tar med den siste perioden dersom det er overlapp.
                        val nyGjeldendeTidslinje =
                            gjeldendeTidslinje
                                .kombinerMed(tidslinje) { gjeldendeUtbetalingsperiode, nyUtbetalingsperiode ->
                                    nyUtbetalingsperiode ?: gjeldendeUtbetalingsperiode
                                }.tilPerioderIkkeNull()
                                .tilTidslinje()
                        put(periodeId, nyGjeldendeTidslinje)

                        // Håndtering av opphør. Da vil periodeId være lik forrigePeriodeId, og vi må sørge for at vi ikke sletter tidslinja vi akkurat har oppdatert.
                        if (periodeId != forrigePeriodeId) {
                            remove(forrigePeriodeId)
                        }
                    }
                }
            }

    private fun Tidslinje<Utbetalingsperiode>.beskjærOgKorrigerPerioderVedOpphør(
        opphørsperiode: Utbetalingsperiode?,
    ): Tidslinje<Utbetalingsperiode> =
        if (opphørsperiode == null) {
            this
        } else {
            this
                .tilPerioderIkkeNull()
                .map { utbetalingsperiode ->
                    // Erstatter eksisterende periode vi opphører med opphørsperioden. Trengs for at kildeBehandlingId skal bli korrekt.
                    if (utbetalingsperiode.verdi.periodeId == opphørsperiode.periodeId) {
                        utbetalingsperiode.copy(verdi = opphørsperiode)
                    } else {
                        utbetalingsperiode
                    }
                }.tilTidslinje()
                // Beskjærer tidslinje slik at alt etter opphørsdato forsvinner
                .beskjærTilOgMed(opphørsperiode.opphør!!.opphørDatoFom.minusDays(1))
        }

    private fun Tidslinje<Utbetalingsperiode>.beskjærTilOgMedEtterIkkeTomTidslinje(tidslinje: Tidslinje<Utbetalingsperiode>): Tidslinje<Utbetalingsperiode> {
        if (tidslinje.erTom()) {
            return this
        }
        return this.beskjærTilOgMedEtter(tidslinje)
    }
}
