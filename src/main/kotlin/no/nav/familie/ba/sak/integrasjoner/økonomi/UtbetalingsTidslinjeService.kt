package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.utbetalingsoppdrag
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærTilOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærTilOgMedEtter
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

        val tidslinjePerKjede = genererTidslinjePerKjede(iverksatteUtbetalingsoppdrag = iverksatteUtbetalingsoppdrag, utbetalingsperioderPerKjede = utbetalingsperioderPerKjede)

        return tidslinjePerKjede.keys.map { sistePeriodeIdIKjede ->
            Utbetalingstidslinje(
                utbetalingsperioder =
                    try {
                        utbetalingsperioderPerKjede.values.single { it.any { utbetalingsperiode -> utbetalingsperiode.periodeId == sistePeriodeIdIKjede } }
                    } catch (e: Exception) {
                        secureLogger.error("SistePeriodeIdIKjede=$sistePeriodeIdIKjede finnes i flere forskjellige kjeder. utbetalingsperioderPerKjede=${utbetalingsperioderPerKjede.values}", e)
                        throw e
                    },
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
                        val forrigePeriodeId = opphørsPeriode?.periodeId ?: utbetalingsperioder.minOfOrNull { it.forrigePeriodeId ?: -1 } ?: -1
                        Pair(utbetalingsperioder, forrigePeriodeId)
                    }
                kjederForFagsak.apply {
                    utbetalingsperidoerPerKjede.forEach { periodeId, (utbetalingsperioder, forrigePeriodeId) ->
                        val sistePeriodeIdIKjede = finnSistePeriodeIdIKjede(forrigePeriodeId, this)
                        val nySistePeriodeIdIKjede = periodeId.takeIf { it > sistePeriodeIdIKjede } ?: sistePeriodeIdIKjede
                        put(nySistePeriodeIdIKjede, getOrDefault(sistePeriodeIdIKjede, emptySet()).plus(utbetalingsperioder))
                        if (nySistePeriodeIdIKjede != sistePeriodeIdIKjede) {
                            remove(sistePeriodeIdIKjede)
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

    private fun genererTidslinjePerKjede(
        iverksatteUtbetalingsoppdrag: List<Utbetalingsoppdrag>,
        utbetalingsperioderPerKjede: Map<Long, Set<Utbetalingsperiode>>,
    ): Map<Long, Tidslinje<Utbetalingsperiode>> =
        iverksatteUtbetalingsoppdrag
            .fold(mutableMapOf()) { kjederForFagsak, utbetalingsoppdrag ->
                val kjederForUtbetalingsperioder =
                    utbetalingsoppdrag.tilUtbetalingsperioderPerKjede().mapValues { (_, kjede) ->
                        val opphørsperiode = kjede.singleOrNull { it.opphør != null }
                        val nyePerioder = kjede.filter { it.opphør == null }.map { Periode(it, fom = it.vedtakdatoFom, tom = it.vedtakdatoTom) }
                        val forrigePeriodeId = opphørsperiode?.periodeId ?: nyePerioder.minOfOrNull { it.verdi.forrigePeriodeId ?: -1 } ?: -1
                        Triple(nyePerioder.tilTidslinje(), forrigePeriodeId, opphørsperiode)
                    }
                kjederForFagsak.apply {
                    kjederForUtbetalingsperioder.forEach { periodeId, (tidslinje, forrigePeriodeId, opphørsperiode) ->
                        // I noen fagsaker har man lagt inn opphør på feil periode i kjede.
                        // Opphør skal i utgangspunktet alltid legges inn på siste periode i kjede, men i noen fagsaker har opphør blitt lagt inn på en tidligere periode i kjeden.
                        // Sørger her for at vi ikke oppretter nye kjeder når dette skjer, men heller finner ut hvilken kjede perioden faller inn under.
                        val sistePeriodeIdIKjede = finnSistePeriodeIdIKjede(forrigePeriodeId, this, utbetalingsperioderPerKjede)
                        val nySistePeriodeIdIKjede = periodeId.takeIf { it > sistePeriodeIdIKjede } ?: sistePeriodeIdIKjede
                        val gjeldendeTidslinje =
                            getOrDefault(sistePeriodeIdIKjede, tomTidslinje())
                                .beskjærOgKorrigerPerioderVedOpphør(opphørsperiode)
                                .beskjærTilOgMedEtterIkkeTomTidslinje(tidslinje)

                        // Sørger for at vi alltid tar med den siste perioden dersom det er overlapp.
                        val nyGjeldendeTidslinje =
                            gjeldendeTidslinje
                                .kombinerMed(tidslinje) { gjeldendeUtbetalingsperiode, nyUtbetalingsperiode ->
                                    nyUtbetalingsperiode ?: gjeldendeUtbetalingsperiode
                                }.tilPerioderIkkeNull()
                                .tilTidslinje()
                        put(nySistePeriodeIdIKjede, nyGjeldendeTidslinje)

                        // Håndtering av opphør. Da vil periodeId være lik forrigePeriodeId, og vi må sørge for at vi ikke sletter tidslinja vi akkurat har oppdatert.
                        if (nySistePeriodeIdIKjede != sistePeriodeIdIKjede) {
                            remove(sistePeriodeIdIKjede)
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

    private fun finnSistePeriodeIdIKjede(
        forrigePeriodeId: Long,
        kjederForFagsak: Map<Long, Set<Utbetalingsperiode>>,
    ): Long = kjederForFagsak.entries.singleOrNull { (_, utbetalingsperioder) -> utbetalingsperioder.any { utbetalingsperiode -> utbetalingsperiode.periodeId == forrigePeriodeId } }?.key ?: -1

    private fun finnSistePeriodeIdIKjede(
        forrigePeriodeId: Long,
        tidslinjerPerKjede: Map<Long, Tidslinje<Utbetalingsperiode>>,
        utbetalingsperioderPerKjede: Map<Long, Set<Utbetalingsperiode>>,
    ): Long =
        tidslinjerPerKjede.keys.singleOrNull { sistePeriodeIdIKjede ->
            val kjedeForSistePeriodeIdIKjede = utbetalingsperioderPerKjede.entries.single { (_, utbetalingsperioder) -> utbetalingsperioder.any { it.periodeId == sistePeriodeIdIKjede } }.key
            val kjedeForForrigePeriodeId = utbetalingsperioderPerKjede.entries.singleOrNull { (_, utbetalingsperidoer) -> utbetalingsperidoer.any { it.periodeId == forrigePeriodeId } }?.key
            kjedeForSistePeriodeIdIKjede == kjedeForForrigePeriodeId
        } ?: -1
}
