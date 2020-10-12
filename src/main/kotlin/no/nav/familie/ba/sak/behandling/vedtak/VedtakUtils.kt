package no.nav.familie.ba.sak.behandling.vedtak

object VedtakUtils {

    fun hentHjemlerBruktIVedtak(vedtak: Vedtak): MutableSet<Int> {
        val hjemler = mutableSetOf<Int>()
        vedtak.utbetalingBegrunnelser.forEach {
            hjemler.addAll(it.behandlingresultatOgVilkårBegrunnelse?.hentHjemler()?.toSet() ?: emptySet())
        }
        return hjemler
    }
}