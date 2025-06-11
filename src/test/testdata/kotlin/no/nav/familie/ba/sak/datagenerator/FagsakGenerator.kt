package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.institusjon.Institusjon
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.skjermetbarnsøker.SkjermetBarnSøker

fun defaultFagsak(aktør: Aktør = tilAktør(randomFnr())) =
    Fagsak(
        1,
        aktør = aktør,
    )

/**
 * Bruk for enhetstest. Bruk lagFagsakUtenId for integrasjonstest
 */
fun lagFagsak(
    id: Long = 1,
    aktør: Aktør = tilAktør(randomFnr()),
    institusjon: Institusjon? = null,
    status: FagsakStatus = FagsakStatus.OPPRETTET,
    type: FagsakType = FagsakType.NORMAL,
    arkivert: Boolean = false,
    skjermetBarnSøker: SkjermetBarnSøker? = null,
) = lagFagsakUtenId(
    aktør = aktør,
    institusjon = institusjon,
    status = status,
    type = type,
    arkivert = arkivert,
    skjermetBarnSøker = skjermetBarnSøker,
).copy(id)

/**
 * Bruk for integrasjonstest. Bruk lagFagsak for enhetstest
 */
fun lagFagsakUtenId(
    aktør: Aktør = tilAktør(randomFnr()),
    institusjon: Institusjon? = null,
    status: FagsakStatus = FagsakStatus.OPPRETTET,
    type: FagsakType = FagsakType.NORMAL,
    arkivert: Boolean = false,
    skjermetBarnSøker: SkjermetBarnSøker? = null,
) = Fagsak(
    id = 0,
    aktør = aktør,
    institusjon = institusjon,
    status = status,
    type = type,
    arkivert = arkivert,
    skjermetBarnSøker = skjermetBarnSøker,
)
