package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.common.erBack2BackIMånedsskifte
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
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
        andelerBarna: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        if (utvidetVilkår.isEmpty() || andelerBarna.isEmpty()) return emptyList()

        val søkerAktør = utvidetVilkår.first().personResultat?.aktør ?: error("Vilkår mangler PersonResultat")

        val datoSegmenter = utvidetVilkår
            .filter { it.resultat == Resultat.OPPFYLT }
            .map {
                it.tilDatoSegment(utvidetVilkår = utvidetVilkår)
            }

        val utvidaTidslinje = LocalDateTimeline(datoSegmenter)

        val barnasTidslinje =
            utledTidslinjeForBarna(andelerBarna)

        val sammenslåttTidslinje = kombinerTidslinjer(utvidaTidslinje, barnasTidslinje)

        val utvidetAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger> = sammenslåttTidslinje.toSegments()
            .filter { segment -> segment.value.any { it.rolle == PersonType.BARN } && segment.value.any { it.rolle == PersonType.SØKER } }
            .flatMap { segment ->
                lagAndelerForSegmentBasertPåSatsperioder(segment, søkerAktør)
            }

        if (utvidetAndeler.isEmpty()) {
            throw FunksjonellFeil(
                "Du har lagt til utvidet barnetrygd for en periode der det ikke er rett til barnetrygd for " +
                    "noen av barna. Hvis du trenger hjelp, ta kontakt med team familie."
            )
        }

        return utvidetAndeler
    }

    private fun lagAndelerForSegmentBasertPåSatsperioder(
        segment: LocalDateSegment<List<PeriodeData>>,
        søkerAktør: Aktør
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        val ordinæreSatserForPeriode = SatsService.hentGyldigSatsFor(
            satstype = SatsType.ORBA,
            stønadFraOgMed = segment.fom.toYearMonth(),
            stønadTilOgMed = segment.tom.toYearMonth()
        )

        if (ordinæreSatserForPeriode.isEmpty()) {
            error("Finner ikke sats for periode fom=${segment.fom}, tom=${segment.tom}")
        }

        val prosentForPeriode =
            segment.value.maxByOrNull { data -> data.prosent }?.prosent
                ?: error("Finner ikke prosent for periode fom=${segment.fom}, tom=${segment.tom}")

        return ordinæreSatserForPeriode.map { satsperiode ->
            val nasjonaltPeriodebeløp = satsperiode.sats.avrundetHeltallAvProsent(prosentForPeriode)
            val aty = AndelTilkjentYtelse(
                behandlingId = behandlingId,
                tilkjentYtelse = tilkjentYtelse,
                aktør = søkerAktør,
                stønadFom = satsperiode.fraOgMed,
                stønadTom = satsperiode.tilOgMed,
                kalkulertUtbetalingsbeløp = nasjonaltPeriodebeløp,
                nasjonaltPeriodebeløp = nasjonaltPeriodebeløp,
                type = YtelseType.UTVIDET_BARNETRYGD,
                sats = satsperiode.sats,
                prosent = prosentForPeriode
            )

            AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(aty)
        }
    }

    data class PeriodeData(val rolle: PersonType, val prosent: BigDecimal = BigDecimal.ZERO)

    private fun utledTidslinjeForBarna(andelerBarna: List<AndelTilkjentYtelseMedEndreteUtbetalinger>): LocalDateTimeline<List<PeriodeData>> {
        val barnasTidslinjer = andelerBarna
            .groupBy { it.aktør }
            .map { lagTidslinjeForBarn(identMedAndeler = it) }

        val sammenlagtTidslinjeForBarna = barnasTidslinjer.reduce { sammenlagt, neste ->
            (kombinerTidslinjer(sammenlagt, neste))
        }

        val barnasSegmenterSlåttSammenHvisLikProsent =
            slåSammenEtterfølgendeSegmenterMedLikProsent(sammenlagtTidslinjeForBarna)

        return LocalDateTimeline(barnasSegmenterSlåttSammenHvisLikProsent)
    }

    private fun lagTidslinjeForBarn(identMedAndeler: Map.Entry<Aktør, List<AndelTilkjentYtelseMedEndreteUtbetalinger>>) =
        LocalDateTimeline(
            identMedAndeler.value.map {
                lagSegmentMedPeriodeData(
                    fom = it.stønadFom.førsteDagIInneværendeMåned(),
                    tom = it.stønadTom.sisteDagIInneværendeMåned(),
                    prosentForPeriode = it.prosent,
                    personType = PersonType.BARN
                )
            }
        )

    private fun slåSammenEtterfølgendeSegmenterMedLikProsent(sammenlagtTidslinjeForBarna: LocalDateTimeline<List<PeriodeData>>): List<LocalDateSegment<List<PeriodeData>>> {
        return sammenlagtTidslinjeForBarna.toSegments()
            .fold(listOf<LocalDateSegment<List<PeriodeData>>>()) { sammenslåttePerioder, nestePeriode ->
                val sistePeriodeSomErSammenslått = sammenslåttePerioder.lastOrNull()
                val segmenterErEtterfølgendeOgHarSammeProsent =
                    sistePeriodeSomErSammenslått?.tom?.toYearMonth() == nestePeriode.fom.forrigeMåned() &&
                        sistePeriodeSomErSammenslått.value.maxOf { it.prosent } == nestePeriode.value.maxOf { it.prosent }

                if (sistePeriodeSomErSammenslått != null && segmenterErEtterfølgendeOgHarSammeProsent) {
                    val prosentForPeriode = sistePeriodeSomErSammenslått.value.maxOf { it.prosent }
                    sammenslåttePerioder.dropLast(1) + lagSegmentMedPeriodeData(
                        fom = sistePeriodeSomErSammenslått.fom,
                        tom = nestePeriode.tom,
                        prosentForPeriode = prosentForPeriode,
                        personType = PersonType.BARN
                    )
                } else {
                    sammenslåttePerioder + lagSegmentMedPeriodeData(
                        fom = nestePeriode.fom,
                        tom = nestePeriode.tom,
                        prosentForPeriode = nestePeriode.value.maxOf { it.prosent },
                        personType = PersonType.BARN
                    )
                }
            }
    }

    private fun lagSegmentMedPeriodeData(
        fom: LocalDate,
        tom: LocalDate,
        prosentForPeriode: BigDecimal,
        personType: PersonType
    ) = LocalDateSegment(
        fom,
        tom,
        listOf(
            PeriodeData(
                rolle = personType,
                prosent = prosentForPeriode
            )
        )
    )

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
    utvidetVilkår: List<VilkårResultat>
): LocalDateSegment<List<UtvidetBarnetrygdGenerator.PeriodeData>> {
    if (this.periodeFom == null) throw Feil("Fom må være satt på søkers periode ved utvidet barnetrygd")
    val fraOgMedDato = this.periodeFom!!.førsteDagINesteMåned()
    val tilOgMedDato = finnTilOgMedDato(tilOgMed = this.periodeTom, vilkårResultater = utvidetVilkår)
    if (tilOgMedDato.toYearMonth() == fraOgMedDato.toYearMonth()
        .minusMonths(1)
    ) {
        throw FunksjonellFeil("Du kan ikke legge inn fom. og tom. innenfor samme kalendermåned. Gå til utvidet barnetrygd vilkåret for å endre.")
    }
    return LocalDateSegment(
        fraOgMedDato,
        tilOgMedDato,
        listOf(UtvidetBarnetrygdGenerator.PeriodeData(rolle = PersonType.SØKER))
    )
}

fun finnTilOgMedDato(
    tilOgMed: LocalDate?,
    vilkårResultater: List<VilkårResultat>
): LocalDate {
    // LocalDateTimeline krasjer i isTimelineOutsideInterval funksjonen dersom vi sender med TIDENES_ENDE,
    // så bruker tidenes ende minus én dag.
    if (tilOgMed == null) return TIDENES_ENDE.minusDays(1)
    val skalVidereføresEnMndEkstra = vilkårResultater.any { vilkårResultat ->
        erBack2BackIMånedsskifte(
            tilOgMed = tilOgMed,
            fraOgMed = vilkårResultat.periodeFom
        )
    }

    return if (skalVidereføresEnMndEkstra) {
        tilOgMed.plusMonths(1).sisteDagIMåned()
    } else {
        tilOgMed.sisteDagIMåned()
    }
}
