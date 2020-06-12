package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import java.time.LocalDate

data class RestVedtak(
        val aktiv: Boolean,
        val vedtaksdato: LocalDate,
        val personBeregninger: List<RestVedtakPerson>,
        val id: Long
)

fun Vedtak.toRestVedtak(restVedtakPerson: List<RestVedtakPerson>) = RestVedtak(
        aktiv = this.aktiv,
        personBeregninger = restVedtakPerson,
        vedtaksdato = this.vedtaksdato,
        id= this.id
)
