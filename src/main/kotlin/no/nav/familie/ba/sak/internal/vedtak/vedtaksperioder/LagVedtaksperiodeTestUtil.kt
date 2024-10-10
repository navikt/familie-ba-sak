package no.nav.familie.ba.sak.internal.vedtak.vedtaksperioder

import no.nav.familie.ba.sak.common.tilddMMyyyy
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser

fun hentTekstForVedtaksperioder(
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
    behandlingId: Long?,
) =
    """
        
    Så forvent følgende vedtaksperioder for behandling $behandlingId
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar     |""" +
        hentVedtaksperiodeRader(vedtaksperioder)

private fun hentVedtaksperiodeRader(vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>) =
    vedtaksperioder.joinToString("") {
        """
      | ${it.fom?.tilddMMyyyy() ?: ""} |${it.tom?.tilddMMyyyy() ?: ""} |${it.type} |               |"""
    }
