package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFiltreringsreglerFaktaSøknad
import no.nav.familie.ba.sak.datagenerator.lagForelderBarnRelasjon
import no.nav.familie.ba.sak.datagenerator.lagGrStatsborgerskap
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonInfo
import no.nav.familie.ba.sak.datagenerator.lagPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagSøknad
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.VergeResponse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.lagDødsfall
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.FiltrerAutomatiskBehandlingData
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.YearMonth

class FaktaOppretterTest {
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val personidentService = mockk<PersonidentService>()
    private val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    private val vilkårvurderer = mockk<Vilkårvurderer>()
    private val tilkjentYtelseValideringService = mockk<TilkjentYtelseValideringService>()
    private val søknadService = mockk<SøknadService>()

    private val inneværendeMåned = YearMonth.of(2024, 5)

    private val faktaOppretter =
        FaktaOppretter(
            personopplysningerService = personopplysningerService,
            personidentService = personidentService,
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            tilkjentYtelseValideringService = tilkjentYtelseValideringService,
            søknadService = søknadService,
            clockProvider = TestClockProvider.lagClockProviderMedFastTidspunkt(inneværendeMåned),
            vilkårvurderer = vilkårvurderer,
        )

    private val behandling = lagBehandling()
    private val søkersIdent = randomFnr()
    private val barnsIdent = randomFnr()
    private val filtrerAutomatiskBehandlingData = FiltrerAutomatiskBehandlingData(søkersIdent, listOf(barnsIdent))
    private val grunnlag =
        lagPersonopplysningGrunnlag(behandlingId = behandling.id) { personopplysningGrunnlag ->
            setOf(
                lagPerson(
                    type = PersonType.SØKER,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    aktør = lagAktør(søkersIdent),
                    bostedsadresser = {
                        listOf(
                            lagGrVegadresse(
                                person = it,
                                periode =
                                    DatoIntervallEntitet(
                                        inneværendeMåned.atDay(1).minusDays(1),
                                        null,
                                    ),
                            ),
                        )
                    },
                ),
                lagPerson(
                    type = PersonType.BARN,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    aktør = lagAktør(barnsIdent),
                    bostedsadresser = {
                        listOf(
                            lagGrVegadresse(
                                person = it,
                                periode =
                                    DatoIntervallEntitet(
                                        inneværendeMåned.atDay(1).minusDays(1),
                                        null,
                                    ),
                            ),
                        )
                    },
                ),
            )
        }
    private val søker = grunnlag.søker
    private val barn = grunnlag.barna.single()

    @BeforeEach
    fun setup() {
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns grunnlag
        every { søknadService.finnDigitalSøknad(behandling.id) } returns lagSøknad(barneIdenterTilPlanleggerBoINorge12Mnd = mapOf(barnsIdent to true))
        every { personidentService.hentAktør(søkersIdent) } returns søker.aktør
        every { personidentService.hentAktørIder(listOf(barnsIdent)) } returns listOf(barn.aktør)
        every { personopplysningerService.harVerge(søker.aktør) } returns VergeResponse(harVerge = false)
        every {
            personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør = søker.aktør, relevanteAktører = any())
        } returns lagPersonInfo(forelderBarnRelasjon = setOf(lagForelderBarnRelasjon(aktør = barn.aktør)))
        every { vilkårvurderer.oppfyllerSøkerVilkårForUtvidetBarnetrygd(behandling, any()) } returns false
        every {
            tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(behandling = behandling, barna = any(), måned = any())
        } returns false
    }

    @Test
    fun `skal opprette fakta for happy case`() {
        // Act
        val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

        // Assert
        assertThat(fakta).isEqualTo(lagFiltreringsreglerFaktaSøknad(søker = søker, barnaSomSkalVurderes = listOf(barn)))
    }

    @Test
    fun `skal kaste feil når det ikke finnes et aktivt personopplysninggrunnlag på behandlingen`() {
        // Arrange
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns null

        // Act & Assert
        val feil = assertThrows<Feil> { faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling) }
        assertThat(feil.message).isEqualTo("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")
    }

    @Test
    fun `skal kaste feil når det ikke finnes en digital søknad på behandlingen`() {
        // Arrange
        every { søknadService.finnDigitalSøknad(behandling.id) } returns null

        // Act & Assert
        val feil = assertThrows<Feil> { faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling) }
        assertThat(feil.message).isEqualTo("Fant ikke digital søknad for behandling ${behandling.id}")
    }

    @Nested
    inner class Søker {
        @Test
        fun `skal sette søker til søkeren i personopplysninggrunnlaget`() {
            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søker).isSameAs(søker)
        }
    }

    @Nested
    inner class SøkerMottarLøpendeUtvidet {
        @Test
        fun `skal sette søkerMottarLøpendeUtvidet til true når behandlingen har underkategori utvidet`() {
            // Arrange
            val utvidetBehandling = lagBehandling(id = behandling.id, underkategori = BehandlingUnderkategori.UTVIDET)

            every { vilkårvurderer.oppfyllerSøkerVilkårForUtvidetBarnetrygd(utvidetBehandling, any()) } returns false

            every {
                tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                    behandling = utvidetBehandling,
                    barna = any(),
                    måned = any(),
                )
            } returns false

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, utvidetBehandling)

            // Assert
            assertThat(fakta.søkerMottarLøpendeUtvidet).isTrue()
        }
    }

    @Nested
    inner class SøkerOppfyllerVilkårForUtvidetBarnetrygd {
        @Test
        fun `skal sette søkerOppfyllerVilkårForUtvidetBarnetrygd fra vilkårvurdereren med søknadsbarna`() {
            // Arrange
            every { vilkårvurderer.oppfyllerSøkerVilkårForUtvidetBarnetrygd(behandling, listOf(barn)) } returns true

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søkerOppfyllerVilkårForUtvidetBarnetrygd).isTrue()
        }
    }

    @Nested
    inner class SøkerMottarEøsBarnetrygd {
        @Test
        fun `skal sette søkerMottarEøsBarnetrygd til true når behandlingen har kategori EØS`() {
            // Arrange
            val eøsBehandling = lagBehandling(id = behandling.id, behandlingKategori = BehandlingKategori.EØS)

            every { vilkårvurderer.oppfyllerSøkerVilkårForUtvidetBarnetrygd(eøsBehandling, any()) } returns false

            every {
                tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                    behandling = eøsBehandling,
                    barna = any(),
                    måned = any(),
                )
            } returns false

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, eøsBehandling)

            // Assert
            assertThat(fakta.søkerMottarEøsBarnetrygd).isTrue()
        }
    }

    @Nested
    inner class BarnaSomSkalVurderes {
        @Test
        fun `skal kun vurdere barn i personopplysninggrunnlaget som det er søkt for i den digitale søknaden`() {
            // Arrange
            val grunnlagMedToBarn =
                lagPersonopplysningGrunnlag(behandlingId = behandling.id) { personopplysningGrunnlag ->
                    setOf(
                        lagPerson(
                            type = PersonType.SØKER,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = søker.aktør,
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = lagAktør(barnsIdent),
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = lagAktør(randomFnr()),
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                    )
                }
            val søknadsbarn = grunnlagMedToBarn.barna.first()

            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns grunnlagMedToBarn
            every { personidentService.hentAktørIder(listOf(barnsIdent)) } returns listOf(søknadsbarn.aktør)

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.barnaSomSkalVurderes).containsExactly(søknadsbarn)
        }

        @Test
        fun `skal utlede barna fra den digitale søknaden selv om barnasIdenter i FiltrerAutomatiskBehandlingData er tom`() {
            // Act
            val fakta =
                faktaOppretter.opprettFakta(
                    filtrerAutomatiskBehandlingData = FiltrerAutomatiskBehandlingData(søkersIdent, emptyList()),
                    behandling = behandling,
                )

            // Assert
            assertThat(fakta.barnaSomSkalVurderes).containsExactly(barn)
        }
    }

    @Nested
    inner class SøkerLever {
        @Test
        fun `skal sette søkerLever til false når søker er død`() {
            // Arrange
            val nyttGrunnlag =
                lagPersonopplysningGrunnlag(behandlingId = behandling.id) { personopplysningGrunnlag ->
                    setOf(
                        lagPerson(
                            type = PersonType.SØKER,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = søker.aktør,
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                            dødsfall = {
                                lagDødsfall(
                                    person = it,
                                    dødsfallDato = inneværendeMåned.atDay(1),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = barn.aktør,
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                    )
                }

            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns nyttGrunnlag

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søkerLever).isFalse()
        }
    }

    @Nested
    inner class BarnaLever {
        @Test
        fun `skal sette barnaLever til false når søknadsbarnet er dødt`() {
            // Arrange
            val nyttGrunnlag =
                lagPersonopplysningGrunnlag(behandlingId = behandling.id) { personopplysningGrunnlag ->
                    setOf(
                        lagPerson(
                            type = PersonType.SØKER,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = søker.aktør,
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = barn.aktør,
                            bostedsadresser = { listOf(lagGrVegadresse(person = it, periode = DatoIntervallEntitet(inneværendeMåned.atDay(1).minusDays(1), null))) },
                            dødsfall = {
                                lagDødsfall(
                                    person = it,
                                    dødsfallDato = inneværendeMåned.atDay(1),
                                )
                            },
                        ),
                    )
                }

            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns nyttGrunnlag

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.barnaLever).isFalse()
        }

        @Test
        fun `skal ikke sette barnaLever til false når dødt barn ikke er med i søknaden`() {
            // Arrange
            val grunnlagMedToBarn =
                lagPersonopplysningGrunnlag(behandlingId = behandling.id) { personopplysningGrunnlag ->
                    setOf(
                        lagPerson(
                            type = PersonType.SØKER,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = søker.aktør,
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = lagAktør(barnsIdent),
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = lagAktør(randomFnr()),
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                            dødsfall = {
                                lagDødsfall(
                                    person = it,
                                    dødsfallDato = inneværendeMåned.atDay(1),
                                )
                            },
                        ),
                    )
                }
            val søknadsbarn = grunnlagMedToBarn.barna.first()

            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns grunnlagMedToBarn
            every { personidentService.hentAktørIder(listOf(barnsIdent)) } returns listOf(søknadsbarn.aktør)

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.barnaLever).isTrue()
        }
    }

    @Nested
    inner class SøkerHarVerge {
        @Test
        fun `skal sette søkerHarVerge til true når søker har verge`() {
            // Arrange
            every { personopplysningerService.harVerge(søker.aktør) } returns VergeResponse(harVerge = true)

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søkerHarVerge).isTrue()
        }
    }

    @Nested
    inner class UtbetalesBarnetrygdForBarnetTilAnnenMottakerIInneværendeMåned {
        @Test
        fun `skal slå opp barnetrygd til annen mottaker for søknadsbarna i inneværende måned`() {
            // Arrange
            every {
                tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                    behandling = behandling,
                    barna = listOf(barn),
                    måned = inneværendeMåned,
                )
            } returns true

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.utbetalesBarnetrygdForBarnetTilAnnenMottakerIInneværendeMåned).isTrue()
            verify(exactly = 1) {
                tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                    behandling = behandling,
                    barna = listOf(barn),
                    måned = inneværendeMåned,
                )
            }
        }
    }

    @Nested
    inner class SøkerHarKryssetPåEøsSpørsmålISøknaden {
        @Test
        fun `skal sette søkerHarKryssetPåEøsSpørsmålISøknaden til true når søker har krysset på EØS-spørsmål i søknaden`() {
            // Arrange
            every {
                søknadService.finnDigitalSøknad(behandling.id)
            } returns
                lagSøknad(
                    barneIdenterTilPlanleggerBoINorge12Mnd = mapOf(barnsIdent to true),
                    harKryssetPåEøsSpørsmål = true,
                )

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søkerHarKryssetPåEøsSpørsmålISøknaden).isTrue()
        }
    }

    @Nested
    inner class SøkerHarKryssetForDeltBostedISøknaden {
        @Test
        fun `skal sette søkerHarKryssetForDeltBostedISøknaden til true når søker har krysset for delt bosted for søknadsbarnet`() {
            // Arrange
            every {
                søknadService.finnDigitalSøknad(behandling.id)
            } returns
                lagSøknad(
                    barneIdenterTilPlanleggerBoINorge12Mnd = mapOf(barnsIdent to true),
                    barneIdenterTilHarKryssetForDeltBosted = mapOf(barnsIdent to true),
                )

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søkerHarKryssetForDeltBostedISøknaden).isTrue()
        }

        @Test
        fun `skal sette søkerHarKryssetForDeltBostedISøknaden til true når kun ett av flere søknadsbarn er krysset av for delt bosted`() {
            // Arrange
            val andreBarnsIdent = randomFnr()

            val grunnlagMedToBarn =
                lagPersonopplysningGrunnlag(behandlingId = behandling.id) { personopplysningGrunnlag ->
                    setOf(
                        lagPerson(
                            type = PersonType.SØKER,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = søker.aktør,
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = lagAktør(barnsIdent),
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = lagAktør(andreBarnsIdent),
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                    )
                }

            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns grunnlagMedToBarn

            every { personidentService.hentAktørIder(listOf(barnsIdent, andreBarnsIdent)) } returns grunnlagMedToBarn.barna.map { it.aktør }

            every {
                søknadService.finnDigitalSøknad(behandling.id)
            } returns
                lagSøknad(
                    barneIdenterTilPlanleggerBoINorge12Mnd = mapOf(barnsIdent to true, andreBarnsIdent to true),
                    barneIdenterTilHarKryssetForDeltBosted = mapOf(andreBarnsIdent to true),
                )

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søkerHarKryssetForDeltBostedISøknaden).isTrue()
        }
    }

    @Nested
    inner class SøkerHarKryssetForFosterhjemEllerBeredskapshjemISøknaden {
        @Test
        fun `skal sette søkerHarKryssetForFosterhjemEllerBeredskapshjemISøknaden til true når søker har krysset for fosterbarn for søknadsbarnet`() {
            // Arrange
            every {
                søknadService.finnDigitalSøknad(behandling.id)
            } returns
                lagSøknad(
                    barneIdenterTilPlanleggerBoINorge12Mnd = mapOf(barnsIdent to true),
                    barneIdenterTilErFosterbarn = mapOf(barnsIdent to true),
                )

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søkerHarKryssetForFosterhjemEllerBeredskapshjemISøknaden).isTrue()
        }

        @Test
        fun `skal sette søkerHarKryssetForFosterhjemEllerBeredskapshjemISøknaden til true når kun ett av flere søknadsbarn er krysset av som fosterbarn`() {
            // Arrange
            val andreBarnsIdent = randomFnr()

            val grunnlagMedToBarn =
                lagPersonopplysningGrunnlag(behandlingId = behandling.id) { personopplysningGrunnlag ->
                    setOf(
                        lagPerson(
                            type = PersonType.SØKER,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = søker.aktør,
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = lagAktør(barnsIdent),
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = lagAktør(andreBarnsIdent),
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                    )
                }

            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns grunnlagMedToBarn

            every { personidentService.hentAktørIder(listOf(barnsIdent, andreBarnsIdent)) } returns grunnlagMedToBarn.barna.map { it.aktør }

            every {
                søknadService.finnDigitalSøknad(behandling.id)
            } returns
                lagSøknad(
                    barneIdenterTilPlanleggerBoINorge12Mnd = mapOf(barnsIdent to true, andreBarnsIdent to true),
                    barneIdenterTilErFosterbarn = mapOf(andreBarnsIdent to true),
                )

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søkerHarKryssetForFosterhjemEllerBeredskapshjemISøknaden).isTrue()
        }
    }

    @Nested
    inner class SøknadenInneholderVedlegg {
        @Test
        fun `skal sette søknadenInneholderVedlegg til true når søknaden inneholder vedlegg`() {
            // Arrange
            every {
                søknadService.finnDigitalSøknad(behandling.id)
            } returns
                lagSøknad(
                    barneIdenterTilPlanleggerBoINorge12Mnd = mapOf(barnsIdent to true),
                    inneholderVedlegg = true,
                )

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søknadenInneholderVedlegg).isTrue()
        }
    }

    @Nested
    inner class SøkerHarIkkeLøpendeUtbetalingOgHarAldriHattUtbetaling {
        @Test
        fun `skal sette søkerHarIkkeLøpendeUtbetalingOgHarAldriHattUtbetaling til false`() {
            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søkerHarIkkeLøpendeUtbetalingOgHarAldriHattUtbetaling).isFalse()
        }
    }

    @Nested
    inner class SøkerHarAdressebeskyttelseGradering6Eller19 {
        @ParameterizedTest
        @EnumSource(value = ADRESSEBESKYTTELSEGRADERING::class, names = ["STRENGT_FORTROLIG", "STRENGT_FORTROLIG_UTLAND"])
        fun `skal sette søkerHarAdressebeskyttelseGradering6Eller19 til true når søker er strengt fortrolig`(gradering: ADRESSEBESKYTTELSEGRADERING) {
            // Arrange
            every {
                personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør = søker.aktør, relevanteAktører = any())
            } returns
                lagPersonInfo(
                    adressebeskyttelseGradering = gradering,
                    forelderBarnRelasjon = setOf(lagForelderBarnRelasjon(aktør = barn.aktør)),
                )

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søkerHarAdressebeskyttelseGradering6Eller19).isTrue()
        }

        @ParameterizedTest
        @EnumSource(value = ADRESSEBESKYTTELSEGRADERING::class, names = ["FORTROLIG", "UGRADERT"])
        fun `skal sette søkerHarAdressebeskyttelseGradering6Eller19 til false når søker ikke er strengt fortrolig`(gradering: ADRESSEBESKYTTELSEGRADERING) {
            // Arrange
            every {
                personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør = søker.aktør, relevanteAktører = any())
            } returns
                lagPersonInfo(
                    adressebeskyttelseGradering = gradering,
                    forelderBarnRelasjon = setOf(lagForelderBarnRelasjon(aktør = barn.aktør)),
                )

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søkerHarAdressebeskyttelseGradering6Eller19).isFalse()
        }
    }

    @Nested
    inner class BarnHarAdressebeskyttelseGradering6Eller19 {
        @ParameterizedTest
        @EnumSource(value = ADRESSEBESKYTTELSEGRADERING::class, names = ["STRENGT_FORTROLIG", "STRENGT_FORTROLIG_UTLAND"])
        fun `skal sette barnHarAdressebeskyttelseGradering6Eller19 til true når søknadsbarnet er strengt fortrolig`(gradering: ADRESSEBESKYTTELSEGRADERING) {
            // Arrange
            every {
                personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør = søker.aktør, relevanteAktører = any())
            } returns
                lagPersonInfo(
                    forelderBarnRelasjon =
                        setOf(
                            lagForelderBarnRelasjon(
                                aktør = barn.aktør,
                                adressebeskyttelseGradering = gradering,
                            ),
                        ),
                )

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.barnHarAdressebeskyttelseGradering6Eller19).isTrue()
        }

        @Test
        fun `skal ikke ta hensyn til adressebeskyttelse for barn som ikke er med i søknaden`() {
            // Arrange
            val grunnlagMedToBarn =
                lagPersonopplysningGrunnlag(behandlingId = behandling.id) { personopplysningGrunnlag ->
                    setOf(
                        lagPerson(
                            type = PersonType.SØKER,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = søker.aktør,
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = lagAktør(barnsIdent),
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = lagAktør(randomFnr()),
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                    )
                }
            val søknadsbarn = grunnlagMedToBarn.barna.first()
            val strengtFortroligBarn = grunnlagMedToBarn.barna.last()

            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns grunnlagMedToBarn

            every { personidentService.hentAktørIder(listOf(barnsIdent)) } returns listOf(søknadsbarn.aktør)

            every {
                personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør = søker.aktør, relevanteAktører = any())
            } returns
                lagPersonInfo(
                    forelderBarnRelasjon =
                        setOf(
                            lagForelderBarnRelasjon(
                                aktør = søknadsbarn.aktør,
                            ),
                            lagForelderBarnRelasjon(
                                aktør = strengtFortroligBarn.aktør,
                                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG,
                            ),
                        ),
                )

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.barnHarAdressebeskyttelseGradering6Eller19).isFalse()
        }
    }

    @Nested
    inner class SøkerOgBarnHarForelderBarnRelasjon {
        @Test
        fun `skal sette søkerOgBarnHarForelderBarnRelasjon til false når søknadsbarnet mangler forelder-barn-relasjon til søker`() {
            // Arrange
            every {
                personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(
                    aktør = søker.aktør,
                    relevanteAktører = any(),
                )
            } returns lagPersonInfo(forelderBarnRelasjon = emptySet())

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søkerOgBarnHarForelderBarnRelasjon).isFalse()
        }

        @Test
        fun `skal sette søkerOgBarnHarForelderBarnRelasjon til false når kun ett av flere søknadsbarn har forelder-barn-relasjon til søker`() {
            // Arrange
            val andreBarnsIdent = randomFnr()

            val grunnlagMedToBarn =
                lagPersonopplysningGrunnlag(behandlingId = behandling.id) { personopplysningGrunnlag ->
                    setOf(
                        lagPerson(
                            type = PersonType.SØKER,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = søker.aktør,
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = lagAktør(barnsIdent),
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = lagAktør(andreBarnsIdent),
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                    )
                }

            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns grunnlagMedToBarn

            every { personidentService.hentAktørIder(listOf(barnsIdent, andreBarnsIdent)) } returns grunnlagMedToBarn.barna.map { it.aktør }

            every {
                søknadService.finnDigitalSøknad(behandling.id)
            } returns lagSøknad(barneIdenterTilPlanleggerBoINorge12Mnd = mapOf(barnsIdent to true, andreBarnsIdent to true))

            every {
                personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør = søker.aktør, relevanteAktører = any())
            } returns lagPersonInfo(forelderBarnRelasjon = setOf(lagForelderBarnRelasjon(aktør = grunnlagMedToBarn.barna.first().aktør)))

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søkerOgBarnHarForelderBarnRelasjon).isFalse()
        }

        @Test
        fun `skal sette søkerOgBarnHarForelderBarnRelasjon til false når det ikke er noen søknadsbarn`() {
            // Arrange
            every { søknadService.finnDigitalSøknad(behandling.id) } returns lagSøknad(barneIdenterTilPlanleggerBoINorge12Mnd = emptyMap())
            every { personidentService.hentAktørIder(emptyList()) } returns emptyList()

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søkerOgBarnHarForelderBarnRelasjon).isFalse()
        }
    }

    @Nested
    inner class SøkerHarAktivNorskBostedsadresse {
        @Test
        fun `skal sette søkerHarAktivNorskBostedsadresse til false når søker mangler bostedsadresse`() {
            // Arrange
            val nyttGrunnlag =
                lagPersonopplysningGrunnlag(behandlingId = behandling.id) { personopplysningGrunnlag ->
                    setOf(
                        lagPerson(
                            type = PersonType.SØKER,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = søker.aktør,
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = barn.aktør,
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                    )
                }

            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns nyttGrunnlag

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søkerHarAktivNorskBostedsadresse).isFalse()
        }

        @Test
        fun `skal sette søkerHarAktivNorskBostedsadresse til false når søkers bostedsadresse ikke er aktiv i dag`() {
            // Arrange
            val nyttGrunnlag =
                lagPersonopplysningGrunnlag(behandlingId = behandling.id) { personopplysningGrunnlag ->
                    setOf(
                        lagPerson(
                            type = PersonType.SØKER,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = søker.aktør,
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(2),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = barn.aktør,
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                    )
                }

            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns nyttGrunnlag

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søkerHarAktivNorskBostedsadresse).isFalse()
        }
    }

    @Nested
    inner class BarnHarAktivNorskBostedsadresse {
        @Test
        fun `skal sette barnHarAktivNorskBostedsadresse til false når søknadsbarnet mangler bostedsadresse`() {
            // Arrange
            val nyttGrunnlag =
                lagPersonopplysningGrunnlag(behandlingId = behandling.id) { personopplysningGrunnlag ->
                    setOf(
                        lagPerson(
                            type = PersonType.SØKER,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = søker.aktør,
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = barn.aktør,
                        ),
                    )
                }

            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns nyttGrunnlag

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.barnHarAktivNorskBostedsadresse).isFalse()
        }

        @Test
        fun `skal sette barnHarAktivNorskBostedsadresse til false når det ikke er noen søknadsbarn`() {
            // Arrange
            every { søknadService.finnDigitalSøknad(behandling.id) } returns lagSøknad(barneIdenterTilPlanleggerBoINorge12Mnd = emptyMap())
            every { personidentService.hentAktørIder(emptyList()) } returns emptyList()

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.barnHarAktivNorskBostedsadresse).isFalse()
        }
    }

    @Nested
    inner class SøkerHarUkrainskStatsborgerskap {
        @Test
        fun `skal sette søkerHarUkrainskStatsborgerskap til true når søker er ukrainsk statsborger`() {
            // Arrange
            val nyttGrunnlag =
                lagPersonopplysningGrunnlag(behandlingId = behandling.id) { personopplysningGrunnlag ->
                    setOf(
                        lagPerson(
                            type = PersonType.SØKER,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = søker.aktør,
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                            statsborgerskap = {
                                listOf(
                                    lagGrStatsborgerskap(
                                        person = it,
                                        landkode = "UKR",
                                        medlemskap = Medlemskap.TREDJELANDSBORGER,
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = barn.aktør,
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                    )
                }

            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns nyttGrunnlag

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.søkerHarUkrainskStatsborgerskap).isTrue()
        }
    }

    @Nested
    inner class BarnHarUkrainskStatsborgerskap {
        @Test
        fun `skal sette barnHarUkrainskStatsborgerskap til true når søknadsbarnet er ukrainsk statsborger`() {
            // Arrange
            val nyttGrunnlag =
                lagPersonopplysningGrunnlag(behandlingId = behandling.id) { personopplysningGrunnlag ->
                    setOf(
                        lagPerson(
                            type = PersonType.SØKER,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = søker.aktør,
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = barn.aktør,
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                            statsborgerskap = {
                                listOf(
                                    lagGrStatsborgerskap(
                                        person = it,
                                        landkode = "UKR",
                                        medlemskap = Medlemskap.TREDJELANDSBORGER,
                                    ),
                                )
                            },
                        ),
                    )
                }

            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns nyttGrunnlag

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.barnHarUkrainskStatsborgerskap).isTrue()
        }

        @Test
        fun `skal ikke sette barnHarUkrainskStatsborgerskap til true når ukrainsk barn ikke er med i søknaden`() {
            // Arrange
            val grunnlagMedToBarn =
                lagPersonopplysningGrunnlag(behandlingId = behandling.id) { personopplysningGrunnlag ->
                    setOf(
                        lagPerson(
                            type = PersonType.SØKER,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = søker.aktør,
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = lagAktør(barnsIdent),
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                        ),
                        lagPerson(
                            type = PersonType.BARN,
                            personopplysningGrunnlag = personopplysningGrunnlag,
                            aktør = lagAktør(randomFnr()),
                            bostedsadresser = {
                                listOf(
                                    lagGrVegadresse(
                                        person = it,
                                        periode =
                                            DatoIntervallEntitet(
                                                inneværendeMåned.atDay(1).minusDays(1),
                                                null,
                                            ),
                                    ),
                                )
                            },
                            statsborgerskap = {
                                listOf(
                                    lagGrStatsborgerskap(
                                        person = it,
                                        landkode = "UKR",
                                        medlemskap = Medlemskap.TREDJELANDSBORGER,
                                    ),
                                )
                            },
                        ),
                    )
                }
            val søknadsbarn = grunnlagMedToBarn.barna.first()

            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns grunnlagMedToBarn
            every { personidentService.hentAktørIder(listOf(barnsIdent)) } returns listOf(søknadsbarn.aktør)

            // Act
            val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

            // Assert
            assertThat(fakta.barnHarUkrainskStatsborgerskap).isFalse()
        }
    }
}
