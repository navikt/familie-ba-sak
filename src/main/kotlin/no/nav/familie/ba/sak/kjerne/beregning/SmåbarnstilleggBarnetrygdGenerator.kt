package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønadTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import java.time.LocalDate

data class SmåbarnstilleggBarnetrygdGenerator(
    val behandlingId: Long,
    val tilkjentYtelse: TilkjentYtelse
) {

    fun lagSmåbarnstilleggAndeler(
        perioderMedFullOvergangsstønad: List<InternPeriodeOvergangsstønad>,
        utvidetAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        barnasAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        barnasAktørerOgFødselsdatoer: List<Pair<Aktør, LocalDate>>
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        if (perioderMedFullOvergangsstønad.isEmpty() || utvidetAndeler.isEmpty() || barnasAndeler.isEmpty()) return emptyList()

        validerUtvidetOgBarnasAndeler(utvidetAndeler = utvidetAndeler, barnasAndeler = barnasAndeler)

        val søkerAktør = utvidetAndeler.first().aktør

        val perioderMedFullOvergangsstønadTidslinje =
            InternPeriodeOvergangsstønadTidslinje(perioderMedFullOvergangsstønad)

        val utvidetBarnetrygdTidslinje = AndelTilkjentYtelseMedEndreteUtbetalingerTidslinje(andelerTilkjentYtelse = utvidetAndeler)

        val barnSomGirRettTilSmåbarnstilleggTidslinje = lagTidslinjeForPerioderMedBarnSomGirRettTilSmåbarnstillegg(
            barnasAndeler = barnasAndeler,
            barnasAktørerOgFødselsdatoer = barnasAktørerOgFødselsdatoer
        )

        val kombinertProsentTidslinje = kombinerAlleTidslinjerTilProsentTidslinje(
            perioderMedFullOvergangsstønadTidslinje,
            utvidetBarnetrygdTidslinje,
            barnSomGirRettTilSmåbarnstilleggTidslinje
        )

        return kombinertProsentTidslinje.filtrerIkkeNull().lagSmåbarnstilleggAndeler(
            søkerAktør = søkerAktør
        )
    }

    private fun Tidslinje<SmåbarnstilleggPeriode, Måned>.lagSmåbarnstilleggAndeler(
        søkerAktør: Aktør
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        return this.perioder().map {
            val stønadFom = it.fraOgMed.tilYearMonth()
            val stønadTom = it.tilOgMed.tilYearMonth()

            val ordinærSatsForPeriode = SatsService.hentGyldigSatsFor(
                satstype = SatsType.SMA,
                stønadFraOgMed = stønadFom,
                stønadTilOgMed = stønadTom
            ).singleOrNull()?.sats
                ?: throw Feil("Skal finnes én ordinær sats for gitt segment oppdelt basert på andeler")

            val prosentIPeriode = it.innhold?.prosent ?: throw Feil("Skal finnes prosent for gitt periode")

            val beløpIPeriode = ordinærSatsForPeriode.avrundetHeltallAvProsent(prosent = prosentIPeriode)

            val andelTilkjentYtelse = AndelTilkjentYtelse(
                behandlingId = behandlingId,
                tilkjentYtelse = tilkjentYtelse,
                aktør = søkerAktør,
                stønadFom = stønadFom,
                stønadTom = stønadTom,
                kalkulertUtbetalingsbeløp = beløpIPeriode,
                nasjonaltPeriodebeløp = beløpIPeriode,
                type = YtelseType.SMÅBARNSTILLEGG,
                sats = ordinærSatsForPeriode,
                prosent = prosentIPeriode
            )

            AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(andelTilkjentYtelse)
        }
    }
}
