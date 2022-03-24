package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestBaseFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestFagsak
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class BeregningServiceTest {

    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    private val behandlingService = mockk<BehandlingService>()
    private val behandlingResultatRepository = mockk<VilkårsvurderingRepository>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val søknadGrunnlagService = mockk<SøknadGrunnlagService>()
    val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    private val endretUtbetalingAndelRepository = mockk<EndretUtbetalingAndelRepository>()
    private val småbarnstilleggService = mockk<SmåbarnstilleggService>()
    private val featureToggleService = mockk<FeatureToggleService>()

    private lateinit var beregningService: BeregningService

    @BeforeEach
    fun setUp() {
        val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
        val fagsakService = mockk<FagsakService>()

        beregningService = BeregningService(
            andelTilkjentYtelseRepository,
            fagsakService,
            behandlingService,
            tilkjentYtelseRepository,
            vilkårsvurderingRepository,
            behandlingRepository,
            personopplysningGrunnlagRepository,
            endretUtbetalingAndelRepository,
            småbarnstilleggService
        )

        every { tilkjentYtelseRepository.slettTilkjentYtelseFor(any()) } just Runs
        every { fagsakService.hentRestFagsak(any()) } answers {
            Ressurs.success(
                defaultFagsak().tilRestBaseFagsak(false, emptyList(), null, null)
                    .tilRestFagsak(emptyList(), emptyList())
            )
        }
        every { featureToggleService.isEnabled(any()) } answers { true }
        every { endretUtbetalingAndelRepository.findByBehandlingId(any()) } answers { emptyList() }
    }

    @Test
    fun `Skal mappe perioderesultat til andel ytelser for innvilget vedtak med 18-års vilkår som sluttdato`() {
        val behandling = lagBehandling()

        val barn1Fnr = randomFnr()
        val søkerFnr = randomFnr()

        val barn1AktørId = tilAktør(barn1Fnr)
        val søkerAktørId = tilAktør(søkerFnr)

        val vilkårsvurdering =
            Vilkårsvurdering(behandling = behandling)

        val periodeFom = LocalDate.of(2020, 1, 1)
        val periodeTom = LocalDate.of(2020, 7, 1)
        val personResultatBarn = lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn1AktørId,
            resultat = Resultat.OPPFYLT,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN
        )

        val personResultatSøker = lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søkerAktørId,
            resultat = Resultat.OPPFYLT,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.SØKER
        )
        vilkårsvurdering.personResultater = setOf(personResultatBarn, personResultatSøker)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søkerFnr,
            barnasIdenter = listOf(barn1Fnr),
            barnFødselsdato = LocalDate.of(2002, 7, 1)
        )
        val slot = slot<TilkjentYtelse>()

        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(any()) } answers { vilkårsvurdering }
        every { tilkjentYtelseRepository.save(any()) } returns lagInitiellTilkjentYtelse(behandling)
        every { søknadGrunnlagService.hentAktiv(any())?.hentSøknadDto() } returns lagSøknadDTO(
            søkerFnr,
            listOf(barn1Fnr)
        )

        beregningService.oppdaterBehandlingMedBeregning(
            behandling = behandling,
            personopplysningGrunnlag = personopplysningGrunnlag
        )

        verify(exactly = 1) { tilkjentYtelseRepository.save(capture(slot)) }

        Assertions.assertEquals(1, slot.captured.andelerTilkjentYtelse.size)
        Assertions.assertEquals(1054, slot.captured.andelerTilkjentYtelse.first().kalkulertUtbetalingsbeløp)
        Assertions.assertEquals(periodeFom.nesteMåned(), slot.captured.andelerTilkjentYtelse.first().stønadFom)
        Assertions.assertEquals(periodeTom.forrigeMåned(), slot.captured.andelerTilkjentYtelse.first().stønadTom)
    }

    @Test
    fun `Skal mappe perioderesultat til andel ytelser for innvilget vedtak som spenner over flere satsperioder`() {
        val behandling = lagBehandling()
        val barn1Fnr = randomFnr()
        val søkerFnr = randomFnr()

        val barn1AktørId = tilAktør(barn1Fnr)
        val søkerAktørId = tilAktør(søkerFnr)

        val vilkårsvurdering =
            Vilkårsvurdering(behandling = behandling)

        val periodeFom = LocalDate.of(2018, 1, 1)
        val periodeTom = LocalDate.of(2020, 7, 1)
        val personResultatBarn = lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn1AktørId,
            resultat = Resultat.OPPFYLT,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN
        )

        val personResultatSøker = lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søkerAktørId,
            resultat = Resultat.OPPFYLT,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.SØKER
        )
        vilkårsvurdering.personResultater = setOf(personResultatBarn, personResultatSøker)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søkerFnr,
            barnasIdenter = listOf(barn1Fnr)
        )
        val slot = slot<TilkjentYtelse>()

        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(any()) } answers { vilkårsvurdering }
        every { tilkjentYtelseRepository.save(any()) } returns lagInitiellTilkjentYtelse(behandling)
        every { søknadGrunnlagService.hentAktiv(any())?.hentSøknadDto() } returns lagSøknadDTO(
            søkerFnr,
            listOf(barn1Fnr)
        )

        beregningService.oppdaterBehandlingMedBeregning(
            behandling = behandling,
            personopplysningGrunnlag = personopplysningGrunnlag
        )

        verify(exactly = 1) { tilkjentYtelseRepository.save(capture(slot)) }

        Assertions.assertEquals(2, slot.captured.andelerTilkjentYtelse.size)

        val andelerTilkjentYtelse = slot.captured.andelerTilkjentYtelse.sortedBy { it.stønadFom }
        val satsPeriode1Slutt = YearMonth.of(2019, 2)
        val satsPeriode2Start = YearMonth.of(2019, 3)

        Assertions.assertEquals(970, andelerTilkjentYtelse.first().kalkulertUtbetalingsbeløp)
        Assertions.assertEquals(periodeFom.nesteMåned(), andelerTilkjentYtelse.first().stønadFom)
        Assertions.assertEquals(satsPeriode1Slutt, andelerTilkjentYtelse.first().stønadTom)

        Assertions.assertEquals(1054, andelerTilkjentYtelse.last().kalkulertUtbetalingsbeløp)
        Assertions.assertEquals(satsPeriode2Start, andelerTilkjentYtelse.last().stønadFom)
        Assertions.assertEquals(periodeTom.toYearMonth(), andelerTilkjentYtelse.last().stønadTom)
    }

    @Test
    fun `Skal verifisere at endret utbetaling andel appliseres på en innvilget utbetaling andel`() {
        val behandling = lagBehandling()
        val barn = tilfeldigPerson(personType = PersonType.BARN)
        val søkerFnr = randomFnr()
        val søkerAktør = tilAktør(søkerFnr)
        val vilkårsvurdering =
            Vilkårsvurdering(behandling = behandling)

        val periodeFom = LocalDate.of(2018, 1, 1)
        val periodeTom = LocalDate.of(2018, 7, 1)
        val avtaletidspunktDeltBosted = LocalDate.of(2018, 7, 1)
        val søkandtidspunkt = LocalDate.of(2018, 9, 1)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søkerFnr,
            barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()),
            barnAktør = listOf(barn.aktør),
            søkerAktør = søkerAktør
        )

        val personResultatBarn = lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = personopplysningGrunnlag.barna.first().aktør,
            resultat = Resultat.OPPFYLT,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN
        )

        val personResultatSøker = lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = personopplysningGrunnlag.søker.aktør,
            resultat = Resultat.OPPFYLT,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.SØKER
        )
        vilkårsvurdering.personResultater = setOf(personResultatBarn, personResultatSøker)

        val slot = slot<TilkjentYtelse>()

        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(any()) } answers { vilkårsvurdering }
        every { tilkjentYtelseRepository.save(any()) } returns lagInitiellTilkjentYtelse(behandling)
        every { søknadGrunnlagService.hentAktiv(any())?.hentSøknadDto() } returns lagSøknadDTO(
            søkerFnr,
            listOf(barn.aktør.aktivFødselsnummer())
        )

        every { endretUtbetalingAndelRepository.findByBehandlingId(any()) } returns
            listOf(
                EndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = barn, prosent = BigDecimal(50),
                    fom = periodeFom.toYearMonth(),
                    tom = periodeTom.toYearMonth(),
                    avtaletidspunktDeltBosted = avtaletidspunktDeltBosted,
                    søknadstidspunkt = søkandtidspunkt,
                    årsak = Årsak.DELT_BOSTED,
                    begrunnelse = "En begrunnelse",
                    andelTilkjentYtelser = mutableListOf(
                        lagAndelTilkjentYtelse(
                            fom = periodeFom.toYearMonth(),
                            tom = periodeTom.toYearMonth()
                        )
                    )
                )
            )

        beregningService.oppdaterBehandlingMedBeregning(
            behandling = behandling,
            personopplysningGrunnlag = personopplysningGrunnlag
        )

        verify(exactly = 1) { tilkjentYtelseRepository.save(capture(slot)) }

        Assertions.assertEquals(1, slot.captured.andelerTilkjentYtelse.size)

        val andelerTilkjentYtelse = slot.captured.andelerTilkjentYtelse.sortedBy { it.stønadFom }

        Assertions.assertEquals(970 / 2, andelerTilkjentYtelse.first().kalkulertUtbetalingsbeløp)
        Assertions.assertEquals(periodeFom.nesteMåned(), andelerTilkjentYtelse.first().stønadFom)
        Assertions.assertEquals(periodeTom.toYearMonth(), andelerTilkjentYtelse.first().stønadTom)
    }

    @Test
    fun `Skal mappe perioderesultat til andel ytelser for avslått vedtak`() {
        val behandling = lagBehandling()
        val barn1Fnr = randomFnr()
        val søkerFnr = randomFnr()
        val barn1AktørId = tilAktør(barn1Fnr)
        val søkerAktørId = tilAktør(søkerFnr)
        val vilkårsvurdering =
            Vilkårsvurdering(behandling = behandling)

        val periodeFom = LocalDate.of(2020, 1, 1)
        val periodeTom = LocalDate.of(2020, 11, 1)
        val personResultatBarn = lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn1AktørId,
            resultat = Resultat.OPPFYLT,
            periodeFom = periodeFom,
            periodeTom = periodeTom
        )

        val personResultatSøker = lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søkerAktørId,
            resultat = Resultat.IKKE_OPPFYLT,
            periodeFom = periodeFom,
            periodeTom = periodeTom
        )
        vilkårsvurdering.personResultater = setOf(personResultatBarn, personResultatSøker)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søkerFnr,
            barnasIdenter = listOf(barn1Fnr)
        )
        val slot = slot<TilkjentYtelse>()

        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(any()) } answers { vilkårsvurdering }
        every { tilkjentYtelseRepository.save(any()) } returns lagInitiellTilkjentYtelse(behandling)
        every { søknadGrunnlagService.hentAktiv(any())?.hentSøknadDto() } returns lagSøknadDTO(
            søkerFnr,
            listOf(barn1Fnr)
        )

        beregningService.oppdaterBehandlingMedBeregning(
            behandling = behandling,
            personopplysningGrunnlag = personopplysningGrunnlag
        )

        verify(exactly = 1) { tilkjentYtelseRepository.save(capture(slot)) }

        Assertions.assertTrue(slot.captured.andelerTilkjentYtelse.isEmpty())
    }

    @Test
    fun `For flere barn med forskjellige perioderesultat skal perioderesultat mappes til andel ytelser`() {
        val behandling = lagBehandling()
        val barnFødselsdato = LocalDate.of(2019, 1, 1)
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val søkerFnr = randomFnr()
        val barn1AktørId = tilAktør(barn1Fnr)
        val barn2AktørId = tilAktør(barn2Fnr)
        val søkerAktørId = tilAktør(søkerFnr)
        val vilkårsvurdering = Vilkårsvurdering(
            behandling = behandling
        )

        val periode1Fom = LocalDate.of(2020, 1, 1)
        val periode1Tom = LocalDate.of(2020, 11, 13)

        val periode2Fom = LocalDate.of(2020, 12, 1)
        val periode2Midt = LocalDate.of(2021, 6, 1)
        val periode2Tom = LocalDate.of(2021, 12, 11)

        val periode3Fom = LocalDate.of(2022, 1, 12)
        val periode3Midt = LocalDate.of(2023, 6, 10)
        val periode3Tom = LocalDate.of(2028, 1, 1)

        val tilleggFom = SatsService.hentDatoForSatsendring(satstype = SatsType.TILLEGG_ORBA, oppdatertBeløp = 1354)

        val personResultat = mutableSetOf(
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = søkerAktørId,
                resultat = Resultat.OPPFYLT,
                periodeFom = periode1Fom,
                periodeTom = periode1Tom,
                lagFullstendigVilkårResultat = true,
                personType = PersonType.SØKER
            ),
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = søkerAktørId,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = periode2Fom,
                periodeTom = periode2Tom,
                lagFullstendigVilkårResultat = true,
                personType = PersonType.SØKER
            ),
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = søkerAktørId,
                resultat = Resultat.OPPFYLT,
                periodeFom = periode3Fom,
                periodeTom = periode3Tom,
                lagFullstendigVilkårResultat = true,
                personType = PersonType.SØKER
            ),
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn1AktørId,
                resultat = Resultat.OPPFYLT,
                periodeFom = periode1Fom.minusYears(1),
                periodeTom = periode3Tom.plusYears(1),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN
            ),
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn2AktørId,
                resultat = Resultat.OPPFYLT,
                periodeFom = periode2Midt,
                periodeTom = periode3Midt,
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN
            )
        )

        vilkårsvurdering.personResultater = personResultat

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søkerFnr,
            barnasIdenter = listOf(barn1Fnr, barn2Fnr),
            barnFødselsdato = barnFødselsdato
        )
        val slot = slot<TilkjentYtelse>()

        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(any()) } answers { vilkårsvurdering }
        every { tilkjentYtelseRepository.save(any()) } returns lagInitiellTilkjentYtelse(behandling)
        every { søknadGrunnlagService.hentAktiv(any())?.hentSøknadDto() } returns lagSøknadDTO(
            søkerFnr,
            listOf(barn1Fnr, barn2Fnr)
        )

        beregningService.oppdaterBehandlingMedBeregning(
            behandling = behandling,
            personopplysningGrunnlag = personopplysningGrunnlag
        )

        verify(exactly = 1) { tilkjentYtelseRepository.save(capture(slot)) }

        Assertions.assertEquals(5, slot.captured.andelerTilkjentYtelse.size)
        val andelerTilkjentYtelse = slot.captured.andelerTilkjentYtelse.sortedBy { it.stønadTom }

        val (andelerBarn1, andelerBarn2) = andelerTilkjentYtelse.partition { it.aktør.aktivFødselsnummer() == barn1Fnr }

        // Barn 1 - første periode (før satsendring)
        Assertions.assertEquals(periode1Fom.nesteMåned(), andelerBarn1[0].stønadFom)
        Assertions.assertEquals(tilleggFom!!.forrigeMåned(), andelerBarn1[0].stønadTom)
        Assertions.assertEquals(1054, andelerBarn1[0].kalkulertUtbetalingsbeløp)

        // Barn 1 - første periode (etter sept 2020 satsendring og før sept 2021 satsendring, før fylte 6 år)
        Assertions.assertEquals(tilleggFom.toYearMonth(), andelerBarn1[1].stønadFom)
        Assertions.assertEquals(periode1Tom.toYearMonth(), andelerBarn1[1].stønadTom)
        Assertions.assertEquals(1354, andelerBarn1[1].kalkulertUtbetalingsbeløp)

        // Barn 1 - andre periode (etter siste satsendring, før fylte 6 år)
        Assertions.assertEquals(periode3Fom.nesteMåned(), andelerBarn1[2].stønadFom)
        Assertions.assertEquals(barnFødselsdato.plusYears(6).forrigeMåned(), andelerBarn1[2].stønadTom)
        Assertions.assertEquals(1676, andelerBarn1[2].kalkulertUtbetalingsbeløp)

        // Barn 1 - andre periode (etter fylte 6 år)
        Assertions.assertEquals(barnFødselsdato.plusYears(6).toYearMonth(), andelerBarn1[3].stønadFom)
        Assertions.assertEquals(periode3Tom.toYearMonth(), andelerBarn1[3].stønadTom)
        Assertions.assertEquals(1054, andelerBarn1[3].kalkulertUtbetalingsbeløp)

        // Barn 2 sin eneste periode (etter siste satsendring)
        Assertions.assertEquals(periode3Fom.nesteMåned(), andelerBarn2[0].stønadFom)
        Assertions.assertEquals(periode3Midt.toYearMonth(), andelerBarn2[0].stønadTom)
        Assertions.assertEquals(1676, andelerBarn2[0].kalkulertUtbetalingsbeløp)
    }

    @Test
    fun `Dersom barn har flere godkjente perioderesultat back2back skal det ikke bli glippe i andel ytelser`() {
        val førstePeriodeTomForBarnet = LocalDate.of(2020, 11, 30)
        kjørScenarioForBack2Backtester(
            førstePeriodeTomForBarnet = førstePeriodeTomForBarnet,
            andrePeriodeFomForBarnet = førstePeriodeTomForBarnet.plusDays(1),
            forventetSluttForFørsteAndelsperiode = førstePeriodeTomForBarnet.plusMonths(1).toYearMonth(),
            forventetStartForAndreAndelsperiode = førstePeriodeTomForBarnet.plusMonths(2).toYearMonth()
        )
    }

    @Test
    fun `Dersom barn har flere godkjente perioderesultat som ikke følger back2back skal det bli glippe på en måned i andel ytelser`() {
        val førstePeriodeTomForBarnet = LocalDate.of(2020, 11, 29)
        kjørScenarioForBack2Backtester(
            førstePeriodeTomForBarnet = førstePeriodeTomForBarnet,
            andrePeriodeFomForBarnet = førstePeriodeTomForBarnet.plusDays(2),
            forventetSluttForFørsteAndelsperiode = førstePeriodeTomForBarnet.toYearMonth(),
            forventetStartForAndreAndelsperiode = førstePeriodeTomForBarnet.plusMonths(2).toYearMonth()
        )
    }

    @Test
    fun `Dersom barn har flere godkjente perioderesultat back2back og delt bosted kun i første periode skal det ikke bli glippe i andel ytelser men beløpsendringen skal inntreffe som normalt neste måned`() {
        val førstePeriodeTomForBarnet = LocalDate.of(2020, 11, 30)
        kjørScenarioForBack2Backtester(
            førstePeriodeTomForBarnet = førstePeriodeTomForBarnet,
            andrePeriodeFomForBarnet = LocalDate.of(2020, 12, 1),
            forventetSluttForFørsteAndelsperiode = førstePeriodeTomForBarnet.plusMonths(1).toYearMonth(),
            forventetStartForAndreAndelsperiode = førstePeriodeTomForBarnet.plusMonths(2).toYearMonth(),
            deltBostedForFørstePeriode = true
        )
    }

    @Test
    fun `Dersom barn har flere godkjente perioderesultat back2back og delt bosted kun i andre periode skal det ikke bli glippe i andel ytelser men beløpsendringen skal inntreffe som normalt neste måned`() {
        val førstePeriodeTomForBarnet = LocalDate.of(2020, 11, 30)
        kjørScenarioForBack2Backtester(
            førstePeriodeTomForBarnet = førstePeriodeTomForBarnet,
            andrePeriodeFomForBarnet = LocalDate.of(2020, 12, 1),
            forventetSluttForFørsteAndelsperiode = førstePeriodeTomForBarnet.plusMonths(1).toYearMonth(),
            forventetStartForAndreAndelsperiode = førstePeriodeTomForBarnet.plusMonths(2).toYearMonth(),
            deltBostedForAndrePeriode = true
        )
    }

    @Test
    fun `Dersom barn har flere godkjente perioderesultat back2back der alle er delt bosted skal det ikke bli glippe i andel ytelser`() {
        val førstePeriodeTomForBarnet = LocalDate.of(2020, 11, 30)
        kjørScenarioForBack2Backtester(
            førstePeriodeTomForBarnet = førstePeriodeTomForBarnet,
            andrePeriodeFomForBarnet = førstePeriodeTomForBarnet.plusDays(1),
            forventetSluttForFørsteAndelsperiode = førstePeriodeTomForBarnet.plusMonths(1).toYearMonth(),
            forventetStartForAndreAndelsperiode = førstePeriodeTomForBarnet.plusMonths(2).toYearMonth(),
            deltBostedForFørstePeriode = true,
            deltBostedForAndrePeriode = true
        )
    }

    private fun kjørScenarioForBack2Backtester(
        førstePeriodeTomForBarnet: LocalDate,
        andrePeriodeFomForBarnet: LocalDate,
        forventetSluttForFørsteAndelsperiode: YearMonth,
        forventetStartForAndreAndelsperiode: YearMonth,
        deltBostedForFørstePeriode: Boolean = false,
        deltBostedForAndrePeriode: Boolean = false
    ) {
        val behandling = lagBehandling()
        val barnFødselsdato = LocalDate.of(2019, 1, 1)
        val barn1Fnr = randomFnr()
        val søkerFnr = randomFnr()
        val barn1AktørId = tilAktør(barn1Fnr)
        val søkerAktørId = tilAktør(søkerFnr)
        val vilkårsvurdering = Vilkårsvurdering(
            behandling = behandling
        )

        val førstePeriodeFomForBarnet = LocalDate.of(2020, 1, 1)
        val andrePeriodeTomForBarnet = LocalDate.of(2021, 12, 11)

        // Den godkjente perioden for søker er ikke så relevant for testen annet enn at den settes før og etter periodene som brukes for barnet.
        val periodeTomForSøker = andrePeriodeTomForBarnet.plusMonths(1)

        val førsteSatsendringFom =
            SatsService.hentDatoForSatsendring(satstype = SatsType.TILLEGG_ORBA, oppdatertBeløp = 1354)!!
        val andreSatsendringFom =
            SatsService.hentDatoForSatsendring(satstype = SatsType.TILLEGG_ORBA, oppdatertBeløp = 1654)!!

        val personResultat = mutableSetOf(
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = søkerAktørId,
                resultat = Resultat.OPPFYLT,
                periodeFom = førstePeriodeFomForBarnet,
                periodeTom = periodeTomForSøker,
                lagFullstendigVilkårResultat = true,
                personType = PersonType.SØKER
            ),
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn1AktørId,
                resultat = Resultat.OPPFYLT,
                periodeFom = førstePeriodeFomForBarnet,
                periodeTom = førstePeriodeTomForBarnet,
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
                erDeltBosted = deltBostedForFørstePeriode
            ),
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn1AktørId,
                resultat = Resultat.OPPFYLT,
                periodeFom = andrePeriodeFomForBarnet,
                periodeTom = andrePeriodeTomForBarnet,
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
                erDeltBosted = deltBostedForAndrePeriode
            )
        )

        vilkårsvurdering.personResultater = personResultat

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søkerFnr,
            barnasIdenter = listOf(barn1Fnr),
            barnFødselsdato = barnFødselsdato
        )
        val slot = slot<TilkjentYtelse>()

        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(any()) } answers { vilkårsvurdering }
        every { tilkjentYtelseRepository.save(any()) } returns lagInitiellTilkjentYtelse(behandling)
        every { søknadGrunnlagService.hentAktiv(any())?.hentSøknadDto() } returns lagSøknadDTO(
            søkerFnr,
            listOf(barn1Fnr)
        )

        beregningService.oppdaterBehandlingMedBeregning(
            behandling = behandling,
            personopplysningGrunnlag = personopplysningGrunnlag
        )

        verify(exactly = 1) { tilkjentYtelseRepository.save(capture(slot)) }

        Assertions.assertEquals(4, slot.captured.andelerTilkjentYtelse.size)
        val andelerTilkjentYtelse = slot.captured.andelerTilkjentYtelse.sortedBy { it.stønadTom }

        // Første periode (før satsendring)
        Assertions.assertEquals(førstePeriodeFomForBarnet.nesteMåned(), andelerTilkjentYtelse[0].stønadFom)
        Assertions.assertEquals(førsteSatsendringFom.forrigeMåned(), andelerTilkjentYtelse[0].stønadTom)
        Assertions.assertEquals(
            if (deltBostedForFørstePeriode) 527 else 1054,
            andelerTilkjentYtelse[0].kalkulertUtbetalingsbeløp
        )

        // Andre periode (fra første satsendring til slutt av første godkjente perioderesultat for barnet)
        Assertions.assertEquals(førsteSatsendringFom.toYearMonth(), andelerTilkjentYtelse[1].stønadFom)
        Assertions.assertEquals(forventetSluttForFørsteAndelsperiode, andelerTilkjentYtelse[1].stønadTom)
        Assertions.assertEquals(
            if (deltBostedForFørstePeriode) 677 else 1354,
            andelerTilkjentYtelse[1].kalkulertUtbetalingsbeløp
        )

        // Tredje periode (fra start av andre godkjente perioderesultat for barnet til neste satsendring).
        // At denne perioden følger back2back med tom for forrige periode er primært det som testes her.
        Assertions.assertEquals(forventetStartForAndreAndelsperiode, andelerTilkjentYtelse[2].stønadFom)
        Assertions.assertEquals(andreSatsendringFom.forrigeMåned(), andelerTilkjentYtelse[2].stønadTom)
        Assertions.assertEquals(
            if (deltBostedForAndrePeriode) 677 else 1354,
            andelerTilkjentYtelse[2].kalkulertUtbetalingsbeløp
        )

        // Fjerde periode (fra siste satsendring til slutt av endre godkjente perioderesultat for barnet)
        Assertions.assertEquals(andreSatsendringFom.toYearMonth(), andelerTilkjentYtelse[3].stønadFom)
        Assertions.assertEquals(andrePeriodeTomForBarnet.toYearMonth(), andelerTilkjentYtelse[3].stønadTom)
        Assertions.assertEquals(
            if (deltBostedForAndrePeriode) 827 else 1654,
            andelerTilkjentYtelse[3].kalkulertUtbetalingsbeløp
        )
    }
}
