package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilFørsteDagIMåneden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilInneværendeMåned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilSisteDagIMåneden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tilPeriodeMedInnhold
import no.nav.familie.ba.sak.kjerne.tidslinje.tilPeriodeUtenInnhold

fun Tidslinje<VilkårRegelverkResultat, Dag>.tilMånedsbasertTidslinjeForVilkårRegelverkResultat() = this
    .tilMånedEtterVilkårsregler { it.erOppfylt() }
    .slåSammenLike()
    .filtrerIkkeNull()

/**
 * Extension-funksjon som konverterer en dag-basert tidslinje til en måned-basert tidslinje etter vilkårsreglene
 * Vilkårsreglene er at innholdet i siste dag forrige måned brukes som innhold for denne måneden
 * hvis innholdet siste dag forrige måned <erOppfylt> og innholdet første dag denne måneden også <erOppfylt>
 */
fun <I> Tidslinje<I, Dag>.tilMånedEtterVilkårsregler(erOppfylt: (I?) -> Boolean) = tidslinje {
    (fraOgMed().tilInneværendeMåned()..tilOgMed().tilInneværendeMåned()).map { måned ->
        val innholdSisteDagForrigeMåned = innholdForTidspunkt(måned.forrige().tilSisteDagIMåneden())
        val innholdFørsteDagDenneMåned = innholdForTidspunkt(måned.tilFørsteDagIMåneden())

        if (erOppfylt(innholdSisteDagForrigeMåned) && erOppfylt(innholdFørsteDagDenneMåned)) {
            måned.tilPeriodeMedInnhold(innholdSisteDagForrigeMåned)
        } else {
            måned.tilPeriodeUtenInnhold()
        }
    }
}
