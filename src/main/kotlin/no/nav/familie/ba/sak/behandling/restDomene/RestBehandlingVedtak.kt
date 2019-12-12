package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtak
import java.time.LocalDate

data class RestBehandlingVedtak(
    val aktiv: Boolean,
    val ansvarligSaksbehandler: String,
    val vedtaksdato: LocalDate,
    val stønadFom: LocalDate,
    val stønadTom: LocalDate
)

fun BehandlingVedtak.toRestBehandlingVedtak() = RestBehandlingVedtak(
    aktiv = this.aktiv,
    ansvarligSaksbehandler = this.ansvarligSaksbehandler,
    vedtaksdato = this.vedtaksdato,
    stønadFom = this.stønadFom,
    stønadTom = this.stønadTom
)
