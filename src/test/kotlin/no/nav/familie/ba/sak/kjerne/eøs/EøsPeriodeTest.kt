package no.nav.familie.ba.sak.kjerne.eøs

// ktlint-disable no-wildcard-imports
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.jan
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.RegelverkMåned
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.VilkårResultatMåned
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.GIFT_PARTNERSKAP
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.UNDER_18_ÅR
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårRegelverk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class EøsPeriodeTest {

    @Test
    fun skalFinneEøsPerioderKomplisertCase() {

        VilkårsvurderingTester(jan(2021))
            .medVilkår("----------------     ", UNDER_18_ÅR)
            .medVilkår("   EEE NNNN  EEEE--- ", BOSATT_I_RIKET)
            .medVilkår("     EEENNEEEEEEEEE  ", LOVLIG_OPPHOLD)
            .medVilkår("NNNNNNNNNNEEEEEEEEEEE", BOR_MED_SØKER)
            .medVilkår("---------------------", GIFT_PARTNERSKAP)
            .harUtfall("     ? ?NN?   EE     ")
    }

    @Test
    fun skalFinneEøsPerioderÅpentCase() {

        VilkårsvurderingTester(jan(2021))
            .medVilkår("--------->", UNDER_18_ÅR)
            .medVilkår(" EEEE--EE>", BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEE>", LOVLIG_OPPHOLD)
            .medVilkår("EEEENNEEE>", BOR_MED_SØKER)
            .medVilkår("--------->", GIFT_PARTNERSKAP)
            .harUtfall("  EE??? E>")
    }

    @Test
    fun skalFinneEøsPerioderÅpentToveisCase() {

        VilkårsvurderingTester(jan(2021))
            .medVilkår("---------->", UNDER_18_ÅR)
            .medVilkår("EEEEE--EE> ", BOSATT_I_RIKET)
            .medVilkår(" E>        ", LOVLIG_OPPHOLD)
            .medVilkår("EE>        ", BOR_MED_SØKER)
            .medVilkår("---------->", GIFT_PARTNERSKAP)
            .harUtfall("  EEE?? EE>")
    }
}

internal class VilkårsvurderingTester(
    val startMåned: YearMonth,
    val vilkårsresultater: List<VilkårResultatMåned> = emptyList()
) {

    fun medVilkår(v: String, vilkår: Vilkår): VilkårsvurderingTester {
        return VilkårsvurderingTester(
            startMåned = this.startMåned,
            vilkårsresultater = this.vilkårsresultater + parseVilkår(v, vilkår)
        )
    }

    fun harUtfall(u: String) {
        val forventedeRegelverkMåneder = parseUtfall(u)
        val faktiskeRegelverkMåneder = EøsUtil.utledMånederMedRegelverk(this.vilkårsresultater)

        assertThat(faktiskeRegelverkMåneder).isEqualTo(forventedeRegelverkMåneder)
    }

    private fun parseUtfall(u: String): Collection<RegelverkMåned> {
        return u.mapIndexed { index, tegn ->
            if (erPeriode(tegn))
                lagRegelverkMåned(startMåned.plusMonths(index.toLong()), tegn)
            else if (tegn == '>') {
                lagRegelverkMåned(MAX_MÅNED, u[index - 1])
            } else if (tegn == '<') {
                lagRegelverkMåned(MIN_MÅNED, u[index + 1])
            } else null
        }.filterNotNull()
    }

    private fun parseVilkår(periodeString: String, vilkår: Vilkår): Collection<VilkårResultatMåned> {
        return periodeString
            .mapIndexed { index, tegn ->
                if (erPeriode(tegn)) {
                    lagVilkårResultatMåned(vilkår, tegn, startMåned.plusMonths(index.toLong()))
                } else if (tegn == '>') {
                    lagVilkårResultatMåned(vilkår, periodeString[index - 1], MAX_MÅNED)
                } else if (tegn == '<') {
                    lagVilkårResultatMåned(vilkår, periodeString[index + 1], MIN_MÅNED)
                } else null
            }
            .filterNotNull()
    }

    private fun lagRegelverkMåned(
        måned: YearMonth,
        tegn: Char
    ): RegelverkMåned? =
        if (erPeriode(tegn))
            RegelverkMåned(måned = måned, vurderesEtter = finnRegelverk(tegn))
        else
            null

    private fun lagVilkårResultatMåned(
        vilkår: Vilkår,
        tegn: Char,
        måned: YearMonth
    ): VilkårResultatMåned? = if (erPeriode(tegn))
        VilkårResultatMåned(
            vilkårType = vilkår,
            resultat = finnResultat(tegn),
            måned = måned,
            vurderesEtter = finnRegelverk(tegn)
        )
    else
        null

    private fun erPeriode(c: Char) =
        when (c) {
            '?', 'E', 'N', '-' -> true
            else -> false
        }

    private fun finnRegelverk(gjeldendeTegn: Char?): VilkårRegelverk? =
        when (gjeldendeTegn) {
            'E' -> VilkårRegelverk.EØS_FORORDNINGEN
            'N' -> VilkårRegelverk.NASJONALE_REGLER
            else -> null
        }

    private fun finnResultat(gjeldendeTegn: Char?) =
        when (gjeldendeTegn) {
            ' ' -> Resultat.IKKE_VURDERT
            '?' -> null
            else -> Resultat.OPPFYLT
        }
}
