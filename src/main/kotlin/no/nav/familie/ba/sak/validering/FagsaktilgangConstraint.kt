package no.nav.familie.ba.sak.validering

import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@Suppress("unused")
@MustBeDocumented
@Constraint(validatedBy = [Fagsaktilgang::class])
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class FagsaktilgangConstraint(val message: String = "Ikke tilgang til sak",
                                         val groups: Array<KClass<*>> = [],
                                         val payload: Array<KClass<out Payload>> = [])
