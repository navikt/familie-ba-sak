package no.nav.familie.ba.sak.common

fun <T> Collection<T>.zeroSingleOrThrow(exception: Collection<T>.() -> Exception): T? =
    if (size in 0..1) {
        singleOrNull()
    } else {
        throw exception()
    }
