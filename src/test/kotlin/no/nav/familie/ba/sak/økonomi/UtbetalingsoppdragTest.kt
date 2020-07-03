package no.nav.familie.ba.sak.økonomi

import io.mockk.*
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.beregning.domene.YtelseType.*
import no.nav.familie.ba.sak.common.*
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate
import java.time.LocalDate.now

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UtbetalingsoppdragPeriodiseringTest {

    lateinit var utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator
    var sisteOppdatertePersonsAndeler = slot<MutableList<AndelTilkjentYtelse>>()

    @BeforeAll
    fun setUp() {
        val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
        val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
        utbetalingsoppdragGenerator = spyk(UtbetalingsoppdragGenerator(andelTilkjentYtelseRepository, personopplysningGrunnlagRepository))

        every { utbetalingsoppdragGenerator.hentSisteOffsetForPerson(any()) } returns null
        every { utbetalingsoppdragGenerator.hentSisteOffsetForPerson(any(), any()) } returns null
        every { utbetalingsoppdragGenerator.hentSisteOffsetPåFagsak(any()) } returns (if (sisteOppdatertePersonsAndeler.isCaptured) sisteOppdatertePersonsAndeler.captured.maxBy { it.periodeOffset!! }?.periodeOffset?.toInt() else null)
        every { utbetalingsoppdragGenerator.lagreOppdaterteAndeler( capture(sisteOppdatertePersonsAndeler) ) } just Runs
        // Persontype er uvesentlig for nåværende tester da vi ikke tester offsets på tvers av behandlinger
        every { utbetalingsoppdragGenerator.hentPersontypeForPerson(any(), any()) } returns PersonType.BARN
    }

    @Test
    fun `skal opprette et nytt utbetalingsoppdrag med felles løpende periodeId og separat kjeding på to personer`() {
        val behandling = lagBehandling()
        val vedtak = lagVedtak(behandling = behandling)
        val personMedFlerePerioder = tilfeldigPerson()
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
                lagAndelTilkjentYtelse("2019-03-01", "2037-02-28", ORDINÆR_BARNETRYGD, 1054, behandling))

        val behandlingResultatType = BehandlingResultatType.INNVILGET
        val utbetalingsoppdrag =
                utbetalingsoppdragGenerator.lagUtbetalingsoppdrag("saksbehandler",
                                                                  vedtak,
                                                                  behandlingResultatType,
                                                                  true,
                                                                  andelerTilkjentYtelse)

        assertEquals(Utbetalingsoppdrag.KodeEndring.NY, utbetalingsoppdrag.kodeEndring)
        assertEquals(3, utbetalingsoppdrag.utbetalingsperiode.size)

        val id = vedtak.id * 1000
        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATRSMA"]!![0], id + 0, null, 660, "2019-04-01", "2023-03-31")
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATRSMA"]!![1], id + 1, id + 0, 660, "2026-05-01", "2027-06-30")
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATR"]!![0], id + 2, null, 1054, "2019-03-01", "2037-02-28")
    }

    @Test
    fun `skal opprette et opphør med felles løpende periodeId og separat kjeding på to personer`() {
        val behandling = lagBehandling()
        val vedtak = lagVedtak(behandling = behandling)
        val personMedFlerePerioder = tilfeldigPerson()
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
                lagAndelTilkjentYtelse("2019-03-01", "2037-02-28", ORDINÆR_BARNETRYGD, 1054, behandling))

        val opphørFom = now()
        val opphørVedtak = lagVedtak(forrigeVedtak = vedtak, opphørsdato = opphørFom)
        val behandlingResultatType = BehandlingResultatType.OPPHØRT
        val utbetalingsoppdrag =
                utbetalingsoppdragGenerator.lagUtbetalingsoppdrag("saksbehandler",
                                                                  opphørVedtak,
                                                                  behandlingResultatType,
                                                                  false,
                                                                  andelerTilkjentYtelse)

        assertEquals(Utbetalingsoppdrag.KodeEndring.UEND, utbetalingsoppdrag.kodeEndring)
        assertEquals(2, utbetalingsoppdrag.utbetalingsperiode.size)

        val id = vedtak.id * 1000

        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATRSMA"]!![0],
                                 id + 1,
                                 id + 0,
                                 660,
                                 "2026-05-01",
                                 "2027-06-30",
                                 dato("2026-05-01"))
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATR"]!![0],
                                 id + 2,
                                 null,
                                 1054,
                                 "2019-03-01",
                                 "2037-02-28",
                                 opphørFom)
    }

    @Test
    fun `skal opprette et opphør hvor periodens fom-dato er opphørsdato når denne er senere`() {
        val behandling = lagBehandling()
        val vedtak = lagVedtak(behandling)
        val andelerTilkjentYtelse = listOf(
                lagAndelTilkjentYtelse("2010-03-01", "2030-02-28", ORDINÆR_BARNETRYGD, 1054, behandling),
                lagAndelTilkjentYtelse("2025-01-01", "2030-02-28", ORDINÆR_BARNETRYGD, 1054, behandling))

        val opphørFom = dato("2020-01-01")
        val opphørVedtak = lagVedtak(forrigeVedtak = vedtak, opphørsdato = opphørFom)
        val behandlingResultatType = BehandlingResultatType.OPPHØRT
        val utbetalingsoppdrag =
                utbetalingsoppdragGenerator.lagUtbetalingsoppdrag("saksbehandler",
                                                                  opphørVedtak,
                                                                  behandlingResultatType,
                                                                  false,
                                                                  andelerTilkjentYtelse)

        assertEquals(Utbetalingsoppdrag.KodeEndring.UEND, utbetalingsoppdrag.kodeEndring)
        assertEquals(2, utbetalingsoppdrag.utbetalingsperiode.size)

        val id = vedtak.id * 1000

        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATR"]!![0],
                                 id,
                                 null,
                                 1054,
                                 "2010-03-01",
                                 "2030-02-28",
                                 opphørFom)
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATR"]!![1],
                                 id + 1,
                                 null,
                                 1054,
                                 "2025-01-01",
                                 "2030-02-28",
                                 dato("2025-01-01"))
    }

    @Test
    fun `skal opprette et nytt utbetalingsoppdrag med to andeler på samme person og separat kjeding for småbarnstillegg`() {
        val behandling = lagBehandling()
        val vedtak = lagVedtak(behandling = behandling)
        val personMedFlerePerioder = tilfeldigPerson()
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
                                                                                   andelerTilkjentYtelse)

        assertEquals(Utbetalingsoppdrag.KodeEndring.NY, utbetalingsoppdrag.kodeEndring)
        assertEquals(3, utbetalingsoppdrag.utbetalingsperiode.size)

        val id = vedtak.id * 1000
        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATRSMA"]!![0], id + 0, null, 660, "2019-04-01", "2023-03-31")
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATRSMA"]!![1], id + 1, id + 0, 660, "2026-05-01", "2027-06-30")
        assertUtbetalingsperiode(utbetalingsperioderPerKlasse["BATR"]!![0], id + 2, null, 1054, "2019-03-01", "2037-02-28")
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
                                                              andelerTilkjentYtelse)
        }
    }

    @Test
    fun `skal ikke tillate for stor offset på periodeId`() {
        val vedtak = lagVedtak()

        val utbetalingsperiodeMal = UtbetalingsperiodeMal(vedtak)
        val behandling = lagBehandling()
        val andelTilkjentYtelse = lagAndelTilkjentYtelse("2019-04-01", "2023-03-31", SMÅBARNSTILLEGG, 660, behandling)

        utbetalingsperiodeMal.lagPeriodeFraAndel(andelTilkjentYtelse, 999, null) //OK

        assertThrows<IllegalArgumentException> {
            utbetalingsperiodeMal.lagPeriodeFraAndel(andelTilkjentYtelse, 1000, 999)
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