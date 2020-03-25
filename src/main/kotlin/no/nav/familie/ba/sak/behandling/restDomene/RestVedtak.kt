package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import java.time.LocalDate

data class RestVedtak(
        val aktiv: Boolean,
        val ansvarligSaksbehandler: String,
        val vedtaksdato: LocalDate,
        val personBeregninger: List<RestVedtakBarn>,
        val id: Long
)

fun Vedtak.toRestVedtak(restVedtakBarn: List<RestVedtakBarn>) = RestVedtak(
        aktiv = this.aktiv,
        ansvarligSaksbehandler = this.ansvarligSaksbehandler,
        personBeregninger = restVedtakBarn,
        vedtaksdato = this.vedtaksdato,
        id= this.id
)
