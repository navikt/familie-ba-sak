package no.nav.familie.ba.sak.kjerne.småbarnstillegg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.tilTidslinjeForSøkersYtelse
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.time.YearMonth

fun kanAutomatiskIverksetteSmåbarnstillegg(
    innvilgedeMånedPerioder: List<MånedPeriode>,
    reduserteMånedPerioder: List<MånedPeriode>,
): Boolean =
    innvilgedeMånedPerioder.all {
        it.fom.isSameOrAfter(
            YearMonth.now(),
        )
    } &&
        reduserteMånedPerioder.all {
            it.fom.isSameOrAfter(
                YearMonth.now(),
            )
        }

fun hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
    forrigeSmåbarnstilleggAndeler: List<AndelTilkjentYtelse>,
    nyeSmåbarnstilleggAndeler: List<AndelTilkjentYtelse>,
): Pair<List<MånedPeriode>, List<MånedPeriode>> {
    val forrigeAndelerTidslinje = forrigeSmåbarnstilleggAndeler.tilTidslinjeForSøkersYtelse(YtelseType.SMÅBARNSTILLEGG)
    val andelerTidslinje = nyeSmåbarnstilleggAndeler.tilTidslinjeForSøkersYtelse(YtelseType.SMÅBARNSTILLEGG)

    val nyeSmåbarnstilleggPerioder =
        forrigeAndelerTidslinje.kombinerMed(andelerTidslinje) { gammel, ny -> ny.takeIf { gammel == null } }

    val fjernedeSmåbarnstilleggPerioder =
        forrigeAndelerTidslinje.kombinerMed(andelerTidslinje) { gammel, ny -> gammel.takeIf { ny == null } }

    return Pair(nyeSmåbarnstilleggPerioder.tilMånedPerioder(), fjernedeSmåbarnstilleggPerioder.tilMånedPerioder())
}

private fun Tidslinje<AndelTilkjentYtelse>.tilMånedPerioder() =
    this.tilPerioderIkkeNull().map {
        MånedPeriode(
            fom = it.fom?.toYearMonth() ?: throw Feil("Fra og med-dato kan ikke være null"),
            tom = it.tom?.toYearMonth() ?: throw Feil("Til og med-dato kan ikke være null"),
        )
    }