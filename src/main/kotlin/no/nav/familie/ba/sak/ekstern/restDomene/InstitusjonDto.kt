package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.validering.OrganisasjonsnummerValidator

data class InstitusjonDto(
    val orgNummer: String?,
    val tssEksternId: String?,
    val navn: String? = null,
) {
    fun valider() {
        if (orgNummer.isNullOrBlank()) {
            throw FunksjonellFeil("Mangler organisasjonsnummer.")
        }
        if (!OrganisasjonsnummerValidator.isValid(orgNummer)) {
            throw FunksjonellFeil("Organisasjonsnummeret er ugyldig.")
        }
        if (tssEksternId.isNullOrBlank()) {
            throw FunksjonellFeil("Mangler tssEksternId.")
        }
    }
}
