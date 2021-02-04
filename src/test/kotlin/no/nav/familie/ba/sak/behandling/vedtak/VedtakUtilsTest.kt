package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.vedtak.VedtakUtils.hentHjemlerBruktIVedtak
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelser
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVedtakBegrunnesle
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class VedtakUtilsTest {

    fun <T : Comparable<T>> erSortertMinstTilStørst(liste: Collection<T>): Boolean {
        return liste.asSequence().zipWithNext { a, b -> a <= b }.all { it }
    }

    @Test
    fun `hjemler skal være sorterte`() {
        VedtakBegrunnelser.values().forEach {
            val vedtak = lagVedtak()
            val vedtakBegrunnelse = lagVedtakBegrunnesle(vedtakBegrunnelse = it)
            vedtak.vedtakBegrunnelser.add(vedtakBegrunnelse)
            val hjemler = hentHjemlerBruktIVedtak(vedtak)
            Assertions.assertTrue(erSortertMinstTilStørst(hjemler))
        }
    }

    @Test
    fun `hjemler skal være unike og sorterte ved kombinasjon av flere begrunnelser`() {
        val vedtakBegrunnelser = arrayOf(VedtakBegrunnelser.INNVILGET_BOSATT_I_RIKTET,
                                         VedtakBegrunnelser.INNVILGET_SATSENDRING)
                .map { lagVedtakBegrunnesle(vedtakBegrunnelse = it) }
                .toMutableSet()
        val vedtak = lagVedtak(vedtakBegrunnelser = vedtakBegrunnelser)
        val hjemler = hentHjemlerBruktIVedtak(vedtak)
        Assertions.assertEquals(hjemler, arrayOf(2, 4, 10, 11).toSet())
        Assertions.assertTrue(erSortertMinstTilStørst(hjemler))
    }
}