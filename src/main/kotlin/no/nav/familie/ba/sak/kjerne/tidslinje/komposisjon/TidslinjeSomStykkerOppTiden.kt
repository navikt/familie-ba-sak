package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt

fun <I, T : Tidsenhet> Tidslinje<I, T>.innholdForTidspunkt(tidspunkt: Tidspunkt<T>): I? =
    perioder().innholdForTidspunkt(tidspunkt)

fun <I, T : Tidsenhet> Collection<Periode<I, T>>.innholdForTidspunkt(tidspunkt: Tidspunkt<T>): I? =
    this.firstOrNull { it.fraOgMed <= tidspunkt && it.tilOgMed >= tidspunkt }?.innhold

data class TidspunktMedInnhold<I, T : Tidsenhet>(
    val tidspunkt: Tidspunkt<T>,
    val innhold: I?
)
