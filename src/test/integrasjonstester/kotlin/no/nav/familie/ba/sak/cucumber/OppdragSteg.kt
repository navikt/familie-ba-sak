package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.mockk
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.cucumber.ValideringUtil.assertSjekkBehandlingIder
import no.nav.familie.ba.sak.cucumber.domeneparser.DomeneparserUtil.groupByBehandlingId
import no.nav.familie.ba.sak.cucumber.domeneparser.ForventetUtbetalingsoppdrag
import no.nav.familie.ba.sak.cucumber.domeneparser.ForventetUtbetalingsperiode
import no.nav.familie.ba.sak.cucumber.domeneparser.OppdragParser
import no.nav.familie.ba.sak.cucumber.domeneparser.OppdragParser.mapTilkjentYtelse
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForIverksettingFactory
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForSimuleringFactory
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForUtbetalingsoppdrag
import no.nav.familie.ba.sak.integrasjoner.økonomi.IdentOgYtelse
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsoppdragGenerator
import no.nav.familie.ba.sak.integrasjoner.økonomi.pakkInnForUtbetaling
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.gjeldendeForrigeOffsetForKjede
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.grupperAndeler
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.oppdaterBeståendeAndelerMedOffset
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory

class OppdragSteg {

    private val utbetalingsoppdragGenerator = UtbetalingsoppdragGenerator(mockk(relaxed = true))
    private var behandlinger = mapOf<Long, Behandling>()
    private var tilkjenteYtelser = listOf<TilkjentYtelse>()
    private var beregnetUtbetalingsoppdrag = mutableMapOf<Long, Utbetalingsoppdrag>()
    private var beregnetUtbetalingsoppdragSimulering = mutableMapOf<Long, Utbetalingsoppdrag>()

    private val logger = LoggerFactory.getLogger(javaClass)

    @Gitt("følgende tilkjente ytelser")
    fun følgendeTilkjenteYtelser(dataTable: DataTable) {
        genererBehandlinger(dataTable)
        tilkjenteYtelser = mapTilkjentYtelse(dataTable, behandlinger)
        if (tilkjenteYtelser.flatMap { it.andelerTilkjentYtelse }.any { it.kildeBehandlingId != null }) {
            error("Kildebehandling skal ikke settes på input, denne settes fra utbetalingsgeneratorn")
        }
    }

    @Når("beregner utbetalingsoppdrag")
    fun `beregner utbetalingsoppdrag`() {
        tilkjenteYtelser.fold(emptyList<TilkjentYtelse>()) { acc, tilkjentYtelse ->
            val behandlingId = tilkjentYtelse.behandlingId
            try {
                beregnetUtbetalingsoppdragSimulering[behandlingId] =
                    beregnUtbetalingsoppdrag(acc, tilkjentYtelse, erSimulering = true)
                beregnetUtbetalingsoppdrag[behandlingId] = beregnUtbetalingsoppdrag(acc, tilkjentYtelse)
            } catch (e: Throwable) {
                logger.error("Feilet beregning av oppdrag for behandling=$behandlingId")
                throw e
            }
            acc + tilkjentYtelse
        }
    }

    private fun beregnUtbetalingsoppdrag(
        acc: List<TilkjentYtelse>,
        tilkjentYtelse: TilkjentYtelse,
        erSimulering: Boolean = false,
    ): Utbetalingsoppdrag {
        val forrigeTilkjentYtelse = acc.lastOrNull()

        val vedtak = lagVedtak(behandling = behandlinger.getValue(tilkjentYtelse.behandlingId))
        val forrigeKjeder = tilKjeder(forrigeTilkjentYtelse, erSimulering)
        val oppdaterteKjeder = tilKjeder(tilkjentYtelse, erSimulering)
        val sisteOffsetPåFagsak = maxOffsetPåFagsak(acc)
        val sisteOffsetPerIdent = gjeldendeForrigeOffsetForKjede(forrigeKjeder)
        oppdaterBeståendeAndelerMedOffset(oppdaterteKjeder, forrigeKjeder)
        return utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
            saksbehandlerId = "saksbehandlerId",
            vedtak = vedtak,
            erFørsteBehandlingPåFagsak = forrigeTilkjentYtelse == null,
            forrigeKjeder = forrigeKjeder,
            sisteOffsetPerIdent = sisteOffsetPerIdent,
            sisteOffsetPåFagsak = sisteOffsetPåFagsak?.toInt(),
            oppdaterteKjeder = oppdaterteKjeder,
            erSimulering = erSimulering,
            endretMigreringsDato = null,
        )
    }

    private fun maxOffsetPåFagsak(acc: List<TilkjentYtelse>) =
        acc.maxOfOrNull { ty ->
            ty.andelerTilkjentYtelse.maxOfOrNull {
                it.periodeOffset ?: error(
                    "Mangler offset for behandling=${it.behandlingId} " +
                        "andel=${it.id} fom=${it.stønadFom} tom=${it.stønadTom}",
                )
            } ?: 0
        }

    @Så("forvent følgende utbetalingsoppdrag")
    fun `forvent følgende utbetalingsoppdrag`(dataTable: DataTable) {
        validerForventetUtbetalingsoppdrag(dataTable, beregnetUtbetalingsoppdrag)
        assertSjekkBehandlingIder(dataTable, beregnetUtbetalingsoppdrag.keys)
    }

    @Så("forvent følgende simulering")
    fun `forvent følgende simulering`(dataTable: DataTable) {
        validerForventetUtbetalingsoppdrag(dataTable, beregnetUtbetalingsoppdragSimulering)
        assertSjekkBehandlingIder(dataTable, beregnetUtbetalingsoppdragSimulering.keys)
    }

    private fun validerForventetUtbetalingsoppdrag(
        dataTable: DataTable,
        beregnetUtbetalingsoppdrag: MutableMap<Long, Utbetalingsoppdrag>,
    ) {
        val medUtbetalingsperiode = true // TODO? Burde denne kunne sendes med som et flagg? Hva gjør den?
        val forventedeUtbetalingsoppdrag = OppdragParser.mapForventetUtbetalingsoppdrag(
            dataTable,
            medUtbetalingsperiode = medUtbetalingsperiode,
        )
        forventedeUtbetalingsoppdrag.forEach { forventetUtbetalingsoppdrag ->
            val behandlingId = forventetUtbetalingsoppdrag.behandlingId
            val utbetalingsoppdrag = beregnetUtbetalingsoppdrag[behandlingId]
                ?: error("Mangler utbetalingsoppdrag for $behandlingId")
            try {
                assertUtbetalingsoppdrag(forventetUtbetalingsoppdrag, utbetalingsoppdrag, medUtbetalingsperiode)
            } catch (e: Throwable) {
                logger.error("Feilet validering av behandling $behandlingId")
                throw e
            }
        }
    }

    private fun tilKjeder(
        tilkjentYtelse: TilkjentYtelse?,
        erSimulering: Boolean = false,
    ): Map<IdentOgYtelse, List<AndelTilkjentYtelseForUtbetalingsoppdrag>> {
        val andelFactory = if (erSimulering) {
            AndelTilkjentYtelseForSimuleringFactory()
        } else {
            AndelTilkjentYtelseForIverksettingFactory()
        }

        return (tilkjentYtelse?.andelerTilkjentYtelse ?: emptyList())
            .filter { it.erAndelSomSkalSendesTilOppdrag() }
            .pakkInnForUtbetaling(andelFactory)
            .let { grupperAndeler(it) }
    }

    private fun genererBehandlinger(dataTable: DataTable) {
        val fagsak = defaultFagsak()
        behandlinger = dataTable.groupByBehandlingId()
            .map { lagBehandling(fagsak = fagsak).copy(id = it.key) }
            .associateBy { it.id }
    }

    // @Gitt("følgende tilkjente ytelser uten andel for {}")

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
                try {
                    assertUtbetalingsperiode(utbetalingsperiode, forventetUtbetalingsperiode)
                } catch (e: Throwable) {
                    logger.error("Feilet validering av rad $index for oppdrag=${forventetUtbetalingsoppdrag.behandlingId}")
                    throw e
                }
            }
        }
    }
}

private fun assertUtbetalingsperiode(
    utbetalingsperiode: Utbetalingsperiode,
    forventetUtbetalingsperiode: ForventetUtbetalingsperiode,
) {
    assertThat(utbetalingsperiode.erEndringPåEksisterendePeriode)
        .isEqualTo(forventetUtbetalingsperiode.erEndringPåEksisterendePeriode)
    assertThat(utbetalingsperiode.klassifisering).isEqualTo(forventetUtbetalingsperiode.ytelse.klassifisering)
    assertThat(utbetalingsperiode.periodeId).isEqualTo(forventetUtbetalingsperiode.periodeId)
    assertThat(utbetalingsperiode.forrigePeriodeId).isEqualTo(forventetUtbetalingsperiode.forrigePeriodeId)
    assertThat(utbetalingsperiode.sats.toInt()).isEqualTo(forventetUtbetalingsperiode.sats)
    assertThat(utbetalingsperiode.satsType).isEqualTo(Utbetalingsperiode.SatsType.MND)
    assertThat(utbetalingsperiode.vedtakdatoFom).isEqualTo(forventetUtbetalingsperiode.fom)
    assertThat(utbetalingsperiode.vedtakdatoTom).isEqualTo(forventetUtbetalingsperiode.tom)
    assertThat(utbetalingsperiode.opphør?.opphørDatoFom).isEqualTo(forventetUtbetalingsperiode.opphør)
    forventetUtbetalingsperiode.kildebehandlingId?.let {
        assertThat(utbetalingsperiode.behandlingId).isEqualTo(forventetUtbetalingsperiode.kildebehandlingId)
    }
}
