package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.Verdi
import no.nav.familie.tidslinje.utvidelser.map

fun <V, R> Tidslinje<V>.mapIkkeNull(mapper: (V) -> R?): Tidslinje<R> =
    this.map { periodeVerdi ->
        when (periodeVerdi) {
            is Verdi -> mapper(periodeVerdi.verdi)?.let { Verdi(it) } ?: Null()
            is Null -> Null()
            is Udefinert -> Udefinert()
        }
    }
