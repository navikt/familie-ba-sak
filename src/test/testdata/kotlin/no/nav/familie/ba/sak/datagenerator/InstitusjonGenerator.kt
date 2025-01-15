package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.kjerne.institusjon.Institusjon

fun lagInstitusjon(
    id: Long = 0L,
    orgNummer: String = "123456789",
    tssEksternId: String? = "tssEksternId",
) = Institusjon(
    id = id,
    orgNummer = orgNummer,
    tssEksternId = tssEksternId,
)
