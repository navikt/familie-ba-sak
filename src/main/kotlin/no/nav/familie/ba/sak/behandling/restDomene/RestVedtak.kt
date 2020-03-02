package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakPerson
import no.nav.familie.ba.sak.behandling.vedtak.VedtakResultat
import java.time.LocalDate

data class RestVedtak(
        val aktiv: Boolean,
        val ansvarligSaksbehandler: String,
        val vedtaksdato: LocalDate,
        val barnasBeregning: List<RestVedtakBarn>,
        val resultat: VedtakResultat,
        val begrunnelse: String
)

fun Vedtak.toRestVedtak(personBeregning: List<VedtakPerson>) = RestVedtak(
        aktiv = this.aktiv,
        ansvarligSaksbehandler = this.ansvarligSaksbehandler,
        barnasBeregning = personBeregning.map { it.toRestVedtakBarn() },
        vedtaksdato = this.vedtaksdato,
        resultat = this.resultat,
        begrunnelse = this.begrunnelse
)
