package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Og
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
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
import no.nav.familie.ba.sak.cucumber.mock.komponentMocks.mockFeatureToggleService
import no.nav.familie.ba.sak.cucumber.mock.mockAndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.BehandlingsinformasjonUtleder
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.EndretMigreringsdatoUtleder
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.KlassifiseringKorrigerer
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.UtbetalingsoppdragGenerator
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.tilUtbetalingsoppdragDto
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningTestUtil.sisteAndelPerIdentNy
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.felles.utbetalingsgenerator.Utbetalingsgenerator
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelMedPeriodeId
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.YearMonth

@Suppress("ktlint:standard:function-naming")
class OppdragSteg {
    var clockProvider = TestClockProvider(Clock.systemDefaultZone())

    var inneværendeMåned: YearMonth = YearMonth.now()

    private var behandlinger = mutableMapOf<Long, Behandling>()
    private var tilkjenteYtelser = mutableMapOf<Long, TilkjentYtelse>()
    private var beregnetUtbetalingsoppdrag = mutableMapOf<Long, BeregnetUtbetalingsoppdragLongId>()
    private var beregnetUtbetalingsoppdragSimulering = mutableMapOf<Long, BeregnetUtbetalingsoppdragLongId>()
    private var endretMigreringsdatoMap = mutableMapOf<Long, BehandlingMigreringsinfo>()
    private var kastedeFeil = mutableMapOf<Long, Exception>()
    private var toggles = mutableMapOf<Long, Map<String, Boolean>>()

    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val featureToggleService = mockFeatureToggleService()
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
            featureToggleService = featureToggleService,
            eksternBehandlingRelasjonService = mockk(),
        )

    private val logger = LoggerFactory.getLogger(javaClass)

    @Gitt("følgende feature toggles")
    fun følgendeFeatureToggles(dataTable: DataTable) {
        toggles =
            dataTable
                .groupByBehandlingId()
                .mapValues {
                    val map = mutableMapOf<String, Boolean>()
                    it.value.forEach { rad ->
                        val featureToggleId = parseString(Domenebegrep.FEATURE_TOGGLE_ID, rad)
                        val featureToggleVerdi = parseBoolean(Domenebegrep.ER_FEATURE_TOGGLE_TOGGLET_PÅ, rad)
                        map[featureToggleId] = featureToggleVerdi
                    }
                    map
                }.toMutableMap()
    }

    @Gitt("følgende tilkjente ytelser")
    fun følgendeTilkjenteYtelser(dataTable: DataTable) {
        genererBehandlinger(dataTable)
        tilkjenteYtelser = mapTilkjentYtelse(dataTable, behandlinger)
        if (tilkjenteYtelser.flatMap { (_, tilkjentYtelse) -> tilkjentYtelse.andelerTilkjentYtelse }.any { it.kildeBehandlingId != null }) {
            throw Feil("Kildebehandling skal ikke settes på input, denne settes fra utbetalingsgeneratorn")
        }
    }

    @Gitt("følgende behandlingsinformasjon")
    fun følgendeBehandlingsinformasjon(dataTable: DataTable) {
        endretMigreringsdatoMap =
            dataTable
                .groupByBehandlingId()
                .mapValues {
                    it.value
                        .map { entry: Map<String, String> -> BehandlingMigreringsinfo(behandling = lagBehandling(id = it.key), migreringsdato = parseÅrMåned(entry[Domenebegrep.ENDRET_MIGRERINGSDATO.nøkkel]!!).toLocalDate()) }
                        .single()
                }.toMutableMap()
    }

    @Når("beregner utbetalingsoppdrag")
    fun `beregner utbetalingsoppdrag`() {
        tilkjenteYtelser.values.fold(emptyList<TilkjentYtelse>()) { tidligereTilkjenteYtelser, tilkjentYtelse ->
            val behandlingId = tilkjentYtelse.behandling.id
            try {
                beregnetUtbetalingsoppdragSimulering[behandlingId] = beregnUtbetalingsoppdragNy(tidligereTilkjenteYtelser, tilkjentYtelse, true)
                beregnetUtbetalingsoppdrag[behandlingId] = beregnUtbetalingsoppdragNy(tidligereTilkjenteYtelser, tilkjentYtelse, false)
                oppdaterTilkjentYtelseMedUtbetalingsoppdrag(beregnetUtbetalingsoppdrag[behandlingId]!!, tilkjentYtelse)
            } catch (exception: Exception) {
                logger.error("Feilet beregning av oppdrag for behandling=$behandlingId")
                kastedeFeil[behandlingId] = exception
            }
            tidligereTilkjenteYtelser + tilkjentYtelse
        }
    }

    @Og("inneværende måned er {}")
    fun `inneværende måned er`(inneværendeMånedString: String) {
        inneværendeMåned = parseÅrMåned(inneværendeMånedString)
        clockProvider = TestClockProvider.lagClockProviderMedFastTidspunkt(inneværendeMåned)
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
        tidligereTilkjenteYtelser: List<TilkjentYtelse>,
        tilkjentYtelse: TilkjentYtelse,
        erSimulering: Boolean = false,
    ): BeregnetUtbetalingsoppdragLongId {
        every {
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(any())
        } returns tidligereTilkjenteYtelser.lastOrNull()?.behandling
        every {
            tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(any())
        } returns tidligereTilkjenteYtelser.lastOrNull()?.copy(utbetalingsoppdrag = jsonMapper.writeValueAsString(beregnetUtbetalingsoppdrag[tidligereTilkjenteYtelser.last().behandling.id]?.utbetalingsoppdrag))
        every {
            behandlingHentOgPersisterService.hentBehandlinger(any())
        } returns behandlinger.filter { it.value.fagsak.id == tilkjentYtelse.behandling.fagsak.id }.values.toList()
        every {
            andelTilkjentYtelseRepository.hentSisteAndelPerIdentOgType(any())
        } answers {
            sisteAndelPerIdentNy(tidligereTilkjenteYtelser).values.toList()
        }
        every {
            behandlingMigreringsinfoRepository.finnSisteBehandlingMigreringsInfoPåFagsak(any())
        } returns endretMigreringsdatoMap[tilkjentYtelse.behandling.id]
        every {
            tilkjentYtelseRepository.findByFagsak(any())
        } returns tidligereTilkjenteYtelser.filter { it.behandling.fagsak.id == tilkjentYtelse.behandling.fagsak.id }.map { it.copy(utbetalingsoppdrag = jsonMapper.writeValueAsString(beregnetUtbetalingsoppdrag[it.behandling.id]?.utbetalingsoppdrag)) }
        every {
            featureToggleService.isEnabled(
                any<FeatureToggle>(),
                any<Long>(),
            )
        } answers {
            val featureToggle = firstArg<FeatureToggle>()
            toggles[tilkjentYtelse.behandling.id]?.get(featureToggle.navn) ?: true
        }
        every { tilkjentYtelseRepository.harFagsakTattIBrukNyKlassekodeForUtvidetBarnetrygd(any()) } answers {
            beregnetUtbetalingsoppdrag.values.any { beregnetUtbetalingsoppdrag ->
                beregnetUtbetalingsoppdrag.utbetalingsoppdrag.utbetalingsperiode.any { utbetalingsperiode ->
                    utbetalingsperiode.klassifisering == "BAUTV-OP"
                }
            }
        }
        val vedtak = lagVedtak(behandling = tilkjentYtelse.behandling)
        val utbetalingsoppdragGenerator =
            UtbetalingsoppdragGenerator(
                Utbetalingsgenerator(),
                KlassifiseringKorrigerer(
                    tilkjentYtelseRepository,
                ),
                BehandlingsinformasjonUtleder(
                    EndretMigreringsdatoUtleder(
                        behandlingHentOgPersisterService,
                        behandlingMigreringsinfoRepository,
                        tilkjentYtelseRepository,
                    ),
                    clockProvider,
                ),
                andelTilkjentYtelseRepository,
                behandlingHentOgPersisterService,
                tilkjentYtelseRepository,
            )
        return utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
            saksbehandlerId = "saksbehandlerId",
            vedtak = vedtak,
            tilkjentYtelse = tilkjentYtelse,
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
                .mapValues { it.value.utbetalingsoppdrag.tilUtbetalingsoppdragDto() }
                .toMutableMap(),
        )
        assertSjekkBehandlingIder(
            dataTable,
            beregnetUtbetalingsoppdrag
                .mapValues { it.value.utbetalingsoppdrag.tilUtbetalingsoppdragDto() }
                .toMutableMap(),
        )
    }

    @Så("forvent følgende oppdaterte andeler")
    fun `forvent følgende oppdaterte andeler`(dataTable: DataTable) {
        validerForventedeOppdaterteAndeler(dataTable, tilkjenteYtelser)
    }

    private fun validerForventedeOppdaterteAndeler(
        dataTable: DataTable,
        tilkjenteYtelser: MutableMap<Long, TilkjentYtelse>,
    ) {
        val forventedeOppdaterteAndelerPerBehandling =
            OppdragParser.mapForventedeAndelerMedPeriodeId(
                dataTable,
            )

        forventedeOppdaterteAndelerPerBehandling.forEach { (behandlingId, forventedeAndelerMedPeriodeId) ->
            val tilkjentYtelse = tilkjenteYtelser[behandlingId] ?: throw Feil("Mangler TilkjentYtelse for behandling $behandlingId")
            val andelerMedPeriodeId =
                tilkjentYtelse.andelerTilkjentYtelse.filter { it.erAndelSomSkalSendesTilOppdrag() }.map {
                    AndelMedPeriodeId(
                        id = it.id.toString(),
                        periodeId = it.periodeOffset!!,
                        forrigePeriodeId = it.forrigePeriodeOffset,
                        kildeBehandlingId = it.kildeBehandlingId!!.toString(),
                    )
                }
            try {
                assertThat(andelerMedPeriodeId).isEqualTo(forventedeAndelerMedPeriodeId)
            } catch (e: Exception) {
                logger.error("Feilet validering av oppdaterte andeler for behandling $behandlingId", e)
                throw e
            }
        }
    }

    @Så("forvent følgende simulering")
    fun `forvent følgende simulering`(dataTable: DataTable) {
        validerForventetUtbetalingsoppdrag(
            dataTable,
            beregnetUtbetalingsoppdragSimulering
                .mapValues { it.value.utbetalingsoppdrag.tilUtbetalingsoppdragDto() }
                .toMutableMap(),
        )
        assertSjekkBehandlingIder(
            dataTable,
            beregnetUtbetalingsoppdragSimulering
                .mapValues { it.value.utbetalingsoppdrag.tilUtbetalingsoppdragDto() }
                .toMutableMap(),
        )
    }

    private fun validerForventetUtbetalingsoppdrag(
        dataTable: DataTable,
        beregnetUtbetalingsoppdrag: MutableMap<Long, Utbetalingsoppdrag>,
    ) {
        val forventedeUtbetalingsoppdrag =
            OppdragParser.mapForventetUtbetalingsoppdrag(
                dataTable,
            )
        forventedeUtbetalingsoppdrag.forEach { forventetUtbetalingsoppdrag ->
            val behandlingId = forventetUtbetalingsoppdrag.behandlingId
            val utbetalingsoppdrag =
                beregnetUtbetalingsoppdrag[behandlingId]
                    ?: throw Feil("Mangler utbetalingsoppdrag for $behandlingId")
            try {
                assertUtbetalingsoppdrag(forventetUtbetalingsoppdrag, utbetalingsoppdrag)
            } catch (exception: Throwable) {
                logger.error("Feilet validering av behandling $behandlingId")
                throw exception
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
                        behandlingType =
                            parseValgfriEnum<BehandlingType>(Domenebegrep.BEHANDLINGSTYPE, sisteRad)
                                ?: BehandlingType.FØRSTEGANGSBEHANDLING,
                        årsak =
                            parseValgfriEnum<BehandlingÅrsak>(Domenebegrep.BEHANDLINGSÅRSAK, sisteRad)
                                ?: BehandlingÅrsak.SØKNAD,
                    )
                }.toMutableMap()
    }

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
            } catch (exception: Throwable) {
                logger.error("Feilet validering av rad $index for oppdrag=${forventetUtbetalingsoppdrag.behandlingId}")
                throw exception
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
