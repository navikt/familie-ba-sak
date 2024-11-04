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
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.cucumber.ValideringUtil.assertSjekkBehandlingIder
import no.nav.familie.ba.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.ba.sak.cucumber.domeneparser.DomeneparserUtil.groupByBehandlingId
import no.nav.familie.ba.sak.cucumber.domeneparser.ForventetUtbetalingsoppdrag
import no.nav.familie.ba.sak.cucumber.domeneparser.ForventetUtbetalingsperiode
import no.nav.familie.ba.sak.cucumber.domeneparser.OppdragParser
import no.nav.familie.ba.sak.cucumber.domeneparser.OppdragParser.mapTilkjentYtelse
import no.nav.familie.ba.sak.cucumber.domeneparser.parseBoolean
import no.nav.familie.ba.sak.cucumber.domeneparser.parseString
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriEnum
import no.nav.familie.ba.sak.cucumber.domeneparser.parseÅrMåned
import no.nav.familie.ba.sak.cucumber.mock.komponentMocks.mockUnleashNextMedContextService
import no.nav.familie.ba.sak.cucumber.mock.mockAndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.cucumber.mock.mockTilkjentYtelseRepository
import no.nav.familie.ba.sak.integrasjoner.økonomi.BehandlingsinformasjonUtleder
import no.nav.familie.ba.sak.integrasjoner.økonomi.JusterUtbetalingsoppdragService
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsoppdragGenerator
import no.nav.familie.ba.sak.integrasjoner.økonomi.tilRestUtbetalingsoppdrag
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.BeregningTestUtil.sisteAndelPerIdentNy
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.felles.utbetalingsgenerator.Utbetalingsgenerator
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import java.time.YearMonth

@Suppress("ktlint:standard:function-naming")
class OppdragSteg {
    private var behandlinger = mutableMapOf<Long, Behandling>()
    private var tilkjenteYtelser = mutableMapOf<Long, TilkjentYtelse>()
    private var beregnetUtbetalingsoppdrag = mutableMapOf<Long, BeregnetUtbetalingsoppdragLongId>()
    private var beregnetUtbetalingsoppdragSimulering = mutableMapOf<Long, BeregnetUtbetalingsoppdragLongId>()
    private var endretMigreringsdatoMap = mutableMapOf<Long, YearMonth>()
    private var kastedeFeil = mutableMapOf<Long, Exception>()
    private var toggles = mutableMapOf<Long, Map<String, Boolean>>()

    private val tilkjentYtelseRepository = mockTilkjentYtelseRepository(tilkjenteYtelser)
    private val unleashNextMedContextService = mockUnleashNextMedContextService()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val andelTilkjentYtelseRepository = mockAndelTilkjentYtelseRepository(tilkjenteYtelser, behandlinger)
    private val behandlingMigreringsinfoRepository = mockk<BehandlingMigreringsinfoRepository>()

    val behandlingService =
        BehandlingService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            behandlingstemaService = mockk(),
            behandlingSøknadsinfoService = mockk(),
            behandlingMigreringsinfoRepository = behandlingMigreringsinfoRepository,
            behandlingMetrikker = mockk(),
            saksstatistikkEventPublisher = mockk(),
            fagsakRepository = mockk(),
            vedtakRepository = mockk(),
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            loggService = mockk(),
            arbeidsfordelingService = mockk(),
            infotrygdService = mockk(),
            vedtaksperiodeService = mockk(),
            taskRepository = mockk(),
            vilkårsvurderingService = mockk(),
        )

    private val utbetalingsoppdragGenerator =
        UtbetalingsoppdragGenerator(
            Utbetalingsgenerator(),
            JusterUtbetalingsoppdragService(
                tilkjentYtelseRepository,
                unleashNextMedContextService,
            ),
            unleashNextMedContextService,
            BehandlingsinformasjonUtleder(
                behandlingHentOgPersisterService,
                behandlingService,
            ),
            andelTilkjentYtelseRepository,
            behandlingHentOgPersisterService,
            tilkjentYtelseRepository,
        )

    private val logger = LoggerFactory.getLogger(javaClass)

    @Gitt("følgende feature toggles")
    fun følgendeFeatureToggles(dataTable: DataTable) {
        toggles =
            dataTable
                .groupByBehandlingId()
                .mapValues {
                    val sisteInnslagPåBehandling = it.value.last()
                    val featureToggleId = parseString(Domenebegrep.FEATURE_TOGGLE_ID, sisteInnslagPåBehandling)
                    val featureToggleVerdi = parseBoolean(Domenebegrep.ER_FEATURE_TOGGLE_TOGGLET_PÅ, sisteInnslagPåBehandling)
                    mapOf(featureToggleId to featureToggleVerdi)
                }.toMutableMap()
    }

    @Gitt("følgende tilkjente ytelser")
    fun følgendeTilkjenteYtelser(dataTable: DataTable) {
        genererBehandlinger(dataTable)
        tilkjenteYtelser = mapTilkjentYtelse(dataTable, behandlinger)
        if (tilkjenteYtelser.flatMap { (_, tilkjentYtelse) -> tilkjentYtelse.andelerTilkjentYtelse }.any { it.kildeBehandlingId != null }) {
            error("Kildebehandling skal ikke settes på input, denne settes fra utbetalingsgeneratorn")
        }
    }

    @Gitt("følgende behandlingsinformasjon")
    fun `følgendeBehandlingsinformasjon`(dataTable: DataTable) {
        endretMigreringsdatoMap =
            dataTable
                .groupByBehandlingId()
                .mapValues {
                    it.value
                        .map { entry: Map<String, String> -> parseÅrMåned(entry[Domenebegrep.ENDRET_MIGRERINGSDATO.nøkkel]!!) }
                        .single()
                }.toMutableMap()
    }

    @Når("beregner utbetalingsoppdrag")
    fun `beregner utbetalingsoppdrag`() {
        tilkjenteYtelser.values.fold(emptyList<TilkjentYtelse>()) { acc, tilkjentYtelse ->
            val behandlingId = tilkjentYtelse.behandling.id
            try {
                beregnetUtbetalingsoppdragSimulering[behandlingId] = beregnUtbetalingsoppdragNy(acc, tilkjentYtelse, true)
                beregnetUtbetalingsoppdrag[behandlingId] = beregnUtbetalingsoppdragNy(acc, tilkjentYtelse, false)
                oppdaterTilkjentYtelseMedUtbetalingsoppdrag(
                    beregnetUtbetalingsoppdrag[behandlingId]!!,
                    tilkjentYtelse,
                )
            } catch (e: Exception) {
                logger.error("EXCEPTION 120391203910: ", e)
                logger.error("Feilet beregning av oppdrag for behandling=$behandlingId")
                kastedeFeil[behandlingId] = e
            }
            acc + tilkjentYtelse
        }
    }

    private fun oppdaterTilkjentYtelseMedUtbetalingsoppdrag(
        beregnetUtbetalingsoppdragLongId: BeregnetUtbetalingsoppdragLongId,
        tilkjentYtelse: TilkjentYtelse,
    ) {
        tilkjentYtelse.andelerTilkjentYtelse.forEach { andel ->
            val andelMedOppdatertOffset = beregnetUtbetalingsoppdragLongId.andeler.find { it.id == andel.id }
            if (andelMedOppdatertOffset != null) {
                andel.periodeOffset = andelMedOppdatertOffset.periodeId
                andel.forrigePeriodeOffset = andelMedOppdatertOffset.forrigePeriodeId
                andel.kildeBehandlingId = andelMedOppdatertOffset.kildeBehandlingId
            }
        }
    }

    private fun beregnUtbetalingsoppdragNy(
        acc: List<TilkjentYtelse>, // TODO : Remove me ?
        tilkjentYtelse: TilkjentYtelse,
        erSimulering: Boolean = false,
    ): BeregnetUtbetalingsoppdragLongId {
        every {
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(any())
        } returns acc.lastOrNull()?.behandling
        every {
            tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(any())
        } returns acc.lastOrNull()
        every {
            behandlingHentOgPersisterService.hentBehandlinger(any())
        } returns behandlinger.filter { it.value.fagsak.id == tilkjentYtelse.behandling.fagsak.id }.values.toList()
        every {
            andelTilkjentYtelseRepository.hentSisteAndelPerIdentOgType(any())
        } returns sisteAndelPerIdentNy(acc).values.toList()
        every {
            behandlingMigreringsinfoRepository.finnSisteMigreringsdatoPåFagsak(any())
        } returns endretMigreringsdatoMap[tilkjentYtelse.behandling.id]?.toLocalDate()
        val vedtak = lagVedtak(behandling = tilkjentYtelse.behandling)
        return utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
            saksbehandlerId = "saksbehandlerId",
            vedtak = vedtak,
            nyTilkjentYtelse = tilkjentYtelse,
            erSimulering = erSimulering,
        )
    }

    @Så("forvent at en exception kastes for behandling {long}")
    fun `forvent at en exception kastes for behandling`(behandlingId: Long) {
        assertThat(kastedeFeil).isNotEmpty
        assertThat(kastedeFeil[behandlingId]).isNotNull
    }

    @Så("forvent følgende utbetalingsoppdrag")
    fun `forvent følgende utbetalingsoppdrag`(dataTable: DataTable) {
        validerForventetUtbetalingsoppdrag(
            dataTable,
            beregnetUtbetalingsoppdrag
                .mapValues { it.value.utbetalingsoppdrag.tilRestUtbetalingsoppdrag() }
                .toMutableMap(),
        )
        assertSjekkBehandlingIder(
            dataTable,
            beregnetUtbetalingsoppdrag
                .mapValues { it.value.utbetalingsoppdrag.tilRestUtbetalingsoppdrag() }
                .toMutableMap(),
        )
    }

    @Så("forvent følgende simulering")
    fun `forvent følgende simulering`(dataTable: DataTable) {
        validerForventetUtbetalingsoppdrag(
            dataTable,
            beregnetUtbetalingsoppdragSimulering
                .mapValues { it.value.utbetalingsoppdrag.tilRestUtbetalingsoppdrag() }
                .toMutableMap(),
        )
        assertSjekkBehandlingIder(
            dataTable,
            beregnetUtbetalingsoppdragSimulering
                .mapValues { it.value.utbetalingsoppdrag.tilRestUtbetalingsoppdrag() }
                .toMutableMap(),
        )
    }

    private fun validerForventetUtbetalingsoppdrag(
        dataTable: DataTable,
        beregnetUtbetalingsoppdrag: MutableMap<Long, Utbetalingsoppdrag>,
    ) {
        val medUtbetalingsperiode = true // TODO? Burde denne kunne sendes med som et flagg? Hva gjør den?
        val forventedeUtbetalingsoppdrag =
            OppdragParser.mapForventetUtbetalingsoppdrag(
                dataTable,
            )
        forventedeUtbetalingsoppdrag.forEach { forventetUtbetalingsoppdrag ->
            val behandlingId = forventetUtbetalingsoppdrag.behandlingId
            val utbetalingsoppdrag =
                beregnetUtbetalingsoppdrag[behandlingId]
                    ?: error("Mangler utbetalingsoppdrag for $behandlingId")
            try {
                assertUtbetalingsoppdrag(forventetUtbetalingsoppdrag, utbetalingsoppdrag, medUtbetalingsperiode)
            } catch (e: Throwable) {
                logger.error("Feilet validering av behandling $behandlingId")
                throw e
            }
        }
    }

    private fun genererBehandlinger(dataTable: DataTable) {
        val fagsak = defaultFagsak()
        behandlinger =
            dataTable
                .groupByBehandlingId()
                .mapValues {
                    val sisteRad = it.value.last()
                    lagBehandling(
                        id = it.key,
                        fagsak = fagsak,
                        behandlingType = parseValgfriEnum<BehandlingType>(Domenebegrep.BEHANDLINGSTYPE, sisteRad) ?: BehandlingType.FØRSTEGANGSBEHANDLING,
                    )
                }.toMutableMap()
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
