package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.kjerne.beregning.UtvidetBarnetrygdUtil.filtrertForPerioderBarnaBorMedSøker
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerKunVerdiMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullOgIkkeTom
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvetTidslinjeForOppfyltVilkårForVoksenPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.tidslinje.Tidslinje

data class UtvidetBarnetrygdGenerator(
    val behandlingId: Long,
    val tilkjentYtelse: TilkjentYtelse,
) {
    fun lagUtvidetBarnetrygdAndeler(
        utvidetVilkår: List<VilkårResultat>,
        andelerBarna: List<AndelTilkjentYtelse>,
        perioderBarnaBorMedSøkerTidslinje: Map<Aktør, Tidslinje<Boolean>>,
    ): List<AndelTilkjentYtelse> {
        if (utvidetVilkår.isEmpty() || andelerBarna.isEmpty()) return emptyList()

        val søkerAktør = utvidetVilkår.first().personResultat?.aktør ?: error("Vilkår mangler PersonResultat")

        val utvidetVilkårTidslinje = utvidetVilkår.tilForskjøvetTidslinjeForOppfyltVilkårForVoksenPerson(Vilkår.UTVIDET_BARNETRYGD)

        val barnasAndelerFiltrertForPerioderBarnaBorMedSøker =
            andelerBarna.tilSeparateTidslinjerForBarna().filtrertForPerioderBarnaBorMedSøker(perioderBarnaBorMedSøkerTidslinje)

        val størsteProsentTidslinje =
            barnasAndelerFiltrertForPerioderBarnaBorMedSøker.values
                .kombinerUtenNullOgIkkeTom { andeler -> andeler.maxOf { it.prosent } }

        val utvidetAndeler =
            utvidetVilkårTidslinje
                .kombinerKunVerdiMed(
                    størsteProsentTidslinje,
                    satstypeTidslinje(SatsType.UTVIDET_BARNETRYGD),
                ) { _, prosent, sats ->
                    val nasjonaltPeriodebeløp = sats.avrundetHeltallAvProsent(prosent)
                    AndelTilkjentYtelseForTidslinje(
                        aktør = søkerAktør,
                        beløp = nasjonaltPeriodebeløp,
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                        sats = sats,
                        prosent = prosent,
                    )
                }.tilAndelerTilkjentYtelse(tilkjentYtelse)

        if (utvidetAndeler.isEmpty()) {
            throw FunksjonellFeil(
                "Du har lagt til utvidet barnetrygd for en periode der det ikke er rett til barnetrygd for " +
                    "noen av barna. Hvis du trenger hjelp, meld sak i Porten.",
            )
        }

        return utvidetAndeler
    }
}
