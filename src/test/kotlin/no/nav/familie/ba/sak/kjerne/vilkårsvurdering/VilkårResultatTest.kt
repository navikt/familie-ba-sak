package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class VilkårResultatTest {

    @Test
    fun `Test valider opsjoner`() {
        assertTrue(opprettVilkårResultat(Vilkår.BOR_MED_SØKER, erDeltBosted = true).validerOpsjoner().isEmpty())
        assertTrue(opprettVilkårResultat(Vilkår.BOSATT_I_RIKET, erSkjønnsmessigVurdert = true).validerOpsjoner().isEmpty())
        assertTrue(opprettVilkårResultat(Vilkår.BOSATT_I_RIKET, erMedlemskapVurdert = true).validerOpsjoner().isEmpty())

        assertFalse(opprettVilkårResultat(Vilkår.LOVLIG_OPPHOLD, erDeltBosted = true).validerOpsjoner().isEmpty())
        assertFalse(opprettVilkårResultat(Vilkår.LOVLIG_OPPHOLD, erSkjønnsmessigVurdert = true).validerOpsjoner().isEmpty())
        assertFalse(opprettVilkårResultat(Vilkår.LOVLIG_OPPHOLD, erMedlemskapVurdert = true).validerOpsjoner().isEmpty())

        assertTrue(opprettVilkårResultat(Vilkår.LOVLIG_OPPHOLD, erDeltBosted = true).validerOpsjoner()
                           .first()
                           .contains("Delt bosted"))
        assertTrue(opprettVilkårResultat(Vilkår.LOVLIG_OPPHOLD, erSkjønnsmessigVurdert = true).validerOpsjoner()
                           .first()
                           .contains("Vurdering annet grunnlag"))
        assertTrue(opprettVilkårResultat(Vilkår.LOVLIG_OPPHOLD, erMedlemskapVurdert = true).validerOpsjoner()
                           .first()
                           .contains("Medlemskap vurdert"))
    }

    private fun opprettVilkårResultat(vilkårType: Vilkår,
                                      erDeltBosted: Boolean = false,
                                      erMedlemskapVurdert: Boolean = false,
                                      erSkjønnsmessigVurdert: Boolean = false) =
            VilkårResultat(vilkårType = vilkårType,
                           erDeltBosted = erDeltBosted,
                           erMedlemskapVurdert = erMedlemskapVurdert,
                           erSkjønnsmessigVurdert = erSkjønnsmessigVurdert,
                           begrunnelse = "",
                           behandlingId = 1,
                           personResultat = null,
                           regelInput = null,
                           regelOutput = null,
                           resultat = Resultat.OPPFYLT)

}