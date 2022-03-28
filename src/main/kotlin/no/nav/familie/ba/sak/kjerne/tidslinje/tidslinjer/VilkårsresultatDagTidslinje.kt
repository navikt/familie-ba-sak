package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilTidspunktEllerDefault
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.time.LocalDate

data class VilkårRegelverkResultat(
    val vilkår: Vilkår,
    val regelverk: Regelverk?,
    val resultat: Resultat?,
    val personType: PersonType = PersonType.BARN,
)

class VilkårsresultatDagTidslinje(
    private val vilkårsresultater: List<VilkårResultat>,
    private val praktiskTidligsteDato: LocalDate,
    private val praktiskSenesteDato: LocalDate
) : Tidslinje<VilkårRegelverkResultat, Dag>() {

    override fun fraOgMed() = vilkårsresultater.minOf {
        it.periodeFom.tilTidspunktEllerDefault { praktiskTidligsteDato }
    }

    override fun tilOgMed() = vilkårsresultater.maxOf {
        it.periodeTom.tilTidspunktEllerDefault { praktiskSenesteDato }
    }

    override fun lagPerioder(): Collection<Periode<VilkårRegelverkResultat, Dag>> {
        return vilkårsresultater.map { it.tilPeriode(praktiskTidligsteDato, praktiskSenesteDato) }
    }
}

fun VilkårResultat.tilPeriode(
    praktiskTidligsteDato: LocalDate,
    praktiskSenesteDato: LocalDate
): Periode<VilkårRegelverkResultat, Dag> {
    val fom = periodeFom.tilTidspunktEllerDefault { praktiskTidligsteDato }
    val tom = periodeTom.tilTidspunktEllerDefault { praktiskSenesteDato }
    return Periode(
        fom, tom,
        VilkårRegelverkResultat(
            vilkår = vilkårType,
            regelverk = vurderesEtter,
            resultat = resultat,
            personType = if (personResultat?.erSøkersResultater() == true) PersonType.SØKER else PersonType.BARN
        )
    )
}
