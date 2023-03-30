package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.erBack2BackIMånedsskifte
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering

object UtvidetBarnetrygdUtil {
    internal fun beregnTilkjentYtelseUtvidet(
        utvidetVilkår: List<VilkårResultat>,
        andelerTilkjentYtelseBarnaMedEtterbetaling3ÅrEndringer: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        tilkjentYtelse: TilkjentYtelse,
        endretUtbetalingAndelerSøker: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        val andelerTilkjentYtelseUtvidet = UtvidetBarnetrygdGenerator(
            behandlingId = tilkjentYtelse.behandling.behandlingId,
            tilkjentYtelse = tilkjentYtelse
        )
            .lagUtvidetBarnetrygdAndeler(
                utvidetVilkår = utvidetVilkår,
                andelerBarna = andelerTilkjentYtelseBarnaMedEtterbetaling3ÅrEndringer.map { it.andel }
            )

        return TilkjentYtelseUtils.oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
            andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseUtvidet,
            endretUtbetalingAndeler = endretUtbetalingAndelerSøker
        )
    }

    internal fun finnUtvidetVilkår(vilkårsvurdering: Vilkårsvurdering): List<VilkårResultat> {
        val utvidetVilkårResultater = vilkårsvurdering.personResultater
            .flatMap { it.vilkårResultater }
            .filter { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD && it.resultat == Resultat.OPPFYLT }

        utvidetVilkårResultater.forEach {
            validerUtvidetVilkårsresultat(
                vilkårResultat = it,
                utvidetVilkårResultater = utvidetVilkårResultater
            )
        }
        return utvidetVilkårResultater
    }

    internal fun validerUtvidetVilkårsresultat(
        vilkårResultat: VilkårResultat,
        utvidetVilkårResultater: List<VilkårResultat>
    ) {
        val fom = vilkårResultat.periodeFom?.toYearMonth()
        val tom = vilkårResultat.periodeTom?.toYearMonth()

        val finnesEtterfølgendeBack2BackPeriode = utvidetVilkårResultater.any {
            erBack2BackIMånedsskifte(
                tilOgMed = vilkårResultat.periodeTom,
                fraOgMed = it.periodeFom
            )
        }

        if (fom == null) {
            throw Feil("Fom må være satt på søkers periode ved utvidet barnetrygd")
        }
        if (fom == tom && !finnesEtterfølgendeBack2BackPeriode) {
            secureLogger.warn("Du kan ikke legge inn fom og tom innenfor samme kalendermåned: $vilkårResultat")
            throw FunksjonellFeil("Du kan ikke legge inn fom og tom innenfor samme kalendermåned. Gå til utvidet barnetrygd vilkåret for å endre")
        }
    }
}
