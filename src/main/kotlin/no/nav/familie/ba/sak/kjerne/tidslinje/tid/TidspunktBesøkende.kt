package no.nav.familie.ba.sak.kjerne.tidslinje.tid

fun <T : Tidsenhet> Tidspunkt<T>.tilNesteMåned() = tilMånedMedOperasjon { it.neste() }

fun <T : Tidsenhet> Tidspunkt<T>.tilForrigeMåned() = tilMånedMedOperasjon { it.forrige() }

private fun <T : Tidsenhet> Tidspunkt<T>.tilMånedMedOperasjon(operasjon: (måned: Tidspunkt<Måned>) -> Tidspunkt<Måned>): Tidspunkt<Måned> {
    val besøkende = object : TidspunktBesøkende {
        lateinit var måned: Tidspunkt<Måned>

        override fun besøkDag(tidspunkt: Tidspunkt<Dag>) {
            besøkMåned(tidspunkt.tilInneværendeMåned())
        }

        override fun besøkMåned(tidspunkt: Tidspunkt<Måned>) {
            måned = operasjon(tidspunkt)
        }
    }

    return this.taImotBesøk(besøkende).måned
}
