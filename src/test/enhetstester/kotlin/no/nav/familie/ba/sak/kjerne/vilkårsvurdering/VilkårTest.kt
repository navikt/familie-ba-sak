package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class VilkårTest {

    @Nested
    inner class `Hent relevante vilkår for persontype BARN` {
        @Test
        fun `For ordinær nasjonal sak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(personType = PersonType.BARN, ytelseType = YtelseType.ORDINÆR_BARNETRYGD, fagsakType = FagsakType.NORMAL)
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
        fun `For utvidet nasjonal sak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(personType = PersonType.BARN, ytelseType = YtelseType.UTVIDET_BARNETRYGD, fagsakType = FagsakType.NORMAL)
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
        fun `For ordinær institusjonssak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(personType = PersonType.BARN, ytelseType = YtelseType.ORDINÆR_BARNETRYGD, fagsakType = FagsakType.INSTITUSJON)
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
        fun `For utvidet institusjonssak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(personType = PersonType.BARN, ytelseType = YtelseType.UTVIDET_BARNETRYGD, fagsakType = FagsakType.INSTITUSJON)
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
        fun `For ordinær enslig mindreårig sak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(personType = PersonType.BARN, ytelseType = YtelseType.ORDINÆR_BARNETRYGD, fagsakType = FagsakType.BARN_ENSLIG_MINDREÅRIG)
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
        fun `For utvidet enslig mindreårig sak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(personType = PersonType.BARN, ytelseType = YtelseType.UTVIDET_BARNETRYGD, fagsakType = FagsakType.BARN_ENSLIG_MINDREÅRIG)
            val vilkårForBarn = setOf(
                Vilkår.UNDER_18_ÅR,
                Vilkår.BOR_MED_SØKER,
                Vilkår.GIFT_PARTNERSKAP,
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD,
                Vilkår.UTVIDET_BARNETRYGD
            )
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }
    }

    @Nested
    inner class `Hent relevante vilkår for persontype SØKER` {
        @Test
        fun `For ordinær nasjonal sak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(personType = PersonType.SØKER, ytelseType = YtelseType.ORDINÆR_BARNETRYGD, fagsakType = FagsakType.NORMAL)
            val vilkårForBarn = setOf(
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD
            )
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }

        @Test
        fun `For utvidet nasjonal sak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(personType = PersonType.SØKER, ytelseType = YtelseType.UTVIDET_BARNETRYGD, fagsakType = FagsakType.NORMAL)
            val vilkårForBarn = setOf(
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD,
                Vilkår.UTVIDET_BARNETRYGD
            )
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }

        @Test
        fun `For ordinær institusjonssak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(personType = PersonType.SØKER, ytelseType = YtelseType.ORDINÆR_BARNETRYGD, fagsakType = FagsakType.INSTITUSJON)
            val vilkårForBarn = emptySet<Vilkår>()
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }

        @Test
        fun `For utvidet institusjonssak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(personType = PersonType.SØKER, ytelseType = YtelseType.UTVIDET_BARNETRYGD, fagsakType = FagsakType.INSTITUSJON)
            val vilkårForBarn = emptySet<Vilkår>()
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }

        @Test
        fun `For ordinær enslig mindreårig sak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(personType = PersonType.SØKER, ytelseType = YtelseType.ORDINÆR_BARNETRYGD, fagsakType = FagsakType.BARN_ENSLIG_MINDREÅRIG)
            val vilkårForBarn = emptySet<Vilkår>()
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }

        @Test
        fun `For utvidet enslig mindreårig sak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(personType = PersonType.SØKER, ytelseType = YtelseType.UTVIDET_BARNETRYGD, fagsakType = FagsakType.BARN_ENSLIG_MINDREÅRIG)
            val vilkårForBarn = emptySet<Vilkår>()
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }
    }
}
