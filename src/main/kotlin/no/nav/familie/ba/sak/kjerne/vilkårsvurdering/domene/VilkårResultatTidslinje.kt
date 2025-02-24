package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.Periode as FamilieFellesPeriode

fun Collection<VilkårResultat>.tilFamilieFellesTidslinje() =
    this
        .map {
            FamilieFellesPeriode(
                verdi = it,
                fom = it.periodeFom,
                tom = it.periodeTom,
            )
        }.tilTidslinje()
