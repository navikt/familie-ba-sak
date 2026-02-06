package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagSøknadDTO
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVegadresse
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.KodeverkService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.falskidentitet.FalskIdentitetService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.kontrakter.felles.PersonIdent
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
    private val featureToggleService = mockk<FeatureToggleService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val falskIdentitetService = mockk<FalskIdentitetService>()

    private val persongrunnlagService =
        spyk(
            PersongrunnlagService(
                personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
                statsborgerskapService = mockk(),
                arbeidsfordelingService = mockk(relaxed = true),
                personopplysningerService = personopplysningerService,
                personidentService = personidentService,
                saksstatistikkEventPublisher = mockk(relaxed = true),
                behandlingHentOgPersisterService = behandlingHentOgPersisterService,
                andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
                loggService = loggService,
                arbeidsforholdService = mockk(),
                vilkårsvurderingService = vilkårsvurderingService,
                kodeverkService = kodeverkService,
                featureToggleService = featureToggleService,
                falskIdentitetService = falskIdentitetService,
            ),
        )

    @BeforeEach
    fun setup() {
        every { featureToggleService.isEnabled(FeatureToggle.ARBEIDSFORHOLD_STRENGERE_NEDHENTING) } returns true
    }

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

                every { personopplysningerService.hentPersoninfoEnkel(barnet.aktør) } returns PersonInfo(barnet.fødselsdato)

                every {
                    personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barnet.aktør)
                } returns PersonInfo(barnet.fødselsdato, barnet.navn, barnet.kjønn)

                every { personopplysningGrunnlagRepository.save(nyttGrunnlag) } returns nyttGrunnlag

                every { featureToggleService.isEnabled(FeatureToggle.FILTRER_ADRESSE_FOR_SØKER_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true
                every { featureToggleService.isEnabled(FeatureToggle.FILTRER_STATSBORGERSKAP_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true
                every { featureToggleService.isEnabled(FeatureToggle.FILTRER_OPPHOLD_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true
                every { featureToggleService.isEnabled(FeatureToggle.FILTRER_SIVILSTAND_FOR_SØKER_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true
                every { featureToggleService.isEnabled(FeatureToggle.HARDKODET_EEAFREG_STATSBORGERSKAP) } returns true

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

        @Test
        fun `skal velge eldste barns fødselsdato i inneværende behandling`() {
            // Arrange
            val søker = lagPerson(type = PersonType.SØKER)
            val eldsteBarnInneværendeBehandling = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2015, 1, 1))
            val yngsteBarnInneværendeBehandling = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2020, 1, 1))
            val mellomBarnForrigeBehandling = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2022, 1, 1))

            val behandling = lagBehandling(behandlingType = BehandlingType.REVURDERING)
            val nyttGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)

            every { persongrunnlagService.lagreOgDeaktiverGammel(any()) } returns nyttGrunnlag

            every { personopplysningerService.hentPersoninfoEnkel(eldsteBarnInneværendeBehandling.aktør) } returns PersonInfo(eldsteBarnInneværendeBehandling.fødselsdato)
            every { personopplysningerService.hentPersoninfoEnkel(yngsteBarnInneværendeBehandling.aktør) } returns PersonInfo(yngsteBarnInneværendeBehandling.fødselsdato)
            every { personopplysningerService.hentPersoninfoEnkel(mellomBarnForrigeBehandling.aktør) } returns PersonInfo(mellomBarnForrigeBehandling.fødselsdato)

            every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(søker.aktør) } returns
                PersonInfo(søker.fødselsdato)
            every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(eldsteBarnInneværendeBehandling.aktør) } returns
                PersonInfo(eldsteBarnInneværendeBehandling.fødselsdato)
            every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(yngsteBarnInneværendeBehandling.aktør) } returns
                PersonInfo(yngsteBarnInneværendeBehandling.fødselsdato)
            every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(mellomBarnForrigeBehandling.aktør) } returns
                PersonInfo(mellomBarnForrigeBehandling.fødselsdato)

            every { personopplysningGrunnlagRepository.save(nyttGrunnlag) } returns nyttGrunnlag
            every { featureToggleService.isEnabled(FeatureToggle.FILTRER_ADRESSE_FOR_SØKER_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true
            every { featureToggleService.isEnabled(FeatureToggle.FILTRER_STATSBORGERSKAP_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true
            every { featureToggleService.isEnabled(FeatureToggle.FILTRER_OPPHOLD_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true
            every { featureToggleService.isEnabled(FeatureToggle.FILTRER_SIVILSTAND_FOR_SØKER_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true

            // Act
            val personopplysningGrunnlag =
                persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                    aktør = søker.aktør,
                    barnFraInneværendeBehandling = listOf(eldsteBarnInneværendeBehandling.aktør, yngsteBarnInneværendeBehandling.aktør),
                    barnFraForrigeBehandling = listOf(mellomBarnForrigeBehandling.aktør),
                    behandling = behandling,
                    målform = Målform.NB,
                )

            // Assert
            verify(exactly = 1) { personopplysningerService.hentPersoninfoEnkel(eldsteBarnInneværendeBehandling.aktør) }
            verify(exactly = 1) { personopplysningerService.hentPersoninfoEnkel(yngsteBarnInneværendeBehandling.aktør) }
            verify(exactly = 1) { personopplysningerService.hentPersoninfoEnkel(mellomBarnForrigeBehandling.aktør) }
            assertThat(personopplysningGrunnlag.personer).hasSize(4)
            assertThat(personopplysningGrunnlag.barna).hasSize(3)
            assertThat(personopplysningGrunnlag.barna.map { it.fødselsdato }).containsExactlyInAnyOrder(
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2022, 1, 1),
            )
        }

        @Test
        fun `skal velge eldste barns fødselsdato i forrige behandling`() {
            // Arrange
            val søker = lagPerson(type = PersonType.SØKER)
            val eldsteBarnForrigeBehandling = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2010, 1, 1))
            val yngsteBarnForrigeBehandling = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2020, 1, 1))
            val mellomBarnInneværendeBehandling = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2015, 1, 1))

            val behandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)
            val nyttGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)

            every { persongrunnlagService.lagreOgDeaktiverGammel(any()) } returns nyttGrunnlag

            every { personopplysningerService.hentPersoninfoEnkel(eldsteBarnForrigeBehandling.aktør) } returns PersonInfo(eldsteBarnForrigeBehandling.fødselsdato)
            every { personopplysningerService.hentPersoninfoEnkel(yngsteBarnForrigeBehandling.aktør) } returns PersonInfo(yngsteBarnForrigeBehandling.fødselsdato)
            every { personopplysningerService.hentPersoninfoEnkel(mellomBarnInneværendeBehandling.aktør) } returns PersonInfo(mellomBarnInneværendeBehandling.fødselsdato)

            every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(søker.aktør) } returns
                PersonInfo(søker.fødselsdato)
            every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(eldsteBarnForrigeBehandling.aktør) } returns
                PersonInfo(eldsteBarnForrigeBehandling.fødselsdato)
            every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(yngsteBarnForrigeBehandling.aktør) } returns
                PersonInfo(yngsteBarnForrigeBehandling.fødselsdato)
            every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(mellomBarnInneværendeBehandling.aktør) } returns
                PersonInfo(mellomBarnInneværendeBehandling.fødselsdato)

            every { personopplysningGrunnlagRepository.save(nyttGrunnlag) } returns nyttGrunnlag

            every { featureToggleService.isEnabled(FeatureToggle.FILTRER_ADRESSE_FOR_SØKER_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true
            every { featureToggleService.isEnabled(FeatureToggle.FILTRER_STATSBORGERSKAP_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true
            every { featureToggleService.isEnabled(FeatureToggle.FILTRER_OPPHOLD_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true
            every { featureToggleService.isEnabled(FeatureToggle.FILTRER_SIVILSTAND_FOR_SØKER_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true

            // Act
            val personopplysningGrunnlag =
                persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                    aktør = søker.aktør,
                    barnFraInneværendeBehandling = listOf(mellomBarnInneværendeBehandling.aktør),
                    barnFraForrigeBehandling = listOf(eldsteBarnForrigeBehandling.aktør, yngsteBarnForrigeBehandling.aktør),
                    behandling = behandling,
                    målform = Målform.NB,
                )

            // Assert
            verify(exactly = 1) { personopplysningerService.hentPersoninfoEnkel(eldsteBarnForrigeBehandling.aktør) }
            verify(exactly = 1) { personopplysningerService.hentPersoninfoEnkel(yngsteBarnForrigeBehandling.aktør) }
            verify(exactly = 1) { personopplysningerService.hentPersoninfoEnkel(mellomBarnInneværendeBehandling.aktør) }
            assertThat(personopplysningGrunnlag.personer).hasSize(4)
            assertThat(personopplysningGrunnlag.barna).hasSize(3)
            assertThat(personopplysningGrunnlag.barna.map { it.fødselsdato }).containsExactlyInAnyOrder(
                LocalDate.of(2010, 1, 1),
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2015, 1, 1),
            )
        }

        @Test
        fun `skal bruke eldste barns fødselsdato som cutoff for å filtrere søkers bostedadresser`() {
            // Arrange
            val søker = lagPerson(type = PersonType.SØKER, fødselsdato = LocalDate.of(1990, 1, 1))
            val eldsteBarn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2015, 1, 1))
            val yngreBarn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2020, 1, 1))
            val behandling = lagBehandling()

            val nyttGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)

            every { persongrunnlagService.lagreOgDeaktiverGammel(any()) } returns nyttGrunnlag
            every { personopplysningerService.hentPersoninfoEnkel(eldsteBarn.aktør) } returns PersonInfo(eldsteBarn.fødselsdato)
            every { personopplysningerService.hentPersoninfoEnkel(yngreBarn.aktør) } returns PersonInfo(yngreBarn.fødselsdato)

            val adresseFørEldsteBarn =
                lagBostedsadresse(
                    gyldigFraOgMed = LocalDate.of(2000, 1, 1),
                    gyldigTilOgMed = LocalDate.of(2014, 12, 31),
                    vegadresse = lagVegadresse(),
                )
            val adresseOverlapperEldsteBarn =
                lagBostedsadresse(
                    gyldigFraOgMed = LocalDate.of(2014, 6, 1),
                    gyldigTilOgMed = LocalDate.of(2016, 6, 1),
                    vegadresse = lagVegadresse(),
                )
            val adresseEtterEldsteBarn =
                lagBostedsadresse(
                    gyldigFraOgMed = LocalDate.of(2016, 6, 2),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(),
                )

            every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(søker.aktør) } returns
                PersonInfo(
                    fødselsdato = søker.fødselsdato,
                    bostedsadresser = listOf(adresseFørEldsteBarn, adresseOverlapperEldsteBarn, adresseEtterEldsteBarn),
                )
            every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(eldsteBarn.aktør) } returns
                PersonInfo(eldsteBarn.fødselsdato)
            every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(yngreBarn.aktør) } returns
                PersonInfo(yngreBarn.fødselsdato)

            every { personopplysningGrunnlagRepository.save(nyttGrunnlag) } returns nyttGrunnlag
            every { kodeverkService.hentPoststed(any()) } returns "Oslo"
            every { featureToggleService.isEnabled(FeatureToggle.FILTRER_ADRESSE_FOR_SØKER_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true
            every { featureToggleService.isEnabled(FeatureToggle.FILTRER_STATSBORGERSKAP_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true
            every { featureToggleService.isEnabled(FeatureToggle.FILTRER_OPPHOLD_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true
            every { featureToggleService.isEnabled(FeatureToggle.FILTRER_SIVILSTAND_FOR_SØKER_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true

            // Act
            val personopplysningGrunnlag =
                persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                    aktør = søker.aktør,
                    barnFraInneværendeBehandling = listOf(eldsteBarn.aktør, yngreBarn.aktør),
                    behandling = behandling,
                    målform = Målform.NB,
                )

            // Assert
            val bostedadresseSøker = personopplysningGrunnlag.søker.bostedsadresser
            assertThat(bostedadresseSøker).hasSize(2)

            val sorterteAdresser = bostedadresseSøker.sortedBy { it.periode?.fom }
            assertThat(sorterteAdresser.first().periode?.tom).isAfter(eldsteBarn.fødselsdato)
            assertThat(sorterteAdresser.last().periode?.fom).isAfter(eldsteBarn.fødselsdato)
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
    }
}
