package no.nav.familie.ba.sak.ekstern.bisys

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelseUtvidet
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.lagMinimalUtbetalingsoppdragString
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.person.pdl.aktor.v2.Type
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BisysServiceTest {
    private lateinit var bisysService: BisysService
    private val mockPersonidentService = mockk<PersonidentService>()
    private val mockFagsakRepository = mockk<FagsakRepository>()
    private val mockBehandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val mockTilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val mockInfotrygdKlient = mockk<InfotrygdBarnetrygdKlient>()

    @BeforeAll
    fun setUp() {
        bisysService =
            BisysService(
                mockBehandlingHentOgPersisterService,
                mockInfotrygdKlient,
                mockFagsakRepository,
                mockPersonidentService,
                mockTilkjentYtelseRepository,
            )
    }

    @Test
    fun `Skal returnere tom liste siden person ikke har finens i infotrygd og barnetrygd`() {
        val fnr = randomFnr()
        val aktør = lagAktør(fnr)

        every { mockPersonidentService.hentAktør(any()) } answers { aktør }
        every { mockPersonidentService.hentAlleFødselsnummerForEnAktør(any()) } answers { listOf(aktør.aktivFødselsnummer()) }
        every { mockPersonidentService.hentIdenter(any(), any()) } answers { listOf(IdentInformasjon(aktør.aktivFødselsnummer(), false, Type.FOLKEREGISTERIDENT.name)) }
        every { mockInfotrygdKlient.hentUtvidetBarnetrygd(fnr, any()) } returns
            BisysUtvidetBarnetrygdResponse(
                perioder = emptyList(),
            )

        every { mockFagsakRepository.finnFagsakForAktør(aktør) } returns null

        val response = bisysService.hentUtvidetBarnetrygd(fnr, LocalDate.of(2021, 1, 1))

        assertThat(response.perioder).hasSize(0)
    }

    @Test
    fun `Skal returnere tom liste hvis tjenesten kalles på en bruker som bare har NPID`() {
        every { mockPersonidentService.hentIdenter(any(), any()) } answers { listOf(IdentInformasjon("ident av typen npid", false, Type.NPID.name)) }

        val response = bisysService.hentUtvidetBarnetrygd("ident av typen npid", LocalDate.of(2021, 1, 1))

        assertThat(response.perioder).hasSize(0)
    }

    @Test
    fun `Skal returnere periode kun fra infotrygd`() {
        val fnr = randomFnr()
        val aktør = lagAktør(fnr)

        every { mockPersonidentService.hentAktør(any()) } answers { aktør }
        every { mockPersonidentService.hentAlleFødselsnummerForEnAktør(any()) } answers { listOf(aktør.aktivFødselsnummer()) }
        every { mockPersonidentService.hentIdenter(any(), any()) } answers { listOf(IdentInformasjon(aktør.aktivFødselsnummer(), false, Type.FOLKEREGISTERIDENT.name)) }

        val periodeInfotrygd =
            UtvidetBarnetrygdPeriode(
                BisysStønadstype.UTVIDET,
                YearMonth.of(2019, 1),
                YearMonth.now(),
                500.0,
                manueltBeregnet = true,
            )
        every { mockInfotrygdKlient.hentUtvidetBarnetrygd(fnr, any()) } returns
            BisysUtvidetBarnetrygdResponse(
                perioder = listOf(periodeInfotrygd),
            )

        every { mockFagsakRepository.finnFagsakForAktør(aktør) } returns null

        val response = bisysService.hentUtvidetBarnetrygd(fnr, LocalDate.of(2021, 1, 1))

        assertThat(response.perioder).hasSize(1).contains(periodeInfotrygd)
    }

    @Test
    fun `Skal returnere utvidet barnetrygdperiode fra basak`() {
        val behandling = lagBehandling()

        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling, utbetalingsoppdrag = lagMinimalUtbetalingsoppdragString(behandlingId = behandling.id))

        val andelTilkjentYtelse =
            lagAndelTilkjentYtelseUtvidet(
                fom = "2020-01",
                tom = "2040-01",
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                behandling = behandling,
                tilkjentYtelse = tilkjentYtelse,
                beløp = 660,
            )
        tilkjentYtelse.andelerTilkjentYtelse.add(andelTilkjentYtelse)

        every { mockInfotrygdKlient.hentUtvidetBarnetrygd(any(), any()) } returns
            BisysUtvidetBarnetrygdResponse(
                perioder = emptyList(),
            )

        every { mockFagsakRepository.finnFagsakForAktør(any()) } returns behandling.fagsak
        every { mockBehandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id) } returns behandling
        every { mockTilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandling.id) } returns andelTilkjentYtelse.tilkjentYtelse
        every { mockPersonidentService.hentAktør(any()) } answers { behandling.fagsak.aktør }
        every { mockPersonidentService.hentAlleFødselsnummerForEnAktør(any()) } answers { listOf(behandling.fagsak.aktør.aktivFødselsnummer()) }
        every { mockPersonidentService.hentIdenter(any(), any()) } answers { listOf(IdentInformasjon(behandling.fagsak.aktør.aktivFødselsnummer(), false, Type.FOLKEREGISTERIDENT.name)) }

        val response =
            bisysService.hentUtvidetBarnetrygd(andelTilkjentYtelse.aktør.aktivFødselsnummer(), LocalDate.of(2021, 1, 1))

        assertThat(response.perioder).hasSize(1)
        assertThat(response.perioder.first().beløp).isEqualTo(660.0)
        assertThat(response.perioder.first().fomMåned).isEqualTo(YearMonth.of(2020, 1))
        assertThat(response.perioder.first().tomMåned).isEqualTo(YearMonth.of(2040, 1))
        assertThat(response.perioder.first().manueltBeregnet).isFalse
    }

    @Test
    fun `Skal slå sammen resultat fra ba-sak og infotrygd`() {
        val behandling = lagBehandling()

        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling, utbetalingsoppdrag = lagMinimalUtbetalingsoppdragString(behandlingId = behandling.id))

        val kalkulertbeløp = 660
        val andelTilkjentYtelse =
            lagAndelTilkjentYtelseUtvidet(
                fom = "2020-01",
                tom = "2040-01",
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                behandling = behandling,
                tilkjentYtelse = tilkjentYtelse,
                beløp = kalkulertbeløp,
            ).copy(prosent = BigDecimal.valueOf(50), sats = 2 * kalkulertbeløp)

        tilkjentYtelse.andelerTilkjentYtelse.add(andelTilkjentYtelse)
        every { mockPersonidentService.hentAktør(any()) } answers { andelTilkjentYtelse.aktør }
        every { mockPersonidentService.hentAlleFødselsnummerForEnAktør(any()) } answers { listOf(andelTilkjentYtelse.aktør.aktivFødselsnummer()) }
        every { mockPersonidentService.hentIdenter(any(), any()) } answers { listOf(IdentInformasjon(andelTilkjentYtelse.aktør.aktivFødselsnummer(), false, Type.FOLKEREGISTERIDENT.name)) }

        val periodeInfotrygd =
            UtvidetBarnetrygdPeriode(
                BisysStønadstype.UTVIDET,
                YearMonth.of(2019, 1),
                YearMonth.of(2019, 12),
                660.0,
                manueltBeregnet = false,
                deltBosted = true,
            )
        every {
            mockInfotrygdKlient.hentUtvidetBarnetrygd(
                andelTilkjentYtelse.aktør.aktivFødselsnummer(),
                any(),
            )
        } returns
            BisysUtvidetBarnetrygdResponse(
                perioder = listOf(periodeInfotrygd),
            )

        every { mockFagsakRepository.finnFagsakForAktør(any()) } returns behandling.fagsak

        every { mockBehandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id) } returns behandling
        every { mockTilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandling.id) } returns andelTilkjentYtelse.tilkjentYtelse

        val response =
            bisysService.hentUtvidetBarnetrygd(andelTilkjentYtelse.aktør.aktivFødselsnummer(), LocalDate.of(2019, 1, 1))

        assertThat(response.perioder).hasSize(1)
        assertThat(response.perioder.first().beløp).isEqualTo(660.0)
        assertThat(response.perioder.first().fomMåned).isEqualTo(YearMonth.of(2019, 1))
        assertThat(response.perioder.first().tomMåned).isEqualTo(YearMonth.of(2040, 1))
        assertThat(response.perioder.first().manueltBeregnet).isFalse
    }

    @Test
    fun `Skal slå sammen resultat fra ba-sak og infotrygd når periodene overlapper`() {
        val behandling = lagBehandling()

        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling, utbetalingsoppdrag = lagMinimalUtbetalingsoppdragString(behandlingId = behandling.id))

        val andelTilkjentYtelse =
            lagAndelTilkjentYtelseUtvidet(
                fom = "2021-08",
                tom = "2029-02",
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                behandling = behandling,
                tilkjentYtelse = tilkjentYtelse,
                beløp = 1054,
            )

        tilkjentYtelse.andelerTilkjentYtelse.add(andelTilkjentYtelse)
        every { mockPersonidentService.hentAktør(any()) } answers { andelTilkjentYtelse.aktør }
        every { mockPersonidentService.hentAlleFødselsnummerForEnAktør(any()) } answers { listOf(andelTilkjentYtelse.aktør.aktivFødselsnummer()) }
        every { mockPersonidentService.hentIdenter(any(), any()) } answers { listOf(IdentInformasjon(andelTilkjentYtelse.aktør.aktivFødselsnummer(), false, Type.FOLKEREGISTERIDENT.name)) }

        val periodeInfotrygd =
            UtvidetBarnetrygdPeriode(
                BisysStønadstype.UTVIDET,
                YearMonth.of(2020, 9),
                YearMonth.of(2021, 12),
                1054.0,
                manueltBeregnet = false,
                deltBosted = false,
            )
        every {
            mockInfotrygdKlient.hentUtvidetBarnetrygd(
                andelTilkjentYtelse.aktør.aktivFødselsnummer(),
                any(),
            )
        } returns
            BisysUtvidetBarnetrygdResponse(
                perioder = listOf(periodeInfotrygd),
            )

        every { mockFagsakRepository.finnFagsakForAktør(any()) } returns behandling.fagsak

        every { mockBehandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id) } returns behandling
        every { mockTilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandling.id) } returns andelTilkjentYtelse.tilkjentYtelse

        val response =
            bisysService.hentUtvidetBarnetrygd(andelTilkjentYtelse.aktør.aktivFødselsnummer(), LocalDate.of(2021, 1, 1))

        assertThat(response.perioder).hasSize(1)
        assertThat(response.perioder.first().beløp).isEqualTo(1054.0)
        assertThat(response.perioder.first().fomMåned).isEqualTo(YearMonth.of(2020, 9))
        assertThat(response.perioder.first().tomMåned).isEqualTo(YearMonth.of(2029, 2))
        assertThat(response.perioder.first().manueltBeregnet).isFalse
    }

    @Test
    fun `Skal slå sammen resultat fra ba-sak og infotrygd, typisk rett etter en migrering, hvor tomMåned i infotrygd er null`() {
        val behandling = lagBehandling()

        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling, utbetalingsoppdrag = lagMinimalUtbetalingsoppdragString(behandlingId = behandling.id))

        val andelTilkjentYtelse =
            lagAndelTilkjentYtelseUtvidet(
                fom = "2022-01",
                tom = "2022-12",
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                behandling = behandling,
                tilkjentYtelse = tilkjentYtelse,
                beløp = 1054,
            )

        tilkjentYtelse.andelerTilkjentYtelse.add(andelTilkjentYtelse)
        every { mockPersonidentService.hentAktør(any()) } answers { andelTilkjentYtelse.aktør }
        every { mockPersonidentService.hentAlleFødselsnummerForEnAktør(any()) } answers { listOf(andelTilkjentYtelse.aktør.aktivFødselsnummer()) }
        every { mockPersonidentService.hentIdenter(any(), any()) } answers { listOf(IdentInformasjon(andelTilkjentYtelse.aktør.aktivFødselsnummer(), false, Type.FOLKEREGISTERIDENT.name)) }

        val periodeInfotrygd =
            UtvidetBarnetrygdPeriode(
                BisysStønadstype.UTVIDET,
                YearMonth.of(2019, 3),
                null,
                1054.0,
                manueltBeregnet = false,
                deltBosted = false,
            )
        every {
            mockInfotrygdKlient.hentUtvidetBarnetrygd(
                andelTilkjentYtelse.aktør.aktivFødselsnummer(),
                any(),
            )
        } returns
            BisysUtvidetBarnetrygdResponse(
                perioder = listOf(periodeInfotrygd),
            )

        every { mockFagsakRepository.finnFagsakForAktør(any()) } returns behandling.fagsak

        every { mockBehandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id) } returns behandling
        every { mockTilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandling.id) } returns andelTilkjentYtelse.tilkjentYtelse

        val response =
            bisysService.hentUtvidetBarnetrygd(andelTilkjentYtelse.aktør.aktivFødselsnummer(), LocalDate.of(2021, 1, 1))

        assertThat(response.perioder).hasSize(1)
        assertThat(response.perioder.first().beløp).isEqualTo(1054.0)
        assertThat(response.perioder.first().fomMåned).isEqualTo(YearMonth.of(2019, 3))
        assertThat(response.perioder.first().tomMåned).isEqualTo(YearMonth.of(2022, 12))
        assertThat(response.perioder.first().manueltBeregnet).isFalse
    }

    @Test
    fun `Skal ikke slå sammen resultat fra ba-sak og infotrygd hvis periode er manuelt beregnet i infotrygd`() {
        val behandling = lagBehandling()

        val tilkjentYtelse =
            lagInitiellTilkjentYtelse(
                behandling = behandling,
                utbetalingsoppdrag = lagMinimalUtbetalingsoppdragString(behandlingId = behandling.id),
            )

        val andelTilkjentYtelse =
            lagAndelTilkjentYtelseUtvidet(
                fom = "2020-01",
                tom = "2040-01",
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                behandling = behandling,
                tilkjentYtelse = tilkjentYtelse,
                beløp = 660,
            )
        tilkjentYtelse.andelerTilkjentYtelse.add(andelTilkjentYtelse)

        every { mockPersonidentService.hentAktør(any()) } answers { andelTilkjentYtelse.aktør }
        every { mockPersonidentService.hentAlleFødselsnummerForEnAktør(any()) } answers { listOf(andelTilkjentYtelse.aktør.aktivFødselsnummer()) }
        every { mockPersonidentService.hentIdenter(any(), any()) } answers { listOf(IdentInformasjon(andelTilkjentYtelse.aktør.aktivFødselsnummer(), false, Type.FOLKEREGISTERIDENT.name)) }

        val periodeInfotrygd =
            UtvidetBarnetrygdPeriode(
                BisysStønadstype.UTVIDET,
                YearMonth.of(2019, 1),
                YearMonth.of(2019, 12),
                660.0,
                manueltBeregnet = true,
                deltBosted = false,
            )
        every {
            mockInfotrygdKlient.hentUtvidetBarnetrygd(
                andelTilkjentYtelse.aktør.aktivFødselsnummer(),
                any(),
            )
        } returns
            BisysUtvidetBarnetrygdResponse(
                perioder = listOf(periodeInfotrygd),
            )

        every { mockFagsakRepository.finnFagsakForAktør(any()) } returns behandling.fagsak
        every { mockBehandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id) } returns behandling
        every { mockTilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandling.id) } returns andelTilkjentYtelse.tilkjentYtelse

        val response =
            bisysService.hentUtvidetBarnetrygd(andelTilkjentYtelse.aktør.aktivFødselsnummer(), LocalDate.of(2019, 1, 1))

        assertThat(response.perioder).hasSize(2)
        assertThat(response.perioder.first().beløp).isEqualTo(660.0)
        assertThat(response.perioder.first().fomMåned).isEqualTo(YearMonth.of(2019, 1))
        assertThat(response.perioder.first().tomMåned).isEqualTo(YearMonth.of(2019, 12))
        assertThat(response.perioder.first().manueltBeregnet).isTrue

        assertThat(response.perioder.last().beløp).isEqualTo(660.0)
        assertThat(response.perioder.last().fomMåned).isEqualTo(YearMonth.of(2020, 1))
        assertThat(response.perioder.last().tomMåned).isEqualTo(YearMonth.of(2040, 1))
        assertThat(response.perioder.last().manueltBeregnet).isFalse
    }
}
