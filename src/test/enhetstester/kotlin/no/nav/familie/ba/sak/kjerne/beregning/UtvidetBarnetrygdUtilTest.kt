package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.UtvidetBarnetrygdUtil.validerUtvidetVilkårsresultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class UtvidetBarnetrygdUtilTest {
    @Test
    fun `Valider utvidet vilkår - skal kaste feil hvis fom og tom er i samme kalendermåned uten etterfølgende periode`() {
        val vilkårResultat = lagVilkårResultat(
            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
            periodeFom = LocalDate.of(2022, 7, 1),
            periodeTom = LocalDate.of(2022, 7, 31),
            resultat = Resultat.OPPFYLT
        )

        assertThrows<FunksjonellFeil> { validerUtvidetVilkårsresultat(vilkårResultat = vilkårResultat, utvidetVilkårResultater = listOf(vilkårResultat)) }
    }

    @Test
    fun `Valider utvidet vilkår - skal ikke kaste feil hvis fom og tom er i samme kalendermåned og har etterfølgende periode`() {
        val vilkårResultat = lagVilkårResultat(
            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
            periodeFom = LocalDate.of(2022, 7, 1),
            periodeTom = LocalDate.of(2022, 7, 31),
            resultat = Resultat.OPPFYLT
        )

        val etterfølgendeVilkårResultat = lagVilkårResultat(
            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
            periodeFom = LocalDate.of(2022, 8, 1),
            periodeTom = LocalDate.of(2022, 12, 10),
            resultat = Resultat.OPPFYLT
        )

        assertDoesNotThrow { validerUtvidetVilkårsresultat(vilkårResultat = vilkårResultat, utvidetVilkårResultater = listOf(vilkårResultat, etterfølgendeVilkårResultat)) }
    }
}
