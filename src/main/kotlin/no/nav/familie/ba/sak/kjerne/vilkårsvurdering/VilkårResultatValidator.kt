package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class VilkårResultatValidator : ConstraintValidator<VilkårResultatConstraint, VilkårResultat> {

    override fun isValid(vilkårResultat: VilkårResultat, constraintValidatorContext: ConstraintValidatorContext): Boolean =
            vilkårResultat.validerOpsjoner().isEmpty()
}
