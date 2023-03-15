package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class VilkårTest {

    @Test
    fun `Hent relevante vilkår for persontype SØKER`() {
        val relevanteVilkår = Vilkår.hentVilkårFor(personType = PersonType.SØKER, fagsakType = FagsakType.NORMAL)
        val vilkårForSøker = setOf(
            Vilkår.BOSATT_I_RIKET,
            Vilkår.LOVLIG_OPPHOLD
        )
        Assertions.assertEquals(vilkårForSøker, relevanteVilkår)
    }

    @Test
    fun `Hent relevante vilkår for persontype BARN`() {
        val relevanteVilkår = Vilkår.hentVilkårFor(personType = PersonType.BARN, fagsakType = FagsakType.NORMAL)
        val vilkårForBarn = setOf(
            Vilkår.UNDER_18_ÅR,
            Vilkår.BOR_MED_SØKER,
            Vilkår.GIFT_PARTNERSKAP,
            Vilkår.BOSATT_I_RIKET,
            Vilkår.LOVLIG_OPPHOLD
        )
        Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
    }

    @Test
    fun `Valider gyldige vilkårspermutasjoner for barn og søker`() {
        Assertions.assertEquals(
            setOf(
                Vilkår.UNDER_18_ÅR,
                Vilkår.BOR_MED_SØKER,
                Vilkår.GIFT_PARTNERSKAP,
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD
            ),
            Vilkår.hentVilkårFor(personType = PersonType.BARN, fagsakType = FagsakType.NORMAL)
        )

        Assertions.assertEquals(
            setOf(
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD
            ),
            Vilkår.hentVilkårFor(personType = PersonType.SØKER, fagsakType = FagsakType.NORMAL)
        )
    }

    @Test
    fun `Valider gyldige vilkårspermutasjoner for barn og søker ved utvidet barnetrygd`() {
        Assertions.assertEquals(
            setOf(
                Vilkår.UNDER_18_ÅR,
                Vilkår.BOR_MED_SØKER,
                Vilkår.GIFT_PARTNERSKAP,
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD
            ),
            Vilkår.hentVilkårFor(personType = PersonType.BARN, ytelseType = YtelseType.UTVIDET_BARNETRYGD, fagsakType = FagsakType.NORMAL)
        )

        Assertions.assertEquals(
            setOf(
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD,
                Vilkår.UTVIDET_BARNETRYGD
            ),
            Vilkår.hentVilkårFor(personType = PersonType.SØKER, ytelseType = YtelseType.UTVIDET_BARNETRYGD, fagsakType = FagsakType.NORMAL)
        )
    }
}
