package no.nav.familie.ba.sak.kjerne.eøs.felles.util

fun <T> Collection<T>.erEkteDelmengdeAv(mengde: Collection<T>) = this.size < mengde.size && mengde.containsAll(this)

fun <T> Collection<T>.replaceLast(replacer: (T) -> T) = this.take(this.size - 1) + replacer(this.last())

fun <T> Collection<T>.replaceFirst(replacer: (T) -> T) =
    if (this.isEmpty()) {
        emptyList()
    } else {
        listOf(replacer(this.first())).plus(this.drop(1))
    }
