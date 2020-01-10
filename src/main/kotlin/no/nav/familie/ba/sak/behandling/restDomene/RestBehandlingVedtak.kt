package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtakBarn
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtakStatus
import java.time.LocalDate

data class RestBehandlingVedtak(
        val aktiv: Boolean,
        val ansvarligSaksbehandler: String,
        val vedtaksdato: LocalDate,
        val barnasBeregning: List<RestBehandlingVedtakBarn?>,
        val status: BehandlingVedtakStatus,
        val stønadFom: LocalDate,
        val stønadTom: LocalDate
)

fun BehandlingVedtak.toRestBehandlingVedtak(barnBeregning: List<BehandlingVedtakBarn?>) = RestBehandlingVedtak(
        aktiv = this.aktiv,
        ansvarligSaksbehandler = this.ansvarligSaksbehandler,
        barnasBeregning = barnBeregning.map { it?.toRestBehandlingVedtakBarn() },
        vedtaksdato = this.vedtaksdato,
        status = this.status,
        stønadFom = this.stønadFom,
        stønadTom = this.stønadTom
)
