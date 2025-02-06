package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.institusjon.Institusjon
import no.nav.familie.ba.sak.kjerne.personident.Aktør

fun defaultFagsak(aktør: Aktør = tilAktør(randomFnr())) =
    Fagsak(
        1,
        aktør = aktør,
    )

fun lagFagsak(
    id: Long = 1,
    aktør: Aktør = tilAktør(randomFnr()),
    institusjon: Institusjon? = null,
    status: FagsakStatus = FagsakStatus.OPPRETTET,
    type: FagsakType = FagsakType.NORMAL,
    arkivert: Boolean = false,
) = lagFagsakUtenId(
    aktør = aktør,
    institusjon = institusjon,
    status = status,
    type = type,
    arkivert = arkivert,
).copy(id)

/**
 * Bruk for integrasjonstest
 */
fun lagFagsakUtenId(
    aktør: Aktør = tilAktør(randomFnr()),
    institusjon: Institusjon? = null,
    status: FagsakStatus = FagsakStatus.OPPRETTET,
    type: FagsakType = FagsakType.NORMAL,
    arkivert: Boolean = false,
) = Fagsak(
    id = 0,
    aktør = aktør,
    institusjon = institusjon,
    status = status,
    type = type,
    arkivert = arkivert,
)
