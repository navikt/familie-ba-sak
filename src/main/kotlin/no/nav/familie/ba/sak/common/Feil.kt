package no.nav.familie.ba.sak.common

import org.springframework.http.HttpStatus

class Feil(message: String,
           val frontendFeilmelding: String? = null,
           val httpStatus: HttpStatus = HttpStatus.OK,
           val throwable: Throwable? = null) : RuntimeException(message)