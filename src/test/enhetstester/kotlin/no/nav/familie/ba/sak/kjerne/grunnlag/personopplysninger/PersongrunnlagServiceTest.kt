package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagDeltBosted
import no.nav.familie.ba.sak.datagenerator.lagFolkeregistermetadata
import no.nav.familie.ba.sak.datagenerator.lagGrMatrikkelDeltBosted
import no.nav.familie.ba.sak.datagenerator.lagGrMatrikkelOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresse
import no.nav.familie.ba.sak.datagenerator.lagMatrikkeladresse
import no.nav.familie.ba.sak.datagenerator.lagOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonInfo
import no.nav.familie.ba.sak.datagenerator.lagPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagSøknadDTO
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.KodeverkService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.kontrakter.felles.PersonIdent
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class PersongrunnlagServiceTest {
    private val personidentService = mockk<PersonidentService>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    private val loggService = mockk<LoggService>()
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val kodeverkService = mockk<KodeverkService>()

    private val persongrunnlagService =
        spyk(
            PersongrunnlagService(
                personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
                statsborgerskapService = mockk(),
                arbeidsfordelingService = mockk(relaxed = true),
                personopplysningerService = personopplysningerService,
                personidentService = personidentService,
                saksstatistikkEventPublisher = mockk(relaxed = true),
                behandlingHentOgPersisterService = mockk(),
                andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
                loggService = loggService,
                arbeidsforholdService = mockk(),
                vilkårsvurderingService = vilkårsvurderingService,
                kodeverkService = kodeverkService,
            ),
        )

    private val dagensDato = LocalDate.now()

    @Test
    fun `Skal sende med barna fra forrige behandling ved førstegangsbehandling nummer to`() {
        val søker = lagPerson()
        val barnFraForrigeBehandling = lagPerson(type = PersonType.BARN)
        val barn = lagPerson(type = PersonType.BARN)

        val barnFnr = barn.aktør.aktivFødselsnummer()
        val søkerFnr = søker.aktør.aktivFødselsnummer()

        val forrigeBehandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)
        val behandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)

        val forrigeBehandlingPersongrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = forrigeBehandling.id,
                personer = arrayOf(søker, barnFraForrigeBehandling),
            )

        val søknadDTO =
            lagSøknadDTO(
                søkerIdent = søkerFnr,
                barnasIdenter = listOf(barnFnr),
            )

        every { personidentService.hentOgLagreAktør(søkerFnr, true) } returns søker.aktør
        every { personidentService.hentOgLagreAktør(barnFnr, true) } returns barn.aktør

        every { persongrunnlagService.hentAktiv(forrigeBehandling.id) } returns forrigeBehandlingPersongrunnlag

        every {
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(
                forrigeBehandling.id,
                barnFraForrigeBehandling.aktør,
            )
        } returns listOf(lagAndelTilkjentYtelse(fom = YearMonth.now(), tom = YearMonth.now()))

        every {
            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns PersonopplysningGrunnlag(behandlingId = behandling.id)

        persongrunnlagService.registrerBarnFraSøknad(
            søknadDTO = søknadDTO,
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = forrigeBehandling,
        )
        verify(exactly = 1) {
            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                aktør = søker.aktør,
                barnFraInneværendeBehandling = listOf(barn.aktør),
                barnFraForrigeBehandling = listOf(barnFraForrigeBehandling.aktør),
                behandling = behandling,
                målform = søknadDTO.søkerMedOpplysninger.målform,
            )
        }
    }

    @Nested
    inner class HentOgLagreSøkerOgBarnINyttGrunnlag {
        @Test
        fun `skal på inst- og EM-saker kun lagre èn instans av barnet, med personType BARN`() {
            val barnet = lagPerson()
            val behandlinger =
                listOf(FagsakType.INSTITUSJON, FagsakType.BARN_ENSLIG_MINDREÅRIG).map { fagsakType ->
                    lagBehandling(fagsak = defaultFagsak().copy(type = fagsakType))
                }
            behandlinger.forEach { behandling ->
                val nyttGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)

                every {
                    persongrunnlagService.lagreOgDeaktiverGammel(any())
                } returns nyttGrunnlag

                every {
                    personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barnet.aktør)
                } returns PersonInfo(barnet.fødselsdato, barnet.navn, barnet.kjønn)

                every { personopplysningGrunnlagRepository.save(nyttGrunnlag) } returns nyttGrunnlag

                persongrunnlagService
                    .hentOgLagreSøkerOgBarnINyttGrunnlag(
                        aktør = barnet.aktør,
                        barnFraInneværendeBehandling = listOf(barnet.aktør),
                        barnFraForrigeBehandling = listOf(barnet.aktør),
                        behandling = behandling,
                        målform = Målform.NB,
                    ).apply {
                        Assertions
                            .assertThat(this.personer)
                            .hasSize(1)
                            .extracting("type")
                            .containsExactly(PersonType.BARN)
                    }
            }
        }

        @Test
        fun `skal filtrer bort adresser hvor opphørstidspunktet er satt i metadataen`() {
            // Arrange
            val behandling = lagBehandling()

            val personopplysningGrunnlag = lagPersonopplysningGrunnlag()

            val søker =
                lagPerson(
                    type = PersonType.SØKER,
                    fødselsdato = dagensDato.minusYears(30),
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    bostedsadresser = { listOf(lagGrVegadresse(matrikkelId = 1L)) },
                    oppholdsadresser = { listOf(lagGrMatrikkelOppholdsadresse(matrikkelId = 1L)) },
                    deltBosted = { listOf(lagGrMatrikkelDeltBosted(matrikkelId = 1L)) },
                )
            personopplysningGrunnlag.personer.add(søker)

            val barn =
                lagPerson(
                    type = PersonType.BARN,
                    fødselsdato = dagensDato.minusYears(2),
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    bostedsadresser = { listOf(lagGrVegadresse(matrikkelId = 1L)) },
                    oppholdsadresser = { listOf(lagGrMatrikkelOppholdsadresse(matrikkelId = 1L)) },
                    deltBosted = { listOf(lagGrMatrikkelDeltBosted(matrikkelId = 1L)) },
                )
            personopplysningGrunnlag.personer.add(barn)

            val personInfoSøker =
                lagPersonInfo(
                    fødselsdato = søker.fødselsdato,
                    bostedsadresser =
                        listOf(
                            lagBostedsadresse(
                                gyldigFraOgMed = dagensDato.minusYears(10),
                                gyldigTilOgMed = dagensDato.plusYears(1),
                                matrikkeladresse = lagMatrikkeladresse(1234L),
                                folkeregistermetadata = lagFolkeregistermetadata(opphoerstidspunkt = dagensDato),
                            ),
                            lagBostedsadresse(
                                gyldigFraOgMed = dagensDato.minusYears(1),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(9876L),
                                folkeregistermetadata = null,
                            ),
                        ),
                    oppholdsadresser =
                        listOf(
                            lagOppholdsadresse(
                                gyldigFraOgMed = dagensDato.minusYears(15),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(4321L),
                                folkeregistermetadata = lagFolkeregistermetadata(opphoerstidspunkt = dagensDato.minusYears(1)),
                            ),
                            lagOppholdsadresse(
                                gyldigFraOgMed = dagensDato.minusYears(2),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(6789L),
                                folkeregistermetadata = null,
                            ),
                        ),
                    deltBosted =
                        listOf(
                            lagDeltBosted(
                                startdatoForKontrakt = dagensDato.minusYears(30),
                                sluttdatoForKontrakt = null,
                                matrikkeladresse = lagMatrikkeladresse(1001L),
                                folkeregistermetadata = lagFolkeregistermetadata(opphoerstidspunkt = dagensDato.minusYears(1)),
                            ),
                            lagDeltBosted(
                                startdatoForKontrakt = dagensDato.minusYears(25),
                                sluttdatoForKontrakt = null,
                                matrikkeladresse = lagMatrikkeladresse(1001L),
                                folkeregistermetadata = null,
                            ),
                        ),
                )

            val personInfoBarn =
                lagPersonInfo(
                    fødselsdato = barn.fødselsdato,
                    bostedsadresser =
                        listOf(
                            lagBostedsadresse(
                                gyldigFraOgMed = dagensDato.minusYears(2),
                                gyldigTilOgMed = dagensDato.plusYears(1),
                                matrikkeladresse = lagMatrikkeladresse(1234L),
                                folkeregistermetadata = lagFolkeregistermetadata(opphoerstidspunkt = dagensDato),
                            ),
                            lagBostedsadresse(
                                gyldigFraOgMed = dagensDato.minusYears(2),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(9876L),
                                folkeregistermetadata = null,
                            ),
                        ),
                    oppholdsadresser =
                        listOf(
                            lagOppholdsadresse(
                                gyldigFraOgMed = dagensDato.minusYears(2),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(4321L),
                                folkeregistermetadata = lagFolkeregistermetadata(opphoerstidspunkt = dagensDato.minusYears(1)),
                            ),
                            lagOppholdsadresse(
                                gyldigFraOgMed = dagensDato.minusYears(1),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(6789L),
                                folkeregistermetadata = null,
                            ),
                        ),
                    deltBosted =
                        listOf(
                            lagDeltBosted(
                                startdatoForKontrakt = dagensDato.minusYears(1),
                                sluttdatoForKontrakt = null,
                                matrikkeladresse = lagMatrikkeladresse(1001L),
                                folkeregistermetadata = lagFolkeregistermetadata(opphoerstidspunkt = dagensDato.minusYears(1)),
                            ),
                            lagDeltBosted(
                                startdatoForKontrakt = dagensDato.minusMonths(6),
                                sluttdatoForKontrakt = null,
                                matrikkeladresse = lagMatrikkeladresse(1001L),
                                folkeregistermetadata = null,
                            ),
                        ),
                )

            every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns personopplysningGrunnlag
            every { personopplysningGrunnlagRepository.saveAndFlush(any()) } returnsArgument 0
            every { personopplysningGrunnlagRepository.save(any()) } returnsArgument 0
            every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(søker.aktør) } returns personInfoSøker
            every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barn.aktør) } returns personInfoBarn
            every { kodeverkService.hentPoststed(any()) } returns null

            // Act
            val grunnlag =
                persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                    aktør = søker.aktør,
                    barnFraInneværendeBehandling = listOf(barn.aktør),
                    barnFraForrigeBehandling = listOf(barn.aktør),
                    behandling = behandling,
                    målform = Målform.NB,
                )

            // Assert
            assertThat(grunnlag.personer).hasSize(2)
            assertThat(grunnlag.personer).anySatisfy {
                assertThat(it.aktør).isEqualTo(søker.aktør)
                assertThat(it.bostedsadresser).hasSize(1)
                assertThat(it.bostedsadresser).anySatisfy {
                    assertThat(it.person).isEqualTo(søker)
                    assertThat(it.periode?.fom).isEqualTo(dagensDato.minusYears(1))
                    assertThat(it.periode?.tom).isNull()
                }
                assertThat(it.oppholdsadresser).hasSize(1)
                assertThat(it.oppholdsadresser).anySatisfy {
                    assertThat(it.person).isEqualTo(søker)
                    assertThat(it.periode?.fom).isEqualTo(dagensDato.minusYears(2))
                    assertThat(it.periode?.tom).isNull()
                }
                assertThat(it.deltBosted).hasSize(1)
                assertThat(it.deltBosted).anySatisfy {
                    assertThat(it.person).isEqualTo(søker)
                    assertThat(it.periode?.fom).isEqualTo(dagensDato.minusYears(25))
                    assertThat(it.periode?.tom).isNull()
                }
            }
            assertThat(grunnlag.personer).anySatisfy {
                assertThat(it.aktør).isEqualTo(barn.aktør)
                assertThat(it.bostedsadresser).hasSize(1)
                assertThat(it.bostedsadresser).anySatisfy {
                    assertThat(it.person).isEqualTo(barn)
                    assertThat(it.periode?.fom).isEqualTo(dagensDato.minusYears(2))
                    assertThat(it.periode?.tom).isNull()
                }
                assertThat(it.oppholdsadresser).hasSize(1)
                assertThat(it.oppholdsadresser).anySatisfy {
                    assertThat(it.person).isEqualTo(barn)
                    assertThat(it.periode?.fom).isEqualTo(dagensDato.minusYears(1))
                    assertThat(it.periode?.tom).isNull()
                }
                assertThat(it.deltBosted).hasSize(1)
                assertThat(it.deltBosted).anySatisfy {
                    assertThat(it.person).isEqualTo(barn)
                    assertThat(it.periode?.fom).isEqualTo(dagensDato.minusMonths(6))
                    assertThat(it.periode?.tom).isNull()
                }
            }
        }
    }

    @Test
    fun `registrerManuellDødsfallPåPerson skal kaste feil dersom man registrer dødsfall dato før personen er født`() {
        val dødsfallsDato = LocalDate.of(2020, 10, 10)
        val person = lagPerson(fødselsdato = dødsfallsDato.plusMonths(10))
        val personFnr = person.aktør.aktivFødselsnummer()
        val behandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                personer = arrayOf(person),
            )

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns personopplysningGrunnlag
        every { personidentService.hentAktør(personFnr) } returns person.aktør

        val funksjonellFeil =
            assertThrows<FunksjonellFeil> {
                persongrunnlagService.registrerManuellDødsfallPåPerson(
                    behandlingId = BehandlingId(behandling.id),
                    personIdent = PersonIdent(personFnr),
                    dødsfallDato = dødsfallsDato,
                    begrunnelse = "test",
                )
            }

        assertThat(funksjonellFeil.melding).isEqualTo("Du kan ikke sette dødsfall dato til en dato som er før SØKER sin fødselsdato")
    }

    @Test
    fun `registrerManuellDødsfallPåPerson skal kaste feil dersom man registrer dødsfall dato når personen allerede har dødsfallsdato registrert`() {
        val dødsfallsDato = LocalDate.of(2020, 10, 10)
        val person =
            lagPerson(fødselsdato = dødsfallsDato.minusMonths(10)).also {
                it.dødsfall = Dødsfall(person = it, dødsfallDato = dødsfallsDato)
            }

        val personFnr = person.aktør.aktivFødselsnummer()
        val behandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                personer = arrayOf(person),
            )

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns personopplysningGrunnlag
        every { personidentService.hentAktør(personFnr) } returns person.aktør

        val funksjonellFeil =
            assertThrows<FunksjonellFeil> {
                persongrunnlagService.registrerManuellDødsfallPåPerson(
                    behandlingId = BehandlingId(behandling.id),
                    personIdent = PersonIdent(personFnr),
                    dødsfallDato = dødsfallsDato,
                    begrunnelse = "test",
                )
            }

        assertThat(funksjonellFeil.melding).isEqualTo("Dødsfall dato er allerede registrert på person med navn ${person.navn}")
    }

    @Test
    fun `registrerManuellDødsfallPåPerson skal endre på vilkår og logge at manuelt dødsfalldato er registrert`() {
        val dødsfallsDato = LocalDate.of(2020, 10, 10)
        val person = lagPerson(fødselsdato = dødsfallsDato.minusMonths(10))

        val personFnr = person.aktør.aktivFødselsnummer()
        val behandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                personer = arrayOf(person),
            )

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns personopplysningGrunnlag
        every { personidentService.hentAktør(personFnr) } returns person.aktør
        every { loggService.loggManueltRegistrertDødsfallDato(any(), any(), "test") } returns mockk()
        every { vilkårsvurderingService.oppdaterVilkårVedDødsfall(any(), any(), any()) } just runs

        persongrunnlagService.registrerManuellDødsfallPåPerson(
            behandlingId = BehandlingId(behandling.id),
            personIdent = PersonIdent(personFnr),
            dødsfallDato = dødsfallsDato,
            begrunnelse = "test",
        )

        verify(exactly = 1) { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) }
        verify(exactly = 1) { personidentService.hentAktør(personFnr) }
        verify(exactly = 1) { loggService.loggManueltRegistrertDødsfallDato(any(), any(), "test") }
        verify(exactly = 1) { vilkårsvurderingService.oppdaterVilkårVedDødsfall(any(), any(), any()) }
    }

    @Nested
    inner class HentAktivForBehandlinger {
        @Test
        fun `skal hente aktive grunnlag for behandlinger`() {
            // Arrange
            val behandlingId1 = 1L
            val behandlingId2 = 2L
            val behandlingIder = setOf(behandlingId1, behandlingId2)

            val grunnlag1 = lagPersonopplysningGrunnlag(behandlingId = behandlingId1)
            val grunnlag2 = lagPersonopplysningGrunnlag(behandlingId = behandlingId2)

            every { personopplysningGrunnlagRepository.hentAktivForBehandlinger(behandlingIder) } returns listOf(grunnlag1, grunnlag2)

            // Act
            val aktive = persongrunnlagService.hentAktivForBehandlinger(behandlingIder)

            // Assert
            assertThat(aktive).hasSize(2)
            assertThat(aktive).containsKeys(behandlingId1, behandlingId2)
            assertThat(aktive[behandlingId1]).isEqualTo(grunnlag1)
            assertThat(aktive[behandlingId2]).isEqualTo(grunnlag2)
        }

        @Test
        fun `skal returner et tomt map hvis en tom collection av behandlingIder blir sendt inn`() {
            // Arrange
            val behandlingIder = emptyList<Long>()

            every { personopplysningGrunnlagRepository.hentAktivForBehandlinger(behandlingIder) } returns emptyList()

            // Act
            val aktive = persongrunnlagService.hentAktivForBehandlinger(behandlingIder)

            // Assert
            assertThat(aktive).isEmpty()
        }
    }

    @Nested
    inner class OppdaterAdresserPåPersoner {
        @Test
        fun `skal filtrer bort adresser hvor opphørstidspunktet er satt i metadataen`() {
            // Arrange
            val personopplysningGrunnlag = lagPersonopplysningGrunnlag()

            val søker =
                lagPerson(
                    type = PersonType.SØKER,
                    fødselsdato = dagensDato.minusYears(30),
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    bostedsadresser = { listOf(lagGrVegadresse(matrikkelId = 1L)) },
                    oppholdsadresser = { listOf(lagGrMatrikkelOppholdsadresse(matrikkelId = 1L)) },
                    deltBosted = { listOf(lagGrMatrikkelDeltBosted(matrikkelId = 1L)) },
                )
            personopplysningGrunnlag.personer.add(søker)

            val barn =
                lagPerson(
                    type = PersonType.BARN,
                    fødselsdato = dagensDato.minusYears(2),
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    bostedsadresser = { listOf(lagGrVegadresse(matrikkelId = 1L)) },
                    oppholdsadresser = { listOf(lagGrMatrikkelOppholdsadresse(matrikkelId = 1L)) },
                    deltBosted = { listOf(lagGrMatrikkelDeltBosted(matrikkelId = 1L)) },
                )
            personopplysningGrunnlag.personer.add(barn)

            val personInfoSøker =
                lagPersonInfo(
                    fødselsdato = søker.fødselsdato,
                    bostedsadresser =
                        listOf(
                            lagBostedsadresse(
                                gyldigFraOgMed = dagensDato.minusYears(10),
                                gyldigTilOgMed = dagensDato.plusYears(1),
                                matrikkeladresse = lagMatrikkeladresse(1234L),
                                folkeregistermetadata = lagFolkeregistermetadata(opphoerstidspunkt = dagensDato),
                            ),
                            lagBostedsadresse(
                                gyldigFraOgMed = dagensDato.minusYears(1),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(9876L),
                                folkeregistermetadata = null,
                            ),
                        ),
                    oppholdsadresser =
                        listOf(
                            lagOppholdsadresse(
                                gyldigFraOgMed = dagensDato.minusYears(15),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(4321L),
                                folkeregistermetadata = lagFolkeregistermetadata(opphoerstidspunkt = dagensDato.minusYears(1)),
                            ),
                            lagOppholdsadresse(
                                gyldigFraOgMed = dagensDato.minusYears(2),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(6789L),
                                folkeregistermetadata = null,
                            ),
                        ),
                    deltBosted =
                        listOf(
                            lagDeltBosted(
                                startdatoForKontrakt = dagensDato.minusYears(30),
                                sluttdatoForKontrakt = null,
                                matrikkeladresse = lagMatrikkeladresse(1001L),
                                folkeregistermetadata = lagFolkeregistermetadata(opphoerstidspunkt = dagensDato.minusYears(1)),
                            ),
                            lagDeltBosted(
                                startdatoForKontrakt = dagensDato.minusYears(25),
                                sluttdatoForKontrakt = null,
                                matrikkeladresse = lagMatrikkeladresse(1001L),
                                folkeregistermetadata = null,
                            ),
                        ),
                )

            val personInfoBarn =
                lagPersonInfo(
                    fødselsdato = barn.fødselsdato,
                    bostedsadresser =
                        listOf(
                            lagBostedsadresse(
                                gyldigFraOgMed = dagensDato.minusYears(2),
                                gyldigTilOgMed = dagensDato.plusYears(1),
                                matrikkeladresse = lagMatrikkeladresse(1234L),
                                folkeregistermetadata = lagFolkeregistermetadata(opphoerstidspunkt = dagensDato),
                            ),
                            lagBostedsadresse(
                                gyldigFraOgMed = dagensDato.minusYears(2),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(9876L),
                                folkeregistermetadata = null,
                            ),
                        ),
                    oppholdsadresser =
                        listOf(
                            lagOppholdsadresse(
                                gyldigFraOgMed = dagensDato.minusYears(2),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(4321L),
                                folkeregistermetadata = lagFolkeregistermetadata(opphoerstidspunkt = dagensDato.minusYears(1)),
                            ),
                            lagOppholdsadresse(
                                gyldigFraOgMed = dagensDato.minusYears(1),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(6789L),
                                folkeregistermetadata = null,
                            ),
                        ),
                    deltBosted =
                        listOf(
                            lagDeltBosted(
                                startdatoForKontrakt = dagensDato.minusYears(1),
                                sluttdatoForKontrakt = null,
                                matrikkeladresse = lagMatrikkeladresse(1001L),
                                folkeregistermetadata = lagFolkeregistermetadata(opphoerstidspunkt = dagensDato.minusYears(1)),
                            ),
                            lagDeltBosted(
                                startdatoForKontrakt = dagensDato.minusMonths(6),
                                sluttdatoForKontrakt = null,
                                matrikkeladresse = lagMatrikkeladresse(1001L),
                                folkeregistermetadata = null,
                            ),
                        ),
                )

            every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(søker.aktør) } returns personInfoSøker
            every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barn.aktør) } returns personInfoBarn
            every { kodeverkService.hentPoststed(any()) } returns null

            // Act
            val oppdatertPersonopplysningGrunnlag = persongrunnlagService.oppdaterAdresserPåPersoner(personopplysningGrunnlag)

            // Assert
            assertThat(oppdatertPersonopplysningGrunnlag.personer).hasSize(2)
            assertThat(oppdatertPersonopplysningGrunnlag.personer).anySatisfy {
                assertThat(it.aktør).isEqualTo(søker.aktør)
                assertThat(it.bostedsadresser).hasSize(1)
                assertThat(it.bostedsadresser).anySatisfy {
                    assertThat(it.person).isEqualTo(søker)
                    assertThat(it.periode?.fom).isEqualTo(dagensDato.minusYears(1))
                    assertThat(it.periode?.tom).isNull()
                }
                assertThat(it.oppholdsadresser).hasSize(1)
                assertThat(it.oppholdsadresser).anySatisfy {
                    assertThat(it.person).isEqualTo(søker)
                    assertThat(it.periode?.fom).isEqualTo(dagensDato.minusYears(2))
                    assertThat(it.periode?.tom).isNull()
                }
                assertThat(it.deltBosted).hasSize(1)
                assertThat(it.deltBosted).anySatisfy {
                    assertThat(it.person).isEqualTo(søker)
                    assertThat(it.periode?.fom).isEqualTo(dagensDato.minusYears(25))
                    assertThat(it.periode?.tom).isNull()
                }
            }
            assertThat(oppdatertPersonopplysningGrunnlag.personer).anySatisfy {
                assertThat(it.aktør).isEqualTo(barn.aktør)
                assertThat(it.bostedsadresser).hasSize(1)
                assertThat(it.bostedsadresser).anySatisfy {
                    assertThat(it.person).isEqualTo(barn)
                    assertThat(it.periode?.fom).isEqualTo(dagensDato.minusYears(2))
                    assertThat(it.periode?.tom).isNull()
                }
                assertThat(it.oppholdsadresser).hasSize(1)
                assertThat(it.oppholdsadresser).anySatisfy {
                    assertThat(it.person).isEqualTo(barn)
                    assertThat(it.periode?.fom).isEqualTo(dagensDato.minusYears(1))
                    assertThat(it.periode?.tom).isNull()
                }
                assertThat(it.deltBosted).hasSize(1)
                assertThat(it.deltBosted).anySatisfy {
                    assertThat(it.person).isEqualTo(barn)
                    assertThat(it.periode?.fom).isEqualTo(dagensDato.minusMonths(6))
                    assertThat(it.periode?.tom).isNull()
                }
            }
        }
    }
}
