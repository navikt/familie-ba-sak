package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.erUlike
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import java.time.LocalDate

fun vedtakOmOvergangsstønadPåvirkerFagsak(
    småbarnstilleggBarnetrygdGenerator: SmåbarnstilleggBarnetrygdGenerator,
    nyePerioderMedFullOvergangsstønad: List<PeriodeOvergangsstønad>,
    forrigeSøkersAndeler: List<AndelTilkjentYtelse>,
    barnasFødselsdatoer: List<LocalDate>
): Boolean {
    val (forrigeSøkersSmåbarnstilleggAndeler, forrigeSøkersAndreAndeler) = forrigeSøkersAndeler.partition { it.erSmåbarnstillegg() }

    val nyeSmåbarnstilleggAndeler = småbarnstilleggBarnetrygdGenerator.lagSmåbarnstilleggAndeler(
        perioderMedFullOvergangsstønad = nyePerioderMedFullOvergangsstønad,
        andelerSøker = forrigeSøkersAndreAndeler,
        barnasFødselsdatoer = barnasFødselsdatoer
    )

    return forrigeSøkersSmåbarnstilleggAndeler.erUlike(nyeSmåbarnstilleggAndeler)
}

fun hentEndredePerioderISmåbarnstillegg(
    forrigeSmåbarnstilleggAndeler: List<AndelTilkjentYtelse>,
    nyeSmåbarnstilleggAndeler: List<AndelTilkjentYtelse>,
): Pair<List<MånedPeriode>, List<MånedPeriode>> {
    val forrigeAndelerTidslinje = LocalDateTimeline(
        forrigeSmåbarnstilleggAndeler.map {
            LocalDateSegment(
                it.stønadFom.førsteDagIInneværendeMåned(),
                it.stønadTom.sisteDagIInneværendeMåned(),
                it
            )
        }
    )
    val andelerTidslinje = LocalDateTimeline(
        nyeSmåbarnstilleggAndeler.map {
            LocalDateSegment(
                it.stønadFom.førsteDagIInneværendeMåned(),
                it.stønadTom.sisteDagIInneværendeMåned(),
                it
            )
        }
    )

    val segmenterLagtTil = andelerTidslinje.disjoint(forrigeAndelerTidslinje)
    val segmenterFjernet = forrigeAndelerTidslinje.disjoint(andelerTidslinje)

    return Pair(
        segmenterLagtTil.toSegments().map { MånedPeriode(fom = it.fom.toYearMonth(), tom = it.tom.toYearMonth()) },
        segmenterFjernet.toSegments().map { MånedPeriode(fom = it.fom.toYearMonth(), tom = it.tom.toYearMonth()) }
    )
}
