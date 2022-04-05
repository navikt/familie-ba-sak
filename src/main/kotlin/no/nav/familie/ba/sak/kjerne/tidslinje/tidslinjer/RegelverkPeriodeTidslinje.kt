package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

private val nødvendigeVilkår = Vilkår.hentVilkårFor(PersonType.BARN)

private val eøsVilkår = listOf(
    Vilkår.BOR_MED_SØKER,
    Vilkår.LOVLIG_OPPHOLD,
    Vilkår.BOSATT_I_RIKET
)

class RegelverkPeriodeTidslinje(
    val regelverkMidlertidigTidslinje: Tidslinje<Regelverk, Måned>
) : Tidslinje<Regelverk, Måned>() {
    override fun fraOgMed() = regelverkMidlertidigTidslinje.fraOgMed()

    override fun tilOgMed(): Tidspunkt<Måned> {
        return when {
            regelverkMidlertidigTidslinje.tilOgMed().tilYearMonthEllerNull()
                ?.isAfter(inneværendeMåned()) == true -> regelverkMidlertidigTidslinje.tilOgMed().somUendeligLengeTil()
            else -> regelverkMidlertidigTidslinje.tilOgMed()
        }
    }

    override fun lagPerioder(): Collection<Periode<Regelverk, Måned>> {
        return regelverkMidlertidigTidslinje.perioder()
            .filter { it.fraOgMed.tilYearMonth() <= inneværendeMåned() }
            .map {
                val tilOgMedYearMonth = it.tilOgMed.tilYearMonthEllerNull()
                Periode(
                    fraOgMed = it.fraOgMed,
                    tilOgMed = tilOgMedYearMonth?.let { yearMonth ->
                        if (yearMonth.isAfter(inneværendeMåned())) Tidspunkt.uendeligLengeTil(yearMonth)
                        else it.tilOgMed
                    } ?: it.tilOgMed,
                    innhold = it.innhold
                )
            }
    }
}

class RegelverkPeriodeKombinator {
    fun kombiner(alleVilkårResultater: Iterable<VilkårRegelverkResultat>): Regelverk? {
        val oppfyllerNødvendigVilkår = alleVilkårResultater
            .filter { it.resultat == Resultat.OPPFYLT }
            .map { it.vilkår }
            .containsAll(nødvendigeVilkår)

        if (!oppfyllerNødvendigVilkår)
            return null

        val alleRelevanteVilkårErEøsVilkår = alleVilkårResultater
            .filter {
                it.regelverk == Regelverk.EØS_FORORDNINGEN
            }.map { it.vilkår }
            .containsAll(eøsVilkår)

        return if (alleRelevanteVilkårErEøsVilkår) Regelverk.EØS_FORORDNINGEN else Regelverk.NASJONALE_REGLER
    }
}

class RegelverkOgOppfyltePerioderKombinator {
    fun kombiner(venstre: Resultat?, høyre: Regelverk?): Regelverk? {
        return when {
            høyre == null || venstre == null -> null
            venstre != Resultat.OPPFYLT -> null
            venstre == Resultat.OPPFYLT && høyre == Regelverk.EØS_FORORDNINGEN -> Regelverk.EØS_FORORDNINGEN
            else -> Regelverk.NASJONALE_REGLER
        }
    }
}
