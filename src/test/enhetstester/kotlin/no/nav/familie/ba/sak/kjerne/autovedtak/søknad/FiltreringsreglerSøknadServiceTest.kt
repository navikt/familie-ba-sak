package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFiltreringResultat
import no.nav.familie.ba.sak.datagenerator.lagPersonInfo
import no.nav.familie.ba.sak.datagenerator.lagSøknad
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.VergeResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FILTRERINGSREGLER_SØKNAD
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.Filtreringsregel
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FiltreringsregelEvaluator
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FiltreringsreglerFaktaSøknad
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene.FiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.FiltrerAutomatiskBehandlingData
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

class FiltreringsreglerSøknadServiceTest {
    private val personopplysningerService = mockk<PersonopplysningerService>(relaxed = true)
    private val personidentService = mockk<PersonidentService>()
    private val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    private val vilkårvurderer = mockk<Vilkårvurderer>()
    private val tilkjentYtelseValideringService = mockk<TilkjentYtelseValideringService>()
    private val filtreringsregelEvaluator = mockk<FiltreringsregelEvaluator>()
    private val søknadService = mockk<SøknadService>()
    private val filtreringResultatRepository = mockk<FiltreringResultatRepository>(relaxed = true)
    private val metrikker = mockk<Metrikker>()
    private val inneværendeMåned = YearMonth.of(2024, 5)

    private val filtreringsreglerSøknadService =
        FiltreringsreglerSøknadService(
            personopplysningerService = personopplysningerService,
            personidentService = personidentService,
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            filtreringResultatRepository = filtreringResultatRepository,
            tilkjentYtelseValideringService = tilkjentYtelseValideringService,
            filtreringsregelEvaluator = filtreringsregelEvaluator,
            søknadService = søknadService,
            clockProvider = TestClockProvider.lagClockProviderMedFastTidspunkt(inneværendeMåned),
            vilkårvurderer = vilkårvurderer,
            metrikker = metrikker,
        )

    @BeforeEach
    fun setup() {
        every { metrikker.oppdaterMetrikker(any()) } just Runs
    }

    @Test
    fun `skal slå opp barnetrygd til annen mottaker for inneværende måned og legge resultatet i fakta`() {
        // Arrange
        val søkersIdent = randomFnr()
        val barnsIdent = randomFnr()
        val behandling = lagBehandling()
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søkersIdent,
                barnasIdenter = listOf(barnsIdent),
            )
        val barn = personopplysningGrunnlag.barna.single()
        val månedSlot = slot<YearMonth>()
        val faktaSlot = slot<FiltreringsreglerFaktaSøknad>()

        every { personidentService.hentAktør(søkersIdent) } returns personopplysningGrunnlag.søker.aktør
        every { personidentService.hentAktørIder(listOf(barnsIdent)) } returns listOf(barn.aktør)
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns personopplysningGrunnlag
        every { vilkårvurderer.oppfyllerSøkerVilkårForUtvidetBarnetrygd(behandling, any()) } returns false
        every {
            tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                behandling = behandling,
                barna = listOf(barn),
                måned = capture(månedSlot),
            )
        } returns true
        every { filtreringsregelEvaluator.evaluerFiltreringsregler(any(), capture(faktaSlot)) } returns emptyList()
        every { søknadService.finnDigitalSøknad(behandling.id) } returns lagSøknad(barneIdenterTilPlanleggerBoINorge12Mnd = mapOf(barnsIdent to true))

        // Act
        filtreringsreglerSøknadService.kjørFiltreringsregler(
            filtrerAutomatiskBehandlingData = FiltrerAutomatiskBehandlingData(søkersIdent, listOf(barnsIdent)),
            behandling = behandling,
        )

        // Assert
        assertThat(månedSlot.captured).isEqualTo(inneværendeMåned)
        assertThat(faktaSlot.captured.utbetalesBarnetrygdForBarnetTilAnnenMottakerIInneværendeMåned).isTrue()
    }

    @Test
    fun `skal utlede fakta om søknaden fra den digitale søknaden og legge resultatet i fakta`() {
        // Arrange
        val søkersIdent = randomFnr()
        val barnsIdent = randomFnr()
        val behandling = lagBehandling()
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søkersIdent,
                barnasIdenter = listOf(barnsIdent),
            )
        val barn = personopplysningGrunnlag.barna.single()
        val faktaSlot = slot<FiltreringsreglerFaktaSøknad>()

        every { personidentService.hentAktør(søkersIdent) } returns personopplysningGrunnlag.søker.aktør
        every { personidentService.hentAktørIder(listOf(barnsIdent)) } returns listOf(barn.aktør)
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns personopplysningGrunnlag
        every { vilkårvurderer.oppfyllerSøkerVilkårForUtvidetBarnetrygd(behandling, any()) } returns false
        every {
            tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                behandling = behandling,
                barna = listOf(barn),
                måned = any(),
            )
        } returns false
        every { filtreringsregelEvaluator.evaluerFiltreringsregler(any(), capture(faktaSlot)) } returns emptyList()
        every { søknadService.finnDigitalSøknad(behandling.id) } returns
            lagSøknad(
                barneIdenterTilPlanleggerBoINorge12Mnd = mapOf(barnsIdent to true),
                harKryssetPåEøsSpørsmål = true,
                inneholderVedlegg = true,
                barneIdenterTilErFosterbarn = mapOf(barnsIdent to true),
                barneIdenterTilHarKryssetForDeltBosted = mapOf(barnsIdent to true),
            )

        // Act
        filtreringsreglerSøknadService.kjørFiltreringsregler(
            filtrerAutomatiskBehandlingData = FiltrerAutomatiskBehandlingData(søkersIdent, listOf(barnsIdent)),
            behandling = behandling,
        )

        // Assert
        assertThat(faktaSlot.captured.søkerHarKryssetPåEøsSpørsmålISøknaden).isTrue()
        assertThat(faktaSlot.captured.søkerHarKryssetForDeltBostedISøknaden).isTrue()
        assertThat(faktaSlot.captured.søkerHarKryssetForFosterhjemEllerBeredskapshjemISøknaden).isTrue()
        assertThat(faktaSlot.captured.søknadenInneholderVedlegg).isTrue()
    }

    @Test
    fun `skal utlede fakta til true når kun ett av flere barn er krysset av for fosterhjem og delt bosted`() {
        // Arrange
        val søkersIdent = randomFnr()
        val førsteBarnsIdent = randomFnr()
        val andreBarnsIdent = randomFnr()
        val behandling = lagBehandling()
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søkersIdent,
                barnasIdenter = listOf(førsteBarnsIdent, andreBarnsIdent),
            )
        val faktaSlot = slot<FiltreringsreglerFaktaSøknad>()

        every { personidentService.hentAktør(søkersIdent) } returns personopplysningGrunnlag.søker.aktør
        every { personidentService.hentAktørIder(listOf(førsteBarnsIdent, andreBarnsIdent)) } returns personopplysningGrunnlag.barna.map { it.aktør }
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns personopplysningGrunnlag
        every { vilkårvurderer.oppfyllerSøkerVilkårForUtvidetBarnetrygd(behandling, any()) } returns false
        every {
            tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                behandling = behandling,
                barna = any(),
                måned = any(),
            )
        } returns false
        every { filtreringsregelEvaluator.evaluerFiltreringsregler(any(), capture(faktaSlot)) } returns emptyList()
        every { søknadService.finnDigitalSøknad(behandling.id) } returns
            lagSøknad(
                barneIdenterTilPlanleggerBoINorge12Mnd = mapOf(førsteBarnsIdent to true, andreBarnsIdent to true),
                barneIdenterTilErFosterbarn = mapOf(andreBarnsIdent to true),
                barneIdenterTilHarKryssetForDeltBosted = mapOf(andreBarnsIdent to true),
            )

        // Act
        filtreringsreglerSøknadService.kjørFiltreringsregler(
            filtrerAutomatiskBehandlingData = FiltrerAutomatiskBehandlingData(søkersIdent, listOf(førsteBarnsIdent, andreBarnsIdent)),
            behandling = behandling,
        )

        // Assert
        assertThat(faktaSlot.captured.søkerHarKryssetForDeltBostedISøknaden).isTrue()
        assertThat(faktaSlot.captured.søkerHarKryssetForFosterhjemEllerBeredskapshjemISøknaden).isTrue()
    }

    @Test
    fun `skal kaste feil når det ikke finnes en digital søknad på behandlingen`() {
        // Arrange
        val søkersIdent = randomFnr()
        val barnsIdent = randomFnr()
        val behandling = lagBehandling()
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søkersIdent,
                barnasIdenter = listOf(barnsIdent),
            )

        every { personidentService.hentAktør(søkersIdent) } returns personopplysningGrunnlag.søker.aktør
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns personopplysningGrunnlag
        every { søknadService.finnDigitalSøknad(behandling.id) } returns null

        // Act & Assert
        val feil =
            assertThrows<Feil> {
                filtreringsreglerSøknadService.kjørFiltreringsregler(
                    filtrerAutomatiskBehandlingData = FiltrerAutomatiskBehandlingData(søkersIdent, listOf(barnsIdent)),
                    behandling = behandling,
                )
            }
        assertThat(feil.message).isEqualTo("Fant ikke digital søknad for behandling ${behandling.id}")
    }

    @Test
    fun `skal utlede barna som skal vurderes fra den digitale søknaden selv om barnasIdenter i FiltrerAutomatiskBehandlingData er tom`() {
        // Arrange
        val søkersIdent = randomFnr()
        val barnsIdent = randomFnr()
        val behandling = lagBehandling()
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søkersIdent,
                barnasIdenter = listOf(barnsIdent),
            )
        val barn = personopplysningGrunnlag.barna.single()
        val faktaSlot = slot<FiltreringsreglerFaktaSøknad>()

        every { personidentService.hentAktør(søkersIdent) } returns personopplysningGrunnlag.søker.aktør
        every { personidentService.hentAktørIder(listOf(barnsIdent)) } returns listOf(barn.aktør)
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns personopplysningGrunnlag
        every { vilkårvurderer.oppfyllerSøkerVilkårForUtvidetBarnetrygd(any(), any()) } returns false
        every {
            tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                behandling = behandling,
                barna = listOf(barn),
                måned = any(),
            )
        } returns false
        every { filtreringsregelEvaluator.evaluerFiltreringsregler(any(), capture(faktaSlot)) } returns emptyList()
        every { søknadService.finnDigitalSøknad(behandling.id) } returns lagSøknad(barneIdenterTilPlanleggerBoINorge12Mnd = mapOf(barnsIdent to true))

        // Act
        filtreringsreglerSøknadService.kjørFiltreringsregler(
            filtrerAutomatiskBehandlingData = FiltrerAutomatiskBehandlingData(søkersIdent, emptyList()),
            behandling = behandling,
        )

        // Assert
        assertThat(faktaSlot.captured.barnaSomSkalVurderes).containsExactly(barn)
    }

    @Test
    fun `skal sette søkerHarAktivNorskBostedsadresse og barnHarAktivNorskBostedsadresse til true`() {
        // Act
        val fakta = kjørFiltreringsregler()

        // Assert
        assertThat(fakta.søkerHarAktivNorskBostedsadresse).isTrue
        assertThat(fakta.barnHarAktivNorskBostedsadresse).isTrue
    }

    @Test
    fun `skal sette søkerHarAktivNorskBostedsadresse til false når søker mangler bostedsadresse`() {
        // Act
        val fakta =
            kjørFiltreringsregler(
                tilpassGrunnlag = { grunnlag ->
                    grunnlag.søker.bostedsadresser.clear()
                },
            )

        // Assert
        assertThat(fakta.søkerHarAktivNorskBostedsadresse).isFalse
    }

    @Test
    fun `skal sette barnHarAktivNorskBostedsadresse til false når listen med søknadsbarn er tom`() {
        // Act
        val fakta = kjørFiltreringsregler(barnasAktører = { emptyList() })

        // Assert
        assertThat(fakta.barnHarAktivNorskBostedsadresse).isFalse
    }

    @Test
    fun `skal sette søkerOgBarnHarForelderBarnRelasjon til true når alle søknadsbarn har foreldre-barn-relasjon til søker`() {
        // Act
        val fakta = kjørFiltreringsregler(barnasAktører = { listOf(it) })

        // Assert
        assertThat(fakta.søkerOgBarnHarForelderBarnRelasjon).isTrue
    }

    @Test
    fun `skal sette søkerOgBarnHarForelderBarnRelasjon til false når listen med søknadsbarn er tom`() {
        // Act
        val fakta = kjørFiltreringsregler(barnasAktører = { emptyList() })

        // Assert
        assertThat(fakta.søkerOgBarnHarForelderBarnRelasjon).isFalse
    }

    @Test
    fun `skal sette søkerOgBarnHarForelderBarnRelasjon til false når søknadsbarn mangler forelder-barn-relasjon til søker`() {
        // Act
        val fakta = kjørFiltreringsregler(personInfo = lagPersonInfo())

        // Assert
        assertThat(fakta.søkerOgBarnHarForelderBarnRelasjon).isFalse
    }

    @Test
    fun `skal sette søkerHarAdressebeskyttelseGradering6Eller19 til true når søker har gradering 6 eller 19`() {
        // Act
        val fakta =
            kjørFiltreringsregler(
                personInfo =
                    lagPersonInfo(
                        adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG,
                    ),
            )

        // Assert
        assertThat(fakta.søkerHarAdressebeskyttelseGradering6Eller19).isTrue
    }

    @Test
    fun `skal sette barnHarAdressebeskyttelseGradering6Eller19 til true når barn har gradering 6 eller 19`() {
        // Act
        val fakta =
            kjørFiltreringsregler(
                barnHarAdressebeskyttelseGradering6Eller19 = true,
            )

        // Assert
        assertThat(fakta.barnHarAdressebeskyttelseGradering6Eller19).isTrue
    }

    @Test
    fun `skal sette barnHarUkrainskStatsborgerskap til true når barn er ukrainsk statsborger`() {
        // Act
        val fakta =
            kjørFiltreringsregler(
                tilpassGrunnlag = { grunnlag ->
                    val barn = grunnlag.barna.single()
                    barn.statsborgerskap =
                        mutableListOf(
                            GrStatsborgerskap(
                                landkode = "UKR",
                                medlemskap = Medlemskap.TREDJELANDSBORGER,
                                person = barn,
                            ),
                        )
                },
            )

        // Assert
        assertThat(fakta.barnHarUkrainskStatsborgerskap).isTrue
    }

    @Test
    fun `skal ikke kaste feil ved oppdatering av metrikker for alle utfall av filtreringsreglene`() {
        // Arrange
        val evalueringer =
            Resultat.entries.flatMap { resultat ->
                FILTRERINGSREGLER_SØKNAD.map {
                    Evaluering(
                        resultat = resultat,
                        evalueringÅrsaker = emptyList(),
                        begrunnelse = "",
                        identifikator = it.identifikator.name,
                    )
                }
            }

        // Act & Assert
        assertDoesNotThrow { kjørFiltreringsregler(evalueringer = evalueringer) }
    }

    @Nested
    inner class HentBegrunnelseForIkkeOppfyltFiltreringsregel {
        @Test
        fun `skal hente begrunnelsen fra den første filtreringsregelen som ikke er oppfylt`() {
            // Arrange
            val behandlingId = 42L
            every { filtreringResultatRepository.finnFiltreringResultater(behandlingId = behandlingId) } returns
                listOf(
                    lagFiltreringResultat(behandlingId = behandlingId, resultat = Resultat.OPPFYLT, begrunnelse = "Mor lever"),
                    lagFiltreringResultat(
                        behandlingId = behandlingId,
                        filtreringsregel = Filtreringsregel.Identifikator.BARN_LEVER,
                        resultat = Resultat.IKKE_OPPFYLT,
                        begrunnelse = "Barnet er dødt",
                    ),
                    lagFiltreringResultat(
                        behandlingId = behandlingId,
                        filtreringsregel = Filtreringsregel.Identifikator.MOR_ER_OVER_18_ÅR,
                        resultat = Resultat.IKKE_OPPFYLT,
                        begrunnelse = "Mor er under 18 år",
                    ),
                )

            // Act
            val begrunnelse = filtreringsreglerSøknadService.hentBegrunnelseForIkkeOppfyltFiltreringsregel(behandlingId = behandlingId)

            // Assert
            assertThat(begrunnelse).isEqualTo("Barnet er dødt")
        }

        @Test
        fun `skal gi en generisk begrunnelse dersom ingen filtreringsregler er lagret for behandlingen`() {
            // Arrange
            val behandlingId = 42L
            every { filtreringResultatRepository.finnFiltreringResultater(behandlingId = behandlingId) } returns emptyList()

            // Act
            val begrunnelse = filtreringsreglerSøknadService.hentBegrunnelseForIkkeOppfyltFiltreringsregel(behandlingId = behandlingId)

            // Assert
            assertThat(begrunnelse).isEqualTo("Søknaden er ikke kandidat for automatisk behandling.")
        }
    }

    private fun kjørFiltreringsregler(
        tilpassGrunnlag: (PersonopplysningGrunnlag) -> Unit = {},
        personInfo: PersonInfo? = null,
        barnHarAdressebeskyttelseGradering6Eller19: Boolean = false,
        barnasAktører: (Aktør) -> List<Aktør> = { listOf(it) },
        evalueringer: List<Evaluering> = emptyList(),
    ): FiltreringsreglerFaktaSøknad {
        // Arrange
        val søkersIdent = randomFnr()
        val barnsIdent = randomFnr()
        val behandling = lagBehandling()
        val grunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søkersIdent,
                barnasIdenter = listOf(barnsIdent),
            )
        val barn = grunnlag.barna.single()
        grunnlag.personer.forEach { person ->
            person.bostedsadresser.forEach {
                it.periode = DatoIntervallEntitet(inneværendeMåned.atDay(1).minusDays(1), null)
            }
        }
        tilpassGrunnlag(grunnlag)
        val aktørBarna = barnasAktører(barn.aktør)

        val faktaSlot = slot<FiltreringsreglerFaktaSøknad>()
        every { personidentService.hentAktør(søkersIdent) } returns grunnlag.søker.aktør
        every { personidentService.hentAktørIder(listOf(barnsIdent)) } returns aktørBarna
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns grunnlag
        every { vilkårvurderer.oppfyllerSøkerVilkårForUtvidetBarnetrygd(behandling, any()) } returns false
        every { personopplysningerService.harVerge(grunnlag.søker.aktør) } returns VergeResponse(false)
        every { søknadService.finnDigitalSøknad(behandling.id) } returns lagSøknad(barneIdenterTilPlanleggerBoINorge12Mnd = mapOf(barnsIdent to true))
        every {
            personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(
                grunnlag.søker.aktør,
                aktørBarna.toSet(),
            )
        } returns (
            personInfo
                ?: lagPersonInfo(
                    forelderBarnRelasjon =
                        setOf(
                            ForelderBarnRelasjon(
                                aktør = barn.aktør,
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                adressebeskyttelseGradering =
                                    ADRESSEBESKYTTELSEGRADERING
                                        .STRENGT_FORTROLIG
                                        .takeIf { barnHarAdressebeskyttelseGradering6Eller19 },
                            ),
                        ),
                )
        )
        every {
            tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                behandling = behandling,
                barna = grunnlag.barna.filter { it.aktør in aktørBarna },
                måned = inneværendeMåned,
            )
        } returns false
        every { filtreringsregelEvaluator.evaluerFiltreringsregler(any(), capture(faktaSlot)) } returns evalueringer

        // Act
        filtreringsreglerSøknadService.kjørFiltreringsregler(
            filtrerAutomatiskBehandlingData = FiltrerAutomatiskBehandlingData(søkersIdent, listOf(barnsIdent)),
            behandling = behandling,
        )

        return faktaSlot.captured
    }
}
