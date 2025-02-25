package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje

fun Collection<VilkårResultat>.tilTidslinje() =
    this
        .map {
            Periode(
                verdi = it,
                fom = it.periodeFom,
                tom = it.periodeTom,
            )
        }.tilTidslinje()
