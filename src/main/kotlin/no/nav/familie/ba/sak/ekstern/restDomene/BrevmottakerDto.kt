package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerDb
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerType

data class BrevmottakerDto(
    val id: Long?,
    val type: MottakerType,
    val navn: String,
    val adresselinje1: String,
    val adresselinje2: String?,
    val postnummer: String,
    val poststed: String,
    val landkode: String,
) {
    fun tilDomene(behandlingId: Long) =
        BrevmottakerDb(
            behandlingId = behandlingId,
            type = type,
            navn = navn,
            adresselinje1 = adresselinje1,
            adresselinje2 = adresselinje2,
            postnummer = postnummer.trim(),
            poststed = poststed.trim(),
            landkode = landkode,
        )

    companion object {
        fun fra(brevmottaker: BrevmottakerDb) =
            BrevmottakerDto(
                id = brevmottaker.id,
                type = brevmottaker.type,
                navn = brevmottaker.navn,
                adresselinje1 = brevmottaker.adresselinje1,
                adresselinje2 = brevmottaker.adresselinje2,
                postnummer = brevmottaker.postnummer,
                poststed = brevmottaker.poststed,
                landkode = brevmottaker.landkode,
            )
    }
}
