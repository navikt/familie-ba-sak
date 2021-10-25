package no.nav.familie.ba.sak.ekstern.bisys

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseUtvidet
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.time.LocalDate
import java.time.YearMonth

@TestInstance(Lifecycle.PER_CLASS)
internal class BisysServiceTest() {

    lateinit var bisysService: BisysService
    var mockPersonopplysningerService = mockk<PersonopplysningerService>()
    var mockFagsakPersonRepository = mockk<FagsakPersonRepository>()
    var mockBehandlingService = mockk<BehandlingService>()
    var mockTilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    var mockInfotrygdClient = mockk<InfotrygdBarnetrygdClient>()

    @BeforeAll
    fun setUp() {
        bisysService = BisysService(
            mockInfotrygdClient,
            mockPersonopplysningerService,
            mockFagsakPersonRepository,
            mockBehandlingService,
            mockTilkjentYtelseRepository
        )
    }

    @Test
    fun `Skal returnere tom liste siden person ikke har finens i infotrygd og barnetrygd`() {
        val fnr = randomFnr()
        every { mockPersonopplysningerService.hentIdenter(Ident(fnr)) } returns listOf(
            IdentInformasjon(
                ident = fnr,
                false,
                "FOLKEREGISTERIDENT"
            )
        )

        every { mockInfotrygdClient.hentUtvidetBarnetrygd(fnr, any()) } returns BisysUtvidetBarnetrygdResponse(
            perioder = emptyList()
        )

        every { mockFagsakPersonRepository.finnFagsak(setOf(PersonIdent(ident = fnr))) } returns null

        val response = bisysService.hentUtvidetBarnetrygd(fnr, LocalDate.of(2021, 1, 1))

        assertThat(response.perioder).hasSize(0)
    }

    @Test
    fun `Skal returnere periode kun fra infotrygd`() {
        val fnr = randomFnr()
        every { mockPersonopplysningerService.hentIdenter(Ident(fnr)) } returns listOf(
            IdentInformasjon(
                ident = fnr,
                false,
                "FOLKEREGISTERIDENT"
            )
        )
        val periodeInfotrygd = UtvidetBarnetrygdPeriode(
            BisysStønadstype.UTVIDET,
            YearMonth.of(2019, 1),
            YearMonth.now(),
            500.0,
            manueltBeregnet = true
        )
        every { mockInfotrygdClient.hentUtvidetBarnetrygd(fnr, any()) } returns BisysUtvidetBarnetrygdResponse(
            perioder = listOf(periodeInfotrygd)
        )

        every { mockFagsakPersonRepository.finnFagsak(setOf(PersonIdent(ident = fnr))) } returns null

        val response = bisysService.hentUtvidetBarnetrygd(fnr, LocalDate.of(2021, 1, 1))

        assertThat(response.perioder).hasSize(1).contains(periodeInfotrygd)
    }

    @Test
    fun `Skal returnere utvidet barnetrygdperiode fra basak`() {
        val behandling = lagBehandling()

        var tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling).copy(utbetalingsoppdrag = "utbetalt")

        val andelTilkjentYtelse =
            lagAndelTilkjentYtelseUtvidet(
                fom = "2020-01",
                tom = "2040-01",
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                behandling = behandling,
                tilkjentYtelse = tilkjentYtelse,
                beløp = 660
            )
        tilkjentYtelse.andelerTilkjentYtelse.add(andelTilkjentYtelse)

        every { mockPersonopplysningerService.hentIdenter(Ident(andelTilkjentYtelse.personIdent)) } returns listOf(
            IdentInformasjon(
                ident = andelTilkjentYtelse.personIdent,
                false,
                "FOLKEREGISTERIDENT"
            )
        )

        every { mockInfotrygdClient.hentUtvidetBarnetrygd(any(), any()) } returns BisysUtvidetBarnetrygdResponse(
            perioder = emptyList()
        )

        every { mockFagsakPersonRepository.finnFagsak(any()) } returns behandling.fagsak
        every { mockBehandlingService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id) } returns behandling
        every { mockTilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandling.id) } returns andelTilkjentYtelse.tilkjentYtelse

        val response = bisysService.hentUtvidetBarnetrygd(andelTilkjentYtelse.personIdent, LocalDate.of(2021, 1, 1))

        assertThat(response.perioder).hasSize(1)
        assertThat(response.perioder.first().beløp).isEqualTo(660.0)
        assertThat(response.perioder.first().fomMåned).isEqualTo(YearMonth.of(2020, 1))
        assertThat(response.perioder.first().tomMåned).isEqualTo(YearMonth.of(2040, 1))
        assertThat(response.perioder.first().manueltBeregnet).isFalse()
    }

    @Test
    fun `Skal slå sammen resultat fra ba-sak og infotrygd`() {
        val behandling = lagBehandling()

        var tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling).copy(utbetalingsoppdrag = "utbetalt")

        val andelTilkjentYtelse =
            lagAndelTilkjentYtelseUtvidet(
                fom = "2020-01",
                tom = "2040-01",
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                behandling = behandling,
                tilkjentYtelse = tilkjentYtelse,
                beløp = 660
            )
        tilkjentYtelse.andelerTilkjentYtelse.add(andelTilkjentYtelse)

        every { mockPersonopplysningerService.hentIdenter(Ident(andelTilkjentYtelse.personIdent)) } returns listOf(
            IdentInformasjon(
                ident = andelTilkjentYtelse.personIdent,
                false,
                "FOLKEREGISTERIDENT"
            )
        )

        val periodeInfotrygd = UtvidetBarnetrygdPeriode(
            BisysStønadstype.UTVIDET,
            YearMonth.of(2019, 1),
            YearMonth.of(2019, 12),
            660.0,
            manueltBeregnet = false
        )
        every {
            mockInfotrygdClient.hentUtvidetBarnetrygd(
                andelTilkjentYtelse.personIdent,
                any()
            )
        } returns BisysUtvidetBarnetrygdResponse(
            perioder = listOf(periodeInfotrygd)
        )

        every { mockFagsakPersonRepository.finnFagsak(any()) } returns behandling.fagsak
        every { mockBehandlingService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id) } returns behandling
        every { mockTilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandling.id) } returns andelTilkjentYtelse.tilkjentYtelse

        val response = bisysService.hentUtvidetBarnetrygd(andelTilkjentYtelse.personIdent, LocalDate.of(2019, 1, 1))

        assertThat(response.perioder).hasSize(1)
        assertThat(response.perioder.first().beløp).isEqualTo(660.0)
        assertThat(response.perioder.first().fomMåned).isEqualTo(YearMonth.of(2019, 1))
        assertThat(response.perioder.first().tomMåned).isEqualTo(YearMonth.of(2040, 1))
        assertThat(response.perioder.first().manueltBeregnet).isFalse()
    }

    @Test
    fun `Skal ikke slå sammen resultat fra ba-sak og infotrygd hvis periode er manuelt beregnet i infotrygd`() {
        val behandling = lagBehandling()

        var tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling).copy(utbetalingsoppdrag = "utbetalt")

        val andelTilkjentYtelse =
            lagAndelTilkjentYtelseUtvidet(
                fom = "2020-01",
                tom = "2040-01",
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                behandling = behandling,
                tilkjentYtelse = tilkjentYtelse,
                beløp = 660
            )
        tilkjentYtelse.andelerTilkjentYtelse.add(andelTilkjentYtelse)

        every { mockPersonopplysningerService.hentIdenter(Ident(andelTilkjentYtelse.personIdent)) } returns listOf(
            IdentInformasjon(
                ident = andelTilkjentYtelse.personIdent,
                false,
                "FOLKEREGISTERIDENT"
            )
        )

        val periodeInfotrygd = UtvidetBarnetrygdPeriode(
            BisysStønadstype.UTVIDET,
            YearMonth.of(2019, 1),
            YearMonth.of(2019, 12),
            660.0,
            manueltBeregnet = true
        )
        every {
            mockInfotrygdClient.hentUtvidetBarnetrygd(
                andelTilkjentYtelse.personIdent,
                any()
            )
        } returns BisysUtvidetBarnetrygdResponse(
            perioder = listOf(periodeInfotrygd)
        )

        every { mockFagsakPersonRepository.finnFagsak(any()) } returns behandling.fagsak
        every { mockBehandlingService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id) } returns behandling
        every { mockTilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandling.id) } returns andelTilkjentYtelse.tilkjentYtelse

        val response = bisysService.hentUtvidetBarnetrygd(andelTilkjentYtelse.personIdent, LocalDate.of(2019, 1, 1))

        assertThat(response.perioder).hasSize(2)
        assertThat(response.perioder.first().beløp).isEqualTo(660.0)
        assertThat(response.perioder.first().fomMåned).isEqualTo(YearMonth.of(2019, 1))
        assertThat(response.perioder.first().tomMåned).isEqualTo(YearMonth.of(2019, 12))
        assertThat(response.perioder.first().manueltBeregnet).isTrue()

        assertThat(response.perioder.last().beløp).isEqualTo(660.0)
        assertThat(response.perioder.last().fomMåned).isEqualTo(YearMonth.of(2020, 1))
        assertThat(response.perioder.last().tomMåned).isEqualTo(YearMonth.of(2040, 1))
        assertThat(response.perioder.last().manueltBeregnet).isFalse()
    }
}
