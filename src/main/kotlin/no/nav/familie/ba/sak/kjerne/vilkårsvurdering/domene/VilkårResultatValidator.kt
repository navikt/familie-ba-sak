package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class VilkårResultatValidator : ConstraintValidator<VilkårResultatConstraint, VilkårResultat> {

    override fun isValid(vilkårResultat: VilkårResultat, constraintValidatorContext: ConstraintValidatorContext?): Boolean {

        if (vilkårResultat.vilkårType != Vilkår.BOSATT_I_RIKET && vilkårResultat.erMedlemskapVurdert) {
            return false
        }

        if (vilkårResultat.vilkårType != Vilkår.BOR_MED_SØKER && vilkårResultat.erDeltBosted) {
            return false
        }

        return true
    }
}
