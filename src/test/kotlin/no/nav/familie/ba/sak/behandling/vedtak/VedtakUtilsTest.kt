package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.vedtak.VedtakUtils.hentHjemlerBruktIVedtak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingresultatOgVilkårBegrunnelse
import no.nav.familie.ba.sak.common.lagUtbetalingBegrunnesle
import no.nav.familie.ba.sak.common.lagVedtak
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class VedtakUtilsTest {

    fun <T : Comparable<T>> erSortertMinstTilStørst(liste: Collection<T>): Boolean {
        return liste.asSequence().zipWithNext { a, b -> a <= b }.all { it }
    }

    @Test
    fun `hjemler skal være sorterte`() {
        BehandlingresultatOgVilkårBegrunnelse.values().forEach {
            val vedtak = lagVedtak()
            val utbetalingBegrunnelse = lagUtbetalingBegrunnesle(behandlingresultatOgVilkårBegrunnelse = it)
            vedtak.utbetalingBegrunnelser.add(utbetalingBegrunnelse)
            val hjemler = hentHjemlerBruktIVedtak(vedtak)
            Assertions.assertTrue(erSortertMinstTilStørst(hjemler))
        }
    }

    @Test
    fun `hjemler skal være unike og sorterte ved kombinasjon av flere begrunnelser`() {
        val utbetalingBegrunnelser = BehandlingresultatOgVilkårBegrunnelse.values()
                .map { lagUtbetalingBegrunnesle(behandlingresultatOgVilkårBegrunnelse = it) }
                .toMutableSet()
        val vedtak = lagVedtak(utbetalingBegrunnelser = utbetalingBegrunnelser)
        val hjemler = hentHjemlerBruktIVedtak(vedtak)
        Assertions.assertEquals(hjemler, hjemler.distinct().toSet())
        Assertions.assertTrue(erSortertMinstTilStørst(hjemler))
    }
}