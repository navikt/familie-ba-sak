package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

interface ToveisKombinator<V, H, R> {
    fun kombiner(venstre: V?, høyre: H?): R?
}

interface ListeKombinator<T, R> {
    fun kombiner(liste: Iterable<T>): R
}
