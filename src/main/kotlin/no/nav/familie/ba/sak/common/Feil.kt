package no.nav.familie.ba.sak.common

import org.springframework.http.HttpStatus

open class Feil(message: String,
                open val frontendFeilmelding: String? = null,
                open val httpStatus: HttpStatus = HttpStatus.OK,
                open val throwable: Throwable? = null) : RuntimeException(message)

open class FunksjonellFeil(open val melding: String,
                           open val frontendFeilmelding: String? = null,
                           open val httpStatus: HttpStatus = HttpStatus.OK,
                           open val throwable: Throwable? = null) : RuntimeException(melding)

class VilkårsvurderingFeil(melding: String,
                           override val frontendFeilmelding: String? = null,
                           override val httpStatus: HttpStatus = HttpStatus.OK,
                           override val throwable: Throwable? = null) : FunksjonellFeil(melding,
                                                                                        frontendFeilmelding,
                                                                                        httpStatus,
                                                                                        throwable)

class UtbetalingsikkerhetFeil(melding: String,
                              override val frontendFeilmelding: String? = null,
                              override val httpStatus: HttpStatus = HttpStatus.OK,
                              override val throwable: Throwable? = null) : FunksjonellFeil(melding,
                                                                                           frontendFeilmelding,
                                                                                           httpStatus,
                                                                                           throwable)
