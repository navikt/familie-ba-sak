package no.nav.familie.ba.sak.common.validering

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

private fun String.erKunTall() = this.all { char -> char.isDigit() }

@Constraint(validatedBy = [OrganisasjonsnummerValidator::class])
@Target(AnnotationTarget.FIELD)
annotation class Organisasjonsnummer(
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<*>> = [],
    val message: String = "Not a valid organization number",
)

class OrganisasjonsnummerValidator : ConstraintValidator<Organisasjonsnummer, String> {
    override fun isValid(
        organisasjonsnummer: String,
        context: ConstraintValidatorContext?,
    ): Boolean {
        organisasjonsnummer.trim().apply {
            val valid =
                harOrganisasjonsnummerRiktigLengde(this) &&
                    organisasjonsnummer.erKunTall() &&
                    erGyldigOrganisasjonsnummer(this)

            if (!valid) {
                context?.disableDefaultConstraintViolation()
                context
                    ?.buildConstraintViolationWithTemplate(
                        "Not a valid organization number $this",
                    )?.addConstraintViolation()
            }

            return valid
        }
    }

    private fun erGyldigOrganisasjonsnummer(organisasjonsnummer: String) =
        getKontrollSifferOrganisasjonsnummer(organisasjonsnummer) ==
            organisasjonsnummer
                .trim()
                .last()
                .toString()
                .toInt()

    private fun harOrganisasjonsnummerRiktigLengde(organisasjonsnummer: String) = (organisasjonsnummer.trim().length == 9)

    private fun getKontrollSifferOrganisasjonsnummer(number: String): Int {
        val sum =
            (0 until number.lastIndex).sumOf {
                number[it].digitToInt() * getVektTallOrganisasjonsnummer(it)
            }

        val rest = sum % 11

        return getKontrollSifferOrganisasjonsnummerFraRest(rest)
    }

    private fun getVektTallOrganisasjonsnummer(i: Int): Int {
        val vekttall = intArrayOf(3, 2, 7, 6, 5, 4, 3, 2)
        return vekttall[i]
    }

    private fun getKontrollSifferOrganisasjonsnummerFraRest(rest: Int): Int = if (rest == 0) 0 else 11 - rest

    companion object {
        private val validator = OrganisasjonsnummerValidator()

        fun isValid(organisasjonsnummer: String) = validator.isValid(organisasjonsnummer, null)
    }
}
