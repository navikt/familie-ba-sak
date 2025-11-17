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
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagSøknadDTO
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.KodeverkService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
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
    private val featureToggleService = mockk<FeatureToggleService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()

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
            ),
        )

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

                every { featureToggleService.isEnabled(FeatureToggle.FILTRER_ADRESSE_FOR_SØKER_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true
                every { featureToggleService.isEnabled(FeatureToggle.FILTRER_STATSBORGERSKAP_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true
                every { featureToggleService.isEnabled(FeatureToggle.FILTRER_OPPHOLD_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true
                every { featureToggleService.isEnabled(FeatureToggle.FILTRER_SIVILSTAND_FOR_SØKER_PÅ_ELDSTE_BARNS_FØDSELSDATO) } returns true

                every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling

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
