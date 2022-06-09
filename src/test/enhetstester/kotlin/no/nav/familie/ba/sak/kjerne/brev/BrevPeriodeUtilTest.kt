package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.kjerne.brev.domene.BrevperiodeData
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrevPeriodeUtilTest {

    @Test
    fun `Skal sortere perioder kronologisk, med avslag til slutt`() {
        val liste = listOf(
            lagBrevperiodeData(
                fom = LocalDate.now().minusMonths(12),
                tom = LocalDate.now().minusMonths(8),
                type = Vedtaksperiodetype.UTBETALING
            ),
            lagBrevperiodeData(
                fom = LocalDate.now().minusMonths(4),
                tom = null,
                type = Vedtaksperiodetype.AVSLAG,
            ),
            lagBrevperiodeData(
                fom = LocalDate.now().minusMonths(7),
                tom = LocalDate.now().minusMonths(4),
                type = Vedtaksperiodetype.OPPHØR
            ),
            lagBrevperiodeData(
                fom = LocalDate.now().minusMonths(3),
                tom = LocalDate.now(),
                type = Vedtaksperiodetype.UTBETALING
            )
        )

        val sortertListe = liste.sorted()

        Assertions.assertTrue(sortertListe.size == 4)
        val førstePeriode = sortertListe.first()
        val andrePeriode = sortertListe[1]
        val tredjePeriode = sortertListe[2]
        val sistePeriode = sortertListe.last()

        Assertions.assertEquals(Vedtaksperiodetype.UTBETALING, førstePeriode.minimertVedtaksperiode.type)
        Assertions.assertEquals(LocalDate.now().minusMonths(12), førstePeriode.minimertVedtaksperiode.fom)
        Assertions.assertEquals(Vedtaksperiodetype.OPPHØR, andrePeriode.minimertVedtaksperiode.type)
        Assertions.assertEquals(LocalDate.now().minusMonths(7), andrePeriode.minimertVedtaksperiode.fom)
        Assertions.assertEquals(Vedtaksperiodetype.UTBETALING, tredjePeriode.minimertVedtaksperiode.type)
        Assertions.assertEquals(LocalDate.now().minusMonths(3), tredjePeriode.minimertVedtaksperiode.fom)
        Assertions.assertEquals(Vedtaksperiodetype.AVSLAG, sistePeriode.minimertVedtaksperiode.type)
        Assertions.assertEquals(LocalDate.now().minusMonths(4), sistePeriode.minimertVedtaksperiode.fom)
    }
}

private fun lagBrevperiodeData(fom: LocalDate?, tom: LocalDate?, type: Vedtaksperiodetype): BrevperiodeData {
    val restBehandlingsgrunnlagForBrev = RestBehandlingsgrunnlagForBrev(personerPåBehandling = emptyList(), minimertePersonResultater = emptyList(), minimerteEndredeUtbetalingAndeler = emptyList())
    return BrevperiodeData(
        restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
        erFørsteVedtaksperiodePåFagsak = false,
        brevMålform = Målform.NB,
        minimertVedtaksperiode = MinimertVedtaksperiode(
            begrunnelser = emptyList(),
            fom = fom,
            tom = tom,
            type = type,
            eøsBegrunnelser = emptyList(),
        ),
        uregistrerteBarn = emptyList(),
        minimerteKompetanser = emptyList(),
    )
}
