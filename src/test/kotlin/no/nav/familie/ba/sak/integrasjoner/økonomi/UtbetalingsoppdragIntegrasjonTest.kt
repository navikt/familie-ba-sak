package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.dato
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.common.årMnd
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.ba.sak.økonomi.ØkonomiUtils
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class UtbetalingsoppdragIntegrasjonTest(
    @Autowired
    private val beregningService: BeregningService,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val økonomiService: ØkonomiService
) : AbstractSpringIntegrationTest() {

    lateinit var utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator

    @BeforeAll
    fun setUp() {
        databaseCleanupService.truncate()
        utbetalingsoppdragGenerator = UtbetalingsoppdragGenerator(beregningService)
    }

    @Test
    fun `skal opprette et nytt utbetalingsoppdrag med felles løpende periodeId og separat kjeding på to personer`() {
        val personMedFlerePerioder = tilfeldigPerson()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(personMedFlerePerioder.personIdent.ident)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val vedtak = lagVedtak(behandling = behandling)
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                "2019-04",
                "2023-03",
                YtelseType.SMÅBARNSTILLEGG,
                660,
                behandling,
                person = personMedFlerePerioder,
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                "2026-05",
                "2027-06",
                YtelseType.SMÅBARNSTILLEGG,
                660,
                behandling,
                person = personMedFlerePerioder,
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                "2019-03",
                "2037-02",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                tilkjentYtelse = tilkjentYtelse
            )
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelse)

        val utbetalingsoppdrag =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOpptaderTilkjentYtelse(
                "saksbehandler",
                vedtak,
                true,
                oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                    andelerTilkjentYtelse
                ),
            )

        assertEquals(Utbetalingsoppdrag.KodeEndring.NY, utbetalingsoppdrag.kodeEndring)
        assertEquals(3, utbetalingsoppdrag.utbetalingsperiode.size)

        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATR")[0],
            0,
            null,
            1054,
            "2019-03-01",
            "2037-02-28"
        )
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATRSMA")[0],
            1,
            null,
            660,
            "2019-04-01",
            "2023-03-31"
        )
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATRSMA")[1],
            2,
            1,
            660,
            "2026-05-01",
            "2027-06-30"
        )
    }

    @Test
    fun `skal opprette et fullstendig opphør for to personer, hvor opphørsdatoer blir første dato i hver kjede`() {
        val personMedFlerePerioder = tilfeldigPerson()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(personMedFlerePerioder.personIdent.ident)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val førsteDatoKjede1 = "2019-04"
        val førsteDatoKjede2 = "2019-03"
        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                førsteDatoKjede1,
                "2023-03",
                YtelseType.SMÅBARNSTILLEGG,
                660,
                behandling,
                person = personMedFlerePerioder,
                periodeIdOffset = 0
            ),
            lagAndelTilkjentYtelse(
                "2026-05",
                "2027-06",
                YtelseType.SMÅBARNSTILLEGG,
                660,
                behandling,
                person = personMedFlerePerioder,
                periodeIdOffset = 1
            ),
            lagAndelTilkjentYtelse(
                førsteDatoKjede2, "2037-02", YtelseType.ORDINÆR_BARNETRYGD, 1054, behandling,
                periodeIdOffset = 2
            )
        )

        val vedtak = lagVedtak(behandling = behandling)

        val utbetalingsoppdrag =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOpptaderTilkjentYtelse(
                saksbehandlerId = "saksbehandler",
                vedtak = vedtak,
                erFørsteBehandlingPåFagsak = false,
                forrigeKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                    andelerTilkjentYtelse
                )
            )

        assertEquals(Utbetalingsoppdrag.KodeEndring.ENDR, utbetalingsoppdrag.kodeEndring)
        assertEquals(2, utbetalingsoppdrag.utbetalingsperiode.size)

        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATRSMA")[0],
            1,
            null,
            660,
            "2026-05-01",
            "2027-06-30",
            årMnd(førsteDatoKjede1).førsteDagIInneværendeMåned()
        )
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATR")[0],
            2,
            null,
            1054,
            "2019-03-01",
            "2037-02-28",
            årMnd(førsteDatoKjede2).førsteDagIInneværendeMåned()
        )
    }

    @Test
    fun `skal opprette revurdering med endring på eksisterende periode`() {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak,
                førsteSteg = StegType.BEHANDLING_AVSLUTTET
            )
        )

        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val person = tilfeldigPerson()
        val vedtak = lagVedtak(behandling)
        val fomDatoSomEndres = "2033-01-01"
        val andelerFørstegangsbehandling = listOf(
            lagAndelTilkjentYtelse(
                "2020-01",
                "2029-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 0,
                person = person,
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                dato(fomDatoSomEndres).toYearMonth().toString(),
                "2034-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 1,
                person = person,
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                "2037-01",
                "2039-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 2,
                person = person,
                tilkjentYtelse = tilkjentYtelse
            )
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerFørstegangsbehandling)
        tilkjentYtelse.utbetalingsoppdrag = "Oppdrag"

        utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOpptaderTilkjentYtelse(
            "saksbehandler",
            vedtak,
            true,
            oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                andelerFørstegangsbehandling
            ),
        )

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val tilkjentYtelse2 = lagInitiellTilkjentYtelse(behandling2)
        val vedtak2 = lagVedtak(behandling2)
        val andelerRevurdering = listOf(
            lagAndelTilkjentYtelse(
                "2020-01",
                "2029-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 0,
                person = person,
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                "2034-01",
                "2034-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 3,
                person = person,
                tilkjentYtelse = tilkjentYtelse2
            ),
            lagAndelTilkjentYtelse(
                "2037-01",
                "2039-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 4,
                person = person,
                tilkjentYtelse = tilkjentYtelse2
            )
        )
        tilkjentYtelse2.andelerTilkjentYtelse.addAll(andelerRevurdering)
        val sisteOffsetPåFagsak = økonomiService.hentSisteOffsetPåFagsak(behandling = behandling2)

        val utbetalingsoppdrag =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOpptaderTilkjentYtelse(
                "saksbehandler",
                vedtak2,
                false,
                forrigeKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                    andelerFørstegangsbehandling
                ),
                sisteOffsetPåFagsak = sisteOffsetPåFagsak,
                oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                    andelerRevurdering
                )
            )

        assertEquals(Utbetalingsoppdrag.KodeEndring.ENDR, utbetalingsoppdrag.kodeEndring)
        assertEquals(3, utbetalingsoppdrag.utbetalingsperiode.size)

        val opphørsperiode = utbetalingsoppdrag.utbetalingsperiode.find { it.opphør != null }
        assertNotNull(opphørsperiode)
        val nyeUtbetalingsPerioderSortert =
            utbetalingsoppdrag.utbetalingsperiode.filter { it.opphør == null }.sortedBy { it.vedtakdatoFom }
        assertEquals(2, nyeUtbetalingsPerioderSortert.size)

        assertUtbetalingsperiode(
            opphørsperiode!!,
            2,
            1,
            1054,
            "2037-01-01",
            "2039-12-31",
            dato(fomDatoSomEndres)
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert.first(),
            3,
            2,
            1054,
            "2034-01-01",
            "2034-12-31"
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert.last(),
            4,
            3,
            1054,
            "2037-01-01",
            "2039-12-31"
        )
    }

    @Test
    fun `Skal opprette revurdering med nytt barn`() {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak,
                førsteSteg = StegType.BEHANDLING_AVSLUTTET
            )
        )
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val person = tilfeldigPerson()
        val vedtak = lagVedtak(behandling)
        val andelerFørstegangsbehandling = listOf(
            lagAndelTilkjentYtelse(
                "2020-01",
                "2029-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 0,
                person = person,
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                "2033-01",
                "2034-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 1,
                person = person,
                tilkjentYtelse = tilkjentYtelse
            )
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerFørstegangsbehandling)
        tilkjentYtelse.utbetalingsoppdrag = "Oppdrag"

        utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOpptaderTilkjentYtelse(
            "saksbehandler",
            vedtak,
            true,
            oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                andelerFørstegangsbehandling
            ),
        )

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val tilkjentYtelse2 = lagInitiellTilkjentYtelse(behandling2)
        val nyPerson = tilfeldigPerson()
        val vedtak2 = lagVedtak(behandling2)
        val andelerRevurdering = listOf(
            lagAndelTilkjentYtelse(
                "2022-01",
                "2034-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 2,
                person = nyPerson,
                tilkjentYtelse = tilkjentYtelse2
            ),
            lagAndelTilkjentYtelse(
                "2037-01",
                "2039-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 3,
                person = nyPerson,
                tilkjentYtelse = tilkjentYtelse2
            )
        )
        tilkjentYtelse2.andelerTilkjentYtelse.addAll(andelerRevurdering)
        val sisteOffsetPåFagsak = økonomiService.hentSisteOffsetPåFagsak(behandling = behandling2)

        val utbetalingsoppdrag =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOpptaderTilkjentYtelse(
                "saksbehandler",
                vedtak2,
                false,
                forrigeKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                    andelerFørstegangsbehandling
                ),
                sisteOffsetPåFagsak = sisteOffsetPåFagsak,
                oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                    andelerRevurdering
                )
            )

        assertEquals(Utbetalingsoppdrag.KodeEndring.ENDR, utbetalingsoppdrag.kodeEndring)
        assertEquals(2, utbetalingsoppdrag.utbetalingsperiode.size)
        val sorterteUtbetalingsperioder = utbetalingsoppdrag.utbetalingsperiode.sortedBy { it.periodeId }
        assertUtbetalingsperiode(sorterteUtbetalingsperioder.first(), 2, null, 1054, "2022-01-01", "2034-12-31")
        assertUtbetalingsperiode(sorterteUtbetalingsperioder.last(), 3, 2, 1054, "2037-01-01", "2039-12-31")
    }

    @Test
    fun `skal opprette et nytt utbetalingsoppdrag med to andeler på samme person og separat kjeding for småbarnstillegg`() {
        val personMedFlerePerioder = tilfeldigPerson()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(personMedFlerePerioder.personIdent.ident)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val vedtak = lagVedtak(behandling = behandling)
        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                "2019-04",
                "2023-03",
                YtelseType.SMÅBARNSTILLEGG,
                660,
                behandling,
                person = personMedFlerePerioder
            ),
            lagAndelTilkjentYtelse(
                "2026-05",
                "2027-06",
                YtelseType.SMÅBARNSTILLEGG,
                660,
                behandling,
                person = personMedFlerePerioder
            ),
            lagAndelTilkjentYtelse(
                "2019-03",
                "2037-02",
                YtelseType.UTVIDET_BARNETRYGD,
                1054,
                behandling,
                person = personMedFlerePerioder
            )
        )

        val utbetalingsoppdrag = utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOpptaderTilkjentYtelse(
            "saksbehandler",
            vedtak,
            true,
            oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                andelerTilkjentYtelse
            ),
        )

        assertEquals(Utbetalingsoppdrag.KodeEndring.NY, utbetalingsoppdrag.kodeEndring)
        assertEquals(3, utbetalingsoppdrag.utbetalingsperiode.size)

        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATR")[0],
            0,
            null,
            1054,
            "2019-03-01",
            "2037-02-28"
        )
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATRSMA")[0],
            1,
            null,
            660,
            "2019-04-01",
            "2023-03-31"
        )
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATRSMA")[1],
            2,
            1,
            660,
            "2026-05-01",
            "2027-06-30"
        )
    }

    @Test
    fun `opprettelse av utbetalingsoppdrag hvor flere har småbarnstillegg kaster feil`() {
        val behandling = lagBehandling()
        val vedtak = lagVedtak(behandling = behandling)
        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse("2019-04", "2023-03", YtelseType.SMÅBARNSTILLEGG, 660, behandling),
            lagAndelTilkjentYtelse("2026-05", "2027-06", YtelseType.SMÅBARNSTILLEGG, 660, behandling)
        )

        assertThrows<java.lang.IllegalArgumentException> {
            utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOpptaderTilkjentYtelse(
                "saksbehandler",
                vedtak,
                true,
                oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                    andelerTilkjentYtelse
                ),
            )
        }
    }

    @Test
    fun `Ved full betalingsoppdrag skal komplett utbetalinsoppdrag genereres også når ingen endring blitt gjort`() {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak,
                førsteSteg = StegType.BEHANDLING_AVSLUTTET
            )
        )

        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val person = tilfeldigPerson()
        val vedtak = lagVedtak(behandling)
        val andelerFørstegangsbehandling = listOf(
            lagAndelTilkjentYtelse(
                "2020-01",
                "2029-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 0,
                person = person,
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                "2030-01",
                "2034-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 1,
                person = person,
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                "2035-01",
                "2039-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 2,
                person = person,
                tilkjentYtelse = tilkjentYtelse
            )
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerFørstegangsbehandling)

        tilkjentYtelse.utbetalingsoppdrag = "Oppdrag"

        utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOpptaderTilkjentYtelse(
            "saksbehandler",
            vedtak,
            true,
            oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                andelerFørstegangsbehandling
            ),
        )

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val tilkjentYtelse2 = lagInitiellTilkjentYtelse(behandling2)
        val vedtak2 = lagVedtak(behandling2)
        val andelerRevurdering = listOf(
            lagAndelTilkjentYtelse(
                "2020-01",
                "2029-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 0,
                person = person,
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                "2030-01",
                "2034-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 3,
                person = person,
                tilkjentYtelse = tilkjentYtelse2
            ),
            lagAndelTilkjentYtelse(
                "2035-01",
                "2039-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 4,
                person = person,
                tilkjentYtelse = tilkjentYtelse2
            )
        )
        tilkjentYtelse2.andelerTilkjentYtelse.addAll(andelerRevurdering)
        val sisteOffsetPåFagsak = økonomiService.hentSisteOffsetPåFagsak(behandling = behandling2)

        val utbetalingsoppdrag =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOpptaderTilkjentYtelse(
                "saksbehandler",
                vedtak2,
                false,
                forrigeKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                    andelerFørstegangsbehandling
                ),
                sisteOffsetPåFagsak = sisteOffsetPåFagsak,
                oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                    andelerRevurdering
                ),
                erSimulering = true
            )

        assertEquals(Utbetalingsoppdrag.KodeEndring.ENDR, utbetalingsoppdrag.kodeEndring)
        assertEquals(4, utbetalingsoppdrag.utbetalingsperiode.size)

        val opphørsperiode = utbetalingsoppdrag.utbetalingsperiode.find { it.opphør != null }
        assertNotNull(opphørsperiode)
        val nyeUtbetalingsPerioderSortert =
            utbetalingsoppdrag.utbetalingsperiode.filter { it.opphør == null }.sortedBy { it.vedtakdatoFom }
        assertEquals(3, nyeUtbetalingsPerioderSortert.size)

        assertUtbetalingsperiode(
            opphørsperiode!!,
            2,
            1,
            1054,
            "2035-01-01",
            "2039-12-31",
            dato("2020-01-01")
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert.first(),
            3,
            2,
            1054,
            "2020-01-01",
            "2029-12-31"
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert[1],
            4,
            3,
            1054,
            "2030-01-01",
            "2034-12-31"
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert.last(),
            5,
            4,
            1054,
            "2035-01-01",
            "2039-12-31"
        )
    }

    @Test
    fun `Ved full betalingsoppdrag skal komplett utbetalinsoppdrag genereres også når bare siste periode blitt endrett`() {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak,
                førsteSteg = StegType.BEHANDLING_AVSLUTTET
            )
        )

        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val person = tilfeldigPerson()
        val vedtak = lagVedtak(behandling)
        val andelerFørstegangsbehandling = listOf(
            lagAndelTilkjentYtelse(
                "2020-01",
                "2029-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 0,
                person = person,
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                "2030-01",
                "2034-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 1,
                person = person,
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                "2035-01",
                "2039-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 2,
                person = person,
                tilkjentYtelse = tilkjentYtelse
            )
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerFørstegangsbehandling)
        tilkjentYtelse.utbetalingsoppdrag = "Oppdrag"

        utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOpptaderTilkjentYtelse(
            "saksbehandler",
            vedtak,
            true,
            oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                andelerFørstegangsbehandling
            ),
        )

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val tilkjentYtelse2 = lagInitiellTilkjentYtelse(behandling2)
        val vedtak2 = lagVedtak(behandling2)
        val andelerRevurdering = listOf(
            lagAndelTilkjentYtelse(
                "2020-01",
                "2029-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 0,
                person = person,
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                "2030-01",
                "2034-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 3,
                person = person,
                tilkjentYtelse = tilkjentYtelse2
            ),
            lagAndelTilkjentYtelse(
                "2035-01",
                "2038-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 4,
                person = person,
                tilkjentYtelse = tilkjentYtelse2
            )
        )
        tilkjentYtelse2.andelerTilkjentYtelse.addAll(andelerRevurdering)

        val sisteOffsetPåFagsak = økonomiService.hentSisteOffsetPåFagsak(behandling = behandling2)

        val utbetalingsoppdrag =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOpptaderTilkjentYtelse(
                "saksbehandler",
                vedtak2,
                false,
                forrigeKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                    andelerFørstegangsbehandling
                ),
                sisteOffsetPåFagsak = sisteOffsetPåFagsak,
                oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                    andelerRevurdering
                ),
                erSimulering = true
            )

        assertEquals(Utbetalingsoppdrag.KodeEndring.ENDR, utbetalingsoppdrag.kodeEndring)
        assertEquals(4, utbetalingsoppdrag.utbetalingsperiode.size)

        val opphørsperiode = utbetalingsoppdrag.utbetalingsperiode.find { it.opphør != null }
        assertNotNull(opphørsperiode)
        val nyeUtbetalingsPerioderSortert =
            utbetalingsoppdrag.utbetalingsperiode.filter { it.opphør == null }.sortedBy { it.vedtakdatoFom }
        assertEquals(3, nyeUtbetalingsPerioderSortert.size)

        assertUtbetalingsperiode(
            opphørsperiode!!,
            2,
            1,
            1054,
            "2035-01-01",
            "2039-12-31",
            dato("2020-01-01")
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert.first(),
            3,
            2,
            1054,
            "2020-01-01",
            "2029-12-31"
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert[1],
            4,
            3,
            1054,
            "2030-01-01",
            "2034-12-31"
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert.last(),
            5,
            4,
            1054,
            "2035-01-01",
            "2038-12-31"
        )
    }

    @Test
    fun `gerg`() {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak,
                førsteSteg = StegType.BEHANDLING_AVSLUTTET
            )
        )

        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val person = tilfeldigPerson()
        val vedtak = lagVedtak(behandling)
        val andelerFørstegangsbehandling = listOf(
            lagAndelTilkjentYtelse(
                "2020-01",
                "2029-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 0,
                person = person,
                tilkjentYtelse = tilkjentYtelse
            )
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerFørstegangsbehandling)
        tilkjentYtelse.utbetalingsoppdrag = "Oppdrag"

        utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOpptaderTilkjentYtelse(
            "saksbehandler",
            vedtak,
            true,
            oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                andelerFørstegangsbehandling
            ),
        )

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            (
                lagBehandling(
                    fagsak,
                    førsteSteg = StegType.BEHANDLING_AVSLUTTET
                )
                )
        )
        val tilkjentYtelse2 = lagInitiellTilkjentYtelse(behandling2)
        val andelerRevurdering = emptyList<AndelTilkjentYtelse>()
        tilkjentYtelse2.andelerTilkjentYtelse.addAll(andelerRevurdering)
        tilkjentYtelse2.utbetalingsoppdrag = "Oppdrag"

        utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOpptaderTilkjentYtelse(
            "saksbehandler",
            vedtak,
            false,
            oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                andelerRevurdering
            ),
        )

        val behandling3 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val tilkjentYtelse3 = lagInitiellTilkjentYtelse(behandling3)
        val andelerRevurdering2 = listOf(
            lagAndelTilkjentYtelse(
                "2020-01",
                "2029-12",
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling3,
                periodeIdOffset = 0,
                person = person,
                tilkjentYtelse = tilkjentYtelse
            )
        )
        tilkjentYtelse3.andelerTilkjentYtelse.addAll(andelerRevurdering2)

        assertEquals(0, økonomiService.hentSisteOffsetPåFagsak(behandling = behandling3))
    }

    private fun assertUtbetalingsperiode(
        utbetalingsperiode: Utbetalingsperiode,
        periodeId: Long,
        forrigePeriodeId: Long?,
        sats: Int,
        fom: String,
        tom: String,
        opphørFom: LocalDate? = null
    ) {
        assertEquals(periodeId, utbetalingsperiode.periodeId)
        assertEquals(forrigePeriodeId, utbetalingsperiode.forrigePeriodeId)
        assertEquals(sats, utbetalingsperiode.sats.toInt())
        assertEquals(dato(fom), utbetalingsperiode.vedtakdatoFom)
        assertEquals(dato(tom), utbetalingsperiode.vedtakdatoTom)
        if (opphørFom != null) {
            assertEquals(opphørFom, utbetalingsperiode.opphør?.opphørDatoFom)
        }
    }
}
