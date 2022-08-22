package no.nav.familie.ba.sak.integrasjoner.ecb

class ECBClientException(override val message: String, override val cause: Throwable? = null) : RuntimeException(message, cause), ECBException
