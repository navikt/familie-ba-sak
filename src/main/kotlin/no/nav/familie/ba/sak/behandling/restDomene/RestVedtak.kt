package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakPerson
import java.time.LocalDate

data class RestVedtak(
        val aktiv: Boolean,
        val ansvarligSaksbehandler: String,
        val vedtaksdato: LocalDate,
        val barnasBeregning: List<RestVedtakBarn>,
        val id: Long
)

fun Vedtak.toRestVedtak(personBeregning: List<VedtakPerson>) = RestVedtak(
        aktiv = this.aktiv,
        ansvarligSaksbehandler = this.ansvarligSaksbehandler,
        barnasBeregning = personBeregning.map { it.toRestVedtakBarn() },
        vedtaksdato = this.vedtaksdato,
        id= this.id
)
