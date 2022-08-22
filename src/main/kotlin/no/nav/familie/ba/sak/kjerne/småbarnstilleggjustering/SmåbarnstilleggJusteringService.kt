package no.nav.familie.ba.sak.kjerne.småbarnstilleggjustering

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.opprettBooleanTidslinje
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerMed
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth
import javax.transaction.Transactional

@Service
class SmåbarnstilleggJusteringService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val loggService: LoggService
) {

    @Transactional
    fun leggTilSmåbarnstilleggPåBehandling(årMåned: YearMonth, behandling: Behandling) {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId = behandling.id)
        val søkersAndeler = tilkjentYtelse.andelerTilkjentYtelse

        val nySmåbarnstillegg = opprettNyttSmåbarnstillegg(behandling, tilkjentYtelse, årMåned, årMåned)

        søkersAndeler.add(nySmåbarnstillegg)

        loggService.opprettSmåbarnstilleggLogg(behandling, "Småbarnstillegg for ${årMåned.tilMånedÅr()} lagt til")
    }

    @Transactional
    fun fjernSmåbarnstilleggPåBehandling(årMåned: YearMonth, behandling: Behandling) {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId = behandling.id)
        val andelTilkjentYtelser = tilkjentYtelse.andelerTilkjentYtelse

        val småbarnstilleggSomHarOverlapperPeriode = andelTilkjentYtelser.first {
            it.erSøkersAndel() && it.erSmåbarnstillegg() && it.overlapperPeriode(
                MånedPeriode(årMåned, årMåned)
            )
        }

        val eksisterendeSmåBarnstilleggTidslinje = AndelTilkjentYtelseTidslinje(listOf(småbarnstilleggSomHarOverlapperPeriode))
        val filtrerBortSingelMånedTidslinje = opprettBooleanTidslinje(årMåned, årMåned)

        val tidslinjeUtenOverlapp =
            eksisterendeSmåBarnstilleggTidslinje
                .filtrerMed(filtrerBortSingelMånedTidslinje).perioder()
                .filter { it.innhold == null }

        val nySmåbarnstillegger = tidslinjeUtenOverlapp.map {
            opprettNyttSmåbarnstillegg(
                behandling,
                tilkjentYtelse,
                it.fraOgMed.tilYearMonth(),
                it.tilOgMed.tilYearMonth()
            )
        }

        andelTilkjentYtelser.remove(småbarnstilleggSomHarOverlapperPeriode)
        andelTilkjentYtelser.addAll(nySmåbarnstillegger)

        loggService.opprettSmåbarnstilleggLogg(
            behandling, "Småbarnstillegg for ${årMåned.tilMånedÅr()} fjernet"
        )
    }

    private fun opprettNyttSmåbarnstillegg(
        behandling: Behandling,
        tilkjentYtelse: TilkjentYtelse,
        stønadFom: YearMonth,
        stønadTom: YearMonth,
    ): AndelTilkjentYtelse {
        val ordinærSatsForPeriode = SatsService.hentGyldigSatsFor(
            satstype = SatsType.SMA, stønadFraOgMed = stønadFom, stønadTilOgMed = stønadTom
        ).singleOrNull()?.sats ?: error("Skal finnes én ordinær sats for gitt segment oppdelt basert på andeler")

        return AndelTilkjentYtelse(
            behandlingId = behandling.id,
            tilkjentYtelse = tilkjentYtelse,
            aktør = behandling.fagsak.aktør,
            stønadFom = stønadFom,
            stønadTom = stønadTom,
            type = YtelseType.SMÅBARNSTILLEGG,
            prosent = BigDecimal(100),
            sats = ordinærSatsForPeriode,
            nasjonaltPeriodebeløp = ordinærSatsForPeriode,
            kalkulertUtbetalingsbeløp = ordinærSatsForPeriode
        )
    }
}
