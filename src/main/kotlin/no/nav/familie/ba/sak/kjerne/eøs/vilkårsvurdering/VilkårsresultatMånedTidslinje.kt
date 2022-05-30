package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.forskyv
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.tilMånedFraSisteDagIMåneden

/**
 * Extension-funksjon som konverterer en dag-basert tidslinje med VilkårRegelverkResultat til en måned-basert tidslinje
 * Regelen er at resultatet blir først skal bli gjeldende fra og med påfølgende måned.
 * I praksis betyr det at det holder å kikke på verdien for siste dag i måneden og bruke den som månedsverdi i påfølgende måned
 *
 * Funksjonen oppnår dette i følgende steg
 * 1. Konverter til månedstidslinje ved å bruke verdien siste dag i månenden.
 *    F.eks blir verdien for 30/4-2020 til verdien for april 2020, 31/5 til mai-2020-verdien, osv
 * 2. Samme verdi i påfølgende måneder blir slått sammen til én periode
 * 3. Perioder som ikke har verdi (dvs er null) blir fjernet
 * 4. Hele tidslinjen forskyves én måned senere, for å oppfylle regelen om påfølgende måned,
 *    dvs april blir til mai, mai blir til juni osv
 */
fun Tidslinje<VilkårRegelverkResultat, Dag>.tilMånedsbasertTidslinjeForVilkårRegelverkResultat() = this
    .tilMånedFraSisteDagIMåneden() // Månedsverdien er verdien fra siste dag i måneden
    .slåSammenLike() // Slå sammen perioder som nå er like
    .filtrerIkkeNull() // Ta bort alle perioder som har null-verdi
    .forskyv(1) // Flytt alt én måned senere
