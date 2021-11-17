package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import javax.validation.Constraint
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [VilkårResultatValidator::class])
@MustBeDocumented
annotation class VilkårResultatConstraint(
    val message: String = "Kombinasjonen av valgt utdypetvurdering og vilkårstyper er ikke lovlig",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = []
)
