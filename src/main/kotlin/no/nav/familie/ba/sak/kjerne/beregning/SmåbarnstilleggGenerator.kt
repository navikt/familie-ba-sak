package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.kombinerUtenNullMed
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.filtrerIkkeNull
import java.time.LocalDate

data class SmåbarnstilleggGenerator(
    val tilkjentYtelse: TilkjentYtelse,
) {
    fun lagSmåbarnstilleggAndeler(
        perioderMedFullOvergangsstønad: List<InternPeriodeOvergangsstønad>,
        utvidetAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        barnasAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        barnasAktørerOgFødselsdatoer: List<Pair<Aktør, LocalDate>>,
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        if (perioderMedFullOvergangsstønad.isEmpty() || utvidetAndeler.isEmpty() || barnasAndeler.isEmpty()) return emptyList()

        validerUtvidetOgBarnasAndeler(utvidetAndeler = utvidetAndeler, barnasAndeler = barnasAndeler)

        val søkerAktør = utvidetAndeler.first().aktør

        val perioderMedFullOvergangsstønadTidslinje = perioderMedFullOvergangsstønad.tilTidslinje()

        val utvidetBarnetrygdTidslinje = utvidetAndeler.tilTidslinje()

        val barnSomGirRettTilSmåbarnstilleggTidslinje =
            lagTidslinjeForPerioderMedBarnSomGirRettTilSmåbarnstillegg(
                barnasAndeler = barnasAndeler,
                barnasAktørerOgFødselsdatoer = barnasAktørerOgFødselsdatoer,
            )

        val kombinertProsentTidslinje =
            kombinerAlleTidslinjerTilProsentTidslinje(
                perioderMedFullOvergangsstønadTidslinje,
                utvidetBarnetrygdTidslinje,
                barnSomGirRettTilSmåbarnstilleggTidslinje,
            )

        return kombinertProsentTidslinje.filtrerIkkeNull().lagSmåbarnstilleggAndeler(
            søkerAktør = søkerAktør,
        )
    }

    private fun Tidslinje<SmåbarnstilleggPeriode>.lagSmåbarnstilleggAndeler(
        søkerAktør: Aktør,
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> =
        this
            .kombinerUtenNullMed(satstypeFamilieFellesTidslinje(SatsType.SMA)) { småbarnstilleggPeriode, sats ->
                val prosentIPeriode = småbarnstilleggPeriode.prosent
                val beløpIPeriode = sats.avrundetHeltallAvProsent(prosent = prosentIPeriode)

                AndelTilkjentYtelseForTidslinje(
                    aktør = søkerAktør,
                    beløp = beløpIPeriode,
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                    sats = sats,
                    prosent = prosentIPeriode,
                )
            }.tilAndelerTilkjentYtelse(tilkjentYtelse)
            .map { AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(it) }
}
