package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import org.springframework.stereotype.Service

@Service
class AndelTilkjentYtelseService(
    private val vilkårsvurderingService: VilkårsvurderingService
) {
    fun vurdertEtter(andeltilkjentYtesle: AndelTilkjentYtelse): Regelverk {
        val relevanteVilkårsResultaer = finnRelevanteVilkårsresulaterForRegelverk(andeltilkjentYtesle)

        return if (relevanteVilkårsResultaer.all { it.vurderesEtter == Regelverk.EØS_FORORDNINGEN }) {
            Regelverk.EØS_FORORDNINGEN
        } else if (relevanteVilkårsResultaer.all { it.vurderesEtter == Regelverk.NASJONALE_REGLER }) {
            Regelverk.NASJONALE_REGLER
        } else {
            error("AndelTilkjentYtelse ${andeltilkjentYtesle.id} er basert på VilkårsResultater med en blanding av nasjonale og EØS regelverk.")
        }
    }

    fun finnRelevanteVilkårsresulaterForRegelverk(andeltilkjentYtelse: AndelTilkjentYtelse): List<VilkårResultat> =
        vilkårsvurderingService.hentAktivForBehandling(andeltilkjentYtelse.behandlingId)
            ?.personResultater
            ?.filter { !it.erSøkersResultater() }
            ?.filter { andeltilkjentYtelse.aktør == it.aktør }
            ?.flatMap { it.vilkårResultater }
            ?.filter {
                andeltilkjentYtelse.stønadFom > it.periodeFom?.toYearMonth() &&
                    (it.periodeTom == null || andeltilkjentYtelse.stønadFom <= it.periodeTom?.toYearMonth())
            }
            ?.filter { vilkårResultat ->
                regelverkavhenigeVilkår().any { it == vilkårResultat.vilkårType }
            } ?: emptyList()

    private fun regelverkavhenigeVilkår(): List<Vilkår> {
        return listOf(
            Vilkår.BOR_MED_SØKER,
            Vilkår.BOSATT_I_RIKET,
            Vilkår.LOVLIG_OPPHOLD,
        )
    }
}
