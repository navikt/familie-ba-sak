package no.nav.familie.ba.sak.behandling.vedtak

import java.util.*

object VedtakUtils {

    fun hentHjemlerBruktIVedtak(vedtak: Vedtak): SortedSet<Int> {
        val hjemler = mutableSetOf<Int>()
        vedtak.vedtakBegrunnelser.forEach {
            hjemler.addAll(it.begrunnelse?.hentHjemler()?.toSet() ?: emptySet())
        }
        return hjemler.toSortedSet()
    }
}