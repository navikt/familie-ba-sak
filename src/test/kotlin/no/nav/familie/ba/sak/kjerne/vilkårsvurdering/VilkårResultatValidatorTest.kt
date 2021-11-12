package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

internal class VilkårResultatValidatorTest {

    // @Test
    // fun `convertere liste av UtdypendeVilkårsvurdering til string`() {
    //     val uvListe = emptyList<UtdypendeVilkårsvurdering>()
    //     val utdypendeVilkårsvurderingListConverter = UtdypendeVilkårsvurderingListConverter()
    //     val string = utdypendeVilkårsvurderingListConverter.convertToDatabaseColumn(uvListe)
    //     println(string)
    // }
    //
    // @Test
    // fun `Test valider opsjoner`() {
    //     val vilkårResultatValidator = VilkårResultatValidator()
    //
    //     assertTrue(
    //         vilkårResultatValidator.isValid(
    //             opprettVilkårResultat(Vilkår.BOR_MED_SØKER, erDeltBosted = true),
    //             null
    //         )
    //     )
    //     assertTrue(
    //         vilkårResultatValidator.isValid(
    //             opprettVilkårResultat(
    //                 Vilkår.BOSATT_I_RIKET,
    //                 erSkjønnsmessigVurdert = true
    //             ),
    //             null
    //         )
    //     )
    //     assertTrue(
    //         vilkårResultatValidator.isValid(
    //             opprettVilkårResultat(
    //                 Vilkår.BOSATT_I_RIKET,
    //                 erMedlemskapVurdert = true
    //             ),
    //             null
    //         )
    //     )
    //     assertTrue(
    //         vilkårResultatValidator.isValid(
    //             opprettVilkårResultat(
    //                 Vilkår.LOVLIG_OPPHOLD,
    //                 erSkjønnsmessigVurdert = true
    //             ),
    //             null
    //         )
    //     )
    //
    //     assertFalse(
    //         vilkårResultatValidator.isValid(
    //             opprettVilkårResultat(Vilkår.LOVLIG_OPPHOLD, erDeltBosted = true),
    //             null
    //         )
    //     )
    //     assertFalse(
    //         vilkårResultatValidator.isValid(
    //             opprettVilkårResultat(
    //                 Vilkår.LOVLIG_OPPHOLD,
    //                 erMedlemskapVurdert = true
    //             ),
    //             null
    //         )
    //     )
    // }

    private fun opprettVilkårResultat(
        vilkårType: Vilkår,
        erDeltBosted: Boolean = false,
        erMedlemskapVurdert: Boolean = false,
        erSkjønnsmessigVurdert: Boolean = false
    ) =
        VilkårResultat(
            vilkårType = vilkårType,
            erDeltBosted = erDeltBosted,
            erMedlemskapVurdert = erMedlemskapVurdert,
            erSkjønnsmessigVurdert = erSkjønnsmessigVurdert,
            begrunnelse = "",
            behandlingId = 1,
            personResultat = null,
            resultat = Resultat.OPPFYLT
        )
}
