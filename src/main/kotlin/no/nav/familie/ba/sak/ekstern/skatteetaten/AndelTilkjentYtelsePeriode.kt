package no.nav.familie.ba.sak.ekstern.skatteetaten

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

interface AndelTilkjentYtelsePeriode {

    fun getId(): Long

    fun getIdent(): String

    fun getFom(): LocalDateTime

    fun getTom(): LocalDateTime

    fun getProsent(): String

    fun getOpprettetDato(): LocalDateTime
}