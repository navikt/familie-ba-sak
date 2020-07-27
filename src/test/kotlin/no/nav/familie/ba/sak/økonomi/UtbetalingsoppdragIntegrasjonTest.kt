package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.YtelseType.*
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDate.now

@SpringBootTest
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UtbetalingsoppdragIntegrasjonTest(
        @Autowired
        private val persongrunnlagService: PersongrunnlagService,

        @Autowired
        private val beregningService: BeregningService,

        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val databaseCleanupService: DatabaseCleanupService
) {

    lateinit var utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator

    @BeforeAll
    fun setUp() {
        databaseCleanupService.truncate()
        utbetalingsoppdragGenerator = UtbetalingsoppdragGenerator(persongrunnlagService, beregningService)
    }

    @Test
    fun `skal opprette et nytt utbetalingsoppdrag med felles løpende periodeId og separat kjeding på to personer`() {
        val personMedFlerePerioder = tilfeldigPerson()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(personMedFlerePerioder.personIdent.ident)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val vedtak = lagVedtak(behandling = behandling)
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val andelerTilkjentYtelse = listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2023-03-31",
                                       SMÅBARNSTILLEGG,
                                       660,
                                       behandling,
                                       person = personMedFlerePerioder,
                                       tilkjentYtelse = tilkjentYtelse),
                lagAndelTilkjentYtelse("2026-05-01",
                                       "2027-06-30",
                                       SMÅBARNSTILLEGG,
                                       660,
                                       behandling,
                                       person = personMedFlerePerioder,
                                       tilkjentYtelse = tilkjentYtelse),
                lagAndelTilkjentYtelse("2019-03-01",
                                       "2037-02-28",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       behandling,
                                       tilkjentYtelse = tilkjentYtelse)
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelse)

        val behandlingResultatType = BehandlingResultatType.INNVILGET
        val utbetalingsoppdrag =
                utbetalingsoppdragGenerator.lagUtbetalingsoppdrag("saksbehandler",
                                                                  vedtak,
                                                                  behandlingResultatType,
                                                                  true,
                                                                  oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                                                                          andelerTilkjentYtelse))

        assertEquals(Utbetalingsoppdrag.KodeEndring.NY, utbetalingsoppdrag.kodeEndring)
        assertEquals(3, utbetalingsoppdrag.utbetalingsperiode.size)

        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATR"]!![0], 0, null, 1054, "2019-03-01", "2037-02-28")
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATRSMA"]!![0], 1, null, 660, "2019-04-01", "2023-03-31")
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATRSMA"]!![1], 2, 1, 660, "2026-05-01", "2027-06-30")
    }

    @Test
    fun `skal opprette et fullstendig opphør med felles løpende periodeId og separat kjeding på to personer`() {
        val behandling = lagBehandling()
        val vedtak = lagVedtak(behandling = behandling)
        val personMedFlerePerioder = tilfeldigPerson()
        val andelerTilkjentYtelse = listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2023-03-31",
                                       SMÅBARNSTILLEGG,
                                       660,
                                       behandling,
                                       person = personMedFlerePerioder,
                                       periodeIdOffset = 0),
                lagAndelTilkjentYtelse("2026-05-01",
                                       "2027-06-30",
                                       SMÅBARNSTILLEGG,
                                       660,
                                       behandling,
                                       person = personMedFlerePerioder,
                                       periodeIdOffset = 1),
                lagAndelTilkjentYtelse("2019-03-01", "2037-02-28", ORDINÆR_BARNETRYGD, 1054, behandling,
                                       periodeIdOffset = 2))

        val opphørFom = now()
        val opphørVedtak = lagVedtak(forrigeVedtak = vedtak, opphørsdato = opphørFom)
        val behandlingResultatType = BehandlingResultatType.OPPHØRT

        val utbetalingsoppdrag =
                utbetalingsoppdragGenerator.lagUtbetalingsoppdrag("saksbehandler",
                                                                  opphørVedtak,
                                                                  behandlingResultatType,
                                                                  false,
                                                                  forrigeKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                                                                          andelerTilkjentYtelse))

        assertEquals(Utbetalingsoppdrag.KodeEndring.UEND, utbetalingsoppdrag.kodeEndring)
        assertEquals(2, utbetalingsoppdrag.utbetalingsperiode.size)


        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATRSMA"]!![0],
                                 1,
                                 null,
                                 660,
                                 "2026-05-01",
                                 "2027-06-30",
                                 opphørFom)
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATR"]!![0],
                                 2,
                                 null,
                                 1054,
                                 "2019-03-01",
                                 "2037-02-28",
                                 opphørFom)
    }


    @Test
    fun `skal opprette et fullstendig opphør hvor periodens fom-dato er opphørsdato når denne er senere`() {
        val behandling = lagBehandling()
        val vedtak = lagVedtak(behandling)
        val andelerTilkjentYtelse = listOf(
                lagAndelTilkjentYtelse("2010-03-01", "2030-02-28", ORDINÆR_BARNETRYGD, 1054, behandling, periodeIdOffset = 0),
                lagAndelTilkjentYtelse("2025-01-01", "2030-02-28", ORDINÆR_BARNETRYGD, 1054, behandling, periodeIdOffset = 1))

        val opphørFom = dato("2020-01-01")
        val opphørVedtak = lagVedtak(forrigeVedtak = vedtak, opphørsdato = opphørFom)
        val behandlingResultatType = BehandlingResultatType.OPPHØRT

        val utbetalingsoppdrag =
                utbetalingsoppdragGenerator.lagUtbetalingsoppdrag("saksbehandler",
                                                                  opphørVedtak,
                                                                  behandlingResultatType,
                                                                  false,
                                                                  forrigeKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                                                                          andelerTilkjentYtelse))

        assertEquals(Utbetalingsoppdrag.KodeEndring.UEND, utbetalingsoppdrag.kodeEndring)
        assertEquals(2, utbetalingsoppdrag.utbetalingsperiode.size)


        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATR"]!![0],
                                 0,
                                 null,
                                 1054,
                                 "2010-03-01",
                                 "2030-02-28",
                                 opphørFom)
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATR"]!![1],
                                 1,
                                 null,
                                 1054,
                                 "2025-01-01",
                                 "2030-02-28",
                                 dato("2025-01-01"))
    }

    @Test
    fun `skal opprette revurdering med endring på eksisterende periode`() {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val tilkjentYtelse2 = lagInitiellTilkjentYtelse(behandling2)
        val person = tilfeldigPerson()
        val vedtak = lagVedtak(behandling)
        val fomDatoSomEndres = "2033-01-01"
        val andelerFørstegangsbehandling = listOf(
                lagAndelTilkjentYtelse("2020-01-01",
                                       "2029-12-31",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       behandling,
                                       periodeIdOffset = 0,
                                       person = person,
                                       tilkjentYtelse = tilkjentYtelse),
                lagAndelTilkjentYtelse(fomDatoSomEndres,
                                       "2034-12-31",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       behandling,
                                       periodeIdOffset = 1,
                                       person = person,
                                       tilkjentYtelse = tilkjentYtelse),
                lagAndelTilkjentYtelse("2037-01-01",
                                       "2039-12-31",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       behandling,
                                       periodeIdOffset = 2,
                                       person = person,
                                       tilkjentYtelse = tilkjentYtelse))
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerFørstegangsbehandling)
        utbetalingsoppdragGenerator.lagUtbetalingsoppdrag("saksbehandler",
                                                          vedtak,
                                                          BehandlingResultatType.INNVILGET,
                                                          true,
                                                          oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                                                                  andelerFørstegangsbehandling))

        val andelerRevurdering = listOf(
                lagAndelTilkjentYtelse("2020-01-01",
                                       "2029-12-31",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       behandling,
                                       periodeIdOffset = 0,
                                       person = person,
                                       tilkjentYtelse = tilkjentYtelse),
                lagAndelTilkjentYtelse("2034-01-01",
                                       "2034-12-31",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       behandling2,
                                       periodeIdOffset = 3,
                                       person = person,
                                       tilkjentYtelse = tilkjentYtelse2),
                lagAndelTilkjentYtelse("2037-01-01",
                                       "2039-12-31",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       behandling2,
                                       periodeIdOffset = 4,
                                       person = person,
                                       tilkjentYtelse = tilkjentYtelse2))
        tilkjentYtelse2.andelerTilkjentYtelse.addAll(andelerRevurdering)

        val behandlingResultatType = BehandlingResultatType.DELVIS_INNVILGET

        val utbetalingsoppdrag =
                utbetalingsoppdragGenerator.lagUtbetalingsoppdrag("saksbehandler",
                                                                  vedtak,
                                                                  behandlingResultatType,
                                                                  false,
                                                                  forrigeKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                                                                          andelerFørstegangsbehandling),
                                                                  oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                                                                          andelerRevurdering))

        assertEquals(Utbetalingsoppdrag.KodeEndring.ENDR, utbetalingsoppdrag.kodeEndring)
        assertEquals(3, utbetalingsoppdrag.utbetalingsperiode.size)

        val opphørsperiode = utbetalingsoppdrag.utbetalingsperiode.find { it.opphør != null }
        assertNotNull(opphørsperiode)
        val nyeUtbetalingsPerioderSortert =
                utbetalingsoppdrag.utbetalingsperiode.filter { it.opphør == null }.sortedBy { it.vedtakdatoFom }
        assertEquals(2, nyeUtbetalingsPerioderSortert.size)

        assertUtbetalingsperiode(opphørsperiode!!,
                                 2,
                                 1,
                                 1054,
                                 "2037-01-01",
                                 "2039-12-31",
                                 dato(fomDatoSomEndres))
        assertUtbetalingsperiode(nyeUtbetalingsPerioderSortert.first(),
                                 3,
                                 2,
                                 1054,
                                 "2034-01-01",
                                 "2034-12-31")
        assertUtbetalingsperiode(nyeUtbetalingsPerioderSortert.last(),
                                 4,
                                 3,
                                 1054,
                                 "2037-01-01",
                                 "2039-12-31")
    }

    @Test
    fun `Skal opprette revurdering med nytt barn`() {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val person = tilfeldigPerson()
        val vedtak = lagVedtak(behandling)
        val andelerFørstegangsbehandling = listOf(
                lagAndelTilkjentYtelse("2020-01-01",
                                       "2029-12-31",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       behandling,
                                       periodeIdOffset = 0,
                                       person = person,
                                       tilkjentYtelse = tilkjentYtelse),
                lagAndelTilkjentYtelse("2033-01-01",
                                       "2034-12-31",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       behandling,
                                       periodeIdOffset = 1,
                                       person = person,
                                       tilkjentYtelse = tilkjentYtelse))
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerFørstegangsbehandling)
        utbetalingsoppdragGenerator.lagUtbetalingsoppdrag("saksbehandler",
                                                          vedtak,
                                                          BehandlingResultatType.INNVILGET,
                                                          true,
                                                          oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                                                                  andelerFørstegangsbehandling))

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val tilkjentYtelse2 = lagInitiellTilkjentYtelse(behandling2)
        val nyPerson = tilfeldigPerson()
        val vedtak2 = lagVedtak(behandling2)
        val andelerRevurdering = listOf(
                lagAndelTilkjentYtelse("2022-01-01",
                                       "2034-12-31",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       behandling2,
                                       periodeIdOffset = 2,
                                       person = nyPerson,
                                       tilkjentYtelse = tilkjentYtelse2),
                lagAndelTilkjentYtelse("2037-01-01",
                                       "2039-12-31",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       behandling2,
                                       periodeIdOffset = 3,
                                       person = nyPerson,
                                       tilkjentYtelse = tilkjentYtelse2))
        tilkjentYtelse2.andelerTilkjentYtelse.addAll(andelerRevurdering)

        val utbetalingsoppdrag =
                utbetalingsoppdragGenerator.lagUtbetalingsoppdrag("saksbehandler",
                                                                  vedtak2,
                                                                  BehandlingResultatType.INNVILGET,
                                                                  false,
                                                                  forrigeKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                                                                          andelerFørstegangsbehandling),
                                                                  oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                                                                          andelerRevurdering))

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
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2023-03-31",
                                       SMÅBARNSTILLEGG,
                                       660,
                                       behandling,
                                       person = personMedFlerePerioder),
                lagAndelTilkjentYtelse("2026-05-01",
                                       "2027-06-30",
                                       SMÅBARNSTILLEGG,
                                       660,
                                       behandling,
                                       person = personMedFlerePerioder),
                lagAndelTilkjentYtelse("2019-03-01",
                                       "2037-02-28",
                                       UTVIDET_BARNETRYGD,
                                       1054,
                                       behandling,
                                       person = personMedFlerePerioder))

        val behandlingResultatType = BehandlingResultatType.INNVILGET
        val utbetalingsoppdrag = utbetalingsoppdragGenerator.lagUtbetalingsoppdrag("saksbehandler",
                                                                                   vedtak,
                                                                                   behandlingResultatType,
                                                                                   true,
                                                                                   oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                                                                                           andelerTilkjentYtelse))

        assertEquals(Utbetalingsoppdrag.KodeEndring.NY, utbetalingsoppdrag.kodeEndring)
        assertEquals(3, utbetalingsoppdrag.utbetalingsperiode.size)

        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATR"]!![0], 0, null, 1054, "2019-03-01", "2037-02-28")
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATRSMA"]!![0], 1, null, 660, "2019-04-01", "2023-03-31")
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATRSMA"]!![1], 2, 1, 660, "2026-05-01", "2027-06-30")
    }

    @Test
    fun `opprettelse av utbetalingsoppdrag hvor flere har småbarnstillegg kaster feil`() {
        val behandling = lagBehandling()
        val vedtak = lagVedtak(behandling = behandling)
        val andelerTilkjentYtelse = listOf(
                lagAndelTilkjentYtelse("2019-04-01", "2023-03-31", SMÅBARNSTILLEGG, 660, behandling),
                lagAndelTilkjentYtelse("2026-05-01", "2027-06-30", SMÅBARNSTILLEGG, 660, behandling))

        val behandlingResultatType = BehandlingResultatType.INNVILGET
        assertThrows<java.lang.IllegalArgumentException> {
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag("saksbehandler",
                                                              vedtak,
                                                              behandlingResultatType,
                                                              true,
                                                              oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(
                                                                      andelerTilkjentYtelse))
        }
    }

    fun assertUtbetalingsperiode(utbetalingsperiode: Utbetalingsperiode,
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