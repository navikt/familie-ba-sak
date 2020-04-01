package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype.*
import no.nav.familie.ba.sak.common.dato
import no.nav.familie.ba.sak.common.lagPersonVedtak
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.fpsak.tidsserie.LocalDateSegment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate.now

internal class UtbetalingsoppdragPeriodiseringTest {

    @Test
    fun `skal opprette et nytt utbetalingsoppdrag med løpende periodeId fordelt på flere klasser`() {
        val vedtak = lagVedtak()
        val personvedtak = listOf(
                lagPersonVedtak("2018-07-01", "2021-06-30", SMÅBARNSTILLEGG, 660, vedtak),
                lagPersonVedtak("2020-04-01", "2023-03-31", SMÅBARNSTILLEGG, 660, vedtak),
                lagPersonVedtak("2020-03-01", "2038-02-28", ORDINÆR_BARNETRYGD, 1054, vedtak),
                lagPersonVedtak("2020-05-01", "2021-02-28", UTVIDET_BARNETRYGD, 1054, vedtak))

        val behandlingResultatType = BehandlingResultatType.INNVILGET
        val utbetalingsoppdrag = lagUtbetalingsoppdrag("saksbehandler", vedtak, behandlingResultatType, personvedtak)

        assertEquals(Utbetalingsoppdrag.KodeEndring.NY, utbetalingsoppdrag.kodeEndring)
        assertEquals(6, utbetalingsoppdrag.utbetalingsperiode.size)

        val id = vedtak.id * 1000
        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATRSMA"]!![0], id + 0, null, 660, "2018-07-01", "2020-03-31")
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATRSMA"]!![1], id + 1, id + 0, 1320, "2020-04-01", "2021-06-30")
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATRSMA"]!![2], id + 2, id + 1, 660, "2021-07-01", "2023-03-31")

        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATR"]!![0], id + 0, null, 1054, "2020-03-01", "2020-04-30")
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATR"]!![1], id + 1, id + 0, 2108, "2020-05-01", "2021-02-28")
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATR"]!![2], id + 2, id + 1, 1054, "2021-03-01", "2038-02-28")
    }

    @Test
    fun `skal opprette et opphør med løpende periodeId fordelt på flere klasser`() {
        val vedtak = lagVedtak()
        val personvedtak = listOf(
                lagPersonVedtak("2018-07-01", "2021-06-30", SMÅBARNSTILLEGG, 660, vedtak),
                lagPersonVedtak("2020-04-01", "2023-03-31", SMÅBARNSTILLEGG, 660, vedtak),
                lagPersonVedtak("2020-03-01", "2038-02-28", ORDINÆR_BARNETRYGD, 1054, vedtak),
                lagPersonVedtak("2020-05-01", "2021-02-28", UTVIDET_BARNETRYGD, 1054, vedtak))

        val opphørVedtak = lagVedtak(forrigeVedtak = vedtak, opphørsdato = now())
        val behandlingResultatType = BehandlingResultatType.OPPHØRT
        val utbetalingsoppdrag = lagUtbetalingsoppdrag("saksbehandler", opphørVedtak, behandlingResultatType, personvedtak)

        assertEquals(Utbetalingsoppdrag.KodeEndring.UEND, utbetalingsoppdrag.kodeEndring)
        assertEquals(2, utbetalingsoppdrag.utbetalingsperiode.size)

        val id = vedtak.id * 1000

        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATRSMA"]!![0], id + 2, id + 1, 660, "2021-07-01", "2023-03-31")
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATR"]!![0], id + 2, id + 1, 1054, "2021-03-01", "2038-02-28")
    }

    @Test()
    fun `skal ikke tillate for stor offset på periodeId`() {
        val vedtak = lagVedtak()

        val utbetalingsperiodeMal = UtbetalingsperiodeMal(vedtak)

        val segment = LocalDateSegment(now(), now().plusDays(1), 100)
        utbetalingsperiodeMal.lagPeriode("A", segment, 999) //OK

        assertThrows<IllegalArgumentException> {
            utbetalingsperiodeMal.lagPeriode("A", segment, 1000)
        }
    }

    fun assertUtbetalingsperiode(utbetalingsperiode: Utbetalingsperiode,
                                 periodeId: Long,
                                 forrigePeriodeId: Long?,
                                 sats: Int,
                                 fom: String,
                                 tom: String
    ) {
        assertEquals(periodeId, utbetalingsperiode.periodeId)
        assertEquals(sats, utbetalingsperiode.sats.toInt())
        assertEquals(dato(fom), utbetalingsperiode.vedtakdatoFom)
        assertEquals(dato(tom), utbetalingsperiode.vedtakdatoTom)
        assertEquals(forrigePeriodeId, utbetalingsperiode.forrigePeriodeId)
    }
}