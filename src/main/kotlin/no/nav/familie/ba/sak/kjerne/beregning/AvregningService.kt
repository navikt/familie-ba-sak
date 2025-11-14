package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.sisteDagIForrigeMåned
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
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
import java.time.temporal.ChronoUnit.MONTHS
import kotlin.math.absoluteValue

@Service
class AvregningService(
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val clockProvider: ClockProvider,
) {
    fun behandlingHarPerioderSomAvregnes(behandlingId: Long): Boolean = hentPerioderMedAvregning(behandlingId).isNotEmpty()

    fun hentPerioderMedAvregning(behandlingId: Long): List<AvregningPeriode> {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        if (behandling.kategori == BehandlingKategori.EØS || behandling.type == BehandlingType.TEKNISK_ENDRING) {
            return emptyList()
        }

        val sisteVedtatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)
                ?: return emptyList()

        val andelerInneværendeBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)
        val andelerForrigeBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id)

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
                        feilutbetaling = feilutbetaling.absoluteValue,
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
        val fom = periode.fom ?: throw Feil("Fra og med-dato kan ikke være null")
        val tom = periode.tom ?: throw Feil("Til og med-dato kan ikke være null")
        val antallMåneder = fom.until(tom, MONTHS) + 1 // +1 for å inkludere siste måned
        val totalEtterbetaling = periode.verdi.etterbetaling * antallMåneder
        val totalFeilutbetaling = periode.verdi.feilutbetaling * antallMåneder
        AvregningPeriode(
            fom = fom,
            tom = tom,
            totalEtterbetaling = totalEtterbetaling.toBigDecimal(),
            totalFeilutbetaling = totalFeilutbetaling.toBigDecimal(),
        )
    }
