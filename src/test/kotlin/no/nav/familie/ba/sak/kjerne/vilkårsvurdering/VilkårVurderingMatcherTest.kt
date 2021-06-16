package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VilkårVurderingMatcherTest {

    private val randomBehandling = lagBehandling()
    private val søkerFnr = randomFnr()

    private fun likeVilkårResultater(a: VilkårResultat?, b: VilkårResultat?): Boolean =
            a?.vilkårType == b?.vilkårType &&
            a?.resultat == b?.resultat &&
            a?.periodeFom == b?.periodeFom &&
            a?.periodeTom == b?.periodeTom &&
            a?.begrunnelse == b?.begrunnelse &&
            a?.erEksplisittAvslagPåSøknad == b?.erEksplisittAvslagPåSøknad

    @Test
    fun `Kopierte vilkår matches og returneres som par`() {
        val vilkårsvurderingOriginal = lagVilkårsvurdering(søkerFnr, randomBehandling, Resultat.OPPFYLT)
        val vilkårsvurderingKopi = vilkårsvurderingOriginal.kopier()
        val vilkårPar = VilkårsvurderingService.matchVilkårResultater(vilkårsvurderingOriginal, vilkårsvurderingKopi)
        assertTrue(vilkårPar.all { likeVilkårResultater(it.first, it.second) })
    }

    @Test
    fun `Vilkår som kun finnes i den ene vilkårsvurderingen returneres alene`() {
        val vilkårsvurderingOriginal = lagVilkårsvurdering(søkerFnr, randomBehandling, Resultat.OPPFYLT)
        val vilkårsvurderingUlik = lagVilkårsvurdering(søkerFnr, randomBehandling, Resultat.IKKE_OPPFYLT)
        val vilkårPar = VilkårsvurderingService.matchVilkårResultater(vilkårsvurderingOriginal, vilkårsvurderingUlik)
        assertTrue(vilkårPar.any { it.first == null && it.second != null })
        assertTrue(vilkårPar.any { it.first != null && it.second == null })
        assertTrue(vilkårPar.none { it.first == null && it.second == null })
        assertTrue(vilkårPar.none { likeVilkårResultater(it.first, it.second) })
    }
}