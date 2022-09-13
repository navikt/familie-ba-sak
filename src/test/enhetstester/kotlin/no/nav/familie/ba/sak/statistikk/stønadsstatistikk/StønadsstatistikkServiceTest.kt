package no.nav.familie.ba.sak.statistikk.stønadsstatistikk

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseUtvidet
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.ClientMocks.Companion.barnFnr
import no.nav.familie.ba.sak.config.ClientMocks.Companion.søkerFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.økonomi.sats
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseOgEndreteUtbetalingerKombinator
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.SøkersAktivitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockKExtension::class)
internal class StønadsstatistikkServiceTest(
    @MockK(relaxed = true)
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,

    @MockK
    private val persongrunnlagService: PersongrunnlagService,

    @MockK
    private val beregningService: BeregningService,

    @MockK
    private val vedtakService: VedtakService,

    @MockK
    private val personopplysningerService: PersonopplysningerService,

    @MockK
    private val kompetanseService: KompetanseService,

    @MockK
    private val vedtakRepository: VedtakRepository,

    @MockK
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService
) {

    private val stønadsstatistikkService =
        StønadsstatistikkService(
            behandlingHentOgPersisterService,
            persongrunnlagService,
            beregningService,
            vedtakService,
            personopplysningerService,
            vedtakRepository,
            kompetanseService,
            andelerTilkjentYtelseOgEndreteUtbetalingerService
        )
    private val behandling = lagBehandling()
    private val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr[0], barnFnr.toList())
    private val barn1 = personopplysningGrunnlag.barna.first()
    private val barn2 = personopplysningGrunnlag.barna.last()

    @BeforeAll
    fun init() {
        MockKAnnotations.init(this)

        val vedtak = lagVedtak(behandling)

        val andelTilkjentYtelseBarn1 = lagAndelTilkjentYtelse(
            barn1.fødselsdato.nesteMåned(),
            barn1.fødselsdato.plusYears(3).toYearMonth(),
            YtelseType.ORDINÆR_BARNETRYGD,
            behandling = behandling,
            person = barn1,
            aktør = barn1.aktør,
            periodeIdOffset = 1

        )
        val andelTilkjentYtelseBarn2PeriodeMed0Beløp = lagAndelTilkjentYtelse(
            barn2.fødselsdato.nesteMåned(),
            barn2.fødselsdato.plusYears(18).forrigeMåned(),
            YtelseType.ORDINÆR_BARNETRYGD,
            behandling = behandling,
            person = barn2,
            beløp = 0,
            aktør = barn2.aktør,
            prosent = BigDecimal(0),
            periodeIdOffset = null
        )

        val kompetanseperioder = setOf<no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse>(
            no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse(
                fom = YearMonth.now(),
                tom = null,
                barnAktører = setOf(barn1.aktør),
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
                annenForeldersAktivitet = AnnenForeldersAktivitet.I_ARBEID,
                annenForeldersAktivitetsland = "PL",
                barnetsBostedsland = "PL",
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND
            )
        )

        val andelTilkjentYtelseSøker = lagAndelTilkjentYtelseUtvidet(
            barn2.fødselsdato.nesteMåned().toString(),
            barn2.fødselsdato.plusYears(2).toYearMonth().toString(),
            YtelseType.UTVIDET_BARNETRYGD,
            behandling = behandling,
            person = personopplysningGrunnlag.søker,
            periodeIdOffset = 3
        )

        val andelerTilkjentYtelse = listOf(
            andelTilkjentYtelseBarn1,
            andelTilkjentYtelseBarn2PeriodeMed0Beløp,
            andelTilkjentYtelseSøker
        )

        every { behandlingHentOgPersisterService.hent(any()) } returns behandling
        every { kompetanseService.hentKompetanser(any()) } returns kompetanseperioder
        every { persongrunnlagService.hentAktivThrows(any()) } returns personopplysningGrunnlag
        every { vedtakService.hentAktivForBehandling(any()) } returns vedtak
        every { personopplysningerService.hentLandkodeUtenlandskBostedsadresse(any()) } returns "DK"
        every { beregningService.hentAndelerTilkjentYtelseForBehandling(any()) } returns andelerTilkjentYtelse
        every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(any()) } returns
            AndelTilkjentYtelseOgEndreteUtbetalingerKombinator(
                andelerTilkjentYtelse,
                emptyList(),
                true
            ).lagAndelerMedEndringer()
    }

    @Test
    fun hentVedtakV2() {
        val vedtak = stønadsstatistikkService.hentVedtakV2(1L)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(vedtak))

        assertEquals(2, vedtak.utbetalingsperioderV2[0].utbetalingsDetaljer.size)
        assertEquals(
            1 * sats(YtelseType.ORDINÆR_BARNETRYGD) + sats(YtelseType.UTVIDET_BARNETRYGD),
            vedtak.utbetalingsperioderV2[0].utbetaltPerMnd
        )

        vedtak.utbetalingsperioderV2
            .flatMap { it.utbetalingsDetaljer.map { ud -> ud.person } }
            .filter { it.personIdent != søkerFnr[0] }
            .forEach {
                assertEquals(0, it.delingsprosentYtelse)
            }
    }

    /**
     * Nye årsaker må legges til VedtakDVH i familie-eksterne-kontrakter når det legges til i Behandling
     *
     * Endringenen må være bakoverkompatibel. Hvis man f.eks. endrer navn på en årsak, så må man være sikker på at det ikke er sendt
     * et slik vedtak til stønaddstatistikk.
     *
     * Hvis det er sendt et slik vedtak, så legger man heller til den nye verdien i VedtakDVH og ikke slette gamle
     *
     */
    @Test
    fun `Skal gi feil hvis det kommer en ny BehandlingÅrsak som det ikke er tatt høyde for mot stønaddstatistkk - Man trenger å oppdatere schema og varsle stønaddstatistikk - Tips i javadoc`() {
        val behandlingsÅrsakIBASak = enumValues<BehandlingÅrsak>().map { it.name }
        val behandlingsÅrsakFraEksternKontrakt =
            enumValues<no.nav.familie.eksterne.kontrakter.BehandlingÅrsak>().map { it.name }

        assertThat(behandlingsÅrsakIBASak).hasSize(behandlingsÅrsakFraEksternKontrakt.size)
            .containsAll(behandlingsÅrsakFraEksternKontrakt)
    }

    /**
     * Nye AnnenForeldersAktivitet må legges til VedtakDVHV2 i familie-eksterne-kontrakter når det legges til i Behandling
     *
     * Endringenen må være bakoverkompatibel. Hvis man f.eks. endrer navn på en AnnenForeldersAktivitet, så må man være sikker på at det ikke er sendt
     * et slik vedtak til stønaddstatistikk.
     *
     * Hvis det er sendt et slik vedtak, så legger man heller til den nye verdien i VedtakDVHV2 og ikke slette gamle
     *
     */
    @Test
    fun `Skal gi feil hvis det kommer en ny AnnenForeldersAktivitet som det ikke er tatt høyde for mot stønaddstatistkk - Man trenger å oppdatere schema og varsle stønaddstatistikk - Tips i javadoc`() {
        val annenForeldersAktivitet = enumValues<AnnenForeldersAktivitet>().map { it.name }
        val annenForeldersAktivitetFraEksternKontrakt =
            enumValues<no.nav.familie.eksterne.kontrakter.AnnenForeldersAktivitet>().map { it.name }

        assertThat(annenForeldersAktivitet).hasSize(annenForeldersAktivitetFraEksternKontrakt.size)
            .containsAll(annenForeldersAktivitetFraEksternKontrakt)
    }

    /**
     * Nye søkersAktivitet må legges til VedtakDVHV2 i familie-eksterne-kontrakter når det legges til i Behandling
     *
     * Endringenen må være bakoverkompatibel. Hvis man f.eks. endrer navn på en SøkersAktivitet, så må man være sikker på at det ikke er sendt
     * et slik vedtak til stønaddstatistikk.
     *
     * Hvis det er sendt et slik vedtak, så legger man heller til den nye verdien i VedtakDVHV2 og ikke slette gamle
     *
     */
    @Test
    fun `Skal gi feil hvis det kommer en ny søkersAktivitet som det ikke er tatt høyde for mot stønaddstatistkk - Man trenger å oppdatere schema og varsle stønaddstatistikk - Tips i javadoc`() {
        val søkersAktivitet = enumValues<SøkersAktivitet>().map { it.name }
        val søkersAktivitetFraEksternKontrakt =
            enumValues<no.nav.familie.eksterne.kontrakter.SøkersAktivitet>().map { it.name }

        assertThat(søkersAktivitetFraEksternKontrakt)
            .containsAll(søkersAktivitet)
    }

    /**
     * Nye KompetanseResultat må legges til VedtakDVHV2 i familie-eksterne-kontrakter når det legges til i Behandling
     *
     * Endringenen må være bakoverkompatibel. Hvis man f.eks. endrer navn på en KompetanseResultat, så må man være sikker på at det ikke er sendt
     * et slik vedtak til stønaddstatistikk.
     *
     * Hvis det er sendt et slik vedtak, så legger man heller til den nye verdien i VedtakDVHV2 og ikke slette gamle
     *
     */
    @Test
    fun `Skal gi feil hvis det kommer en ny KompetanseResultat som det ikke er tatt høyde for mot stønaddstatistkk - Man trenger å oppdatere schema og varsle stønaddstatistikk - Tips i javadoc`() {
        val kompetanseResultat = enumValues<KompetanseResultat>().map { it.name }
        val kompetanseResultatFraEksternKontrakt =
            enumValues<no.nav.familie.eksterne.kontrakter.KompetanseResultat>().map { it.name }

        assertThat(kompetanseResultat).hasSize(kompetanseResultatFraEksternKontrakt.size)
            .containsAll(kompetanseResultatFraEksternKontrakt)
    }

    /**
     * Nye behandlingstyper må legges til VedtakDVH i familie-eksterne-kontrakter når det legges til i Behandling
     *
     * Endringenen må være bakoverkompatibel. Hvis man f.eks. endrer navn på en type, så må man være sikker på at det ikke er sendt
     * et slik vedtak til stønaddstatistikk.
     *
     * Hvis det er sendt et slik vedtak, så legger man heller til den nye verdien i VedtakDVH og ikke slette gamle
     *
     */
    @Test
    fun `Skal gi feil hvis det kommer en ny BehandlingType som det ikke er tatt høyde for mot stønaddstatistkk - Man trenger å oppdatere schema og varsle stønaddstatistikk`() {
        val behandlingsTypeIBasak = enumValues<BehandlingType>().map { it.name }
        val behandlingsTypeFraStønadskontrakt =
            enumValues<no.nav.familie.eksterne.kontrakter.BehandlingType>().map { it.name }

        assertThat(behandlingsTypeIBasak).hasSize(behandlingsTypeFraStønadskontrakt.size)
            .containsAll(behandlingsTypeFraStønadskontrakt)
    }
}
