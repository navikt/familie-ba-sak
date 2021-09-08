package no.nav.familie.ba.sak.kjerne.vedtak

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.ekstern.restDomene.RestPerson
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakUtils.hentHjemlerBruktIVedtak
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.*
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.dokument.BrevPeriodeService
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon.Companion.tilBrevTekst
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.hentMånedOgÅrForBegrunnelse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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
            assertTrue(erSortertMinstTilStørst(hjemler))
        }
    }

    @Test
    fun `hjemler skal være unike og sorterte ved kombinasjon av flere begrunnelser`() {
        val vedtakBegrunnelser = arrayOf(
                VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET,
                VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING
        )
                .map { lagVedtakBegrunnesle(vedtakBegrunnelse = it) }
                .toMutableSet()
        val vedtak = lagVedtak(vedtakBegrunnelser = vedtakBegrunnelser)
        val hjemler = hentHjemlerBruktIVedtak(vedtak)
        Assertions.assertEquals(hjemler, arrayOf(2, 4, 10, 11).toSet())
        assertTrue(erSortertMinstTilStørst(hjemler))
    }


    /**
     * Korrekt rekkefølge:
     * 1. Utbetalings-, opphørs- og avslagsperioder sortert på fom-dato
     * 2. Avslagsperioder uten datoer
     */
    @Test
    fun `vedtaksperioder sorteres korrekt til brev`() {

        val avslagMedTomDatoInneværendeMåned = Avslagsperiode(
                periodeFom = LocalDate.now().minusMonths(6),
                periodeTom = LocalDate.now(),
                vedtaksperiodetype = Vedtaksperiodetype.AVSLAG
        )
        val avslagUtenTomDato =
                Avslagsperiode(
                        periodeFom = LocalDate.now().minusMonths(5),
                        periodeTom = null,
                        vedtaksperiodetype = Vedtaksperiodetype.AVSLAG
                )
        val opphørsperiode = Opphørsperiode(
                periodeFom = LocalDate.now().minusMonths(4),
                periodeTom = LocalDate.now().minusMonths(1),
                vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
        )

        val utbetalingsperiode = Utbetalingsperiode(
                periodeFom = LocalDate.now().minusMonths(3),
                periodeTom = LocalDate.now().minusMonths(1),
                vedtaksperiodetype = Vedtaksperiodetype.UTBETALING,
                antallBarn = 1,
                ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD),
                utbetaltPerMnd = 1054,
                utbetalingsperiodeDetaljer = listOf(
                        UtbetalingsperiodeDetalj(
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
                        )
                )
        )

        val avslagUtenDatoer =
                Avslagsperiode(periodeFom = null, periodeTom = null, vedtaksperiodetype = Vedtaksperiodetype.AVSLAG)

        val sorterteVedtaksperioder = BrevPeriodeService.sorterVedtaksperioderForBrev(
                listOf(
                        utbetalingsperiode,
                        opphørsperiode,
                        avslagMedTomDatoInneværendeMåned,
                        avslagUtenDatoer,
                        avslagUtenTomDato
                ).shuffled()
        )

        // Utbetalingsperiode, opphørspersiode og avslagsperiode med fom-dato sorteres kronologisk
        Assertions.assertEquals(avslagMedTomDatoInneværendeMåned, sorterteVedtaksperioder[0])
        Assertions.assertEquals(avslagUtenTomDato, sorterteVedtaksperioder[1])
        Assertions.assertEquals(opphørsperiode, sorterteVedtaksperioder[2])
        Assertions.assertEquals(utbetalingsperiode, sorterteVedtaksperioder[3])

        // Avslag uten datoer legger seg til slutt
        Assertions.assertEquals(avslagUtenDatoer, sorterteVedtaksperioder[4])
    }

    @Test
    fun `Begrunnelsestekster for typen INNVILGET_OMSORG_FOR_BARN`() {
        var brevtekst = VedtakBegrunnelseSpesifikasjon.INNVILGET_OMSORG_FOR_BARN.hentBeskrivelse(
                barnasFødselsdatoer = listOf(LocalDate.of(2020, 5, 17)),
                månedOgÅrBegrunnelsenGjelderFor = "juli 2021",
                målform = Målform.NB
        )
        Assertions.assertEquals("Du får barnetrygd for barn født 17.05.20 fordi du har omsorgen for barnet fra juli 2021.", brevtekst)
        brevtekst = VedtakBegrunnelseSpesifikasjon.INNVILGET_OMSORG_FOR_BARN.hentBeskrivelse(
                barnasFødselsdatoer = listOf(LocalDate.of(2020, 5, 17), LocalDate.of(2017, 10, 1)),
                månedOgÅrBegrunnelsenGjelderFor = "juli 2021",
                målform = Målform.NB
        )
        Assertions.assertEquals("Du får barnetrygd for barn født 01.10.17 og 17.05.20 fordi du har omsorgen for barna fra juli 2021.", brevtekst)
        brevtekst = VedtakBegrunnelseSpesifikasjon.INNVILGET_OMSORG_FOR_BARN.hentBeskrivelse(
                barnasFødselsdatoer = listOf(LocalDate.of(2020, 5, 17)),
                månedOgÅrBegrunnelsenGjelderFor = "juli 2021",
                målform = Målform.NN
        )
        Assertions.assertEquals("Du får barnetrygd for barn fødd 17.05.20 fordi du har omsorga for barnet frå juli 2021.", brevtekst)
        brevtekst = VedtakBegrunnelseSpesifikasjon.INNVILGET_OMSORG_FOR_BARN.hentBeskrivelse(
                barnasFødselsdatoer = listOf(LocalDate.of(2020, 5, 17), LocalDate.of(2017, 10, 1)),
                månedOgÅrBegrunnelsenGjelderFor = "juli 2021",
                målform = Målform.NN
        )
        Assertions.assertEquals("Du får barnetrygd for barn fødd 01.10.17 og 17.05.20 fordi du har omsorga for barna frå juli 2021.", brevtekst)
    }

    @Test
    fun `Begrunnelse av typen AVSLAG uten periode gir korrekt formatert brevtekst uten datoer`() {
        val begrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET
        val brevtekst = begrunnelse.hentBeskrivelse(
                barnasFødselsdatoer = listOf(LocalDate.of(1814, 5, 17)),
                månedOgÅrBegrunnelsenGjelderFor = begrunnelse.vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
                        periode = Periode(
                                fom = TIDENES_MORGEN,
                                tom = TIDENES_ENDE
                        )
                ),
                målform = Målform.NB
        )
        Assertions.assertEquals("Barnetrygd for barn født 17.05.14 fordi barnet ikke er bosatt i Norge.", brevtekst)
    }

    @Test
    fun `Begrunnelse av typen AVSLAG med kun fom gir korrekt formatert brevtekst med kun fom`() {
        val begrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET
        val brevtekst = begrunnelse.hentBeskrivelse(
                barnasFødselsdatoer = listOf(LocalDate.of(1814, 5, 17)),
                månedOgÅrBegrunnelsenGjelderFor = begrunnelse.vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
                        periode = Periode(
                                fom = LocalDate.of(1814, 12, 12),
                                tom = TIDENES_ENDE
                        )
                ),
                målform = Målform.NB
        )
        Assertions.assertEquals(
                "Barnetrygd for barn født 17.05.14 fordi barnet ikke er bosatt i Norge fra desember 1814.",
                brevtekst
        )
    }

    @Test
    fun `Begrunnelse av typen AVSLAG med både fom og tom gir korrekt formatert brevtekst med fom og tom`() {
        val begrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET
        val brevtekst = begrunnelse.hentBeskrivelse(
                barnasFødselsdatoer = listOf(LocalDate.of(1814, 5, 17)),
                månedOgÅrBegrunnelsenGjelderFor = begrunnelse.vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
                        periode = Periode(
                                fom = LocalDate.of(1814, 12, 12),
                                tom = LocalDate.of(1815, 12, 12)
                        )
                ),
                målform = Målform.NB
        )
        Assertions.assertEquals(
                "Barnetrygd for barn født 17.05.14 fordi barnet ikke er bosatt i Norge fra desember 1814 til desember 1815.",
                brevtekst
        )
    }

    @Test
    fun `Begrunnelse av typen AVSLAG med flere barn viser til barna i flertall`() {
        val begrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET
        val brevtekst =
                begrunnelse.hentBeskrivelse(
                        barnasFødselsdatoer = listOf(LocalDate.of(1814, 5, 17), LocalDate.of(1814, 1, 1)),
                        månedOgÅrBegrunnelsenGjelderFor = begrunnelse.vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
                                periode = Periode(
                                        fom = TIDENES_MORGEN,
                                        tom = TIDENES_ENDE
                                )
                        ),
                        målform = Målform.NB
                )
        Assertions.assertEquals("Barnetrygd for barn født 01.01.14 og 17.05.14 fordi barna ikke er bosatt i Norge.", brevtekst)
    }

    @Test
    fun `Valider at ingen begrunnelser med fødselsdatoer formaterer dato feil`() {
        val begrunnelserMedFødselsdatoer = VedtakBegrunnelseSpesifikasjon.values().filter {
            it != VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR && it != VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR
        }
                .map {
                    it.hentBeskrivelse(
                            barnasFødselsdatoer = listOf(
                                    LocalDate.of(1997, 12, 30),
                                    LocalDate.of(1998, 12, 30),
                                    LocalDate.of(1999, 12, 30)
                            ), målform = Målform.NB
                    )
                }
                .filter { it.contains("barn født ") }
        assertTrue(begrunnelserMedFødselsdatoer.all { it.contains("30.12.97, 30.12.98 og 30.12.99") })

        val fødselsDatoForAlder6 = LocalDate.now().minusYears(6).førsteDagIInneværendeMåned()
        val annenFødselsDatoForAlder6 = fødselsDatoForAlder6.plusDays(10)
        val fødselsDatoForAlder18 = LocalDate.now().minusYears(18)
        val fødselsdatoer = listOf(
                fødselsDatoForAlder6,
                annenFødselsDatoForAlder6,
                fødselsDatoForAlder18,
        )
        val beskrivelseBarn6år = VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR.hentBeskrivelse(
                barnasFødselsdatoer = fødselsdatoer, målform = Målform.NB
        )
        val datoerIBrev = listOf(fødselsDatoForAlder6, annenFødselsDatoForAlder6).tilBrevTekst()
        assertTrue(beskrivelseBarn6år.equals("Barnetrygden reduseres fordi barn født $datoerIBrev er 6 år."))

        val beskrivelseBarn18år = VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR.hentBeskrivelse(
                barnasFødselsdatoer = fødselsdatoer, målform = Målform.NN
        )
        val datoIBrev = listOf(fødselsDatoForAlder18).tilBrevTekst()
        assertTrue(beskrivelseBarn18år.equals("Barnetrygda er redusert fordi barn fødd $datoIBrev er 18 år."))
    }

    @Test
    fun `Valider at alle begrunnelser som ikke er fritekst har hjemler`() {
        val begrunnelser = VedtakBegrunnelseSpesifikasjon.values()
                .filterNot { it.erFritekstBegrunnelse() }
        assertTrue(begrunnelser.none { it.hentHjemler().isEmpty() })
    }
}