package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.sisteDagIForrigeMåned
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.simulering.domene.AvregningPeriode
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærTilOgMed
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
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
    fun hentPerioderMedAvregning(behandlingId: Long): List<AvregningPeriode> {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val sisteIverksatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id)
                ?: return emptyList()

        val andelerInneværendeBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)
        val andelerForrigeBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id)

        val sisteDagIForrigeMåned = LocalDate.now(clockProvider.get()).sisteDagIForrigeMåned()

        val andelerInneværendeBehandlingTidslinjer = andelerInneværendeBehandling.tilTidslinjerPerAktørOgType().beskjærTilOgMed(sisteDagIForrigeMåned)
        val andelerForrigeBehandlingTidslinjer = andelerForrigeBehandling.tilTidslinjerPerAktørOgType().beskjærTilOgMed(sisteDagIForrigeMåned)

        val tidslinjerMedDifferanser = lagTidslinjerMedDifferanser(andelerInneværendeBehandlingTidslinjer, andelerForrigeBehandlingTidslinjer)

        val perioderMedEtterbetalingerOgFeilutbetalinger =
            summerEtterbetalingerOgFeilutbetalinger(tidslinjerMedDifferanser)
                .tilPerioderIkkeNull()
                .tilAvregningPerioder()

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
                if (etterbetaling != 0 && feilutbetaling != 0) {
                    EtterbetalingOgFeilutbetaling(
                        etterbetaling = etterbetaling,
                        feilutbetaling = -feilutbetaling,
                    )
                } else {
                    null
                }
            }
}

data class EtterbetalingOgFeilutbetaling(
    val etterbetaling: Int,
    val feilutbetaling: Int,
)

private fun List<Periode<EtterbetalingOgFeilutbetaling>>.tilAvregningPerioder(): List<AvregningPeriode> =
    this.map { periode ->
        AvregningPeriode(
            fom = periode.fom ?: throw Feil("Fra og med-dato kan ikke være null"),
            tom = periode.tom ?: throw Feil("Til og med-dato kan ikke være null"),
            etterbetaling = periode.verdi.etterbetaling.toBigDecimal(),
            feilutbetaling = periode.verdi.feilutbetaling.toBigDecimal(),
        )
    }
