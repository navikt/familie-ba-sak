package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.restDomene.RestPerson
import no.nav.familie.ba.sak.behandling.vedtak.VedtakUtils.hentHjemlerBruktIVedtak
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.*
import no.nav.familie.ba.sak.behandling.vilkår.*
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon.Companion.finnVilkårFor
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.brev.BrevPeriodeService
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


    /**
     * Korrekt rekkefølge:
     * 1. Utbetalings-, opphørs- og avslagsperioder sortert på fom-dato
     * 2. Avslagsperioder uten datoer
     */
    @Test
    fun `vedtaksperioder sorteres korrekt til brev`() {

        val avslagMedTomDatoInneværendeMåned = Avslagsperiode(periodeFom = LocalDate.now().minusMonths(6),
                                                              periodeTom = LocalDate.now(),
                                                              vedtaksperiodetype = Vedtaksperiodetype.AVSLAG)
        val avslagUtenTomDato =
                Avslagsperiode(periodeFom = LocalDate.now().minusMonths(5),
                               periodeTom = null,
                               vedtaksperiodetype = Vedtaksperiodetype.AVSLAG)
        val opphørsperiode = Opphørsperiode(periodeFom = LocalDate.now().minusMonths(4),
                                            periodeTom = LocalDate.now().minusMonths(1),
                                            vedtaksperiodetype = Vedtaksperiodetype.OPPHØR)

        val utbetalingsperiode = Utbetalingsperiode(periodeFom = LocalDate.now().minusMonths(3),
                                                    periodeTom = LocalDate.now().minusMonths(1),
                                                    vedtaksperiodetype = Vedtaksperiodetype.UTBETALING,
                                                    antallBarn = 1,
                                                    ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD),
                                                    utbetaltPerMnd = 1054,
                                                    utbetalingsperiodeDetaljer = listOf(UtbetalingsperiodeDetalj(
                                                            person = RestPerson(
                                                                    type = PersonType.BARN,
                                                                    fødselsdato = LocalDate.now(),
                                                                    personIdent = "",
                                                                    navn = "",
                                                                    kjønn = Kjønn.UKJENT,
                                                                    målform = Målform.NN
                                                            ),
                                                            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                                            utbetaltPerMnd = 1054,
                                                    )))

        val avslagUtenDatoer =
                Avslagsperiode(periodeFom = null, periodeTom = null, vedtaksperiodetype = Vedtaksperiodetype.AVSLAG)

        val sorterteVedtaksperioder = BrevPeriodeService.sorterVedtaksperioderForBrev(listOf(utbetalingsperiode,
                                                                                             opphørsperiode,
                                                                                             avslagMedTomDatoInneværendeMåned,
                                                                                             avslagUtenDatoer,
                                                                                             avslagUtenTomDato).shuffled())

        // Utbetalingsperiode, opphørspersiode og avslagsperiode med fom-dato sorteres kronologisk
        Assertions.assertEquals(avslagMedTomDatoInneværendeMåned, sorterteVedtaksperioder[0])
        Assertions.assertEquals(avslagUtenTomDato, sorterteVedtaksperioder[1])
        Assertions.assertEquals(opphørsperiode, sorterteVedtaksperioder[2])
        Assertions.assertEquals(utbetalingsperiode, sorterteVedtaksperioder[3])

        // Avslag uten datoer legger seg til slutt
        Assertions.assertEquals(avslagUtenDatoer, sorterteVedtaksperioder[4])
    }

    @Test
    fun `Begrunnelse av typen AVSLAG uten periode gir korrekt formatert brevtekst uten datoer`() {
        val begrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET
        val brevtekst = begrunnelse.hentBeskrivelse(barnasFødselsdatoer = listOf(LocalDate.of(1814, 5, 17)),
                                                    månedOgÅrBegrunnelsenGjelderFor = begrunnelse.vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
                                                            periode = Periode(fom = TIDENES_MORGEN,
                                                                              tom = TIDENES_ENDE)
                                                    ),
                                                    målform = Målform.NB)
        Assertions.assertEquals("Barnetrygd fordi barn født 17.05.14 ikke er bosatt i Norge.", brevtekst)
    }

    @Test
    fun `Begrunnelse av typen AVSLAG med kun fom gir korrekt formatert brevtekst med kun fom`() {
        val begrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET
        val brevtekst = begrunnelse.hentBeskrivelse(barnasFødselsdatoer = listOf(LocalDate.of(1814, 5, 17)),
                                                    månedOgÅrBegrunnelsenGjelderFor = begrunnelse.vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
                                                            periode = Periode(fom = LocalDate.of(1814, 12, 12),
                                                                              tom = TIDENES_ENDE)
                                                    ),
                                                    målform = Målform.NB)
        Assertions.assertEquals("Barnetrygd fordi barn født 17.05.14 ikke er bosatt i Norge fra desember 1814.", brevtekst)
    }

    @Test
    fun `Begrunnelse av typen AVSLAG med både fom og tom gir korrekt formatert brevtekst med fom og tom`() {
        val begrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET
        val brevtekst = begrunnelse.hentBeskrivelse(barnasFødselsdatoer = listOf(LocalDate.of(1814, 5, 17)),
                                                    månedOgÅrBegrunnelsenGjelderFor = begrunnelse.vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
                                                            periode = Periode(fom = LocalDate.of(1814, 12, 12),
                                                                              tom = LocalDate.of(1815, 12, 12))
                                                    ),
                                                    målform = Målform.NB)
        Assertions.assertEquals("Barnetrygd fordi barn født 17.05.14 ikke er bosatt i Norge fra desember 1814 til desember 1815.",
                                brevtekst)
    }

    @Test
    fun `Valider at ingen vilkår er knyttet til mer enn én begrunnelse`() {
        assertDoesNotThrow { VedtakBegrunnelseSpesifikasjon.values().map { it.finnVilkårFor() } }
    }


}