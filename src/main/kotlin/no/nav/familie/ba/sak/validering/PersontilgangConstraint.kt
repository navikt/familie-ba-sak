package no.nav.familie.ba.sak.validering

import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@Suppress("unused")
@MustBeDocumented
@Constraint(validatedBy = [Persontilgang::class])
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PersontilgangConstraint(val message: String = "Ikke tilgang til personer",
                                         val groups: Array<KClass<*>> = [],
                                         val payload: Array<KClass<out Payload>> = [])
