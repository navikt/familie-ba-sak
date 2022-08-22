package no.nav.familie.ba.sak.integrasjoner.ecb

interface ECBException {
    val message: String
    val cause: Throwable?
}
