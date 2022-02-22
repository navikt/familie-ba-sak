package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.common.erDagenFør
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseUtils.erBack2BackIMånedsskifte
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseUtils.slåSammenPerioderSomIkkeSkulleHaVærtSplittet
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import java.math.BigDecimal
import java.time.LocalDate

data class UtvidetBarnetrygdGenerator(
    val behandlingId: Long,
    val tilkjentYtelse: TilkjentYtelse
) {

    fun lagUtvidetBarnetrygdAndeler(
        utvidetVilkår: List<VilkårResultat>,
        andelerBarna: List<AndelTilkjentYtelse>
    ): List<AndelTilkjentYtelse> {
        if (utvidetVilkår.isEmpty() || andelerBarna.isEmpty()) return emptyList()

        val søkerAktør = utvidetVilkår.first().personResultat?.aktør ?: error("Vilkår mangler PersonResultat")

        val datoSegmenter = utvidetVilkår
            .filter { it.resultat == Resultat.OPPFYLT }
            .map {
                it.tilDatoSegment(utvidetVilkår, søkerAktør)
            }

        val utvidaTidslinje = LocalDateTimeline(datoSegmenter)

        val barnasTidslinjer: List<LocalDateTimeline<List<PeriodeData>>> = andelerBarna
            .groupBy { it.aktør }
            .map { identMedAndeler ->
                LocalDateTimeline(
                    identMedAndeler.value.map {
                        LocalDateSegment(
                            it.stønadFom.førsteDagIInneværendeMåned(),
                            it.stønadTom.sisteDagIInneværendeMåned(),
                            listOf(
                                PeriodeData(
                                    aktør = identMedAndeler.key,
                                    rolle = PersonType.BARN,
                                    prosent = it.prosent
                                )
                            )
                        )
                    }
                )
            }

        val sammenslåttTidslinje = barnasTidslinjer.fold(utvidaTidslinje) { sammenlagt, neste ->
            (kombinerTidslinjer(sammenlagt, neste))
        }

        val utvidetAndeler = sammenslåttTidslinje.toSegments()
            .filter { segment -> segment.value.any { it.rolle == PersonType.BARN } && segment.value.any { it.rolle == PersonType.SØKER } }
            .map {
                val ordinærSatsForPeriode = SatsService.hentGyldigSatsFor(
                    satstype = SatsType.ORBA,
                    stønadFraOgMed = it.fom.toYearMonth(),
                    stønadTilOgMed = it.tom.toYearMonth()
                )
                    .singleOrNull()?.sats
                    ?: error("Skal finnes én ordinær sats for gitt segment oppdelt basert på andeler")
                val prosentForPeriode =
                    it.value.maxByOrNull { data -> data.prosent }?.prosent ?: error("Finner ikke prosent")
                AndelTilkjentYtelse(
                    behandlingId = behandlingId,
                    tilkjentYtelse = tilkjentYtelse,
                    aktør = søkerAktør,
                    stønadFom = it.fom.toYearMonth(),
                    stønadTom = it.tom.toYearMonth(),
                    kalkulertUtbetalingsbeløp = ordinærSatsForPeriode.avrundetHeltallAvProsent(prosentForPeriode),
                    type = YtelseType.UTVIDET_BARNETRYGD,
                    sats = ordinærSatsForPeriode,
                    prosent = prosentForPeriode
                )
            }

        if (utvidetAndeler.isEmpty()) {
            throw FunksjonellFeil(
                "Du har lagt til utvidet barnetrygd for en periode der det ikke er rett til barnetrygd for " +
                    "noen av barna. Hvis du trenger hjelp, ta kontakt med team familie."
            )
        }

        return slåSammenPerioderSomIkkeSkulleHaVærtSplittet(
            andelerTilkjentYtelse = utvidetAndeler.toMutableList(),
            skalAndelerSlåsSammen = ::skalUtvidetAndelerSlåsSammen
        )
    }

    data class PeriodeData(val aktør: Aktør, val rolle: PersonType, val prosent: BigDecimal = BigDecimal.ZERO)

    private fun skalUtvidetAndelerSlåsSammen(
        førsteAndel: AndelTilkjentYtelse,
        nesteAndel: AndelTilkjentYtelse
    ): Boolean =
        førsteAndel.stønadTom.sisteDagIInneværendeMåned()
            .erDagenFør(nesteAndel.stønadFom.førsteDagIInneværendeMåned()) &&
            førsteAndel.kalkulertUtbetalingsbeløp == nesteAndel.kalkulertUtbetalingsbeløp

    private fun kombinerTidslinjer(
        sammenlagtTidslinje: LocalDateTimeline<List<PeriodeData>>,
        tidslinje: LocalDateTimeline<List<PeriodeData>>
    ): LocalDateTimeline<List<PeriodeData>> {
        val sammenlagt =
            sammenlagtTidslinje.combine(
                tidslinje,
                StandardCombinators::bothValues,
                LocalDateTimeline.JoinStyle.CROSS_JOIN
            ) as LocalDateTimeline<List<List<PeriodeData>>>

        return LocalDateTimeline(
            sammenlagt.toSegments().map {
                LocalDateSegment(it.fom, it.tom, it.value.flatten())
            }
        )
    }
}

fun VilkårResultat.tilDatoSegment(
    utvidetVilkår: List<VilkårResultat>,
    søkerAktør: Aktør
): LocalDateSegment<List<UtvidetBarnetrygdGenerator.PeriodeData>> {
    if (this.periodeFom == null) throw Feil("Fom må være satt på søkers periode ved utvida barnetrygd")
    val fraOgMedDato = this.periodeFom!!.førsteDagINesteMåned()
    val tilOgMedDato = finnTilOgMedDatoForUtvidetSegment(tilOgMed = this.periodeTom, vilkårResultater = utvidetVilkår)
    if (tilOgMedDato.toYearMonth() == fraOgMedDato.toYearMonth()
        .minusMonths(1)
    ) throw FunksjonellFeil("Du kan ikke legge inn fom. og tom. innenfor samme kalendermåned.")
    return LocalDateSegment(
        fraOgMedDato,
        tilOgMedDato,
        listOf(UtvidetBarnetrygdGenerator.PeriodeData(aktør = søkerAktør, rolle = PersonType.SØKER))
    )
}

private fun finnTilOgMedDatoForUtvidetSegment(
    tilOgMed: LocalDate?,
    vilkårResultater: List<VilkårResultat>
): LocalDate {
    // LocalDateTimeline krasjer i isTimelineOutsideInterval funksjonen dersom vi sender med TIDENES_ENDE,
    // så bruker tidenes ende minus én dag.
    if (tilOgMed == null) return TIDENES_ENDE.minusDays(1)
    val utvidetSkalVidereføresEnMndEkstra = vilkårResultater.any { vilkårResultat ->
        erBack2BackIMånedsskifte(
            tilOgMed = tilOgMed,
            fraOgMed = vilkårResultat.periodeFom
        )
    }

    return if (utvidetSkalVidereføresEnMndEkstra) {
        tilOgMed.plusMonths(1).sisteDagIMåned()
    } else tilOgMed.sisteDagIMåned()
}
