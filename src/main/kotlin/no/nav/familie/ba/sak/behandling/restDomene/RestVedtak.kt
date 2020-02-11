package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakBarn
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakResultat
import java.time.LocalDate

data class RestVedtak(
        val aktiv: Boolean,
        val ansvarligSaksbehandler: String,
        val vedtaksdato: LocalDate,
        val barnasBeregning: List<RestVedtakBarn?>,
        val resultat: VedtakResultat
)

fun Vedtak.toRestVedtak(barnBeregning: List<VedtakBarn?>) = RestVedtak(
        aktiv = this.aktiv,
        ansvarligSaksbehandler = this.ansvarligSaksbehandler,
        barnasBeregning = barnBeregning.map { it?.toRestVedtakBarn() },
        vedtaksdato = this.vedtaksdato,
        resultat = this.resultat
)
