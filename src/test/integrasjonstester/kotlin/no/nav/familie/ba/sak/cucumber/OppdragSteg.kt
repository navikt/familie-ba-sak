package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.mockk
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
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
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.assertj.core.api.Assertions.assertThat

class OppdragSteg {

    private val utbetalingsoppdragGenerator = UtbetalingsoppdragGenerator(mockk(relaxed = true))
    private var behandlinger = mapOf<Long, Behandling>()
    private var tilkjenteYtelser = listOf<TilkjentYtelse>()
    private var beregnetUtbetalingsoppdrag = mutableMapOf<Long, Utbetalingsoppdrag>()

    @Gitt("følgende tilkjente ytelser")
    fun følgendeTilkjenteYtelser(dataTable: DataTable) {
        genererBehandlinger(dataTable)
        tilkjenteYtelser = mapTilkjentYtelse(dataTable, behandlinger)
    }

    @Når("beregner utbetalingsoppdrag")
    fun `beregner utbetalingsoppdrag`() {
        tilkjenteYtelser.fold(emptyList<TilkjentYtelse>()) { acc, tilkjentYtelse ->
            val forrigeTilkjentYtelse = acc.lastOrNull()

            val vedtak = lagVedtak(behandling = tilkjentYtelse.behandling)
            val forrigeKjeder = tilKjeder(forrigeTilkjentYtelse)
            val oppdaterteKjeder = tilKjeder(tilkjentYtelse)
            val sisteOffsetPåFagsak =
                acc.flatMap { it.andelerTilkjentYtelse.map { it.periodeOffset } }.maxByOrNull { it!! }
            val sisteOffsetPerIdent = gjeldendeForrigeOffsetForKjede(forrigeKjeder)
            val utbetalingsoppdrag = utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                saksbehandlerId = "saksbehandlerId",
                vedtak = vedtak,
                erFørsteBehandlingPåFagsak = forrigeTilkjentYtelse == null,
                forrigeKjeder = forrigeKjeder,
                sisteOffsetPerIdent = sisteOffsetPerIdent,
                sisteOffsetPåFagsak = sisteOffsetPåFagsak?.toInt(),
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
        val forventedeUtbetalingsoppdrag = OppdragParser.mapForventetUtbetalingsoppdrag(
            dataTable,
            medUtbetalingsperiode = true
        ) // TODO medUtbetalingsperiode
        // assertSjekkBehandlingIder(forventedeUtbetalingsoppdrag.map { it.behandlingId }, false) // verifiser at alle behandlinger er verifisert
        forventedeUtbetalingsoppdrag.forEach { forventetUtbetalingsoppdrag ->
            val behandlingId = forventetUtbetalingsoppdrag.behandlingId
            val utbetalingsoppdrag = beregnetUtbetalingsoppdrag[behandlingId]
                ?: error("Mangler utbetalingsoppdrag for $behandlingId")
            assertUtbetalingsoppdrag(forventetUtbetalingsoppdrag, utbetalingsoppdrag, true)
        }
    }

    private fun tilKjeder(tilkjentYtelse: TilkjentYtelse?): Map<String, List<AndelTilkjentYtelseForUtbetalingsoppdrag>> {
        return (tilkjentYtelse?.andelerTilkjentYtelse ?: emptyList())
            .pakkInnForUtbetaling(AndelTilkjentYtelseForIverksettingFactory())
            .let { kjedeinndelteAndeler(it) }
    }

    private fun genererBehandlinger(dataTable: DataTable) {
        val fagsak = defaultFagsak()
        behandlinger = dataTable.groupByBehandlingId()
            .map { lagBehandling(fagsak = fagsak).copy(id = it.key) }
            .associateBy { it.id }
    }

    // @Gitt("følgende tilkjente ytelser uten andel for {}")
}

private fun assertUtbetalingsoppdrag(
    forventetUtbetalingsoppdrag: ForventetUtbetalingsoppdrag,
    utbetalingsoppdrag: Utbetalingsoppdrag,
    medUtbetalingsperiode: Boolean = true
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
    forventetUtbetalingsperiode: ForventetUtbetalingsperiode
) {
    assertThat(utbetalingsperiode.erEndringPåEksisterendePeriode)
        .isEqualTo(forventetUtbetalingsperiode.erEndringPåEksisterendePeriode)
    assertThat(utbetalingsperiode.klassifisering).isEqualTo(YtelseType.ORDINÆR_BARNETRYGD.klassifisering)
    assertThat(utbetalingsperiode.periodeId).isEqualTo(forventetUtbetalingsperiode.periodeId)
    assertThat(utbetalingsperiode.forrigePeriodeId).isEqualTo(forventetUtbetalingsperiode.forrigePeriodeId)
    assertThat(utbetalingsperiode.sats.toInt()).isEqualTo(forventetUtbetalingsperiode.sats)
    assertThat(utbetalingsperiode.satsType).isEqualTo(forventetUtbetalingsperiode.satsType)
    assertThat(utbetalingsperiode.vedtakdatoFom).isEqualTo(forventetUtbetalingsperiode.fom)
    assertThat(utbetalingsperiode.vedtakdatoTom).isEqualTo(forventetUtbetalingsperiode.tom)
    assertThat(utbetalingsperiode.opphør?.opphørDatoFom).isEqualTo(forventetUtbetalingsperiode.opphør)
}
