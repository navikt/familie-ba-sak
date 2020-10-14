package no.nav.familie.ba.sak.common

import org.springframework.http.HttpStatus

open class Feil(message: String,
                open val frontendFeilmelding: String? = null,
                open val httpStatus: HttpStatus = HttpStatus.OK,
                open val throwable: Throwable? = null) : RuntimeException(message)

class VilkårsvurderingFeil(message: String,
                           override val frontendFeilmelding: String? = null,
                           override val httpStatus: HttpStatus = HttpStatus.OK,
                           override val throwable: Throwable? = null) : Feil(message, frontendFeilmelding, httpStatus, throwable)

class UtbetalingsikkerhetFeil(message: String,
                              override val frontendFeilmelding: String? = null,
                              override val httpStatus: HttpStatus = HttpStatus.OK,
                              override val throwable: Throwable? = null) : Feil(message,
                                                                                frontendFeilmelding,
                                                                                httpStatus,
                                                                                throwable)
