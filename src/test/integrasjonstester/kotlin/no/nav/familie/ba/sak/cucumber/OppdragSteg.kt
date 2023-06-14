package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.cucumber.ValideringUtil.assertSjekkBehandlingIder
import no.nav.familie.ba.sak.cucumber.domeneparser.DomenebegrepBehandlingsinformasjon
import no.nav.familie.ba.sak.cucumber.domeneparser.DomeneparserUtil.groupByBehandlingId
import no.nav.familie.ba.sak.cucumber.domeneparser.ForventetUtbetalingsoppdrag
import no.nav.familie.ba.sak.cucumber.domeneparser.ForventetUtbetalingsperiode
import no.nav.familie.ba.sak.cucumber.domeneparser.OppdragParser
import no.nav.familie.ba.sak.cucumber.domeneparser.OppdragParser.mapTilkjentYtelse
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriÅrMåned
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForIverksettingFactory
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForSimuleringFactory
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForUtbetalingsoppdrag
import no.nav.familie.ba.sak.integrasjoner.økonomi.IdentOgYtelse
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsoppdragGenerator
import no.nav.familie.ba.sak.integrasjoner.økonomi.oppdrag.IdentOgType
import no.nav.familie.ba.sak.integrasjoner.økonomi.oppdrag.UtbetalingsoppdragService
import no.nav.familie.ba.sak.integrasjoner.økonomi.pakkInnForUtbetaling
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.gjeldendeForrigeOffsetForKjede
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.grupperAndeler
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.oppdaterBeståendeAndelerMedOffset
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import java.time.YearMonth

class OppdragSteg {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val utbetalingsoppdragGenerator = UtbetalingsoppdragGenerator(mockk(relaxed = true))
    private var behandlinger = mapOf<Long, Behandling>()
    private var behandlingsinformasjon = mutableMapOf<Long, Behandlingsinformasjon>()
    private var tilkjenteYtelser = listOf<TilkjentYtelse>()
    private var tilkjenteYtelserNy = listOf<TilkjentYtelse>() // for å unngå krøll mellom gamle og nye
    private var beregnetUtbetalingsoppdrag = mutableMapOf<Long, Utbetalingsoppdrag>()
    private var beregnetUtbetalingsoppdragNy = mutableMapOf<Long, Utbetalingsoppdrag>()
    private var beregnetUtbetalingsoppdragSimulering = mutableMapOf<Long, Utbetalingsoppdrag>()
    private var beregnetUtbetalingsoppdragSimuleringNy = mutableMapOf<Long, Utbetalingsoppdrag>()

    private val tilkjentYtelseForBehandling = mutableMapOf<Long, TilkjentYtelse>()
    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>().apply {
        val repo = this
        every { repo.findByBehandling(any()) } answers { tilkjentYtelseForBehandling.getValue(firstArg()) }
        every { repo.save(any()) } answers {
            val tilkjentYtelse = firstArg<TilkjentYtelse>()
            tilkjentYtelseForBehandling[tilkjentYtelse.behandling.id] = tilkjentYtelse
            tilkjentYtelse
        }
        every { repo.sisteAndelPerKjedeForFagsak(any()) } answers {
            tilkjentYtelseForBehandling.filterValues { it.utbetalingsoppdrag != null }
                .flatMap { it.value.andelerTilkjentYtelse }
                .groupBy { IdentOgType(it.aktør.aktivFødselsnummer(), it.type) }
                .mapValues {
                    val maxOffset = it.value.filter { it.periodeOffset != null }.maxOfOrNull { it.periodeOffset!! }
                    it.value.filter { it.periodeOffset == maxOffset }.sortedBy { it.id }.first()
                }
        }
    }
    private val utbetalingsoppdragService = UtbetalingsoppdragService(tilkjentYtelseRepository)

    private val fagsak = defaultFagsak()

    @Gitt("følgende behandlingsinformasjon")
    fun følgendeBehandlinger(dataTable: DataTable) {
        opprettBehandlingsinformasjon(dataTable)
    }

    @Gitt("følgende tilkjente ytelser")
    fun følgendeTilkjenteYtelser(dataTable: DataTable) {
        genererBehandlinger(dataTable)
        tilkjenteYtelser = mapTilkjentYtelse(dataTable, behandlinger)
        if (tilkjenteYtelser.flatMap { it.andelerTilkjentYtelse }.any { it.kildeBehandlingId != null }) {
            error("Kildebehandling skal ikke settes på input, denne settes fra utbetalingsgeneratorn")
        }
        tilkjenteYtelserNy = mapTilkjentYtelse(dataTable, behandlinger)
        if (tilkjenteYtelserNy.flatMap { it.andelerTilkjentYtelse }.any { it.kildeBehandlingId != null }) {
            error("Kildebehandling skal ikke settes på input, denne settes fra utbetalingsgeneratorn")
        }
    }

    @Når("beregner utbetalingsoppdrag")
    fun `beregner utbetalingsoppdrag`() {
        tilkjenteYtelser.fold(emptyList<TilkjentYtelse>()) { acc, tilkjentYtelse ->
            val behandlingId = tilkjentYtelse.behandling.id
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
        tilkjenteYtelserNy.fold(emptyList<TilkjentYtelse>()) { acc, tilkjentYtelse ->
            val behandlingId = tilkjentYtelse.behandling.id
            tilkjentYtelseRepository.save(tilkjentYtelse)
            try {
                beregnUtbetalingsoppdragNy(behandlingId, acc.lastOrNull()?.behandling?.id)
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

        val vedtak = lagVedtak(behandling = tilkjentYtelse.behandling)
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
            endretMigreringsDato = hentMigreringsdato(tilkjentYtelse.behandling.id, tilkjenteYtelser),
        )
    }

    private fun beregnUtbetalingsoppdragNy(
        behandlingId: Long,
        forrigeBehandlingId: Long?,
        erSimulering: Boolean = false,
    ) {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId)
        val vedtak = lagVedtak(behandling = tilkjentYtelse.behandling)
        val utbetalingsoppdrag = utbetalingsoppdragService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
            behandlingId,
            vedtak,
            forrigeBehandlingId,
            erSimulering,
            hentMigreringsdato(behandlingId, tilkjenteYtelserNy)
        )
        beregnetUtbetalingsoppdragNy[behandlingId] = utbetalingsoppdrag
    }

    // Prøver å gjøre det samme som [ØkonomiService.beregnOmMigreringsDatoErEndret]
    private fun hentMigreringsdato(behandlingId: Long, tilkjentYtelser: List<TilkjentYtelse>): YearMonth? {
        val forrigeTilstand =
            tilkjentYtelser[behandlingId.toInt() - 1].andelerTilkjentYtelse.minOfOrNull { it.stønadFom }
        val minMigreringsdato =
            behandlingsinformasjon.filter { it.key <= behandlingId }.mapNotNull { it.value.endretMigreringsdato }
                .minOrNull()
        return if (forrigeTilstand != null && minMigreringsdato != null && forrigeTilstand > minMigreringsdato) {
            minMigreringsdato
        } else {
            null
        }
    }

    private fun maxOffsetPåFagsak(acc: List<TilkjentYtelse>) =
        acc.maxOfOrNull { ty ->
            ty.andelerTilkjentYtelse.filter { it.erAndelSomSkalSendesTilOppdrag() }.maxOfOrNull {
                it.periodeOffset ?: error(
                    "Mangler offset for behandling=${it.behandlingId} " +
                        "andel=${it.id} fom=${it.stønadFom} tom=${it.stønadTom}",
                )
            } ?: 0
        }

    @Så("forvent følgende utbetalingsoppdrag")
    fun `forvent følgende utbetalingsoppdrag`(dataTable: DataTable) {
        validerForventetUtbetalingsoppdrag(dataTable, beregnetUtbetalingsoppdrag)
        assertSjekkBehandlingIder(dataTable, beregnetUtbetalingsoppdrag)
    }

    @Så("forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator")
    fun `forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator`(dataTable: DataTable) {
        validerForventetUtbetalingsoppdrag(dataTable, beregnetUtbetalingsoppdragNy)
        assertSjekkBehandlingIder(dataTable, beregnetUtbetalingsoppdragNy)
    }

    @Så("forvent følgende simulering")
    fun `forvent følgende simulering`(dataTable: DataTable) {
        validerForventetUtbetalingsoppdrag(dataTable, beregnetUtbetalingsoppdragSimulering)
        assertSjekkBehandlingIder(dataTable, beregnetUtbetalingsoppdragSimulering)
        // TODO assert ny simulering
    }

    private fun validerForventetUtbetalingsoppdrag(
        dataTable: DataTable,
        beregnetUtbetalingsoppdrag: MutableMap<Long, Utbetalingsoppdrag>,
    ) {
        val forventedeUtbetalingsoppdrag = OppdragParser.mapForventetUtbetalingsoppdrag(dataTable)
        forventedeUtbetalingsoppdrag.forEach { forventetUtbetalingsoppdrag ->
            val behandlingId = forventetUtbetalingsoppdrag.behandlingId
            val utbetalingsoppdrag = beregnetUtbetalingsoppdrag[behandlingId]
                ?: error("Mangler utbetalingsoppdrag for $behandlingId")
            try {
                assertUtbetalingsoppdrag(forventetUtbetalingsoppdrag, utbetalingsoppdrag)
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
        behandlinger = dataTable.groupByBehandlingId()
            .map { lagBehandling(fagsak = fagsak).copy(id = it.key) }
            .associateBy { it.id }
        behandlinger.entries.forEach {
            if (!behandlingsinformasjon.containsKey(it.key)) {
                behandlingsinformasjon[it.key] = Behandlingsinformasjon()
            }
        }
    }

    private fun opprettBehandlingsinformasjon(dataTable: DataTable) {
        dataTable.groupByBehandlingId().map { (behandlingId, rader) ->
            val rad = rader.single()
            val endretMigreringsdato =
                parseValgfriÅrMåned(DomenebegrepBehandlingsinformasjon.ENDRET_MIGRERINGSDATO, rad)

            behandlingsinformasjon[behandlingId] = Behandlingsinformasjon(
                endretMigreringsdato = endretMigreringsdato
            )
        }
    }

    // @Gitt("følgende tilkjente ytelser uten andel for {}")

    private fun assertUtbetalingsoppdrag(
        forventetUtbetalingsoppdrag: ForventetUtbetalingsoppdrag,
        utbetalingsoppdrag: Utbetalingsoppdrag,
    ) {
        assertThat(utbetalingsoppdrag.kodeEndring).isEqualTo(forventetUtbetalingsoppdrag.kodeEndring)
        assertThat(utbetalingsoppdrag.utbetalingsperiode).hasSize(forventetUtbetalingsoppdrag.utbetalingsperiode.size)
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

private data class Behandlingsinformasjon(
    val endretMigreringsdato: YearMonth? = null
)

private fun assertUtbetalingsperiode(
    utbetalingsperiode: Utbetalingsperiode,
    forventetUtbetalingsperiode: ForventetUtbetalingsperiode,
) {
    assertThat(utbetalingsperiode.erEndringPåEksisterendePeriode)
        .`as`("erEndringPåEksisterendePeriode")
        .isEqualTo(forventetUtbetalingsperiode.erEndringPåEksisterendePeriode)
    assertThat(utbetalingsperiode.klassifisering)
        .`as`("klassifisering")
        .isEqualTo(forventetUtbetalingsperiode.ytelse.klassifisering)
    assertThat(utbetalingsperiode.periodeId)
        .`as`("periodeId")
        .isEqualTo(forventetUtbetalingsperiode.periodeId)
    assertThat(utbetalingsperiode.forrigePeriodeId)
        .`as`("forrigePeriodeId")
        .isEqualTo(forventetUtbetalingsperiode.forrigePeriodeId)
    assertThat(utbetalingsperiode.sats.toInt())
        .`as`("sats")
        .isEqualTo(forventetUtbetalingsperiode.sats)
    assertThat(utbetalingsperiode.satsType)
        .`as`("satsType")
        .isEqualTo(Utbetalingsperiode.SatsType.MND)
    assertThat(utbetalingsperiode.vedtakdatoFom)
        .`as`("fom")
        .isEqualTo(forventetUtbetalingsperiode.fom)
    assertThat(utbetalingsperiode.vedtakdatoTom)
        .`as`("tom")
        .isEqualTo(forventetUtbetalingsperiode.tom)
    assertThat(utbetalingsperiode.opphør?.opphørDatoFom)
        .`as`("opphør")
        .isEqualTo(forventetUtbetalingsperiode.opphør)
    forventetUtbetalingsperiode.kildebehandlingId?.let {
        assertThat(utbetalingsperiode.behandlingId)
            .`as`("kildebehandlingId")
            .isEqualTo(forventetUtbetalingsperiode.kildebehandlingId)
    }
}
