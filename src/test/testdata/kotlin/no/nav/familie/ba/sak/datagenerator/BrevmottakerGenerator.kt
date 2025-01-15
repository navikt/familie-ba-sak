package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerDb
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerType

fun lagBrevmottakerDb(
    behandlingId: Long,
    type: MottakerType = MottakerType.FULLMEKTIG,
    navn: String = "Test Testesen",
    adresselinje1: String = "En adresse her",
    adresselinje2: String? = null,
    postnummer: String = "0661",
    poststed: String = "Oslo",
    landkode: String = "NO",
) = BrevmottakerDb(
    behandlingId = behandlingId,
    type = type,
    navn = navn,
    adresselinje1 = adresselinje1,
    adresselinje2 = adresselinje2,
    postnummer = postnummer,
    poststed = poststed,
    landkode = landkode,
)
