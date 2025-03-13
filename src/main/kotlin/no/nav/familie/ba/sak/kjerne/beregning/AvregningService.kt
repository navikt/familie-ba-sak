package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.sisteDagIForrigeMåned
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærTilOgMed
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.outerJoin
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import java.time.LocalDate

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
        val sisteDagIForrigeMåned = LocalDate.now(clockProvider.get()).sisteDagIForrigeMåned()

        return etterbetalingerOgFeilutbetalinger(andelerInneværendeBehandling, andelerForrigeBehandling, sisteDagIForrigeMåned)
    }

    fun etterbetalingerOgFeilutbetalinger(
        andelerInneværendeBehandling: List<AndelTilkjentYtelse>,
        andelerForrigeBehandling: List<AndelTilkjentYtelse>,
        sisteDagIForrigeMåned: LocalDate,
    ): List<Periode<EtterbetalingOgFeilutbetaling>> {
        val andelerIFortidenInneværendeBehandlingTidslinjer = andelerInneværendeBehandling.tilTidslinjerPerAktørOgType().beskjærTilOgMed(sisteDagIForrigeMåned)
        val andelerIFortidenForrigeBehandlingTidslinjer = andelerForrigeBehandling.tilTidslinjerPerAktørOgType().beskjærTilOgMed(sisteDagIForrigeMåned)

        val tidslinjerMedDifferanser =
            andelerIFortidenInneværendeBehandlingTidslinjer
                .outerJoin(andelerIFortidenForrigeBehandlingTidslinjer) { nyAndel, gammelAndel ->
                    when {
                        nyAndel == null && gammelAndel == null -> null
                        nyAndel == null && gammelAndel != null -> -gammelAndel.kalkulertUtbetalingsbeløp
                        nyAndel != null && gammelAndel == null -> nyAndel.kalkulertUtbetalingsbeløp
                        else -> (nyAndel!!.kalkulertUtbetalingsbeløp - gammelAndel!!.kalkulertUtbetalingsbeløp).takeIf { it != 0 }
                    }
                }.values

        val perioderMedEtterbetalingerOgFeilutbetalinger =
            tidslinjerMedDifferanser
                .kombiner { differanser ->
                    val (etterbetaling, feilutbetaling) = differanser.partition { it > 0 }.toList().map { it.sum() }
                    if (etterbetaling != 0 || feilutbetaling != 0) {
                        EtterbetalingOgFeilutbetaling(
                            etterbetaling = etterbetaling,
                            feilutbetaling = -feilutbetaling,
                        )
                    } else {
                        null
                    }
                }.tilPerioderIkkeNull()

        return perioderMedEtterbetalingerOgFeilutbetalinger
    }
}

data class EtterbetalingOgFeilutbetaling(
    val etterbetaling: Int,
    val feilutbetaling: Int,
)
