package no.nav.familie.ba.sak.kjerne.småbarnstilleggkorrigering

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.opprettBooleanTidslinje
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth
import javax.transaction.Transactional

@Service
class SmåbarnstilleggKorrigeringService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val loggService: LoggService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService
) {

    @Transactional
    fun leggTilSmåbarnstilleggPåBehandling(årMåned: YearMonth, behandling: Behandling): AndelTilkjentYtelse {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId = behandling.id)
        val andelTilkjentYtelser = tilkjentYtelse.andelerTilkjentYtelse

        val andelerMedEndringer = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        val overlappendePeriodeFinnes = finnOverlappendeSmåbarnstilleggPeriode(andelerMedEndringer, årMåned)

        if (overlappendePeriodeFinnes != null) {
            throw FunksjonellFeil("Det er ikke mulig å legge til småbarnstillegg for ${årMåned.tilMånedÅr()} fordi det allerede finnes småbarnstillegg for denne perioden")
        }

        val nySmåbarnstillegg = opprettNyttSmåbarnstillegg(behandling, tilkjentYtelse, årMåned, årMåned)

        andelTilkjentYtelser.add(nySmåbarnstillegg)

        loggService.opprettSmåbarnstilleggLogg(behandling, "Småbarnstillegg for ${årMåned.tilMånedÅr()} lagt til")

        return nySmåbarnstillegg
    }

    @Transactional
    fun fjernSmåbarnstilleggPåBehandling(årMåned: YearMonth, behandling: Behandling): List<AndelTilkjentYtelse> {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId = behandling.id)
        val andelTilkjentYtelser = tilkjentYtelse.andelerTilkjentYtelse

        val andelerMedEndringer = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        val småBarnstilleggSomHarOverlappendePeriode =
            finnOverlappendeSmåbarnstilleggPeriode(andelerMedEndringer, årMåned)
                ?: throw FunksjonellFeil("Det er ikke mulig å fjerne småbarnstillegg for ${årMåned.tilMånedÅr()} fordi det ikke finnes småbarnstillegg for denne perioden")

        val eksisterendeSmåBarnstilleggTidslinje =
            AndelTilkjentYtelseTidslinje(listOf(småBarnstilleggSomHarOverlappendePeriode))
        val filtrerBortSingelMånedTidslinje = opprettBooleanTidslinje(årMåned, årMåned)

        val perioderUtenOverlapp =
            eksisterendeSmåBarnstilleggTidslinje
                .filtrerMed(filtrerBortSingelMånedTidslinje).perioder()
                .filter { it.innhold == null }

        val nyOppsplittetSmåbarnstillegg = perioderUtenOverlapp.map {
            opprettNyttSmåbarnstillegg(
                behandling,
                tilkjentYtelse,
                it.fraOgMed.tilYearMonth(),
                it.tilOgMed.tilYearMonth()
            )
        }

        andelTilkjentYtelser.remove(småBarnstilleggSomHarOverlappendePeriode.andel)
        andelTilkjentYtelser.addAll(nyOppsplittetSmåbarnstillegg)

        loggService.opprettSmåbarnstilleggLogg(behandling, "Småbarnstillegg for ${årMåned.tilMånedÅr()} fjernet")

        return nyOppsplittetSmåbarnstillegg
    }

    private fun opprettNyttSmåbarnstillegg(
        behandling: Behandling,
        tilkjentYtelse: TilkjentYtelse,
        stønadFom: YearMonth,
        stønadTom: YearMonth
    ): AndelTilkjentYtelse {
        val ordinærSatsForPeriode = SatsService.hentGyldigSatsFor(
            satstype = SatsType.SMA,
            stønadFraOgMed = stønadFom,
            stønadTilOgMed = stønadTom
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

    private fun finnOverlappendeSmåbarnstilleggPeriode(
        andelTilkjentYtelser: Collection<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        årMåned: YearMonth
    ) = andelTilkjentYtelser.firstOrNull {
        it.erSmåbarnstillegg() && it.overlapperPeriode(MånedPeriode(årMåned, årMåned))
    }
}
