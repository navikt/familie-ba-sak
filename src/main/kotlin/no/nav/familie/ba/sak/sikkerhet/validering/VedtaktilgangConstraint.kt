package no.nav.familie.ba.sak.sikkerhet.validering

import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@Suppress("unused")
@MustBeDocumented
@Constraint(validatedBy = [Vedtaktilgang::class])
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class VedtaktilgangConstraint(
    val message: String = "Ikke tilgang til vedtak",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
