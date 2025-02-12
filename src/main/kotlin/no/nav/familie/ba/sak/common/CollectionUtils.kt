package no.nav.familie.ba.sak.common

inline fun <T> Collection<T>.zeroSingleOrThrow(exception: Collection<T>.() -> Exception): T? =
    if (size in 0..1) {
        singleOrNull()
    } else {
        throw exception()
    }

fun <T> Collection<T>.containsExactly(vararg elements: T): Boolean {
    if (this.size != elements.size) {
        return false
    }
    this.forEachIndexed { index, element ->
        if (element != elements[index]) {
            return false
        }
    }
    return true
}
