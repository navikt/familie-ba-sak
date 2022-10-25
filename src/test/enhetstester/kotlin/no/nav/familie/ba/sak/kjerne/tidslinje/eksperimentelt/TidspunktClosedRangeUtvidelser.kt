package no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt

import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo

fun <T : Tidsenhet> Tidspunkt<T>.ogSenere() = this.somUendeligLengeTil().rangeTo(this.somUendeligLengeTil())
fun <T : Tidsenhet> Tidspunkt<T>.ogTidligere() = this.somUendeligLengeSiden().rangeTo(this.somUendeligLengeSiden())
