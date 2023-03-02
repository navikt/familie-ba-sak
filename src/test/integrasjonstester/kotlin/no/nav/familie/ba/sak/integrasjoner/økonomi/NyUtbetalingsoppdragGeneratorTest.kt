package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.dato
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.common.årMnd
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonInfo
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.NyUtbetalingsoppdragGenerator
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.UtbetalingsoppdragService
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.VedtakMedTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth

class NyUtbetalingsoppdragGeneratorTest(
    @Autowired
    private val beregningService: BeregningService,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val utbetalingsoppdragService: UtbetalingsoppdragService,

    @Autowired
    private val tilkjentYtelseRepository: TilkjentYtelseRepository
) : AbstractSpringIntegrationTest() {

    lateinit var utbetalingsoppdragGenerator: NyUtbetalingsoppdragGenerator

    @BeforeEach
    fun setUp() {
        databaseCleanupService.truncate()
        utbetalingsoppdragGenerator = NyUtbetalingsoppdragGenerator()
    }

    @Test
    fun `skal opprette et nytt utbetalingsoppdrag med felles løpende periodeId og separat kjeding på to personer`() {
        val personMedFlerePerioder = tilfeldigPerson()
        val tilfeldigPerson = tilfeldigPerson()
        val fagsak =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(personMedFlerePerioder.aktør.aktivFødselsnummer())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val vedtak = lagVedtak(behandling = behandling)
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2019-04"),
                årMnd("2023-03"),
                YtelseType.SMÅBARNSTILLEGG,
                660,
                behandling,
                person = personMedFlerePerioder,
                aktør = hentOgLagreAktør(personMedFlerePerioder),
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                årMnd("2026-05"),
                årMnd("2027-06"),
                YtelseType.SMÅBARNSTILLEGG,
                660,
                behandling,
                person = personMedFlerePerioder,
                aktør = hentOgLagreAktør(personMedFlerePerioder),
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                årMnd("2019-03"),
                årMnd("2037-02"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                tilkjentYtelse = tilkjentYtelse,
                person = tilfeldigPerson,
                aktør = hentOgLagreAktør(tilfeldigPerson)
            )
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelse)

        val oppdatertTilkjentYtelse = utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
            lagVedtakMedTilkjentYtelse(
                vedtak = vedtak,
                tilkjentYtelse = tilkjentYtelse
            ),
            AndelTilkjentYtelseForIverksettingFactory()
        )
        val utbetalingsoppdrag = konvertTilUtbetalingsoppdrag(oppdatertTilkjentYtelse.utbetalingsoppdrag)
        assertEquals(Utbetalingsoppdrag.KodeEndring.NY, utbetalingsoppdrag.kodeEndring)
        assertEquals(3, utbetalingsoppdrag.utbetalingsperiode.size)

        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATR")[0],
            0,
            null,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2019-03-01",
            "2037-02-28"
        )
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATRSMA")[0],
            1,
            null,
            fagsak.aktør.aktivFødselsnummer(),
            660,
            "2019-04-01",
            "2023-03-31"
        )
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATRSMA")[1],
            2,
            1,
            fagsak.aktør.aktivFødselsnummer(),
            660,
            "2026-05-01",
            "2027-06-30"
        )
    }

    @Test
    fun `skal opprette et fullstendig opphør for to personer, hvor opphørsdatoer blir første dato i hver kjede`() {
        val personMedFlerePerioder = tilfeldigPerson()
        val fagsak =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(personMedFlerePerioder.aktør.aktivFødselsnummer())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val forrigeTilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val førsteDatoKjede1 = årMnd("2019-04")
        val førsteDatoKjede2 = årMnd("2019-03")
        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                førsteDatoKjede1,
                årMnd("2023-03"),
                YtelseType.SMÅBARNSTILLEGG,
                660,
                behandling,
                person = personMedFlerePerioder,
                aktør = hentOgLagreAktør(personMedFlerePerioder),
                periodeIdOffset = 0,
                tilkjentYtelse = forrigeTilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                årMnd("2026-05"),
                årMnd("2027-06"),
                YtelseType.SMÅBARNSTILLEGG,
                660,
                behandling,
                person = personMedFlerePerioder,
                aktør = hentOgLagreAktør(personMedFlerePerioder),
                periodeIdOffset = 1,
                tilkjentYtelse = forrigeTilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                førsteDatoKjede2,
                årMnd("2037-02"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 2,
                tilkjentYtelse = forrigeTilkjentYtelse
            )
        )
        forrigeTilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelse)
        val vedtak = lagVedtak(behandling = behandling)

        val oppdatertTilkjentYtelse = utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
            vedtakMedTilkjentYtelse = lagVedtakMedTilkjentYtelse(
                vedtak = vedtak,
                // blank tilkjentYtelse som ikke har andeler for fullstendig opphør
                tilkjentYtelse = lagInitiellTilkjentYtelse(behandling),
                sisteOffsetPerIdent = ØkonomiUtils.gjeldendeForrigeOffsetForKjede(
                    ØkonomiUtils.kjedeinndelteAndeler(
                        andelerTilkjentYtelse.forIverksetting()
                    )
                ),
                sisteOffsetPåFagsak = 0
            ),
            AndelTilkjentYtelseForIverksettingFactory(),
            forrigeTilkjentYtelse = forrigeTilkjentYtelse
        )
        val utbetalingsoppdrag = konvertTilUtbetalingsoppdrag(oppdatertTilkjentYtelse.utbetalingsoppdrag)
        assertEquals(Utbetalingsoppdrag.KodeEndring.ENDR, utbetalingsoppdrag.kodeEndring)
        assertEquals(2, utbetalingsoppdrag.utbetalingsperiode.size)

        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATRSMA")[0],
            1,
            null,
            fagsak.aktør.aktivFødselsnummer(),
            660,
            "2026-05-01",
            "2027-06-30",
            førsteDatoKjede1.førsteDagIInneværendeMåned()
        )
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATR")[0],
            2,
            null,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2019-03-01",
            "2037-02-28",
            førsteDatoKjede2.førsteDagIInneværendeMåned()
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
                årMnd("2020-01"),
                årMnd("2029-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 0,
                person = person,
                aktør = hentOgLagreAktør(person),
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                dato(fomDatoSomEndres).toYearMonth(),
                årMnd("2034-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 1,
                person = person,
                aktør = hentOgLagreAktør(person),
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                årMnd("2037-01"),
                årMnd("2039-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 2,
                person = person,
                aktør = hentOgLagreAktør(person),
                tilkjentYtelse = tilkjentYtelse
            )
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerFørstegangsbehandling)

        val oppdatertTilkjentYtelse = utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
            lagVedtakMedTilkjentYtelse(
                vedtak = vedtak,
                tilkjentYtelse = tilkjentYtelse
            ),
            AndelTilkjentYtelseForIverksettingFactory()
        )
        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val tilkjentYtelse2 = lagInitiellTilkjentYtelse(behandling2)
        val vedtak2 = lagVedtak(behandling2)
        val andelerRevurdering = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2020-01"),
                årMnd("2029-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 0,
                person = person,
                aktør = hentOgLagreAktør(person),
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                årMnd("2034-01"),
                årMnd("2034-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 3,
                person = person,
                aktør = hentOgLagreAktør(person),
                tilkjentYtelse = tilkjentYtelse2
            ),
            lagAndelTilkjentYtelse(
                årMnd("2037-01"),
                årMnd("2039-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 4,
                person = person,
                aktør = hentOgLagreAktør(person),
                tilkjentYtelse = tilkjentYtelse2
            )
        )
        tilkjentYtelse2.andelerTilkjentYtelse.addAll(andelerRevurdering)
        val sisteOffsetPåFagsak = 2

        val oppdatertTilkjentYtelse2 = utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
            lagVedtakMedTilkjentYtelse(
                vedtak = vedtak2,
                sisteOffsetPerIdent = ØkonomiUtils.gjeldendeForrigeOffsetForKjede(
                    ØkonomiUtils.kjedeinndelteAndeler(
                        andelerFørstegangsbehandling.forIverksetting()
                    )
                ),
                sisteOffsetPåFagsak = sisteOffsetPåFagsak,
                tilkjentYtelse = tilkjentYtelse2
            ),
            AndelTilkjentYtelseForIverksettingFactory(),
            forrigeTilkjentYtelse = oppdatertTilkjentYtelse
        )
        val utbetalingsoppdrag = konvertTilUtbetalingsoppdrag(oppdatertTilkjentYtelse2.utbetalingsoppdrag)
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
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2037-01-01",
            "2039-12-31",
            dato(fomDatoSomEndres)
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert.first(),
            3,
            2,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2034-01-01",
            "2034-12-31"
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert.last(),
            4,
            3,
            fagsak.aktør.aktivFødselsnummer(),
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
        val aktør = personidentService.hentOgLagreAktør(randomFnr(), true)
        val person = tilfeldigPerson(aktør = aktør)
        val vedtak = lagVedtak(behandling)
        val andelerFørstegangsbehandling = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2020-01"),
                årMnd("2029-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 0,
                person = person,
                aktør = aktør,
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                årMnd("2033-01"),
                årMnd("2034-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 1,
                person = person,
                aktør = aktør,
                tilkjentYtelse = tilkjentYtelse
            )
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerFørstegangsbehandling)
        tilkjentYtelse.utbetalingsoppdrag = "Oppdrag"

        val oppdatertTilkjentYtelse = utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
            lagVedtakMedTilkjentYtelse(
                vedtak = vedtak,
                tilkjentYtelse = tilkjentYtelse
            ),
            AndelTilkjentYtelseForIverksettingFactory()
        )

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val tilkjentYtelse2 = lagInitiellTilkjentYtelse(behandling2)
        val nyAktør = personidentService.hentOgLagreAktør(randomFnr(), true)
        val nyPerson = tilfeldigPerson(aktør = nyAktør)
        val vedtak2 = lagVedtak(behandling2)
        val andelerRevurdering = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2022-01"),
                årMnd("2034-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 2,
                person = nyPerson,
                aktør = nyAktør,
                tilkjentYtelse = tilkjentYtelse2
            ),
            lagAndelTilkjentYtelse(
                årMnd("2037-01"),
                årMnd("2039-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 3,
                person = nyPerson,
                aktør = personidentService.hentOgLagreAktør(nyPerson.aktør.aktørId, true),
                tilkjentYtelse = tilkjentYtelse2
            )
        )
        tilkjentYtelse2.andelerTilkjentYtelse.addAll(andelerRevurdering)
        val sisteOffsetPåFagsak = 1

        val oppdatertTilkjentYtelse2 = utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
            lagVedtakMedTilkjentYtelse(
                vedtak = vedtak2,
                sisteOffsetPerIdent = ØkonomiUtils.gjeldendeForrigeOffsetForKjede(
                    ØkonomiUtils.kjedeinndelteAndeler(
                        andelerFørstegangsbehandling.forIverksetting()
                    )
                ),
                sisteOffsetPåFagsak = sisteOffsetPåFagsak,
                tilkjentYtelse = tilkjentYtelse2
            ),
            AndelTilkjentYtelseForIverksettingFactory(),
            forrigeTilkjentYtelse = oppdatertTilkjentYtelse
        )
        val utbetalingsoppdrag = konvertTilUtbetalingsoppdrag(oppdatertTilkjentYtelse2.utbetalingsoppdrag)
        assertEquals(Utbetalingsoppdrag.KodeEndring.ENDR, utbetalingsoppdrag.kodeEndring)
        assertEquals(3, utbetalingsoppdrag.utbetalingsperiode.size)
        val sorterteUtbetalingsperioder = utbetalingsoppdrag.utbetalingsperiode.sortedBy { it.periodeId }
        assertUtbetalingsperiode(
            sorterteUtbetalingsperioder[0],
            1,
            0,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2033-01-01",
            "2034-12-31"
        )
        assertUtbetalingsperiode(
            sorterteUtbetalingsperioder[1],
            2,
            null,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2022-01-01",
            "2034-12-31"
        )
        assertUtbetalingsperiode(
            sorterteUtbetalingsperioder[2],
            3,
            2,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2037-01-01",
            "2039-12-31"
        )
    }

    @Test
    fun `skal opprette et nytt utbetalingsoppdrag med to andeler på samme person og separat kjeding for småbarnstillegg`() {
        val personMedFlerePerioder = tilfeldigPerson()
        val fagsak =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(personMedFlerePerioder.aktør.aktivFødselsnummer())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val vedtak = lagVedtak(behandling = behandling)
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2019-04"),
                årMnd("2023-03"),
                YtelseType.SMÅBARNSTILLEGG,
                660,
                behandling,
                person = personMedFlerePerioder,
                aktør = personidentService.hentOgLagreAktør(personMedFlerePerioder.aktør.aktørId, true),
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                årMnd("2026-05"),
                årMnd("2027-06"),
                YtelseType.SMÅBARNSTILLEGG,
                660,
                behandling,
                person = personMedFlerePerioder,
                aktør = personidentService.hentOgLagreAktør(personMedFlerePerioder.aktør.aktørId, true),
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                årMnd("2019-03"),
                årMnd("2037-02"),
                YtelseType.UTVIDET_BARNETRYGD,
                1054,
                behandling,
                person = personMedFlerePerioder,
                aktør = personidentService.hentOgLagreAktør(personMedFlerePerioder.aktør.aktørId, true),
                tilkjentYtelse = tilkjentYtelse
            )
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelse)
        val oppdatertTilkjentYtelse = utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
            lagVedtakMedTilkjentYtelse(
                vedtak = vedtak,
                tilkjentYtelse = tilkjentYtelse
            ),
            AndelTilkjentYtelseForIverksettingFactory()
        )
        val utbetalingsoppdrag = konvertTilUtbetalingsoppdrag(oppdatertTilkjentYtelse.utbetalingsoppdrag)
        assertEquals(Utbetalingsoppdrag.KodeEndring.NY, utbetalingsoppdrag.kodeEndring)
        assertEquals(3, utbetalingsoppdrag.utbetalingsperiode.size)

        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATR")[0],
            0,
            null,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2019-03-01",
            "2037-02-28"
        )
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATRSMA")[0],
            1,
            null,
            fagsak.aktør.aktivFødselsnummer(),
            660,
            "2019-04-01",
            "2023-03-31"
        )
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATRSMA")[1],
            2,
            1,
            fagsak.aktør.aktivFødselsnummer(),
            660,
            "2026-05-01",
            "2027-06-30"
        )
    }

    @Test
    fun `opprettelse av utbetalingsoppdrag hvor flere har småbarnstillegg kaster feil`() {
        val behandling = lagBehandling()
        val vedtak = lagVedtak(behandling = behandling)
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                fom = årMnd("2019-04"),
                tom = årMnd("2023-03"),
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                beløp = 660,
                behandling = behandling,
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                fom = årMnd("2026-05"),
                tom = årMnd("2027-06"),
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                beløp = 660,
                behandling = behandling,
                tilkjentYtelse = tilkjentYtelse
            )
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelse)

        val exception = assertThrows<java.lang.IllegalArgumentException> {
            utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
                lagVedtakMedTilkjentYtelse(
                    vedtak = vedtak,
                    erSimulering = true,
                    tilkjentYtelse = tilkjentYtelse
                ),
                AndelTilkjentYtelseForIverksettingFactory()
            )
        }
        assertEquals("Finnes flere personer med småbarnstillegg", exception.message)
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
                årMnd("2020-01"),
                årMnd("2029-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 0,
                person = person,
                aktør = hentOgLagreAktør(person),
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                årMnd("2030-01"),
                årMnd("2034-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 1,
                person = person,
                aktør = hentOgLagreAktør(person),
                tilkjentYtelse = tilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                årMnd("2035-01"),
                årMnd("2039-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 2,
                person = person,
                aktør = hentOgLagreAktør(person),
                tilkjentYtelse = tilkjentYtelse
            )
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerFørstegangsbehandling)

        val oppdatertTilkjentYtelse = utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
            lagVedtakMedTilkjentYtelse(
                vedtak = vedtak,
                tilkjentYtelse = tilkjentYtelse
            ),
            AndelTilkjentYtelseForIverksettingFactory()
        )

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val tilkjentYtelse2 = lagInitiellTilkjentYtelse(behandling2)
        val vedtak2 = lagVedtak(behandling2)
        val andelerRevurdering = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2020-01"),
                årMnd("2029-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 0,
                person = person,
                aktør = hentOgLagreAktør(person),
                tilkjentYtelse = tilkjentYtelse2
            ),
            lagAndelTilkjentYtelse(
                årMnd("2030-01"),
                årMnd("2034-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 3,
                person = person,
                aktør = hentOgLagreAktør(person),
                tilkjentYtelse = tilkjentYtelse2
            ),
            lagAndelTilkjentYtelse(
                årMnd("2035-01"),
                årMnd("2039-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 4,
                person = person,
                aktør = hentOgLagreAktør(person),
                tilkjentYtelse = tilkjentYtelse2
            )
        )
        tilkjentYtelse2.andelerTilkjentYtelse.addAll(andelerRevurdering)
        val sisteOffsetPåFagsak = 2

        val oppdatertTilkjentYtelse2 = utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
            lagVedtakMedTilkjentYtelse(
                vedtak = vedtak2,
                erSimulering = true,
                sisteOffsetPerIdent = ØkonomiUtils.gjeldendeForrigeOffsetForKjede(
                    ØkonomiUtils.kjedeinndelteAndeler(
                        andelerFørstegangsbehandling.forIverksetting()
                    )
                ),
                sisteOffsetPåFagsak = sisteOffsetPåFagsak,
                tilkjentYtelse = tilkjentYtelse2
            ),
            AndelTilkjentYtelseForIverksettingFactory(),
            forrigeTilkjentYtelse = oppdatertTilkjentYtelse
        )
        val utbetalingsoppdrag = konvertTilUtbetalingsoppdrag(oppdatertTilkjentYtelse2.utbetalingsoppdrag)
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
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2035-01-01",
            "2039-12-31",
            dato("2020-01-01")
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert.first(),
            3,
            2,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2020-01-01",
            "2029-12-31"
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert[1],
            4,
            3,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2030-01-01",
            "2034-12-31"
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert.last(),
            5,
            4,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2035-01-01",
            "2039-12-31"
        )
    }

    @Test
    fun `Skal teste uthenting av offset på revurderinger`() {
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
                årMnd("2020-01"),
                årMnd("2029-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 0,
                person = person,
                aktør = hentOgLagreAktør(person),
                tilkjentYtelse = tilkjentYtelse
            )
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerFørstegangsbehandling)

        val oppdatertTilkjentYtelse = utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
            lagVedtakMedTilkjentYtelse(
                vedtak = vedtak,
                tilkjentYtelse = tilkjentYtelse
            ),
            AndelTilkjentYtelseForIverksettingFactory()
        )
        oppdatertTilkjentYtelse.andelerTilkjentYtelse.forEach { it.tilkjentYtelse = oppdatertTilkjentYtelse }
        beregningService.lagreTilkjentYtelseMedOppdaterteAndeler(oppdatertTilkjentYtelse)

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak,
                førsteSteg = StegType.BEHANDLING_AVSLUTTET
            )
        )
        val tilkjentYtelse2 = lagInitiellTilkjentYtelse(behandling2)
        val vedtak2 = lagVedtak(behandling2)
        val andelerRevurdering = emptyList<AndelTilkjentYtelse>()
        tilkjentYtelse2.andelerTilkjentYtelse.addAll(andelerRevurdering)

        val oppdatertTilkjentYtelse2 = utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
            lagVedtakMedTilkjentYtelse(
                vedtak = vedtak2,
                tilkjentYtelse = tilkjentYtelse2,
                sisteOffsetPåFagsak = 0,
                sisteOffsetPerIdent = ØkonomiUtils.gjeldendeForrigeOffsetForKjede(
                    ØkonomiUtils.kjedeinndelteAndeler(
                        andelerFørstegangsbehandling.forIverksetting()
                    )
                )
            ),
            AndelTilkjentYtelseForIverksettingFactory(),
            forrigeTilkjentYtelse = oppdatertTilkjentYtelse
        )

        beregningService.lagreTilkjentYtelseMedOppdaterteAndeler(oppdatertTilkjentYtelse2)

        val behandling3 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val tilkjentYtelse3 = lagInitiellTilkjentYtelse(behandling3)
        val andelerRevurdering2 = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2020-01"),
                årMnd("2029-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling3,
                periodeIdOffset = 0,
                person = person,
                aktør = hentOgLagreAktør(person),
                tilkjentYtelse = tilkjentYtelse3
            )
        )
        tilkjentYtelse3.andelerTilkjentYtelse.addAll(andelerRevurdering2)

        assertEquals(0, beregningService.hentSisteOffsetPåFagsak(behandling = behandling3))
    }

    @Test
    fun `Skal opphøre tideligere utbetaling hvis barnet ikke har utbetaling i den nye behandlingen`() {
        val søker = tilfeldigPerson()
        val førsteBarnet = tilfeldigPerson()
        val andreBarnet = tilfeldigPerson()

        val fagsak =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        val førsteBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val førsteVedtak = lagVedtak(behandling = førsteBehandling)

        val førsteTilkjentYtelse = lagInitiellTilkjentYtelse(førsteBehandling)
        val førsteAndelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2019-04"),
                årMnd("2023-03"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1345,
                førsteBehandling,
                person = førsteBarnet,
                aktør = hentOgLagreAktør(førsteBarnet),
                tilkjentYtelse = førsteTilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                årMnd("2023-04"),
                årMnd("2027-06"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                førsteBehandling,
                person = førsteBarnet,
                aktør = hentOgLagreAktør(førsteBarnet),
                tilkjentYtelse = førsteTilkjentYtelse
            )
        )
        førsteTilkjentYtelse.andelerTilkjentYtelse.addAll(førsteAndelerTilkjentYtelse)
        førsteTilkjentYtelse.utbetalingsoppdrag = "utbetalingsoppdrg"
        tilkjentYtelseRepository.saveAndFlush(førsteTilkjentYtelse)

        utbetalingsoppdragService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
            førsteVedtak,
            "Z123",
            AndelTilkjentYtelseForIverksettingFactory()
        )
        førsteBehandling.status = BehandlingStatus.AVSLUTTET
        førsteBehandling.leggTilBehandlingStegTilstand(StegType.BEHANDLING_AVSLUTTET)
        behandlingService.lagre(førsteBehandling)

        val andreBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak,
                behandlingType = BehandlingType.REVURDERING
            )
        )
        val andreVedtak = lagVedtak(behandling = andreBehandling)

        val andreTilkjentYtelse = lagInitiellTilkjentYtelse(andreBehandling)
        val andreAndelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2019-04"),
                årMnd("2023-03"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1345,
                andreBehandling,
                person = andreBarnet,
                aktør = hentOgLagreAktør(andreBarnet),
                tilkjentYtelse = andreTilkjentYtelse
            ),
            lagAndelTilkjentYtelse(
                årMnd("2023-04"),
                årMnd("2027-06"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                andreBehandling,
                person = andreBarnet,
                aktør = hentOgLagreAktør(andreBarnet),
                tilkjentYtelse = andreTilkjentYtelse
            )
        )
        andreTilkjentYtelse.andelerTilkjentYtelse.addAll(andreAndelerTilkjentYtelse)
        tilkjentYtelseRepository.saveAndFlush(andreTilkjentYtelse)

        val tilkjentYtelse =
            utbetalingsoppdragService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                andreVedtak,
                "Z123",
                AndelTilkjentYtelseForIverksettingFactory()
            )
        val utbetalingsoppdrag =
            objectMapper.readValue(tilkjentYtelse.utbetalingsoppdrag, Utbetalingsoppdrag::class.java)
        assertEquals(Utbetalingsoppdrag.KodeEndring.ENDR, utbetalingsoppdrag.kodeEndring)
        assertEquals(3, utbetalingsoppdrag.utbetalingsperiode.size)
        assertEquals(true, utbetalingsoppdrag.utbetalingsperiode.first().erEndringPåEksisterendePeriode)
        assertEquals(Opphør(YearMonth.of(2019, 4).toLocalDate()), utbetalingsoppdrag.utbetalingsperiode.first().opphør)
        assertEquals(0, utbetalingsoppdrag.utbetalingsperiode.first().forrigePeriodeId)
        assertEquals(false, utbetalingsoppdrag.utbetalingsperiode[1].erEndringPåEksisterendePeriode)
        assertNull(utbetalingsoppdrag.utbetalingsperiode[1].opphør)
        assertNull(utbetalingsoppdrag.utbetalingsperiode[1].forrigePeriodeId)
    }

    @Test
    fun `skal opprette et nytt utbetalingsoppdrag for institusjon`() {
        val tilfeldigPerson = tilfeldigPerson()
        val fagsak =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(
                tilfeldigPerson.aktør.aktivFødselsnummer(),
                fagsakType = FagsakType.INSTITUSJON,
                institusjon = InstitusjonInfo(ORGNUMMER, TSS_ID_INSTITUSJON)
            )
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val vedtak = lagVedtak(behandling = behandling)
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2019-03"),
                årMnd("2037-02"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                tilkjentYtelse = tilkjentYtelse,
                person = tilfeldigPerson,
                aktør = hentOgLagreAktør(tilfeldigPerson)
            )
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelse)

        val oppdatertTilkjentYtelse = utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
            lagVedtakMedTilkjentYtelse(
                vedtak = vedtak,
                tilkjentYtelse = tilkjentYtelse
            ),
            AndelTilkjentYtelseForIverksettingFactory()
        )
        val utbetalingsoppdrag = konvertTilUtbetalingsoppdrag(oppdatertTilkjentYtelse.utbetalingsoppdrag)
        assertEquals(Utbetalingsoppdrag.KodeEndring.NY, utbetalingsoppdrag.kodeEndring)
        assertEquals(1, utbetalingsoppdrag.utbetalingsperiode.size)

        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATR")[0],
            0,
            null,
            TSS_ID_INSTITUSJON,
            1054,
            "2019-03-01",
            "2037-02-28"
        )
    }

    private fun hentOgLagreAktør(personMedFlerePerioder: Person) =
        personidentService.hentOgLagreAktør(personMedFlerePerioder.aktør.aktivFødselsnummer(), true)

    private fun lagVedtakMedTilkjentYtelse(
        vedtak: Vedtak,
        tilkjentYtelse: TilkjentYtelse,
        sisteOffsetPerIdent: Map<String, Int> = emptyMap(),
        sisteOffsetPåFagsak: Int? = null,
        erSimulering: Boolean = false
    ) = VedtakMedTilkjentYtelse(
        tilkjentYtelse = tilkjentYtelse,
        vedtak = vedtak,
        saksbehandlerId = "saksbehandler",
        sisteOffsetPerIdent = sisteOffsetPerIdent,
        sisteOffsetPåFagsak = sisteOffsetPåFagsak,
        erSimulering = erSimulering
    )

    private fun konvertTilUtbetalingsoppdrag(utbetalingsoppdragIString: String?) =
        objectMapper.readValue(utbetalingsoppdragIString, Utbetalingsoppdrag::class.java)

    private fun assertUtbetalingsperiode(
        utbetalingsperiode: Utbetalingsperiode,
        periodeId: Long,
        forrigePeriodeId: Long?,
        utbetalesTils: String,
        sats: Int,
        fom: String,
        tom: String,
        opphørFom: LocalDate? = null
    ) {
        assertEquals(periodeId, utbetalingsperiode.periodeId)
        assertEquals(forrigePeriodeId, utbetalingsperiode.forrigePeriodeId)
        assertEquals(sats, utbetalingsperiode.sats.toInt())
        assertEquals(utbetalesTils, utbetalingsperiode.utbetalesTil)
        assertEquals(dato(fom), utbetalingsperiode.vedtakdatoFom)
        assertEquals(dato(tom), utbetalingsperiode.vedtakdatoTom)
        if (opphørFom != null) {
            assertEquals(opphørFom, utbetalingsperiode.opphør?.opphørDatoFom)
        }
    }

    companion object {
        private const val TSS_ID_INSTITUSJON = "80000"
        private const val ORGNUMMER = "987654321"
    }
}
