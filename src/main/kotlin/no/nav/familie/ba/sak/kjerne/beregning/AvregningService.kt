package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærTilOgMed
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.outerJoin
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class AvregningService(
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val clockProvider: ClockProvider,
) {
    fun etterbetalingerOgFeilutbetalinger(behandling: Behandling): List<Periode<EtterbetalingOgFeilutbetaling>> {
        val sisteIverksatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id)
                ?: return emptyList()

        val andelerInneværendeBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)
        val andelerForrigeBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id)
        val inneværendeMåned = YearMonth.now(clockProvider.get())

        return etterbetalingerOgFeilutbetalinger(andelerInneværendeBehandling, andelerForrigeBehandling, inneværendeMåned)
    }

    fun etterbetalingerOgFeilutbetalinger(
        andelerInneværendeBehandling: List<AndelTilkjentYtelse>,
        andelerForrigeBehandling: List<AndelTilkjentYtelse>,
        inneværendeMåned: YearMonth,
    ): List<Periode<EtterbetalingOgFeilutbetaling>> {
        val sisteDagIForrigeMåned = inneværendeMåned.forrigeMåned().sisteDagIInneværendeMåned()
        val andelerIFortidenInneværendeBehandlingTidslinjer = andelerInneværendeBehandling.tilTidslinjerPerAktørOgType().beskjærTilOgMed(sisteDagIForrigeMåned)
        val andelerIFortidenForrigeBehandlingTidslinjer = andelerForrigeBehandling.tilTidslinjerPerAktørOgType().beskjærTilOgMed(sisteDagIForrigeMåned)

        val tidslinjerMedDifferanser = lagTidslinjerMedDifferanser(andelerIFortidenInneværendeBehandlingTidslinjer, andelerIFortidenForrigeBehandlingTidslinjer)

        val perioderMedEtterbetalingerOgFeilutbetalinger = summerEtterbetalingerOgFeilutbetalinger(tidslinjerMedDifferanser).tilPerioderIkkeNull()

        return perioderMedEtterbetalingerOgFeilutbetalinger
    }

    private fun lagTidslinjerMedDifferanser(
        andelerIFortidenInneværendeBehandlingTidslinjer: Map<Pair<Aktør, YtelseType>, Tidslinje<AndelTilkjentYtelse>>,
        andelerIFortidenForrigeBehandlingTidslinjer: Map<Pair<Aktør, YtelseType>, Tidslinje<AndelTilkjentYtelse>>,
    ): Collection<Tidslinje<Int>> =
        andelerIFortidenInneværendeBehandlingTidslinjer
            .outerJoin(andelerIFortidenForrigeBehandlingTidslinjer) { nyAndel, gammelAndel ->
                when {
                    nyAndel == null && gammelAndel == null -> 0
                    nyAndel == null && gammelAndel != null -> -gammelAndel.kalkulertUtbetalingsbeløp
                    nyAndel != null && gammelAndel == null -> nyAndel.kalkulertUtbetalingsbeløp
                    else -> nyAndel!!.kalkulertUtbetalingsbeløp - gammelAndel!!.kalkulertUtbetalingsbeløp
                }
            }.values

    private fun summerEtterbetalingerOgFeilutbetalinger(tidslinjerMedDifferanser: Collection<Tidslinje<Int>>): Tidslinje<EtterbetalingOgFeilutbetaling> =
        tidslinjerMedDifferanser
            .kombiner { differanser ->
                val (etterbetaling, feilutbetaling) = differanser.partition { it > 0 }.toList().map { it.sum() }
                EtterbetalingOgFeilutbetaling(
                    etterbetaling = etterbetaling,
                    feilutbetaling = -feilutbetaling,
                )
            }
}

data class EtterbetalingOgFeilutbetaling(
    val etterbetaling: Int,
    val feilutbetaling: Int,
)
