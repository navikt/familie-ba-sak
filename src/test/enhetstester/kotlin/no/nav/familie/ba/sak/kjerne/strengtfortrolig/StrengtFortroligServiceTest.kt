package no.nav.familie.ba.sak.kjerne.strengtfortrolig

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.ekstern.restDomene.ArbeidsfordelingPåBehandlingDto
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.BehandlingUnderkategoriDTO
import no.nav.familie.ba.sak.ekstern.restDomene.EndretUtbetalingAndelDto
import no.nav.familie.ba.sak.ekstern.restDomene.KompetanseDto
import no.nav.familie.ba.sak.ekstern.restDomene.PersonDto
import no.nav.familie.ba.sak.ekstern.restDomene.PersonMedAndelerDto
import no.nav.familie.ba.sak.ekstern.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.UtvidetBehandlingDto
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.logg.Logg
import no.nav.familie.ba.sak.kjerne.logg.LoggType
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.strengtfortrolig.StrengtFortroligService.Companion.BEGRUNNELSE_STRENGT_FORTROLIG
import no.nav.familie.ba.sak.kjerne.strengtfortrolig.StrengtFortroligService.Companion.SKJERMET_BARN
import no.nav.familie.ba.sak.kjerne.strengtfortrolig.StrengtFortroligService.Companion.SKJERMET_BARN_FØDSELSDATO
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ba.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class StrengtFortroligServiceTest {
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository = mockk()
    private val familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository = mockk()

    private val strengtFortroligService =
        StrengtFortroligService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            familieIntegrasjonerTilgangskontrollService = familieIntegrasjonerTilgangskontrollService,
            featureToggleService = featureToggleService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
        )

    private val søkerAktør = randomAktør()
    private val barnAktør = randomAktør()
    private val fagsak = lagFagsak(aktør = søkerAktør)
    private val behandling = lagBehandling(fagsak)
    private val sisteIverksatteBehandling = lagBehandling(fagsak)

    @BeforeEach
    fun setUp() {
        mockBrukerContext("testbruker")
        every { featureToggleService.isEnabled(FeatureToggle.TILLAT_TILGANG_SKJERMET_BARN_UTEN_LØPENDE_ANDELER) } returns true
        every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
        every { personopplysningGrunnlagRepository.finnSøkerOgBarnAktørerTilFagsak(fagsak.id) } returns
            setOf(
                PersonEnkel(aktør = søkerAktør, type = PersonType.SØKER, fødselsdato = LocalDate.of(1990, 1, 1), dødsfallDato = null, målform = Målform.NB),
                PersonEnkel(aktør = barnAktør, type = PersonType.BARN, fødselsdato = LocalDate.of(2020, 6, 15), dødsfallDato = null, målform = Målform.NB),
            )
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns sisteIverksatteBehandling
    }

    @AfterEach
    fun tearDown() {
        clearBrukerContext()
    }

    @Nested
    inner class AnonymiserStrengtFortroligeBarnTest {
        @Test
        fun `skal returnere uendret utvidet behandling dto når saksbehandler har tilgang til alle personer`() {
            // Arrange
            mockSaksbehandlerHarTilgangTilAllePersoner()
            val utvidetBehandlingDto =
                lagMinimalUtvidetBehandlingDto(
                    personer = listOf(lagPersonDto(barnAktør.aktivFødselsnummer())),
                )

            // Act
            val resultat = strengtFortroligService.anonymiserStrengtFortroligBarn(utvidetBehandlingDto, fagsak)

            // Assert
            assertThat(resultat).isEqualTo(utvidetBehandlingDto)
        }

        @Test
        fun `skal anonymisere barn med strengt diskresjonskode når saksbehandler ikke har tilgang til barn samt at barn ikke har løpende andeler`() {
            // Arrange
            mockSaksbehandlerHarTilgangTilSøkerMenIkkeBarn()
            mockBarnUtenLøpendeAndeler()
            val barnIdent = barnAktør.aktivFødselsnummer()
            val søkerIdent = søkerAktør.aktivFødselsnummer()

            val utvidetBehandlingDto =
                lagMinimalUtvidetBehandlingDto(
                    personer =
                        listOf(
                            lagPersonDto(søkerIdent, type = PersonType.SØKER, navn = "Søker Søkersen"),
                            lagPersonDto(barnIdent, navn = "Barn med diskresjonskode"),
                        ),
                    personerMedAndelerTilkjentYtelse =
                        listOf(
                            PersonMedAndelerDto(personIdent = barnIdent, beløp = 1000, stønadFom = YearMonth.of(2023, 1), stønadTom = YearMonth.of(2024, 12), ytelsePerioder = emptyList()),
                        ),
                    endretUtbetalingAndeler =
                        listOf(
                            EndretUtbetalingAndelDto(id = 1, personIdenter = listOf(barnIdent), prosent = null, fom = null, tom = null, årsak = null, avtaletidspunktDeltBosted = null, søknadstidspunkt = null, begrunnelse = null, erTilknyttetAndeler = null),
                        ),
                    kompetanser =
                        listOf(
                            KompetanseDto(id = 1, fom = null, tom = null, barnIdenter = listOf(barnIdent)),
                        ),
                    søknadsgrunnlag =
                        SøknadDTO(
                            underkategori = BehandlingUnderkategoriDTO.ORDINÆR,
                            søkerMedOpplysninger = SøkerMedOpplysninger(ident = søkerIdent),
                            barnaMedOpplysninger = listOf(BarnMedOpplysninger(ident = barnIdent, navn = "Barn med diskresjonskode", fødselsdato = LocalDate.of(2020, 6, 15))),
                        ),
                )

            // Act
            val resultat = strengtFortroligService.anonymiserStrengtFortroligBarn(utvidetBehandlingDto, fagsak)

            // Assert
            val skjermetBarn = resultat.personer.find { it.type == PersonType.BARN }!!
            assertThat(skjermetBarn.navn).isEqualTo("$SKJERMET_BARN 1")
            assertThat(skjermetBarn.fødselsdato).isEqualTo(SKJERMET_BARN_FØDSELSDATO)
            assertThat(skjermetBarn.personIdent).isEqualTo("$SKJERMET_BARN 1")
            assertThat(skjermetBarn.skjermesForBruker).isTrue()

            val søkerPerson = resultat.personer.find { it.type == PersonType.SØKER }!!
            assertThat(søkerPerson.navn).isEqualTo("Søker Søkersen")
            assertThat(søkerPerson.skjermesForBruker).isFalse()

            assertThat(resultat.personerMedAndelerTilkjentYtelse.first().personIdent).isEqualTo("$SKJERMET_BARN 1")
            assertThat(resultat.personerMedAndelerTilkjentYtelse.first().skjermesForBruker).isTrue()
            assertThat(resultat.endretUtbetalingAndeler.first().personIdenter).containsExactly("$SKJERMET_BARN 1")
            assertThat(resultat.endretUtbetalingAndeler.first().inneholderBarnSomSkalSkjermes).isTrue()
            assertThat(resultat.kompetanser.first().barnIdenter).containsExactly("$SKJERMET_BARN 1")
            assertThat(resultat.kompetanser.first().inneholderBarnSomSkalSkjermes).isTrue()

            val anonymisertBarnISøknad = resultat.søknadsgrunnlag!!.barnaMedOpplysninger.first()
            assertThat(anonymisertBarnISøknad.ident).isEqualTo("$SKJERMET_BARN 1")
            assertThat(anonymisertBarnISøknad.navn).isEqualTo("$SKJERMET_BARN 1")
            assertThat(anonymisertBarnISøknad.fødselsdato).isEqualTo(SKJERMET_BARN_FØDSELSDATO)
            assertThat(anonymisertBarnISøknad.skjermesForBruker).isTrue()
        }
    }

    @Nested
    inner class FiltrerLoggForStrengtFortroligeBarnTest {
        @Test
        fun `skal filtrere bort logginnslag som inneholder personident til skjermet barn`() {
            // Arrange
            mockSaksbehandlerHarTilgangTilSøkerMenIkkeBarn()
            mockBarnUtenLøpendeAndeler()
            val barnIdent = barnAktør.aktivFødselsnummer()

            val logger =
                listOf(
                    Logg(behandlingId = behandling.id, type = LoggType.BEHANDLING_OPPRETTET, rolle = BehandlerRolle.SYSTEM, tekst = "Behandling opprettet"),
                    Logg(behandlingId = behandling.id, type = LoggType.BARN_LAGT_TIL, rolle = BehandlerRolle.SAKSBEHANDLER, tekst = "BARN (4 år) | $barnIdent lagt til"),
                )

            // Act
            val resultat = strengtFortroligService.anonymiserLoggForStrengtFortroligeBarn(logger, behandling.id)

            // Assert
            assertThat(resultat).hasSize(2)
            assertThat(resultat.single { it.type == LoggType.BARN_LAGT_TIL }.tekst).isEqualTo("Informasjon om strengt fortrolig barn er filtrert ut.")
        }

        @Test
        fun `skal returnere alle logginnslag når saksbehandler har tilgang til alle personer`() {
            // Arrange
            mockSaksbehandlerHarTilgangTilAllePersoner()
            val barnIdent = barnAktør.aktivFødselsnummer()

            val logger =
                listOf(
                    Logg(behandlingId = behandling.id, type = LoggType.BEHANDLING_OPPRETTET, rolle = BehandlerRolle.SYSTEM, tekst = "Behandling opprettet"),
                    Logg(behandlingId = behandling.id, type = LoggType.BARN_LAGT_TIL, rolle = BehandlerRolle.SAKSBEHANDLER, tekst = "BARN (4 år) | $barnIdent lagt til"),
                )

            // Act
            val resultat = strengtFortroligService.anonymiserLoggForStrengtFortroligeBarn(logger, behandling.id)

            // Assert
            assertThat(resultat).hasSize(2)
            assertThat(resultat.single { it.type == LoggType.BARN_LAGT_TIL }.tekst).isEqualTo("BARN (4 år) | $barnIdent lagt til")
        }
    }

    @Nested
    inner class SaksbehandlerManglerKunTilgangTilSkjermedeBarnUtenLøpendeAndelerTest {
        @Test
        fun `skal returnere true når saksbehandler kun mangler tilgang til skjermet barn uten løpende andeler`() {
            // Arrange
            mockSaksbehandlerHarTilgangTilSøkerMenIkkeBarn()
            mockBarnUtenLøpendeAndeler()
            val tilganger =
                listOf(
                    Tilgang(søkerAktør.aktivFødselsnummer(), true),
                    Tilgang(barnAktør.aktivFødselsnummer(), false, BEGRUNNELSE_STRENGT_FORTROLIG),
                )

            // Act
            val resultat =
                strengtFortroligService.saksbehandlerManglerKunTilgangTilSkjermedeBarnUtenLøpendeAndeler(fagsak, tilganger)

            // Assert
            assertThat(resultat).isTrue()
        }

        @Test
        fun `skal returnere false når saksbehandler har tilgang til alle personer`() {
            // Arrange
            val tilganger =
                listOf(
                    Tilgang(søkerAktør.aktivFødselsnummer(), true),
                    Tilgang(barnAktør.aktivFødselsnummer(), true),
                )

            // Act
            val resultat =
                strengtFortroligService.saksbehandlerManglerKunTilgangTilSkjermedeBarnUtenLøpendeAndeler(fagsak, tilganger)

            // Assert
            assertThat(resultat).isFalse()
        }

        @Test
        fun `skal returnere false når saksbehandler mangler tilgang til skjermet barn med løpende andeler`() {
            // Arrange
            mockSaksbehandlerHarTilgangTilSøkerMenIkkeBarn()
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2099, 12),
                        behandling = sisteIverksatteBehandling,
                        aktør = barnAktør,
                    ),
                )
            val tilganger =
                listOf(
                    Tilgang(søkerAktør.aktivFødselsnummer(), true),
                    Tilgang(barnAktør.aktivFødselsnummer(), false, BEGRUNNELSE_STRENGT_FORTROLIG),
                )

            // Act
            val resultat =
                strengtFortroligService.saksbehandlerManglerKunTilgangTilSkjermedeBarnUtenLøpendeAndeler(fagsak, tilganger)

            // Assert
            assertThat(resultat).isFalse()
        }

        @Test
        fun `skal returnere false når saksbehandler mangler tilgang til søker`() {
            // Arrange
            mockBarnUtenLøpendeAndeler()
            every { familieIntegrasjonerTilgangskontrollService.sjekkTilgangTilPersoner(any()) } returns
                mapOf(
                    barnAktør.aktivFødselsnummer() to Tilgang(barnAktør.aktivFødselsnummer(), false, BEGRUNNELSE_STRENGT_FORTROLIG),
                )
            val tilganger =
                listOf(
                    Tilgang(søkerAktør.aktivFødselsnummer(), false, BEGRUNNELSE_STRENGT_FORTROLIG),
                    Tilgang(barnAktør.aktivFødselsnummer(), false, BEGRUNNELSE_STRENGT_FORTROLIG),
                )

            // Act
            val resultat =
                strengtFortroligService.saksbehandlerManglerKunTilgangTilSkjermedeBarnUtenLøpendeAndeler(fagsak, tilganger)

            // Assert
            assertThat(resultat).isFalse()
        }
    }

    @Nested
    inner class HentSkjermedeBarnUtenLøpendeAndelerSaksbehandlerIkkeHarTilgangTilTest {
        @Test
        fun `skal returnere tomt sett når saksbehandler har tilgang til alle barn`() {
            // Arrange
            mockSaksbehandlerHarTilgangTilAllePersoner()

            // Act
            val resultat = strengtFortroligService.hentSkjermedeBarnUtenLøpendeAndelerSaksbehandlerIkkeHarTilgangTil(fagsak)

            // Assert
            assertThat(resultat).isEmpty()
        }

        @Test
        fun `skal returnere ident for skjermet barn uten løpende andeler`() {
            // Arrange
            mockSaksbehandlerHarTilgangTilSøkerMenIkkeBarn()
            mockBarnUtenLøpendeAndeler()

            // Act
            val resultat = strengtFortroligService.hentSkjermedeBarnUtenLøpendeAndelerSaksbehandlerIkkeHarTilgangTil(fagsak)

            // Assert
            assertThat(resultat).containsExactly(barnAktør.aktivFødselsnummer())
        }

        @Test
        fun `skal returnere tomt sett når skjermet barn fortsatt har løpende andeler`() {
            // Arrange
            mockSaksbehandlerHarTilgangTilSøkerMenIkkeBarn()
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.of(2023, 1),
                        tom = YearMonth.of(2099, 12),
                        behandling = sisteIverksatteBehandling,
                        aktør = barnAktør,
                    ),
                )

            // Act
            val resultat = strengtFortroligService.hentSkjermedeBarnUtenLøpendeAndelerSaksbehandlerIkkeHarTilgangTil(fagsak)

            // Assert
            assertThat(resultat).isEmpty()
        }

        @Test
        fun `skal returnere tomt sett når skjermet barn aldri har hatt andeler`() {
            // Arrange
            mockSaksbehandlerHarTilgangTilSøkerMenIkkeBarn()
            mockBarnUtenAndeler()

            // Act
            val resultat = strengtFortroligService.hentSkjermedeBarnUtenLøpendeAndelerSaksbehandlerIkkeHarTilgangTil(fagsak)

            // Assert
            assertThat(resultat).isEmpty()
        }

        @Test
        fun `skal returnere tomt sett når fagsaken ikke har noe persongrunnlag`() {
            // Arrange
            every { personopplysningGrunnlagRepository.finnSøkerOgBarnAktørerTilFagsak(fagsak.id) } returns emptySet()

            // Act
            val resultat = strengtFortroligService.hentSkjermedeBarnUtenLøpendeAndelerSaksbehandlerIkkeHarTilgangTil(fagsak)

            // Assert
            assertThat(resultat).isEmpty()
        }

        @Test
        fun `skal returnere tomt sett når kallet er systemkjøring`() {
            // Arrange
            clearBrukerContext()
            mockBrukerContext(SikkerhetContext.SYSTEM_FORKORTELSE)

            // Act
            val resultat = strengtFortroligService.hentSkjermedeBarnUtenLøpendeAndelerSaksbehandlerIkkeHarTilgangTil(fagsak)

            // Assert
            assertThat(resultat).isEmpty()
        }

        @Test
        fun `skal returnere tomt sett når feature toggle er deaktivert`() {
            // Arrange
            every { featureToggleService.isEnabled(FeatureToggle.TILLAT_TILGANG_SKJERMET_BARN_UTEN_LØPENDE_ANDELER) } returns false

            // Act
            val resultat = strengtFortroligService.hentSkjermedeBarnUtenLøpendeAndelerSaksbehandlerIkkeHarTilgangTil(fagsak)

            // Assert
            assertThat(resultat).isEmpty()
        }

        @Test
        fun `skal returnere tomt sett når barn mangler tilgang av annen årsak enn strengt fortrolig`() {
            // Arrange
            every { familieIntegrasjonerTilgangskontrollService.sjekkTilgangTilPersoner(any()) } returns
                mapOf(
                    barnAktør.aktivFødselsnummer() to Tilgang(barnAktør.aktivFødselsnummer(), false, "Annen årsak"),
                )
            mockBarnUtenLøpendeAndeler()

            // Act
            val resultat = strengtFortroligService.hentSkjermedeBarnUtenLøpendeAndelerSaksbehandlerIkkeHarTilgangTil(fagsak)

            // Assert
            assertThat(resultat).isEmpty()
        }
    }

    @Nested
    inner class FiltrerVekkVedtaksperioderMedSkjermetBarnTest {
        @Test
        fun `skal filtrere vekk perioder som inneholder skjermet barn`() {
            // Arrange
            mockSaksbehandlerHarTilgangTilSøkerMenIkkeBarn()
            mockBarnUtenLøpendeAndeler()

            val periodeMedSkjermetBarn =
                lagUtvidetVedtaksperiodeDto(
                    id = 1L,
                    identer = listOf(søkerAktør.aktivFødselsnummer(), barnAktør.aktivFødselsnummer()),
                )
            val periodeUtenSkjermetBarn =
                lagUtvidetVedtaksperiodeDto(
                    id = 2L,
                    identer = listOf(søkerAktør.aktivFødselsnummer()),
                )

            // Act
            val resultat =
                strengtFortroligService.anonymiserSkjermetBarnIVedtaksperioder(
                    vedtaksperioder = listOf(periodeMedSkjermetBarn, periodeUtenSkjermetBarn),
                    fagsak = fagsak,
                )

            // Assert
            assertThat(resultat).containsExactly(periodeUtenSkjermetBarn)
        }

        @Test
        fun `skal ikke filtrere noe når saksbehandler har tilgang til alle personer`() {
            // Arrange
            mockSaksbehandlerHarTilgangTilAllePersoner()

            val perioder =
                listOf(
                    lagUtvidetVedtaksperiodeDto(id = 1L, identer = listOf(søkerAktør.aktivFødselsnummer(), barnAktør.aktivFødselsnummer())),
                    lagUtvidetVedtaksperiodeDto(id = 2L, identer = listOf(søkerAktør.aktivFødselsnummer())),
                )

            // Act
            val resultat =
                strengtFortroligService.anonymiserSkjermetBarnIVedtaksperioder(
                    vedtaksperioder = perioder,
                    fagsak = fagsak,
                )

            // Assert
            assertThat(resultat).isEqualTo(perioder)
        }
    }

    private fun lagUtvidetVedtaksperiodeDto(
        id: Long,
        identer: List<String>,
    ) = no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelserDto(
        id = id,
        fom = LocalDate.of(2023, 1, 1),
        tom = LocalDate.of(2023, 12, 31),
        type = no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype.UTBETALING,
        begrunnelser = emptyList(),
        gyldigeBegrunnelser = emptyList(),
        utbetalingsperiodeDetaljer =
            identer.map {
                no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj(
                    person = lagPersonDto(it),
                    ytelseType = no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.ORDINÆR_BARNETRYGD,
                    utbetaltPerMnd = 1000,
                    erPåvirketAvEndring = false,
                    endringsårsak = null,
                    prosent = java.math.BigDecimal.valueOf(100),
                )
            },
    )

    private fun lagPersonDto(
        personIdent: String,
        type: PersonType = PersonType.BARN,
        navn: String = "Test Person",
    ) = PersonDto(
        type = type,
        fødselsdato = LocalDate.of(2020, 6, 15),
        personIdent = personIdent,
        navn = navn,
        kjønn = Kjønn.KVINNE,
        målform = Målform.NB,
    )

    private fun lagMinimalUtvidetBehandlingDto(
        personer: List<PersonDto> = emptyList(),
        personerMedAndelerTilkjentYtelse: List<PersonMedAndelerDto> = emptyList(),
        endretUtbetalingAndeler: List<EndretUtbetalingAndelDto> = emptyList(),
        kompetanser: List<KompetanseDto> = emptyList(),
        søknadsgrunnlag: SøknadDTO? = null,
    ) = UtvidetBehandlingDto(
        behandlingId = behandling.id,
        steg = StegType.VILKÅRSVURDERING,
        stegTilstand = emptyList(),
        status = BehandlingStatus.UTREDES,
        resultat = Behandlingsresultat.IKKE_VURDERT,
        skalBehandlesAutomatisk = false,
        type = BehandlingType.FØRSTEGANGSBEHANDLING,
        kategori = BehandlingKategori.NASJONAL,
        underkategori = BehandlingUnderkategoriDTO.ORDINÆR,
        årsak = BehandlingÅrsak.SØKNAD,
        opprettetTidspunkt = LocalDateTime.now(),
        endretAv = "testbruker",
        arbeidsfordelingPåBehandling = ArbeidsfordelingPåBehandlingDto("4820", "NAV Porsgrunn"),
        søknadsgrunnlag = søknadsgrunnlag,
        personer = personer,
        personResultater = emptyList(),
        fødselshendelsefiltreringResultater = emptyList(),
        utbetalingsperioder = emptyList(),
        personerMedAndelerTilkjentYtelse = personerMedAndelerTilkjentYtelse,
        endretUtbetalingAndeler = endretUtbetalingAndeler,
        kompetanser = kompetanser,
        tilbakekreving = null,
        vedtak = null,
        totrinnskontroll = null,
        aktivSettPåVent = null,
        migreringsdato = null,
        valutakurser = emptyList(),
        utenlandskePeriodebeløp = emptyList(),
        korrigertEtterbetaling = null,
        korrigertVedtak = null,
        feilutbetaltValuta = emptyList(),
        brevmottakere = emptyList(),
        refusjonEøs = emptyList(),
        søknadMottattDato = null,
        tilbakekrevingsvedtakMotregning = null,
        manglendeSvalbardmerking = emptyList(),
        manglendeFinnmarkmerking = null,
    )

    private fun mockSaksbehandlerHarTilgangTilSøkerMenIkkeBarn() {
        every { familieIntegrasjonerTilgangskontrollService.sjekkTilgangTilPersoner(any()) } returns
            mapOf(
                søkerAktør.aktivFødselsnummer() to Tilgang(søkerAktør.aktivFødselsnummer(), true),
                barnAktør.aktivFødselsnummer() to Tilgang(barnAktør.aktivFødselsnummer(), false, BEGRUNNELSE_STRENGT_FORTROLIG),
            )
    }

    private fun mockSaksbehandlerHarTilgangTilAllePersoner() {
        every { familieIntegrasjonerTilgangskontrollService.sjekkTilgangTilPersoner(any()) } returns
            mapOf(
                søkerAktør.aktivFødselsnummer() to Tilgang(søkerAktør.aktivFødselsnummer(), true),
                barnAktør.aktivFødselsnummer() to Tilgang(barnAktør.aktivFødselsnummer(), true),
            )
    }

    private fun mockBarnUtenLøpendeAndeler() {
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id) } returns
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2024, 12),
                    behandling = sisteIverksatteBehandling,
                    aktør = barnAktør,
                ),
            )
    }

    private fun mockBarnUtenAndeler() {
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id) } returns
            emptyList()
    }
}
