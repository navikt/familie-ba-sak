package no.nav.familie.ba.sak.kjerne.vedtak

import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.dokument.sorter
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate


class VedtakUtilsTest {

    fun <T : Comparable<T>> erSortertMinstTilStørst(liste: Collection<T>): Boolean {
        return liste.asSequence().zipWithNext { a, b -> a <= b }.all { it }
    }

    /**
     * Korrekt rekkefølge:
     * 1. Utbetalings-, opphørs- og avslagsperioder sortert på fom-dato
     * 2. Avslagsperioder uten datoer
     */
    @Test
    fun `vedtaksperioder sorteres korrekt til brev`() {

        val avslagMedTomDatoInneværendeMåned = lagVedtaksperiodeMedBegrunnelser(
                type = Vedtaksperiodetype.AVSLAG,
                fom = LocalDate.now().minusMonths(6),
                tom = LocalDate.now(),

                )
        val avslagUtenTomDato =
                lagVedtaksperiodeMedBegrunnelser(
                        fom = LocalDate.now().minusMonths(5),
                        tom = null,
                        type = Vedtaksperiodetype.AVSLAG
                )
        val opphørsperiode = lagVedtaksperiodeMedBegrunnelser(
                fom = LocalDate.now().minusMonths(4),
                tom = LocalDate.now().minusMonths(1),
                type = Vedtaksperiodetype.OPPHØR
        )

        val utbetalingsperiode = lagVedtaksperiodeMedBegrunnelser(
                fom = LocalDate.now().minusMonths(3),
                tom = LocalDate.now().minusMonths(1),
                type = Vedtaksperiodetype.UTBETALING,
        )

        val avslagUtenDatoer = lagVedtaksperiodeMedBegrunnelser(
                fom = null,
                tom = null,
                type = Vedtaksperiodetype.AVSLAG,
        )

        val sorterteVedtaksperioder =
                listOf(
                        utbetalingsperiode,
                        opphørsperiode,
                        avslagMedTomDatoInneværendeMåned,
                        avslagUtenDatoer,
                        avslagUtenTomDato
                ).shuffled().sorter()

        // Utbetalingsperiode, opphørspersiode og avslagsperiode med fom-dato sorteres kronologisk
        Assertions.assertEquals(avslagMedTomDatoInneværendeMåned, sorterteVedtaksperioder[0])
        Assertions.assertEquals(avslagUtenTomDato, sorterteVedtaksperioder[1])
        Assertions.assertEquals(opphørsperiode, sorterteVedtaksperioder[2])
        Assertions.assertEquals(utbetalingsperiode, sorterteVedtaksperioder[3])

        // Avslag uten datoer legger seg til slutt
        Assertions.assertEquals(avslagUtenDatoer, sorterteVedtaksperioder[4])
    }

    @Test
    fun `Valider at alle begrunnelser som ikke er fritekst har hjemler`() {
        val begrunnelser = VedtakBegrunnelseSpesifikasjon.values()
                .filterNot { it.erFritekstBegrunnelse() }
        assertTrue(begrunnelser.none { it.hentHjemler().isEmpty() })
    }
}