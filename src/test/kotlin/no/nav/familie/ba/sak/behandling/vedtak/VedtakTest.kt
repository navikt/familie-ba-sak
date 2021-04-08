package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.restDomene.RestPostFritekstVedtakBegrunnelser
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVedtakBegrunnesle
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakTest {

    @Test
    fun `Legg til fritekster til vedtakbegrunnelser`() {
        val fritekst1 = "fritekst1";
        val fritekst2 = "fritekst2";

        val vedtak = lagVedtak();

        vedtak.settFritekstbegrunnelser(RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now().minusMonths(1),
                                                                           tom = LocalDate.now(),
                                                                           fritekster = listOf(fritekst1, fritekst2),
                                                                           vedtaksperiodetype = Vedtaksperiodetype.OPPHØR))

        assertTrue(vedtak.vedtakBegrunnelser.any { it.brevBegrunnelse == fritekst1 })
        assertTrue(vedtak.vedtakBegrunnelser.any { it.brevBegrunnelse == fritekst2 })
    }

    @Test
    fun `Sjekk at gamle fritekster blir overskrevet når nye blir lagt til vedtakbegrunnelser`() {
        val fritekst1 = "fritekst1";
        val fritekst2 = "fritekst2";
        val fritekst3 = "fritekst3";
        val fritekst4 = "fritekst4";

        val vedtak = lagVedtak();

        vedtak.settFritekstbegrunnelser(RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now().minusMonths(1),
                                                                           tom = LocalDate.now(),
                                                                           fritekster = listOf(fritekst1, fritekst2),
                                                                           vedtaksperiodetype = Vedtaksperiodetype.OPPHØR))

        vedtak.settFritekstbegrunnelser(RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now().minusMonths(1),
                                                                           tom = LocalDate.now(),
                                                                           fritekster = listOf(fritekst3, fritekst4),
                                                                           vedtaksperiodetype = Vedtaksperiodetype.OPPHØR))
        assertFalse(vedtak.vedtakBegrunnelser.any { it.brevBegrunnelse == fritekst1 })
        assertFalse(vedtak.vedtakBegrunnelser.any { it.brevBegrunnelse == fritekst2 })
        assertTrue(vedtak.vedtakBegrunnelser.any { it.brevBegrunnelse == fritekst3 })
        assertTrue(vedtak.vedtakBegrunnelser.any { it.brevBegrunnelse == fritekst4 })
    }

    @Test
    fun `valider opphør fritekst trenger begrunnelse av tilsvarende type og for samme periode`() {
        val fritekst1 = "fritekst1";
        val fritekst2 = "fritekst2";

        val vedtak = lagVedtak();

        vedtak.settFritekstbegrunnelser(RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now().minusMonths(1),
                                                                           tom = LocalDate.now(),
                                                                           fritekster = listOf(fritekst1, fritekst2),
                                                                           vedtaksperiodetype = Vedtaksperiodetype.OPPHØR))

        Assertions.assertThrows(FunksjonellFeil::class.java) {
            vedtak.validerVedtakBegrunnelserForFritekstOpphørOgReduksjon()
        }
    }

    @Test
    fun `valider reduksjon fritekst trenger begrunnelse av tilsvarende type og for samme periode`() {
        val fritekst1 = "fritekst1";
        val fritekst2 = "fritekst2";

        val vedtak = lagVedtak();

        vedtak.settFritekstbegrunnelser(RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now().minusMonths(1),
                                                                           tom = LocalDate.now().minusMonths(1),
                                                                           fritekster = listOf(fritekst1, fritekst2),
                                                                           vedtaksperiodetype = Vedtaksperiodetype.UTBETALING))

        Assertions.assertThrows(FunksjonellFeil::class.java) {
            vedtak.validerVedtakBegrunnelserForFritekstOpphørOgReduksjon()
        }
    }

    @Test
    fun `valider avslag fritekst trenger ikke begrunnelse av tilsvarende type`() {
        val fritekst1 = "fritekst1";
        val fritekst2 = "fritekst2";

        val vedtak = lagVedtak();

        vedtak.settFritekstbegrunnelser(RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now().minusMonths(1),
                                                                           tom = LocalDate.now(),
                                                                           fritekster = listOf(fritekst1, fritekst2),
                                                                           vedtaksperiodetype = Vedtaksperiodetype.AVSLAG))


        assertTrue(vedtak.validerVedtakBegrunnelserForFritekstOpphørOgReduksjon())
    }

    @Test
    fun `valider opphør fritekst med begrunnelse av tilsvarende type og for samme periode validerer`() {
        val fritekst1 = "fritekst1";
        val fritekst2 = "fritekst2";

        val vedtak = lagVedtak();

        vedtak.settFritekstbegrunnelser(RestPostFritekstVedtakBegrunnelser(fom = LocalDate.now().minusMonths(1),
                                                                           tom = LocalDate.now(),
                                                                           fritekster = listOf(fritekst1, fritekst2),
                                                                           vedtaksperiodetype = Vedtaksperiodetype.OPPHØR))


        vedtak.leggTilBegrunnelse(lagVedtakBegrunnesle(fom = LocalDate.now().minusMonths(1),
                                     tom = LocalDate.now(),
                                     vedtak = vedtak,
                                     vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.OPPHØR_BARN_FLYTTET_FRA_SØKER))


        assertTrue(vedtak.validerVedtakBegrunnelserForFritekstOpphørOgReduksjon())
    }
}