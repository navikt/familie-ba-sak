package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import javax.validation.Constraint
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = [VilkårResultatValidator::class])
@Documented
annotation class VilkårResultatConstraint(val message: String = "Kombinasjonen av valgt utdypetvurdering og vilkårstyper er ikke lovlig",
                                          val groups: Array<KClass<*>> = [],
                                          val payload: Array<KClass<out Any>> = [])
