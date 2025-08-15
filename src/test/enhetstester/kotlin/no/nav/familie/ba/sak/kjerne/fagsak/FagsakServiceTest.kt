package no.nav.familie.ba.sak.kjerne.fagsak

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagVisningsbehandling
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomBarnFnr
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.FagsakDeltagerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonInfo
import no.nav.familie.ba.sak.ekstern.restDomene.RestSkjermetBarnSøker
import no.nav.familie.ba.sak.ekstern.restDomene.tilDto
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.skyggesak.SkyggesakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.kjerne.institusjon.InstitusjonService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.skjermetbarnsøker.SkjermetBarnSøkerRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class FagsakServiceTest {
    private val fagsakRepository = mockk<FagsakRepository>()
    private val personRepository = mockk<PersonRepository>()
    private val andelerTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val personidentService = mockk<PersonidentService>()
    private val utvidetBehandlingService = mockk<UtvidetBehandlingService>()
    private val behandlingService = mockk<BehandlingService>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val familieIntegrasjonerTilgangskontrollService = mockk<FamilieIntegrasjonerTilgangskontrollService>()
    private val saksstatistikkEventPublisher = mockk<SaksstatistikkEventPublisher>()
    private val skyggesakService = mockk<SkyggesakService>()
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val institusjonService = mockk<InstitusjonService>()
    private val organisasjonService = mockk<OrganisasjonService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val unleashService = mockk<UnleashNextMedContextService>()
    private val skjermetBarnSøkerRepository = mockk<SkjermetBarnSøkerRepository>()
    private val integrasjonClient = mockk<IntegrasjonClient>()
    private val fagsakService =
        FagsakService(
            fagsakRepository = fagsakRepository,
            personRepository = personRepository,
            andelerTilkjentYtelseRepository = andelerTilkjentYtelseRepository,
            personidentService = personidentService,
            utvidetBehandlingService = utvidetBehandlingService,
            behandlingService = behandlingService,
            personopplysningerService = personopplysningerService,
            familieIntegrasjonerTilgangskontrollService = familieIntegrasjonerTilgangskontrollService,
            saksstatistikkEventPublisher = saksstatistikkEventPublisher,
            skyggesakService = skyggesakService,
            vedtaksperiodeService = vedtaksperiodeService,
            institusjonService = institusjonService,
            organisasjonService = organisasjonService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            unleashService = unleashService,
            skjermetBarnSøkerRepository = skjermetBarnSøkerRepository,
            integrasjonClient = integrasjonClient,
        )

    @Nested
    inner class LagRestMinimalFagsak {
        @Test
        fun `skal lage rest minimal fagsak`() {
            // Arrange
            val fagsak = lagFagsak()

            val visningsbehandling1 =
                lagVisningsbehandling(
                    behandlingId = 1L,
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                    aktiv = false,
                )

            val visningsbehandling2 =
                lagVisningsbehandling(
                    behandlingId = 2L,
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    aktiv = false,
                )

            val sisteBehandlingSomErVedtatt =
                lagBehandling(
                    fagsak = fagsak,
                    id = visningsbehandling2.behandlingId,
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    aktiv = false,
                )

            val utbetalingsperiode =
                Utbetalingsperiode(
                    periodeFom = LocalDate.now().minusYears(1),
                    periodeTom = LocalDate.now().plusYears(1),
                    vedtaksperiodetype = Vedtaksperiodetype.UTBETALING,
                    utbetalingsperiodeDetaljer = emptyList(),
                    ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD),
                    antallBarn = 1,
                    utbetaltPerMnd = 3000,
                )

            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak
            every { behandlingHentOgPersisterService.finnAktivForFagsak(fagsak.id) } returns null
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns sisteBehandlingSomErVedtatt
            every { vedtaksperiodeService.hentUtbetalingsperioder(sisteBehandlingSomErVedtatt) } returns listOf(utbetalingsperiode)
            every { behandlingHentOgPersisterService.hentVisningsbehandlinger(fagsak.id) } returns listOf(visningsbehandling1, visningsbehandling2)
            every { behandlingService.hentMigreringsdatoPåFagsak(fagsak.id) } returns null

            // Act
            val restMinimalFagsak = fagsakService.lagRestMinimalFagsak(fagsak.id)

            // Assert
            assertThat(restMinimalFagsak.opprettetTidspunkt).isNotNull()
            assertThat(restMinimalFagsak.id).isEqualTo(fagsak.id)
            assertThat(restMinimalFagsak.fagsakeier).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(restMinimalFagsak.søkerFødselsnummer).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(restMinimalFagsak.status).isEqualTo(fagsak.status)
            assertThat(restMinimalFagsak.løpendeKategori).isEqualTo(sisteBehandlingSomErVedtatt.kategori)
            assertThat(restMinimalFagsak.løpendeUnderkategori).isEqualTo(sisteBehandlingSomErVedtatt.underkategori)
            assertThat(restMinimalFagsak.gjeldendeUtbetalingsperioder).hasSize(1)
            assertThat(restMinimalFagsak.gjeldendeUtbetalingsperioder).containsExactly(utbetalingsperiode)
            assertThat(restMinimalFagsak.underBehandling).isFalse()
            assertThat(restMinimalFagsak.migreringsdato).isNull()
            assertThat(restMinimalFagsak.fagsakType).isEqualTo(FagsakType.NORMAL)
            assertThat(restMinimalFagsak.institusjon).isNull()
            assertThat(restMinimalFagsak.behandlinger).hasSize(2)
            assertThat(restMinimalFagsak.behandlinger).anySatisfy {
                assertThat(it.behandlingId).isEqualTo(visningsbehandling1.behandlingId)
                assertThat(it.opprettetTidspunkt).isEqualTo(visningsbehandling1.opprettetTidspunkt)
                assertThat(it.aktivertTidspunkt).isEqualTo(visningsbehandling1.aktivertTidspunkt)
                assertThat(it.kategori).isEqualTo(visningsbehandling1.kategori)
                assertThat(it.underkategori).isEqualTo(visningsbehandling1.underkategori.tilDto())
                assertThat(it.aktiv).isEqualTo(visningsbehandling1.aktiv)
                assertThat(it.årsak).isEqualTo(visningsbehandling1.opprettetÅrsak)
                assertThat(it.type).isEqualTo(visningsbehandling1.type)
                assertThat(it.status).isEqualTo(visningsbehandling1.status)
                assertThat(it.resultat).isEqualTo(visningsbehandling1.resultat)
                assertThat(it.vedtaksdato).isEqualTo(visningsbehandling1.vedtaksdato)
            }
            assertThat(restMinimalFagsak.behandlinger).anySatisfy {
                assertThat(it.behandlingId).isEqualTo(visningsbehandling2.behandlingId)
                assertThat(it.opprettetTidspunkt).isEqualTo(visningsbehandling2.opprettetTidspunkt)
                assertThat(it.aktivertTidspunkt).isEqualTo(visningsbehandling2.aktivertTidspunkt)
                assertThat(it.kategori).isEqualTo(visningsbehandling2.kategori)
                assertThat(it.underkategori).isEqualTo(visningsbehandling2.underkategori.tilDto())
                assertThat(it.aktiv).isEqualTo(visningsbehandling2.aktiv)
                assertThat(it.årsak).isEqualTo(visningsbehandling2.opprettetÅrsak)
                assertThat(it.type).isEqualTo(visningsbehandling2.type)
                assertThat(it.status).isEqualTo(visningsbehandling2.status)
                assertThat(it.resultat).isEqualTo(visningsbehandling2.resultat)
                assertThat(it.vedtaksdato).isEqualTo(visningsbehandling2.vedtaksdato)
            }
        }
    }

    @Nested
    inner class HentEllerOpprettFagsakTest {
        @Test
        fun `Skal kaste funksjonell feil dersom man forsøker å lage en fagsak med type skjermet barn i automatiske løyper`() {
            // Arrange
            every { unleashService.isEnabled(FeatureToggle.SKAL_BRUKE_FAGSAKTYPE_SKJERMET_BARN) } returns true

            val barnIdent = randomBarnFnr(alder = 5)
            val søkerIdent = randomFnr()
            val restSkjermetBarnSøker = RestSkjermetBarnSøker(søkerIdent)

            // Act && Assert
            val frontendFeilmelding =
                assertThrows<FunksjonellFeil> {
                    fagsakService.hentEllerOpprettFagsak(
                        personIdent = barnIdent,
                        skjermetBarnSøker = restSkjermetBarnSøker,
                        fraAutomatiskBehandling = true,
                        type = FagsakType.SKJERMET_BARN,
                    )
                }.frontendFeilmelding

            assertThat(frontendFeilmelding).isEqualTo("Kan ikke opprette fagsak med fagsaktype SKJERMET_BARN automatisk")
        }

        @Test
        fun `Skal kaste funksjonell feil dersom man forsøker å lage en fagsak med type skjermet barn uten at toggle er på`() {
            // Arrange
            every { unleashService.isEnabled(FeatureToggle.SKAL_BRUKE_FAGSAKTYPE_SKJERMET_BARN) } returns false

            val barnIdent = randomBarnFnr(alder = 5)
            val søkerIdent = randomFnr()
            val restSkjermetBarnSøker = RestSkjermetBarnSøker(søkerIdent)

            // Act && Assert
            val frontendFeilmelding =
                assertThrows<FunksjonellFeil> {
                    fagsakService.hentEllerOpprettFagsak(
                        personIdent = barnIdent,
                        skjermetBarnSøker = restSkjermetBarnSøker,
                        fraAutomatiskBehandling = true,
                        type = FagsakType.SKJERMET_BARN,
                    )
                }.frontendFeilmelding

            assertThat(frontendFeilmelding).isEqualTo("Fagsaktype SKJERMET_BARN er ikke støttet i denne versjonen av tjenesten.")
        }

        @Test
        fun `Skal kaste funksjonell feil dersom man forsøker å lage en fagsak med type skjermet barn men ikke sender med søkers ident`() {
            // Arrange
            val barnIdent = randomBarnFnr(alder = 5)
            val barnAktør = randomAktør(barnIdent)

            every { unleashService.isEnabled(FeatureToggle.SKAL_BRUKE_FAGSAKTYPE_SKJERMET_BARN) } returns true
            every { personidentService.hentOgLagreAktør(barnIdent, true) } returns barnAktør

            // Act && Assert
            val frontendFeilmelding =
                assertThrows<FunksjonellFeil> {
                    fagsakService.hentEllerOpprettFagsak(
                        personIdent = barnIdent,
                        skjermetBarnSøker = null,
                        fraAutomatiskBehandling = false,
                        type = FagsakType.SKJERMET_BARN,
                    )
                }.frontendFeilmelding

            assertThat(frontendFeilmelding).isEqualTo("Mangler påkrevd variabel søkersident for skjermet barn søker")
        }

        @Test
        fun `Skal kaste funksjonell feil dersom man forsøker å lage en fagsak med type skjermet barn men søker og barn har samme ident`() {
            // Arrange
            val ident = randomBarnFnr(alder = 5)
            val barnIdent = ident
            val søkerIdent = ident
            val barnAktør = randomAktør(barnIdent)

            val restSkjermetBarnSøker = RestSkjermetBarnSøker(søkerIdent)

            every { unleashService.isEnabled(FeatureToggle.SKAL_BRUKE_FAGSAKTYPE_SKJERMET_BARN) } returns true
            every { personidentService.hentOgLagreAktør(barnIdent, true) } returns barnAktør

            // Act && Assert
            val frontendFeilmelding =
                assertThrows<FunksjonellFeil> {
                    fagsakService.hentEllerOpprettFagsak(
                        personIdent = barnIdent,
                        skjermetBarnSøker = restSkjermetBarnSøker,
                        fraAutomatiskBehandling = false,
                        type = FagsakType.SKJERMET_BARN,
                    )
                }.frontendFeilmelding

            assertThat(frontendFeilmelding).isEqualTo("Søker og barn søkt for kan ikke være lik for fagsak type skjermet barn")
        }

        @Test
        fun `Skal returnere eksisterende fagsak dersom man forsøker å lage en fagsak med type skjermet barn men samme kombinasjon av barn og søker finnes allerede`() {
            // Arrange
            val barnIdent = randomBarnFnr(alder = 5)
            val søkerIdent = randomFnr()
            val barnAktør = randomAktør(barnIdent)
            val søkerAktør = randomAktør(søkerIdent)
            val fagsak = lagFagsak(1, type = FagsakType.SKJERMET_BARN)

            val restSkjermetBarnSøker = RestSkjermetBarnSøker(søkerIdent)

            every { unleashService.isEnabled(FeatureToggle.SKAL_BRUKE_FAGSAKTYPE_SKJERMET_BARN) } returns true
            every { personidentService.hentOgLagreAktør(barnIdent, true) } returns barnAktør
            every { personidentService.hentOgLagreAktør(søkerIdent, true) } returns søkerAktør
            every { fagsakRepository.finnFagsakForSkjermetBarnSøker(barnAktør, søkerAktør) } returns fagsak

            // Act && Assert
            val returnertFagsak =
                fagsakService.hentEllerOpprettFagsak(
                    personIdent = barnIdent,
                    skjermetBarnSøker = restSkjermetBarnSøker,
                    fraAutomatiskBehandling = false,
                    type = FagsakType.SKJERMET_BARN,
                )

            assertThat(returnertFagsak).isEqualTo(fagsak)
        }
    }

    @Nested
    inner class HentFagsakDeltager {
        @Test
        fun `skal returnere tom liste når person ikke finnes`() {
            // Arrange
            val ident = randomFnr()
            every { personidentService.hentAktørOrNullHvisIkkeAktivFødselsnummer(ident) } returns null

            // Act
            val resultat = fagsakService.hentFagsakDeltager(ident)

            // Assert
            assertThat(resultat).isEmpty()
        }

        @Test
        fun `skal returnere kun maskert deltager når søker ikke har tilgang`() {
            // Arrange
            val (barnIdent, barnAktør) = randomBarnFnr().let { it to randomAktør(it) }
            every { personidentService.hentAktørOrNullHvisIkkeAktivFødselsnummer(barnIdent) } returns barnAktør
            every {
                familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(barnAktør)
            } returns
                RestPersonInfo(
                    personIdent = barnAktør.aktivFødselsnummer(),
                    adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG,
                )

            // Act
            val resultat = fagsakService.hentFagsakDeltager(barnIdent)

            // Assert
            assertThat(resultat).hasSize(1)
            assertThat(resultat.single().adressebeskyttelseGradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG)
            assertThat(resultat.single().harTilgang).isFalse()
            assertThat(resultat.single().rolle).isEqualTo(FagsakDeltagerRolle.UKJENT)
        }

        @Test
        fun `skal returnere grunnleggende info hvis person ikke har fagsak`() {
            // Arrange
            val (ident, aktør) = randomBarnFnr().let { it to randomAktør(it) }
            val navn = "Mock Mockesen"
            val personInfo = PersonInfo(fødselsdato = LocalDate.now().minusYears(30), kjønn = Kjønn.KVINNE, navn = navn)
            every { personidentService.hentAktørOrNullHvisIkkeAktivFødselsnummer(ident) } returns aktør
            every { familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(aktør) } returns null
            every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør) } returns personInfo
            every { fagsakRepository.finnFagsakerForAktør(aktør) } returns emptyList()
            every { personRepository.findByAktør(aktør) } returns emptyList()
            every { integrasjonClient.sjekkErEgenAnsattBulk(any()) } returns emptyMap()

            // Act
            val resultat = fagsakService.hentFagsakDeltager(ident)

            // Assert
            assertThat(resultat).hasSize(1)
            val deltager = resultat.single()
            assertThat(deltager.ident).isEqualTo(ident)
            assertThat(deltager.navn).isEqualTo(navn)
            assertThat(deltager.rolle).isEqualTo(FagsakDeltagerRolle.UKJENT)
            assertThat(deltager.fagsakId).isNull()
        }

        @Test
        fun `søk på barn med to foreldre skal returnere alle tre`() {
            // Arrange
            val (barnIdent, barnAktør) = randomBarnFnr().let { it to randomAktør(it) }
            val (morIdent, morAktør) = randomFnr().let { it to randomAktør(it) }
            val (farIdent, farAktør) = randomFnr().let { it to randomAktør(it) }
            val aktører = listOf(barnAktør, morAktør, farAktør)
            val barnInfo =
                PersonInfo(
                    fødselsdato = LocalDate.now().minusYears(30),
                    forelderBarnRelasjon =
                        setOf(
                            ForelderBarnRelasjon(
                                aktør = morAktør,
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.MOR,
                            ),
                            ForelderBarnRelasjon(
                                aktør = farAktør,
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.FAR,
                            ),
                        ),
                )
            every { personidentService.hentAktørOrNullHvisIkkeAktivFødselsnummer(barnIdent) } returns barnAktør
            every {
                familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(match { it in aktører })
            } returns null
            every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barnAktør) } returns barnInfo
            every {
                integrasjonClient.sjekkErEgenAnsattBulk(match { it.containsAll(listOf(barnIdent, morIdent, farIdent)) })
            } returns emptyMap()
            every { fagsakRepository.finnFagsakerForAktør(match { it in aktører }) } returns emptyList()

            // Act
            val resultat = fagsakService.hentFagsakDeltager(barnIdent)

            // Assert
            assertThat(resultat).hasSize(3)
            assertThat(resultat.single { it.ident == barnIdent }.rolle).isEqualTo(FagsakDeltagerRolle.BARN)
            assertThat(resultat.single { it.ident == morIdent }.rolle).isEqualTo(FagsakDeltagerRolle.FORELDER)
            assertThat(resultat.single { it.ident == farIdent }.rolle).isEqualTo(FagsakDeltagerRolle.FORELDER)
        }

        @Test
        fun `skal sette korrekt egen ansatt status basert på respons fra integrasjoner`() {
            // Arrange
            val (barnIdent, barnAktør) = randomBarnFnr().let { it to randomAktør(it) }
            val (morIdent, morAktør) = randomFnr().let { it to randomAktør(it) }
            val (farIdent, farAktør) = randomFnr().let { it to randomAktør(it) }
            val aktører = listOf(barnAktør, morAktør, farAktør)
            val barnInfo =
                PersonInfo(
                    fødselsdato = LocalDate.now().minusYears(6),
                    forelderBarnRelasjon =
                        setOf(
                            ForelderBarnRelasjon(
                                aktør = morAktør,
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.MOR,
                            ),
                            ForelderBarnRelasjon(
                                aktør = farAktør,
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.FAR,
                            ),
                        ),
                )
            every { personidentService.hentAktørOrNullHvisIkkeAktivFødselsnummer(barnIdent) } returns barnAktør
            every {
                familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(match { it in aktører })
            } returns null
            every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barnAktør) } returns barnInfo
            every { fagsakRepository.finnFagsakerForAktør(match { it in aktører }) } returns emptyList()
            every { personRepository.findByAktør(match { it in aktører }) } returns emptyList()
            every {
                integrasjonClient.sjekkErEgenAnsattBulk(match { it.containsAll(listOf(barnIdent, morIdent, farIdent)) })
            } returns mapOf(barnIdent to false, morIdent to true) // Far utelates

            // Act
            val resultat = fagsakService.hentFagsakDeltager(barnIdent)

            // Assert
            assertThat(resultat.single { it.ident == barnIdent }.erEgenAnsatt).isFalse()
            assertThat(resultat.single { it.ident == morIdent }.erEgenAnsatt).isTrue()
            assertThat(resultat.single { it.ident == farIdent }.erEgenAnsatt).isNull()
        }
    }
}
