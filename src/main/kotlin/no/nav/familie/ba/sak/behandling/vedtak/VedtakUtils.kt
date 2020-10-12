package no.nav.familie.ba.sak.behandling.vedtak

import java.util.*

object VedtakUtils {

    fun hentHjemlerBruktIVedtak(vedtak: Vedtak): SortedSet<Int> {
        val hjemler = mutableSetOf<Int>()
        vedtak.utbetalingBegrunnelser.forEach {
            hjemler.addAll(it.behandlingresultatOgVilkårBegrunnelse?.hentHjemler()?.toSet() ?: emptySet())
        }
        return hjemler.toSortedSet()
    }
}