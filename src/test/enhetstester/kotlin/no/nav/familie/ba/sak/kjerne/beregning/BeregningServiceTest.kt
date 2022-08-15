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
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestBaseFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestFagsak
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
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
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val søknadGrunnlagService = mockk<SøknadGrunnlagService>()
    private val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    private val endretUtbetalingAndelRepository = mockk<EndretUtbetalingAndelRepository>()
    private val småbarnstilleggService = mockk<SmåbarnstilleggService>()
    private val featureToggleService = mockk<FeatureToggleService>()

    private lateinit var beregningService: BeregningService

    @BeforeEach
    fun setUp() {
        val fagsakService = mockk<FagsakService>()

        beregningService = BeregningService(
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            behandlingRepository = behandlingRepository,
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            endretUtbetalingAndelRepository = endretUtbetalingAndelRepository,
            småbarnstilleggService = småbarnstilleggService,
            featureToggleService = featureToggleService
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
    fun `Skal ikke iverksettes i økonomi hvis mangler utbetalins perioder grunnet endret utbetalings periode`() {
        val skalIkkeIverksette = opprettAtyMedEndretUtbetalingsPeriode(
            behandlinsResultat = Behandlingsresultat.INNVILGET_OG_OPPHØRT,
            BehandlingUnderkategori.ORDINÆR,
            0,
            true
        )

        Assertions.assertTrue(skalIkkeIverksette)
    }

    @Test
    fun `Skal iverksettes i økonomi hvis mangler utbetalins perioder men behandlinsresultat ikke er innvilget og opphørt`() {
        val skalIkkeIverksette = opprettAtyMedEndretUtbetalingsPeriode(
            behandlinsResultat = Behandlingsresultat.INNVILGET,
            BehandlingUnderkategori.ORDINÆR,
            0,
            true
        )

        Assertions.assertFalse(skalIkkeIverksette)
    }

    @Test
    fun `Skal iverksettes i økonomi om utbetalins perioder finnes`() {
        val skalIkkeIverksette = opprettAtyMedEndretUtbetalingsPeriode(
            behandlinsResultat = Behandlingsresultat.INNVILGET_OG_OPPHØRT,
            BehandlingUnderkategori.ORDINÆR,
            100,
            true
        )

        Assertions.assertFalse(skalIkkeIverksette)
    }

    @Test
    fun `Skal iverksettes i økonomi hvis mangler utbetalins perioder men mangler endringsperioder`() {
        val skalIkkeIverksette = opprettAtyMedEndretUtbetalingsPeriode(
            behandlinsResultat = Behandlingsresultat.INNVILGET_OG_OPPHØRT,
            BehandlingUnderkategori.ORDINÆR,
            0,
            false
        )

        Assertions.assertFalse(skalIkkeIverksette)
    }

    @Test
    fun `Skal iverksettes i økonomi hvis mangler utbetalins perioder men er av underkategori UTVIDET`() {
        val skalIkkeIverksette = opprettAtyMedEndretUtbetalingsPeriode(
            behandlinsResultat = Behandlingsresultat.INNVILGET_OG_OPPHØRT,
            BehandlingUnderkategori.UTVIDET,
            0,
            true
        )

        Assertions.assertFalse(skalIkkeIverksette)
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
    fun `Skal ikke oppdatere utvidet andeler basert på endringsperioder med årsak=delt bosted`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(2))
        val søker = lagPerson(type = PersonType.SØKER, fødselsdato = LocalDate.now().minusYears(31))

        val fom = LocalDate.now().minusMonths(8)
        val tom = LocalDate.now().plusYears(3)

        val utvidetFom = LocalDate.now().minusMonths(3)
        val utvidetTom = LocalDate.now().plusMonths(5)

        val endretUtbetalingAndelFom = utvidetFom.minusMonths(2).toYearMonth()
        val endretUtbetalingAndelTom = utvidetTom.minusMonths(2).toYearMonth()

        val andelerTilkjentYtelse = genererAndelerTilkjentYtelseForScenario(
            endretUtbetalingÅrsak = Årsak.DELT_BOSTED,
            endretUtbetalingProsent = BigDecimal.ZERO,
            endretUtbetalingFom = endretUtbetalingAndelFom,
            endretUtbetalingTom = endretUtbetalingAndelTom,
            generellFom = fom,
            generellTom = tom,
            utvidetFom = utvidetFom,
            utvidetTom = utvidetTom,
            barn = barn,
            søker = søker
        )

        Assertions.assertEquals(5, andelerTilkjentYtelse.size)

        val (andelerSøker, andelerBarn) = andelerTilkjentYtelse.partition { it.aktør.aktivFødselsnummer() == søker.aktør.aktivFødselsnummer() }

        Assertions.assertEquals(3, andelerBarn.size)
        // Barn før endringsperiode
        Assertions.assertEquals(barn.aktør, andelerBarn[0].aktør)
        Assertions.assertEquals(BigDecimal(50), andelerBarn[0].prosent)
        Assertions.assertEquals(fom.plusMonths(1).toYearMonth(), andelerBarn[0].stønadFom)
        Assertions.assertEquals(endretUtbetalingAndelFom.minusMonths(1), andelerBarn[0].stønadTom)

        // Barn under endringsperiode
        Assertions.assertEquals(barn.aktør, andelerBarn[1].aktør)
        Assertions.assertEquals(BigDecimal.ZERO, andelerBarn[1].prosent)
        Assertions.assertEquals(endretUtbetalingAndelFom, andelerBarn[1].stønadFom)
        Assertions.assertEquals(endretUtbetalingAndelTom, andelerBarn[1].stønadTom)

        // Barn etter endringsperiode
        Assertions.assertEquals(barn.aktør, andelerBarn[2].aktør)
        Assertions.assertEquals(BigDecimal(50), andelerBarn[2].prosent)
        Assertions.assertEquals(endretUtbetalingAndelTom.plusMonths(1), andelerBarn[2].stønadFom)
        Assertions.assertEquals(tom.toYearMonth(), andelerBarn[2].stønadTom)

        Assertions.assertEquals(2, andelerSøker.size)

        val (andelerUtvidet, andelerSmåbarnstillegg) = andelerSøker.partition { it.erUtvidet() }

        Assertions.assertEquals(1, andelerUtvidet.size)
        // Søker - utvidet under og etter endringsperiode
        Assertions.assertEquals(søker.aktør, andelerUtvidet[0].aktør)
        Assertions.assertEquals(BigDecimal(50), andelerUtvidet[0].prosent)
        Assertions.assertEquals(utvidetFom.plusMonths(1).toYearMonth(), andelerUtvidet[0].stønadFom)
        Assertions.assertEquals(utvidetTom.toYearMonth(), andelerUtvidet[0].stønadTom)

        Assertions.assertEquals(1, andelerSmåbarnstillegg.size)
        // Søker - småbarnstillegg
        Assertions.assertEquals(søker.aktør, andelerSmåbarnstillegg[0].aktør)
        Assertions.assertEquals(BigDecimal(100), andelerSmåbarnstillegg[0].prosent)
        Assertions.assertEquals(utvidetFom.plusMonths(1).toYearMonth(), andelerSmåbarnstillegg[0].stønadFom)
        Assertions.assertEquals(utvidetTom.toYearMonth(), andelerSmåbarnstillegg[0].stønadTom)
    }

    @Test
    fun `Skal oppdatere utvidet andeler og småbarnstillegg med riktig periode og sats ved endringsperiode med årsak=etterbetaling 3år`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(2))
        val søker = lagPerson(type = PersonType.SØKER, fødselsdato = LocalDate.now().minusYears(31))

        val fom = LocalDate.now().minusMonths(8)
        val tom = LocalDate.now().plusYears(3)

        val utvidetFom = LocalDate.now().minusMonths(3)
        val utvidetTom = LocalDate.now().plusMonths(5)

        val endretUtbetalingAndelFom = utvidetFom.minusMonths(2).toYearMonth()
        val endretUtbetalingAndelTom = utvidetTom.minusMonths(2).toYearMonth()

        val andelerTilkjentYtelse = genererAndelerTilkjentYtelseForScenario(
            endretUtbetalingÅrsak = Årsak.ETTERBETALING_3ÅR,
            endretUtbetalingProsent = BigDecimal.ZERO,
            endretUtbetalingFom = endretUtbetalingAndelFom,
            endretUtbetalingTom = endretUtbetalingAndelTom,
            generellFom = fom,
            generellTom = tom,
            utvidetFom = utvidetFom,
            utvidetTom = utvidetTom,
            barn = barn,
            søker = søker
        )

        Assertions.assertEquals(7, andelerTilkjentYtelse.size)

        val (andelerSøker, andelerBarn) = andelerTilkjentYtelse.partition { it.aktør.aktivFødselsnummer() == søker.aktør.aktivFødselsnummer() }

        Assertions.assertEquals(3, andelerBarn.size)
        // Barn før endringsperiode
        Assertions.assertEquals(barn.aktør, andelerBarn[0].aktør)
        Assertions.assertEquals(BigDecimal(100), andelerBarn[0].prosent)
        Assertions.assertEquals(fom.plusMonths(1).toYearMonth(), andelerBarn[0].stønadFom)
        Assertions.assertEquals(endretUtbetalingAndelFom.minusMonths(1), andelerBarn[0].stønadTom)

        // Barn under endringsperiode
        Assertions.assertEquals(barn.aktør, andelerBarn[1].aktør)
        Assertions.assertEquals(BigDecimal.ZERO, andelerBarn[1].prosent)
        Assertions.assertEquals(endretUtbetalingAndelFom, andelerBarn[1].stønadFom)
        Assertions.assertEquals(endretUtbetalingAndelTom, andelerBarn[1].stønadTom)

        // Barn etter endringsperiode
        Assertions.assertEquals(barn.aktør, andelerBarn[2].aktør)
        Assertions.assertEquals(BigDecimal(100), andelerBarn[2].prosent)
        Assertions.assertEquals(endretUtbetalingAndelTom.plusMonths(1), andelerBarn[2].stønadFom)
        Assertions.assertEquals(tom.toYearMonth(), andelerBarn[2].stønadTom)

        Assertions.assertEquals(4, andelerSøker.size)

        val (andelerUtvidet, andelerSmåbarnstillegg) = andelerSøker.partition { it.erUtvidet() }

        Assertions.assertEquals(2, andelerUtvidet.size)
        // Søker - utvidet under endringsperiode
        Assertions.assertEquals(søker.aktør, andelerUtvidet[0].aktør)
        Assertions.assertEquals(BigDecimal.ZERO, andelerUtvidet[0].prosent)
        Assertions.assertEquals(utvidetFom.plusMonths(1).toYearMonth(), andelerUtvidet[0].stønadFom)
        Assertions.assertEquals(endretUtbetalingAndelTom, andelerUtvidet[0].stønadTom)

        // Søker - utvidet etter endringsperiode
        Assertions.assertEquals(søker.aktør, andelerUtvidet[1].aktør)
        Assertions.assertEquals(BigDecimal(100), andelerUtvidet[1].prosent)
        Assertions.assertEquals(endretUtbetalingAndelTom.plusMonths(1), andelerUtvidet[1].stønadFom)
        Assertions.assertEquals(utvidetTom.toYearMonth(), andelerUtvidet[1].stønadTom)

        Assertions.assertEquals(2, andelerSmåbarnstillegg.size)
        // Søker - småbarnstillegg under endringsperiode
        Assertions.assertEquals(søker.aktør, andelerSmåbarnstillegg[0].aktør)
        Assertions.assertEquals(BigDecimal.ZERO, andelerSmåbarnstillegg[0].prosent)
        Assertions.assertEquals(utvidetFom.plusMonths(1).toYearMonth(), andelerSmåbarnstillegg[0].stønadFom)
        Assertions.assertEquals(endretUtbetalingAndelTom, andelerSmåbarnstillegg[0].stønadTom)

        // Søker - småbarnstillegg etter endringsperiode
        Assertions.assertEquals(søker.aktør, andelerSmåbarnstillegg[1].aktør)
        Assertions.assertEquals(BigDecimal(100), andelerSmåbarnstillegg[1].prosent)
        Assertions.assertEquals(endretUtbetalingAndelTom.plusMonths(1), andelerSmåbarnstillegg[1].stønadFom)
        Assertions.assertEquals(utvidetTom.toYearMonth(), andelerSmåbarnstillegg[1].stønadTom)
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

    fun opprettAtyMedEndretUtbetalingsPeriode(
        behandlinsResultat: Behandlingsresultat = Behandlingsresultat.INNVILGET_OG_OPPHØRT,
        behandlingUnderkategori: BehandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
        beløp: Int,
        endretUtbetaling: Boolean
    ): Boolean {
        val behandling = lagBehandling(resultat = behandlinsResultat, underkategori = behandlingUnderkategori)

        val barn1Fnr = randomFnr()

        val barn1AktørId = tilAktør(barn1Fnr)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = barn1Fnr,
            barnasIdenter = listOf(barn1Fnr),
            barnFødselsdato = LocalDate.of(
                2002, 7, 1,
            )
        )

        val periodeFom = LocalDate.now().toYearMonth().minusMonths(1)
        val periodeTom = LocalDate.now().toYearMonth().plusMonths(1)
        val aty =
            if (endretUtbetaling) {
                lagAndelTilkjentYtelse(
                    fom = periodeFom,
                    tom = periodeTom,
                    beløp = beløp,
                    endretUtbetalingAndeler = listOf(
                        EndretUtbetalingAndel(behandlingId = behandling.id)
                    )
                )
            } else {
                lagAndelTilkjentYtelse(
                    fom = periodeFom,
                    tom = periodeTom,
                    beløp = beløp,
                    endretUtbetalingAndeler = emptyList()
                )
            }
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns personopplysningGrunnlag
        every {
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(
                behandling.id,
                barn1AktørId
            )
        } returns listOf(aty)
        every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(any()) } returns null

        return beregningService.innvilgetSøknadUtenUtbetalingsperioderGrunnetEndringsPerioder(
            behandling = behandling,
        )
    }

    private fun genererAndelerTilkjentYtelseForScenario(endretUtbetalingÅrsak: Årsak, endretUtbetalingProsent: BigDecimal, endretUtbetalingFom: YearMonth, endretUtbetalingTom: YearMonth, generellFom: LocalDate, generellTom: LocalDate, utvidetFom: LocalDate, utvidetTom: LocalDate, barn: Person, søker: Person): List<AndelTilkjentYtelse> {
        val behandling = lagBehandling()
        val vilkårsvurdering = lagVilkårsvurdering(søkerAktør = søker.aktør, behandling = behandling, resultat = Resultat.OPPFYLT)

        val søkerPersonResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søker.aktør
        )

        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = generellFom,
                    periodeTom = generellTom,
                    begrunnelse = "",
                    behandlingId = behandling.id
                ),
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = generellFom,
                    periodeTom = generellTom,
                    begrunnelse = "",
                    behandlingId = behandling.id
                ),
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = utvidetFom,
                    periodeTom = utvidetTom,
                    begrunnelse = "",
                    behandlingId = behandling.id
                ),
            )
        )

        val barnPersonResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn.aktør
        )

        barnPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = generellFom,
                    periodeTom = generellTom,
                    begrunnelse = "",
                    behandlingId = behandling.id
                ),
                VilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = generellFom,
                    periodeTom = generellTom,
                    begrunnelse = "",
                    behandlingId = behandling.id
                ),
                VilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = generellFom,
                    periodeTom = generellTom,
                    begrunnelse = "",
                    behandlingId = behandling.id,
                    utdypendeVilkårsvurderinger = if (endretUtbetalingÅrsak == Årsak.DELT_BOSTED) listOf(UtdypendeVilkårsvurdering.DELT_BOSTED) else emptyList()
                ),
                VilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.GIFT_PARTNERSKAP,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = generellFom,
                    periodeTom = generellTom,
                    begrunnelse = "",
                    behandlingId = behandling.id
                ),
                VilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.UNDER_18_ÅR,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barn.fødselsdato,
                    periodeTom = barn.fødselsdato.plusYears(18),
                    begrunnelse = "",
                    behandlingId = behandling.id
                ),
            )
        )

        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        val endretUtbetalingAndel =
            EndretUtbetalingAndel(
                behandlingId = behandling.id,
                person = barn,
                fom = endretUtbetalingFom,
                tom = endretUtbetalingTom,
                årsak = endretUtbetalingÅrsak,
                prosent = endretUtbetalingProsent,
                avtaletidspunktDeltBosted = LocalDate.now().minusMonths(1),
                søknadstidspunkt = LocalDate.now(),
                begrunnelse = "Dette er en begrunnelse"
            )

        val personopplysningGrunnlag = PersonopplysningGrunnlag(
            behandlingId = behandling.id,
            personer = mutableSetOf(barn, søker)
        )

        val slot = slot<TilkjentYtelse>()

        every { endretUtbetalingAndelRepository.findByBehandlingId(behandlingId = behandling.id) } answers { listOf(endretUtbetalingAndel) }
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = behandling.id) } answers { vilkårsvurdering }
        every { tilkjentYtelseRepository.save(any()) } returns lagInitiellTilkjentYtelse(behandling)
        every { småbarnstilleggService.hentOgLagrePerioderMedFullOvergangsstønad(any(), any()) } answers {
            listOf(
                InternPeriodeOvergangsstønad(
                    personIdent = søker.aktør.aktivFødselsnummer(),
                    fomDato = utvidetFom,
                    tomDato = utvidetTom
                )
            )
        }
        every { featureToggleService.isEnabled(FeatureToggleConfig.NY_MÅTE_Å_GENERERE_UTVIDET_ANDELER) } returns true

        beregningService.oppdaterBehandlingMedBeregning(
            behandling = behandling,
            personopplysningGrunnlag = personopplysningGrunnlag,
            nyEndretUtbetalingAndel = endretUtbetalingAndel
        )
        verify(exactly = 1) { tilkjentYtelseRepository.save(capture(slot)) }
        return slot.captured.andelerTilkjentYtelse.sortedBy { it.stønadTom }
    }
}
