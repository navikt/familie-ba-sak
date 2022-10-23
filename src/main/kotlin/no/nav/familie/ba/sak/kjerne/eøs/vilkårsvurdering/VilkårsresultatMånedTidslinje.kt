package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilForrigeMåned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilFørsteDagIMåneden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilNesteMåned
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
 * Extension-funksjon som konverterer en dag-basert tidslinje til en måned-basert tidslinje med VilkårRegelverkResultat
 * Funksjonen itererer fra måneden FØR fra-og-med-måned til måneden ETTER til-og-med-måneden for å ta hensyn til uendelighet
 * Reglene er at vilkårret for siste dag i forrige måned og første dag i inneværende måned må være oppfylt
 * Da brukes regelverket for inneværende måned. Dvs slik:
 * 2020-04-30   | 2020-05-01    -> Resultat
 * Oppfylt EØS  | Oppfylt Nasj. -> 2020-05 Oppfylt Nasj
 * Oppfylt Nasj | Opppfylt EØS  -> 2020-05 Oppfylt EØS
 * Oppfylt Nasj | Opppfylt Nasj -> 2020-05 Oppfylt Nasj
 * Oppfylt EØS  | Opppfylt EØS  -> 2020-05 Oppfylt EØS
 * Oppfylt EØS  | Ikke oppfylt  -> <Tomt>
 * Oppfylt Nasj | Ikke oppfylt  -> <Tomt>
 * Ikke oppfylt | Oppfylt EØS   -> <Tomt>
 * Ikke oppfylt | Oppfylt Nasj  -> <Tomt>
 */
fun <I> Tidslinje<I, Dag>.tilMånedEtterVilkårsregler(erOppfylt: (I?) -> Boolean) = tidslinje {
    (fraOgMed().tilForrigeMåned()..tilOgMed().tilNesteMåned()).map { måned ->
        val innholdSisteDagForrigeMåned = innholdForTidspunkt(måned.forrige().tilSisteDagIMåneden())
        val innholdFørsteDagDenneMåned = innholdForTidspunkt(måned.tilFørsteDagIMåneden())

        if (erOppfylt(innholdSisteDagForrigeMåned) && erOppfylt(innholdFørsteDagDenneMåned)) {
            måned.tilPeriodeMedInnhold(innholdFørsteDagDenneMåned)
        } else {
            måned.tilPeriodeUtenInnhold()
        }
    }
}
