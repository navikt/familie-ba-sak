package no.nav.familie.ba.sak.kjerne.eøs.tidslinjer

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.komprimer
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.forskyv
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.tilMåned

fun Tidslinje<VilkårRegelverkResultat, Dag>.tilVilkårsresultaterMånedTidslinje() = this
    .tilMåned { it.last() } // Månedsverdien er verdien fra siste dag i måneden
    .komprimer() // Slå sammen perioder som nå er like
    .filtrerIkkeNull() // Ta bort alle perioder som har null-verdi
    .forskyv(1) // Flytt alt én måned senere
