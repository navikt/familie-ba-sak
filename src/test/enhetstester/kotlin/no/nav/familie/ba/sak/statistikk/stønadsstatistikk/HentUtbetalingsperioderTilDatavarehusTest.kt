package no.nav.familie.ba.sak.statistikk.stønadsstatistikk

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelseUtvidet
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HentUtbetalingsperioderTilDatavarehusTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>(relaxed = true)
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val vedtakService = mockk<VedtakService>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val kompetanseService = mockk<KompetanseService>()
    private val vedtakRepository = mockk<VedtakRepository>()
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService = mockk<AndelerTilkjentYtelseOgEndreteUtbetalingerService>()

    private val stønadsstatistikkService =
        StønadsstatistikkService(
            behandlingHentOgPersisterService,
            persongrunnlagService,
            vedtakService,
            personopplysningerService,
            vedtakRepository,
            kompetanseService,
            andelerTilkjentYtelseOgEndreteUtbetalingerService,
        )
    private val behandling = lagBehandling()
    private val søkerFnr = "12345678910"
    private val barnFnr = listOf(randomFnr(), randomFnr())
    private val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, barnFnr)
    private val barn1 = personopplysningGrunnlag.barna.first()

    @Test
    fun `hentUtbetalingsperioderTilDatavarehus() setter ikke DelytelseId hvis behandlingen ikke er sendt til oppdrag`() {
        // Arrange
        val andelTilkjentYtelseBarn =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                barn1.fødselsdato.nesteMåned(),
                barn1.fødselsdato.plusYears(3).toYearMonth(),
                YtelseType.ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = barn1,
                aktør = barn1.aktør,
            )

        setup(andelTilkjentYtelseBarn)

        // Act
        val utbetalingsPerioder = stønadsstatistikkService.hentUtbetalingsperioderTilDatavarehus(behandling, personopplysningGrunnlag)

        // Assert
        val delytelseIder = utbetalingsPerioder.flatMap { it.utbetalingsDetaljer }.map { it.delytelseId }
        assertThat(delytelseIder).containsOnlyNulls()
    }

    @Test
    fun `hentUtbetalingsperioderTilDatavarehus() setter DelytelseId hvis behandlingen er sendt til oppdrag`() {
        // Arrange
        val andelTilkjentYtelseBarn =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                barn1.fødselsdato.nesteMåned(),
                barn1.fødselsdato.plusYears(3).toYearMonth(),
                YtelseType.ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = barn1,
                aktør = barn1.aktør,
                periodeIdOffset = 1,
            )
        setup(andelTilkjentYtelseBarn)

        // Act
        val utbetalingsPerioder = stønadsstatistikkService.hentUtbetalingsperioderTilDatavarehus(behandling, personopplysningGrunnlag)

        // Assert
        val delytelseIder = utbetalingsPerioder.flatMap { it.utbetalingsDetaljer }.map { it.delytelseId }
        assertThat(delytelseIder).contains("11")
    }

    private fun setup(andelTilkjentYtelseBarn: AndelTilkjentYtelseMedEndreteUtbetalinger) {
        val vedtak = lagVedtak(behandling)

        val kompetanseperioder =
            setOf(
                no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse(
                    fom = YearMonth.now(),
                    tom = null,
                    barnAktører = setOf(barn1.aktør),
                    søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                    annenForeldersAktivitet = KompetanseAktivitet.I_ARBEID,
                    annenForeldersAktivitetsland = "PL",
                    barnetsBostedsland = "PL",
                    resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                ),
                no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse(
                    fom = null,
                    tom = null,
                    barnAktører = emptySet(),
                    søkersAktivitet = null,
                    annenForeldersAktivitet = null,
                    annenForeldersAktivitetsland = null,
                    barnetsBostedsland = null,
                    resultat = null,
                ),
            )

        val andelTilkjentYtelseSøker =
            lagAndelTilkjentYtelseUtvidet(
                barn1.fødselsdato.nesteMåned().toString(),
                barn1.fødselsdato
                    .plusYears(2)
                    .toYearMonth()
                    .toString(),
                YtelseType.UTVIDET_BARNETRYGD,
                behandling = behandling,
                person = personopplysningGrunnlag.søker,
            )

        val andelerTilkjentYtelse =
            listOf(
                andelTilkjentYtelseBarn,
                AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(andelTilkjentYtelseSøker),
            )

        every { behandlingHentOgPersisterService.hent(any()) } returns behandling
        every { kompetanseService.hentKompetanser(any()) } returns kompetanseperioder
        every { persongrunnlagService.hentAktivThrows(any()) } returns personopplysningGrunnlag
        every { vedtakService.hentAktivForBehandling(any()) } returns vedtak
        every { personopplysningerService.hentLandkodeAlpha2UtenlandskBostedsadresse(any()) } returns "DK"
        every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(any()) } returns
            andelerTilkjentYtelse
    }
}
