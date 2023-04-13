package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.cucumber.domeneparser.DomeneparserUtil.groupByBehandlingId
import no.nav.familie.ba.sak.cucumber.domeneparser.ForventetUtbetalingsoppdrag
import no.nav.familie.ba.sak.cucumber.domeneparser.ForventetUtbetalingsperiode
import no.nav.familie.ba.sak.cucumber.domeneparser.OppdragParser
import no.nav.familie.ba.sak.cucumber.domeneparser.OppdragParser.mapTilkjentYtelse
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForIverksettingFactory
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForUtbetalingsoppdrag
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsoppdragGenerator
import no.nav.familie.ba.sak.integrasjoner.økonomi.pakkInnForUtbetaling
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.gjeldendeForrigeOffsetForKjede
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.kjedeinndelteAndeler
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.simulering.lagBehandling
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import org.assertj.core.api.Assertions.assertThat

class OppdragSteg {

    private val utbetalingsoppdragGenerator = UtbetalingsoppdragGenerator(mockk(relaxed = true))
    private var behandlinger = mutableMapOf<Long, Behandling>()
    private var tilkjenteYtelser = mutableListOf<TilkjentYtelse>()
    private var beregnetUtbetalingsoppdrag = mutableMapOf<Long, Utbetalingsoppdrag>()

    @Gitt("følgende tilkjente ytelser")
    fun følgendeTilkjenteYtelser(dataTable: DataTable) {
        settBehandlinger(dataTable)
        tilkjenteYtelser = mapTilkjentYtelse(dataTable, behandlinger).toMutableList()
    }

    @Når("beregner utbetalingsoppdrag")
    fun `beregner utbetalingsoppdrag`() {
        tilkjenteYtelser.fold(emptyList<TilkjentYtelse>()) { acc, tilkjentYtelse ->
            val forrigeTilkjentYtelse = acc.lastOrNull()
            val forrigeKjeder = tilKjeder(forrigeTilkjentYtelse)
            val oppdaterteKjeder = tilKjeder(tilkjentYtelse)
            val utbetalingsoppdrag = utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                saksbehandlerId = "saksbehandlerId",
                vedtak = lagVedtak(behandling = tilkjentYtelse.behandling),
                erFørsteBehandlingPåFagsak = forrigeTilkjentYtelse == null,
                forrigeKjeder = forrigeKjeder,
                sisteOffsetPerIdent = gjeldendeForrigeOffsetForKjede(forrigeKjeder),
                sisteOffsetPåFagsak = null,
                oppdaterteKjeder = oppdaterteKjeder,
                erSimulering = false,
                endretMigreringsDato = null
            )
            beregnetUtbetalingsoppdrag[tilkjentYtelse.behandling.id] = utbetalingsoppdrag

            acc + tilkjentYtelse
        }
    }

    @Så("forvent følgende utbetalingsoppdrag")
    fun `forvent følgende utbetalingsoppdrag`(dataTable: DataTable) {
        val forventedeUtbetalingsoppdrag = OppdragParser.mapForventetUtbetalingsoppdrag(dataTable, medUtbetalingsperiode = true) // TODO medUtbetalingsperiode
        // assertSjekkBehandlingIder(forventedeUtbetalingsoppdrag.map { it.behandlingId }, false) // verifiser at alle behandlinger er verifisert
        forventedeUtbetalingsoppdrag.forEach { forventetUtbetalingsoppdrag ->
            val behandlingId = forventetUtbetalingsoppdrag.behandlingId
            val utbetalingsoppdrag = beregnetUtbetalingsoppdrag[behandlingId]
                ?: error("Mangler utbetalingsoppdrag for $behandlingId")
            assertUtbetalingsoppdrag(forventetUtbetalingsoppdrag, utbetalingsoppdrag, false)
        }
    }

    private fun tilKjeder(tilkjentYtelse: TilkjentYtelse?): Map<String, List<AndelTilkjentYtelseForUtbetalingsoppdrag>> {
        val andeler = tilkjentYtelse?.andelerTilkjentYtelse ?: emptyList()
        return andeler
            .pakkInnForUtbetaling(AndelTilkjentYtelseForIverksettingFactory())
            .let { kjedeinndelteAndeler(it) }
    }

    private fun settBehandlinger(dataTable: DataTable) {
        val groupByBehandlingId = dataTable.groupByBehandlingId()
        behandlinger = groupByBehandlingId
            .map { lagBehandling().copy(id = it.key) }
            .associateBy { it.id }
            .toMutableMap()
    }

    // @Gitt("følgende tilkjente ytelser uten andel for {}")
}

private fun assertUtbetalingsoppdrag(
    forventetUtbetalingsoppdrag: ForventetUtbetalingsoppdrag,
    utbetalingsoppdrag: Utbetalingsoppdrag,
    medUtbetalingsperiode: Boolean = true,
) {
    assertThat(utbetalingsoppdrag.kodeEndring).isEqualTo(forventetUtbetalingsoppdrag.kodeEndring)
    assertThat(utbetalingsoppdrag.utbetalingsperiode).hasSize(forventetUtbetalingsoppdrag.utbetalingsperiode.size)
    if (medUtbetalingsperiode) {
        forventetUtbetalingsoppdrag.utbetalingsperiode.forEachIndexed { index, forventetUtbetalingsperiode ->
            val utbetalingsperiode = utbetalingsoppdrag.utbetalingsperiode[index]
            assertUtbetalingsperiode(utbetalingsperiode, forventetUtbetalingsperiode)
        }
    }
}

private fun assertUtbetalingsperiode(
    utbetalingsperiode: Utbetalingsperiode,
    forventetUtbetalingsperiode: ForventetUtbetalingsperiode,
) {
    assertThat(utbetalingsperiode.erEndringPåEksisterendePeriode)
        .isEqualTo(forventetUtbetalingsperiode.erEndringPåEksisterendePeriode)
    assertThat(utbetalingsperiode.klassifisering).isEqualTo(Ytelsestype.BARNETRYGD.kode)
    assertThat(utbetalingsperiode.periodeId).isEqualTo(forventetUtbetalingsperiode.periodeId)
    assertThat(utbetalingsperiode.forrigePeriodeId).isEqualTo(forventetUtbetalingsperiode.forrigePeriodeId)
    assertThat(utbetalingsperiode.sats.toInt()).isEqualTo(forventetUtbetalingsperiode.sats)
    assertThat(utbetalingsperiode.satsType).isEqualTo(forventetUtbetalingsperiode.satsType)
    assertThat(utbetalingsperiode.vedtakdatoFom).isEqualTo(forventetUtbetalingsperiode.fom)
    assertThat(utbetalingsperiode.vedtakdatoTom).isEqualTo(forventetUtbetalingsperiode.tom)
    assertThat(utbetalingsperiode.opphør?.opphørDatoFom).isEqualTo(forventetUtbetalingsperiode.opphør)
}
