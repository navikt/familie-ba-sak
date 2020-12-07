package no.nav.familie.ba.sak.beregning

import io.mockk.*
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.toRestFagsak
import no.nav.familie.ba.sak.behandling.vilkår.Vilkårsvurdering
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingRepository
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class BeregningServiceTest {

    val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    val behandlingResultatRepository = mockk<VilkårsvurderingRepository>()
    val behandlingRepository = mockk<BehandlingRepository>()
    val søknadGrunnlagService = mockk<SøknadGrunnlagService>()
    val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()

    lateinit var beregningService: BeregningService

    @BeforeEach
    fun setUp() {
        val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
        val fagsakService = mockk<FagsakService>()

        beregningService = BeregningService(andelTilkjentYtelseRepository,
                                            fagsakService,
                                            tilkjentYtelseRepository,
                                            behandlingResultatRepository,
                                            behandlingRepository,
                                            personopplysningGrunnlagRepository)

        every { andelTilkjentYtelseRepository.slettAlleAndelerTilkjentYtelseForBehandling(any()) } just Runs
        every { tilkjentYtelseRepository.slettTilkjentYtelseFor(any()) } just Runs
        every { fagsakService.hentRestFagsak(any()) } answers {
            Ressurs.success(defaultFagsak.toRestFagsak(emptyList()))
        }
    }

    @Test
    fun `Skal mappe perioderesultat til andel ytelser for innvilget vedtak med 18-års vilkår som sluttdato`() {
        val behandling = lagBehandling()
        val barn1Fnr = randomFnr()
        val søkerFnr = randomFnr()
        val vilkårsvurdering =
                Vilkårsvurdering(behandling = behandling)

        val periodeFom = LocalDate.of(2020, 1, 1)
        val periodeTom = LocalDate.of(2020, 7, 1)
        val personResultatBarn = lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
                                                   fnr = barn1Fnr,
                                                   resultat = Resultat.OPPFYLT,
                                                   periodeFom = periodeFom,
                                                   periodeTom = periodeTom,
                                                   lagFullstendigVilkårResultat = true,
                                                   personType = PersonType.BARN
        )

        val personResultatSøker = lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
                                                    fnr = søkerFnr,
                                                    resultat = Resultat.OPPFYLT,
                                                    periodeFom = periodeFom,
                                                    periodeTom = periodeTom,
                                                    lagFullstendigVilkårResultat = true,
                                                    personType = PersonType.SØKER
        )
        vilkårsvurdering.personResultater = setOf(personResultatBarn, personResultatSøker)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandlingId = behandling.id,
                                                                       søkerPersonIdent = søkerFnr,
                                                                       barnasIdenter = listOf(barn1Fnr),
                                                                       barnFødselsdato = LocalDate.of(2002, 7, 1))
        val slot = slot<TilkjentYtelse>()

        every { behandlingResultatRepository.findByBehandlingAndAktiv(any()) } answers { vilkårsvurdering }
        every { tilkjentYtelseRepository.save(any<TilkjentYtelse>()) } returns lagInitiellTilkjentYtelse(behandling)
        every { søknadGrunnlagService.hentAktiv(any())?.hentSøknadDto() } returns lagSøknadDTO(søkerFnr,
                                                                                               listOf(barn1Fnr))

        beregningService.oppdaterBehandlingMedBeregning(behandling = behandling,
                                                        personopplysningGrunnlag = personopplysningGrunnlag)

        verify(exactly = 1) { tilkjentYtelseRepository.save(capture(slot)) }

        Assertions.assertEquals(1, slot.captured.andelerTilkjentYtelse.size)
        Assertions.assertEquals(1054, slot.captured.andelerTilkjentYtelse.first().beløp)
        Assertions.assertEquals(periodeFom.nesteMåned(), slot.captured.andelerTilkjentYtelse.first().stønadFom)
        Assertions.assertEquals(periodeTom.forrigeMåned(), slot.captured.andelerTilkjentYtelse.first().stønadTom)
    }

    @Test
    fun `Skal mappe perioderesultat til andel ytelser for innvilget vedtak som spenner over flere satsperioder`() {
        val behandling = lagBehandling()
        val barn1Fnr = randomFnr()
        val søkerFnr = randomFnr()
        val vilkårsvurdering =
                Vilkårsvurdering(behandling = behandling)

        val periodeFom = LocalDate.of(2018, 1, 1)
        val periodeTom = LocalDate.of(2020, 7, 1)
        val personResultatBarn = lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
                                                   fnr = barn1Fnr,
                                                   resultat = Resultat.OPPFYLT,
                                                   periodeFom = periodeFom,
                                                   periodeTom = periodeTom,
                                                   lagFullstendigVilkårResultat = true,
                                                   personType = PersonType.BARN
        )

        val personResultatSøker = lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
                                                    fnr = søkerFnr,
                                                    resultat = Resultat.OPPFYLT,
                                                    periodeFom = periodeFom,
                                                    periodeTom = periodeTom,
                                                    lagFullstendigVilkårResultat = true,
                                                    personType = PersonType.SØKER
        )
        vilkårsvurdering.personResultater = setOf(personResultatBarn, personResultatSøker)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandlingId = behandling.id,
                                                                       søkerPersonIdent = søkerFnr,
                                                                       barnasIdenter = listOf(barn1Fnr))
        val slot = slot<TilkjentYtelse>()

        every { behandlingResultatRepository.findByBehandlingAndAktiv(any()) } answers { vilkårsvurdering }
        every { tilkjentYtelseRepository.save(any<TilkjentYtelse>()) } returns lagInitiellTilkjentYtelse(behandling)
        every { søknadGrunnlagService.hentAktiv(any())?.hentSøknadDto() } returns lagSøknadDTO(søkerFnr,
                                                                                               listOf(barn1Fnr))

        beregningService.oppdaterBehandlingMedBeregning(behandling = behandling,
                                                        personopplysningGrunnlag = personopplysningGrunnlag)

        verify(exactly = 1) { tilkjentYtelseRepository.save(capture(slot)) }

        Assertions.assertEquals(2, slot.captured.andelerTilkjentYtelse.size)

        val andelerTilkjentYtelse = slot.captured.andelerTilkjentYtelse.sortedBy { it.stønadFom }
        val satsPeriode1Slutt = YearMonth.of(2019, 2)
        val satsPeriode2Start = YearMonth.of(2019, 3)

        Assertions.assertEquals(970, andelerTilkjentYtelse.first().beløp)
        Assertions.assertEquals(periodeFom.nesteMåned(), andelerTilkjentYtelse.first().stønadFom)
        Assertions.assertEquals(satsPeriode1Slutt, andelerTilkjentYtelse.first().stønadTom)

        Assertions.assertEquals(1054, andelerTilkjentYtelse.last().beløp)
        Assertions.assertEquals(satsPeriode2Start, andelerTilkjentYtelse.last().stønadFom)
        Assertions.assertEquals(periodeTom.toYearMonth(), andelerTilkjentYtelse.last().stønadTom)
    }

    @Test
    fun `Skal mappe perioderesultat til andel ytelser for avslått vedtak`() {
        val behandling = lagBehandling()
        val barn1Fnr = randomFnr()
        val søkerFnr = randomFnr()
        val vilkårsvurdering =
                Vilkårsvurdering(behandling = behandling)

        val periodeFom = LocalDate.of(2020, 1, 1)
        val periodeTom = LocalDate.of(2020, 11, 1)
        val personResultatBarn = lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
                                                   fnr = barn1Fnr,
                                                   resultat = Resultat.OPPFYLT,
                                                   periodeFom = periodeFom,
                                                   periodeTom = periodeTom
        )

        val personResultatSøker = lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
                                                    fnr = søkerFnr,
                                                    resultat = Resultat.IKKE_OPPFYLT,
                                                    periodeFom = periodeFom,
                                                    periodeTom = periodeTom
        )
        vilkårsvurdering.personResultater = setOf(personResultatBarn, personResultatSøker)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandlingId = behandling.id,
                                                                       søkerPersonIdent = søkerFnr,
                                                                       barnasIdenter = listOf(barn1Fnr))
        val slot = slot<TilkjentYtelse>()

        every { behandlingResultatRepository.findByBehandlingAndAktiv(any()) } answers { vilkårsvurdering }
        every { tilkjentYtelseRepository.save(any<TilkjentYtelse>()) } returns lagInitiellTilkjentYtelse(behandling)
        every { søknadGrunnlagService.hentAktiv(any())?.hentSøknadDto() } returns lagSøknadDTO(søkerFnr,
                                                                                               listOf(barn1Fnr))

        beregningService.oppdaterBehandlingMedBeregning(behandling = behandling,
                                                        personopplysningGrunnlag = personopplysningGrunnlag)

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
                lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
                                  fnr = søkerFnr,
                                  resultat = Resultat.OPPFYLT,
                                  periodeFom = periode1Fom,
                                  periodeTom = periode1Tom,
                                  lagFullstendigVilkårResultat = true,
                                  personType = PersonType.SØKER
                ),
                lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
                                  fnr = søkerFnr,
                                  resultat = Resultat.IKKE_OPPFYLT,
                                  periodeFom = periode2Fom,
                                  periodeTom = periode2Tom,
                                  lagFullstendigVilkårResultat = true,
                                  personType = PersonType.SØKER
                ),
                lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
                                  fnr = søkerFnr,
                                  resultat = Resultat.OPPFYLT,
                                  periodeFom = periode3Fom,
                                  periodeTom = periode3Tom,
                                  lagFullstendigVilkårResultat = true,
                                  personType = PersonType.SØKER
                ),
                lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
                                  fnr = barn1Fnr,
                                  resultat = Resultat.OPPFYLT,
                                  periodeFom = periode1Fom.minusYears(1),
                                  periodeTom = periode3Tom.plusYears(1),
                                  lagFullstendigVilkårResultat = true,
                                  personType = PersonType.BARN
                ),
                lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
                                  fnr = barn2Fnr,
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

        every { behandlingResultatRepository.findByBehandlingAndAktiv(any()) } answers { vilkårsvurdering }
        every { tilkjentYtelseRepository.save(any<TilkjentYtelse>()) } returns lagInitiellTilkjentYtelse(behandling)
        every { søknadGrunnlagService.hentAktiv(any())?.hentSøknadDto() } returns lagSøknadDTO(søkerFnr,
                                                                                               listOf(barn1Fnr, barn2Fnr))

        beregningService.oppdaterBehandlingMedBeregning(behandling = behandling,
                                                        personopplysningGrunnlag = personopplysningGrunnlag)

        verify(exactly = 1) { tilkjentYtelseRepository.save(capture(slot)) }

        Assertions.assertEquals(5, slot.captured.andelerTilkjentYtelse.size)
        val andelerTilkjentYtelse = slot.captured.andelerTilkjentYtelse.sortedBy { it.stønadTom }


        val (andelerBarn1, andelerBarn2) = andelerTilkjentYtelse.partition { it.personIdent == barn1Fnr }

        // Barn 1 - første periode (før satsendring)
        Assertions.assertEquals(periode1Fom.nesteMåned(), andelerBarn1[0].stønadFom)
        Assertions.assertEquals(tilleggFom!!.forrigeMåned(), andelerBarn1[0].stønadTom)
        Assertions.assertEquals(1054, andelerBarn1[0].beløp)

        // Barn 1 - første periode (etter satsendring)
        Assertions.assertEquals(tilleggFom.toYearMonth(), andelerBarn1[1].stønadFom)
        Assertions.assertEquals(periode1Tom.toYearMonth(), andelerBarn1[1].stønadTom)
        Assertions.assertEquals(1354, andelerBarn1[1].beløp)

        // Barn 1 - andre periode (før fylte 6 år)
        Assertions.assertEquals(periode3Fom.nesteMåned(), andelerBarn1[2].stønadFom)
        Assertions.assertEquals(barnFødselsdato.plusYears(6).forrigeMåned(), andelerBarn1[2].stønadTom)
        Assertions.assertEquals(1354, andelerBarn1[2].beløp)

        // Barn 1 - andre periode (etter fylte 6 år)
        Assertions.assertEquals(barnFødselsdato.plusYears(6).toYearMonth(), andelerBarn1[3].stønadFom)
        Assertions.assertEquals(periode3Tom.toYearMonth(), andelerBarn1[3].stønadTom)
        Assertions.assertEquals(1054, andelerBarn1[3].beløp)

        // Barn 2 - eneste periode (etter satsendring)
        Assertions.assertEquals(periode3Fom.nesteMåned(), andelerBarn2[0].stønadFom)
        Assertions.assertEquals(periode3Midt.toYearMonth(), andelerBarn2[0].stønadTom)
        Assertions.assertEquals(1354, andelerBarn2[0].beløp)

    }
}
