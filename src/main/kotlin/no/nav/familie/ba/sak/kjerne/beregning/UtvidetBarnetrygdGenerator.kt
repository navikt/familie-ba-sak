package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagINesteMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators

data class UtvidetBarnetrygdGenerator(
        val behandlingId: Long,
        val tilkjentYtelse: TilkjentYtelse
) {

    // TODO: Beløp her må erstattes med prosent

    fun lagUtvidetBarnetrygdAndeler(utvidetVilkår: List<VilkårResultat>,
                                    andelerBarna: List<AndelTilkjentYtelse>): List<AndelTilkjentYtelse> {
        if (utvidetVilkår.isEmpty() || andelerBarna.isEmpty()) return emptyList()

        val søkerIdent = utvidetVilkår.first().personResultat?.personIdent ?: error("Vilkår mangler PersonResultat")

        val utvidaTidslinje = LocalDateTimeline(
                utvidetVilkår
                        .filter { it.resultat == Resultat.OPPFYLT }
                        .map {
                            if (it.periodeFom == null || it.periodeTom == null) throw Feil("Fom og tom må være satt på søkers periode ved utvida barnetrygd")
                            LocalDateSegment(
                                    it.periodeFom!!.førsteDagINesteMåned(),
                                    it.periodeTom!!.sisteDagINesteMåned(),
                                    listOf(PeriodeData(ident = søkerIdent, rolle = PersonType.SØKER))
                            )
                        })

        val barnasTidslinjer = andelerBarna
                .groupBy { it.personIdent }
                .map { identMedAndeler ->
                    LocalDateTimeline(identMedAndeler.value.map {
                        LocalDateSegment(
                                it.stønadFom.førsteDagIInneværendeMåned(),
                                it.stønadTom.sisteDagIInneværendeMåned(),
                                listOf(PeriodeData(ident = identMedAndeler.key, rolle = PersonType.BARN, beløp = it.beløp))
                        )
                    })
                }

        val sammenslåttTidslinje = barnasTidslinjer.fold(utvidaTidslinje) { sammenlagt, neste ->
            (kombinerTidslinjer(sammenlagt, neste))
        }

        return sammenslåttTidslinje.toSegments()
                .filter { segement -> segement.value.any { it.rolle == PersonType.BARN } && segement.value.any { it.rolle == PersonType.SØKER } }
                .map {
                    AndelTilkjentYtelse(
                            behandlingId = behandlingId,
                            tilkjentYtelse = tilkjentYtelse,
                            personIdent = søkerIdent,
                            stønadFom = it.fom.toYearMonth(),
                            stønadTom = it.tom.toYearMonth(),
                            beløp = it.value.maxByOrNull { data -> data.beløp }?.beløp ?: error("Finner ikke beløp"),
                            type = YtelseType.UTVIDET_BARNETRYGD
                    )
                }
    }

    private data class PeriodeData(val ident: String, val rolle: PersonType, val beløp: Int = 0)

    private fun kombinerTidslinjer(sammenlagtTidslinje: LocalDateTimeline<List<PeriodeData>>,
                                   tidslinje: LocalDateTimeline<List<PeriodeData>>): LocalDateTimeline<List<PeriodeData>> {
        val sammenlagt =
                sammenlagtTidslinje.combine(tidslinje,
                                            StandardCombinators::bothValues,
                                            LocalDateTimeline.JoinStyle.CROSS_JOIN) as LocalDateTimeline<List<List<PeriodeData>>>

        return LocalDateTimeline(sammenlagt.toSegments().map {
            LocalDateSegment(it.fom, it.tom, it.value.flatten())
        })
    }
}