package no.nav.familie.ba.sak.common

import org.springframework.http.HttpStatus

class FunksjonellFeil(message: String,
                      val funksjonellFeilmelding: String,
                      val httpStatus: HttpStatus = HttpStatus.OK) : RuntimeException(message)

class TekniskFeil(message: String,
                  val httpStatus: HttpStatus = HttpStatus.OK,
                  val exception: Throwable) : RuntimeException(message, exception)