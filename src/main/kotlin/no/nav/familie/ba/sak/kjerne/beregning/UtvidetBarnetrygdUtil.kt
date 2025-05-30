package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.erBack2BackIMånedsskifte
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.lagForskjøvetTidslinjeForOppfylteVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.utvidelser.leftJoin

object UtvidetBarnetrygdUtil {
    internal fun beregnTilkjentYtelseUtvidet(
        andelerTilkjentYtelseBarnaMedEtterbetaling3ÅrEller3MndEndringer: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        tilkjentYtelse: TilkjentYtelse,
        endretUtbetalingAndelerSøker: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>,
        personResultater: Set<PersonResultat>,
        skalBeholdeSplittI0krAndeler: Boolean,
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        val andelerTilkjentYtelseUtvidet =
            UtvidetBarnetrygdGenerator(
                behandlingId = tilkjentYtelse.behandling.id,
                tilkjentYtelse = tilkjentYtelse,
            ).lagUtvidetBarnetrygdAndeler(
                utvidetVilkår = personResultater.finnUtvidetVilkår(),
                andelerBarna = andelerTilkjentYtelseBarnaMedEtterbetaling3ÅrEller3MndEndringer.map { it.andel },
                perioderBarnaBorMedSøkerTidslinje = personResultater.tilPerioderBarnaBorMedSøkerTidslinje(),
            )

        return AndelTilkjentYtelseMedEndretUtbetalingGenerator.lagAndelerMedEndretUtbetalingAndeler(
            andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseUtvidet,
            endretUtbetalingAndeler = endretUtbetalingAndelerSøker,
            tilkjentYtelse = tilkjentYtelse,
            skalBeholdeSplittI0krAndeler = skalBeholdeSplittI0krAndeler,
        )
    }

    fun Set<PersonResultat>.tilPerioderBarnaBorMedSøkerTidslinje(): Map<Aktør, Tidslinje<Boolean>> =
        this.associate { personResultat ->
            personResultat.aktør to
                personResultat.vilkårResultater
                    .lagForskjøvetTidslinjeForOppfylteVilkår(Vilkår.BOR_MED_SØKER)
                    .mapVerdi { vilkårResultat ->
                        vilkårResultat?.utdypendeVilkårsvurderinger?.none {
                            it in
                                listOf(
                                    UtdypendeVilkårsvurdering.BARN_BOR_I_EØS_MED_ANNEN_FORELDER,
                                    UtdypendeVilkårsvurdering.BARN_BOR_I_STORBRITANNIA_MED_ANNEN_FORELDER,
                                )
                        }
                    }
        }

    fun Map<Aktør, Tidslinje<AndelTilkjentYtelse>>.filtrertForPerioderBarnaBorMedSøker(perioderBarnaBorMedSøkerTidslinje: Map<Aktør, Tidslinje<Boolean>>): Map<Aktør, Tidslinje<AndelTilkjentYtelse>> =
        this.leftJoin(perioderBarnaBorMedSøkerTidslinje) { andel, barnBorMedSøker ->
            when (barnBorMedSøker) {
                true -> andel
                else -> null
            }
        }

    private fun Set<PersonResultat>.finnUtvidetVilkår(): List<VilkårResultat> {
        val utvidetVilkårResultater =
            this
                .flatMap { it.vilkårResultater }
                .filter { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD && it.resultat == Resultat.OPPFYLT }

        utvidetVilkårResultater.forEach { validerUtvidetVilkårsresultat(vilkårResultat = it, utvidetVilkårResultater = utvidetVilkårResultater) }
        return utvidetVilkårResultater
    }

    internal fun validerUtvidetVilkårsresultat(
        vilkårResultat: VilkårResultat,
        utvidetVilkårResultater: List<VilkårResultat>,
    ) {
        val fom = vilkårResultat.periodeFom?.toYearMonth()
        val tom = vilkårResultat.periodeTom?.toYearMonth()

        val finnesEtterfølgendeBack2BackPeriode = utvidetVilkårResultater.any { erBack2BackIMånedsskifte(tilOgMed = vilkårResultat.periodeTom, fraOgMed = it.periodeFom) }

        if (fom == null) {
            throw Feil("Fom må være satt på søkers periode ved utvidet barnetrygd")
        }
        if (fom == tom && !finnesEtterfølgendeBack2BackPeriode) {
            secureLogger.warn("Du kan ikke legge inn fom og tom innenfor samme kalendermåned: $vilkårResultat")
            throw FunksjonellFeil("Du kan ikke legge inn fom og tom innenfor samme kalendermåned. Gå til utvidet barnetrygd vilkåret for å endre")
        }
    }
}
