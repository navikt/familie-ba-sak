package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseUtils.erBack2BackIMånedsskifte
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
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

        val søkerIdent = utvidetVilkår.first().personResultat?.personIdent ?: error("Vilkår mangler PersonResultat")

        val utvidaTidslinje = LocalDateTimeline(
            utvidetVilkår
                .filter { it.resultat == Resultat.OPPFYLT }
                .map {
                    if (it.periodeFom == null) throw Feil("Fom må være satt på søkers periode ved utvida barnetrygd")
                    LocalDateSegment(
                        it.periodeFom!!.førsteDagINesteMåned(),
                        finnTilOgMedDatoForUtvidetSegment(tilOgMed = it.periodeTom, vilkårResultater = utvidetVilkår),
                        listOf(PeriodeData(ident = søkerIdent, rolle = PersonType.SØKER))
                    )
                }
        )

        val barnasTidslinjer: List<LocalDateTimeline<List<PeriodeData>>> = andelerBarna
            .groupBy { it.personIdent }
            .map { identMedAndeler ->
                LocalDateTimeline(
                    identMedAndeler.value.map {
                        LocalDateSegment(
                            it.stønadFom.førsteDagIInneværendeMåned(),
                            it.stønadTom.sisteDagIInneværendeMåned(),
                            listOf(
                                PeriodeData(
                                    ident = identMedAndeler.key,
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

        return sammenslåttTidslinje.toSegments()
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
                    personIdent = søkerIdent,
                    stønadFom = it.fom.toYearMonth(),
                    stønadTom = it.tom.toYearMonth(),
                    kalkulertUtbetalingsbeløp = ordinærSatsForPeriode.avrundetHeltallAvProsent(prosentForPeriode),
                    type = YtelseType.UTVIDET_BARNETRYGD,
                    sats = ordinærSatsForPeriode,
                    prosent = prosentForPeriode
                )
            }
    }

    private data class PeriodeData(val ident: String, val rolle: PersonType, val prosent: BigDecimal = BigDecimal.ZERO)

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

    private fun finnTilOgMedDatoForUtvidetSegment(
        tilOgMed: LocalDate?,
        vilkårResultater: List<VilkårResultat>
    ): LocalDate {
        if (tilOgMed == null) return TIDENES_ENDE
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
}
