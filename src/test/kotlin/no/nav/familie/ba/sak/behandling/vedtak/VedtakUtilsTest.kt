package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.vedtak.VedtakUtils.hentHjemlerBruktIVedtak
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon.Companion.finnVilkårFor
import no.nav.familie.ba.sak.behandling.vilkår.hentMånedOgÅrForBegrunnelse
import no.nav.familie.ba.sak.common.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate


class VedtakUtilsTest {

    fun <T : Comparable<T>> erSortertMinstTilStørst(liste: Collection<T>): Boolean {
        return liste.asSequence().zipWithNext { a, b -> a <= b }.all { it }
    }

    @Test
    fun `hjemler skal være sorterte`() {
        VedtakBegrunnelseSpesifikasjon.values().forEach {
            val vedtak = lagVedtak()
            val vedtakBegrunnelse = lagVedtakBegrunnesle(vedtakBegrunnelse = it)
            vedtak.vedtakBegrunnelser.add(vedtakBegrunnelse)
            val hjemler = hentHjemlerBruktIVedtak(vedtak)
            Assertions.assertTrue(erSortertMinstTilStørst(hjemler))
        }
    }

    @Test
    fun `hjemler skal være unike og sorterte ved kombinasjon av flere begrunnelser`() {
        val vedtakBegrunnelser = arrayOf(VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET,
                                         VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING)
                .map { lagVedtakBegrunnesle(vedtakBegrunnelse = it) }
                .toMutableSet()
        val vedtak = lagVedtak(vedtakBegrunnelser = vedtakBegrunnelser)
        val hjemler = hentHjemlerBruktIVedtak(vedtak)
        Assertions.assertEquals(hjemler, arrayOf(2, 4, 10, 11).toSet())
        Assertions.assertTrue(erSortertMinstTilStørst(hjemler))
    }

    @Test
    fun `Begrunnelse av typen AVSLAG uten periode gir korrekt formatert brevtekst uten datoer`() {
        val begrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET
        val brevtekst = begrunnelse.hentBeskrivelse(barnasFødselsdatoer = LocalDate.of(1814, 5, 17).tilKortString(),
                                                    månedOgÅrBegrunnelsenGjelderFor = begrunnelse.vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
                                                            periode = Periode(fom = TIDENES_MORGEN,
                                                                              tom = TIDENES_ENDE)
                                                    ),
                                                    målform = Målform.NB)
        Assertions.assertEquals("Du og/eller barn født 17.05.14 ikke er bosatt i Norge.", brevtekst)
    }

    @Test
    fun `Begrunnelse av typen AVSLAG med kun fom gir korrekt formatert brevtekst med kun fom`() {
        val begrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET
        val brevtekst = begrunnelse.hentBeskrivelse(barnasFødselsdatoer = LocalDate.of(1814, 5, 17).tilKortString(),
                                                    månedOgÅrBegrunnelsenGjelderFor = begrunnelse.vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
                                                            periode = Periode(fom = LocalDate.of(1814, 12, 12),
                                                                              tom = TIDENES_ENDE)
                                                    ),
                                                    målform = Målform.NB)
        Assertions.assertEquals("Du og/eller barn født 17.05.14 ikke er bosatt i Norge fra desember 1814.", brevtekst)
    }

    @Test
    fun `Begrunnelse av typen AVSLAG med både fom og tom gir korrekt formatert brevtekst med fom og tom`() {
        val begrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET
        val brevtekst = begrunnelse.hentBeskrivelse(barnasFødselsdatoer = LocalDate.of(1814, 5, 17).tilKortString(),
                                                    månedOgÅrBegrunnelsenGjelderFor = begrunnelse.vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
                                                            periode = Periode(fom = LocalDate.of(1814, 12, 12),
                                                                              tom = LocalDate.of(1815, 12, 12))
                                                    ),
                                                    målform = Målform.NB)
        Assertions.assertEquals("Du og/eller barn født 17.05.14 ikke er bosatt i Norge fra desember 1814 til desember 1815.",
                                brevtekst)
    }

    @Test
    fun `Valider at ingen vilkår er knyttet til mer enn én begrunnelse`() {
        assertDoesNotThrow { VedtakBegrunnelseSpesifikasjon.values().map { it.finnVilkårFor() } }
    }


}