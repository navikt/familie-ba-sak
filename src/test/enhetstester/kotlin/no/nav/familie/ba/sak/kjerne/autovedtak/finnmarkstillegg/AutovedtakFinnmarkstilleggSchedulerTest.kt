package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.FeatureToggle.KJØRING_AUTOVEDTAK_FINNMARKSTILLEGG
import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.domene.FinnmarkstilleggKjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.domene.FinnmarkstilleggKjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.event.ContextClosedEvent
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class AutovedtakFinnmarkstilleggSchedulerTest {
    private val leaderClientService = mockk<LeaderClientService>()
    private val fagsakRepository = mockk<FagsakRepository>()
    private val unleashService = mockk<UnleashNextMedContextService>()
    private val opprettTaskService = mockk<OpprettTaskService>()
    private val finnmarkstilleggKjøringRepository = mockk<FinnmarkstilleggKjøringRepository>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val pdlRestClient = mockk<SystemOnlyPdlRestClient>()

    private val autovedtakFinnmarkstilleggScheduler =
        AutovedtakFinnmarkstilleggScheduler(
            leaderClientService = leaderClientService,
            fagsakRepository = fagsakRepository,
            unleashService = unleashService,
            opprettTaskService = opprettTaskService,
            finnmarkstilleggKjøringRepository = finnmarkstilleggKjøringRepository,
            persongrunnlagService = persongrunnlagService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            pdlRestClient = pdlRestClient,
        )

    private val søker1 = lagPerson()
    private val søker2 = lagPerson()

    private val behandling1 = lagBehandling(id = 1, fagsak = lagFagsak(id = 1))
    private val behandling2 = lagBehandling(id = 2, fagsak = lagFagsak(id = 2))

    private val persongrunnlag1 =
        lagTestPersonopplysningGrunnlag(behandlingId = behandling1.id, søker1)

    private val persongrunnlag2 =
        lagTestPersonopplysningGrunnlag(behandlingId = behandling2.id, søker2)

    @BeforeEach
    fun setup() {
        every { unleashService.isEnabled(KJØRING_AUTOVEDTAK_FINNMARKSTILLEGG) } returns true
        every { leaderClientService.isLeader() } returns true

        every { finnmarkstilleggKjøringRepository.findByFagsakId(any()) } returns null
        every { finnmarkstilleggKjøringRepository.save(any()) } returns mockk()

        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(behandling1.fagsak.id) } returns behandling1
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(behandling2.fagsak.id) } returns behandling2

        every { persongrunnlagService.hentAktiv(behandling1.id) } returns persongrunnlag1
        every { persongrunnlagService.hentAktiv(behandling2.id) } returns persongrunnlag2

        every { opprettTaskService.opprettAutovedtakFinnmarkstilleggTask(any()) } returns mockk()
    }

    @Test
    fun `triggAutovedtakFinnmarkstillegg skal ikke kjøre når feature toggle er disabled`() {
        // Arrange
        every { unleashService.isEnabled(KJØRING_AUTOVEDTAK_FINNMARKSTILLEGG) } returns false

        // Act
        autovedtakFinnmarkstilleggScheduler.triggAutovedtakFinnmarkstillegg()

        // Assert
        verify(exactly = 0) { leaderClientService.isLeader() }
        verify(exactly = 0) { fagsakRepository.finnLøpendeFagsakerForFinnmarkstilleggKjøring(any()) }
    }

    @Test
    fun `opprettTaskerForAutovedtakFinnmarkstillegg skal ikke kjøre når isShuttingDown er true`() {
        // Arrange
        autovedtakFinnmarkstilleggScheduler.onApplicationEvent(mockk<ContextClosedEvent>())

        // Act
        autovedtakFinnmarkstilleggScheduler.triggAutovedtakFinnmarkstillegg()

        // Assert
        verify(exactly = 0) { leaderClientService.isLeader() }
        verify(exactly = 0) { fagsakRepository.finnLøpendeFagsakerForFinnmarkstilleggKjøring(any()) }
    }

    @Test
    fun `opprettTaskerForAutovedtakFinnmarkstillegg skal ikke kjøre når ikke leader`() {
        // Arrange
        every { leaderClientService.isLeader() } returns false

        // Act
        autovedtakFinnmarkstilleggScheduler.triggAutovedtakFinnmarkstillegg()

        // Assert
        verify(exactly = 1) { leaderClientService.isLeader() }
        verify(exactly = 0) { fagsakRepository.finnLøpendeFagsakerForFinnmarkstilleggKjøring(any()) }
    }

    @Test
    fun `triggAutovedtakFinnmarkstillegg skal kjøre når feature toggle er enabled`() {
        // Arrange
        every { fagsakRepository.finnLøpendeFagsakerForFinnmarkstilleggKjøring(any()) } returns
            PageImpl(emptyList(), Pageable.ofSize(1000), 0)

        // Act
        autovedtakFinnmarkstilleggScheduler.triggAutovedtakFinnmarkstillegg()

        // Assert
        verify(exactly = 1) { leaderClientService.isLeader() }
        verify(exactly = 1) { fagsakRepository.finnLøpendeFagsakerForFinnmarkstilleggKjøring(any()) }
    }

    @Test
    fun `opprettTaskerForAutovedtakFinnmarkstillegg skal opprette tasks for fagsaker med personer i Finnmark eller Nord-Troms`() {
        // Arrange
        every { fagsakRepository.finnLøpendeFagsakerForFinnmarkstilleggKjøring(any()) } returns
            PageImpl(listOf(behandling1.fagsak.id, behandling2.fagsak.id), Pageable.ofSize(1000), 2)

        every { pdlRestClient.hentBostedsadresseOgDeltBostedForPersoner(listOf(søker1.aktør.aktivFødselsnummer(), søker2.aktør.aktivFødselsnummer())) } returns
            mapOf(
                søker1.aktør.aktivFødselsnummer() to
                    mockk(relaxed = true) {
                        every { nåværendeBostedEllerDeltBostedErIFinnmarkEllerNordTroms() } returns true
                    },
                søker2.aktør.aktivFødselsnummer() to
                    mockk(relaxed = true) {
                        every { nåværendeBostedEllerDeltBostedErIFinnmarkEllerNordTroms() } returns false
                    },
            )

        // Act
        autovedtakFinnmarkstilleggScheduler.triggAutovedtakFinnmarkstillegg()

        // Assert
        verify(exactly = 1) { opprettTaskService.opprettAutovedtakFinnmarkstilleggTask(behandling1.fagsak.id) }
        verify(exactly = 0) { opprettTaskService.opprettAutovedtakFinnmarkstilleggTask(behandling2.fagsak.id) }
        verify(exactly = 2) { finnmarkstilleggKjøringRepository.save(FinnmarkstilleggKjøring(fagsakId = behandling1.fagsak.id)) }
    }

    @Test
    fun `opprettTaskerForAutovedtakFinnmarkstillegg skal ikke opprette duplikat kjøring når den allerede eksisterer`() {
        // Arrange
        every { fagsakRepository.finnLøpendeFagsakerForFinnmarkstilleggKjøring(any()) } returns
            PageImpl(listOf(behandling1.fagsak.id), Pageable.ofSize(1000), 1)

        every { pdlRestClient.hentBostedsadresseOgDeltBostedForPersoner(listOf(søker1.aktør.aktivFødselsnummer())) } returns
            mapOf(
                søker1.aktør.aktivFødselsnummer() to
                    mockk(relaxed = true) {
                        every { nåværendeBostedEllerDeltBostedErIFinnmarkEllerNordTroms() } returns true
                    },
            )

        every { finnmarkstilleggKjøringRepository.findByFagsakId(behandling1.fagsak.id) } returns
            FinnmarkstilleggKjøring(fagsakId = behandling1.fagsak.id)

        // Act
        autovedtakFinnmarkstilleggScheduler.triggAutovedtakFinnmarkstillegg()

        // Assert
        verify(exactly = 0) { opprettTaskService.opprettAutovedtakFinnmarkstilleggTask(behandling1.fagsak.id) }
        verify(exactly = 0) { finnmarkstilleggKjøringRepository.save(any()) }
    }

    @Test
    fun `opprettTaskerForAutovedtakFinnmarkstillegg skal håndtere fagsaker uten iverksatt behandling`() {
        // Arrange
        every { fagsakRepository.finnLøpendeFagsakerForFinnmarkstilleggKjøring(any()) } returns
            PageImpl(listOf(behandling1.fagsak.id), Pageable.ofSize(1000), 1)

        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(behandling1.fagsak.id) } returns null

        // Act
        autovedtakFinnmarkstilleggScheduler.triggAutovedtakFinnmarkstillegg()

        // Assert
        verify(exactly = 0) { persongrunnlagService.hentAktiv(any()) }
        verify(exactly = 0) { pdlRestClient.hentBostedsadresseOgDeltBostedForPersoner(any()) }
        verify(exactly = 0) { opprettTaskService.opprettAutovedtakFinnmarkstilleggTask(any()) }
    }

    @Test
    fun `opprettTaskerForAutovedtakFinnmarkstillegg skal håndtere behandling uten persongrunnlag`() {
        // Arrange
        every { fagsakRepository.finnLøpendeFagsakerForFinnmarkstilleggKjøring(any()) } returns
            PageImpl(listOf(behandling1.fagsak.id), Pageable.ofSize(1000), 1)

        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(behandling1.fagsak.id) } returns behandling1
        every { persongrunnlagService.hentAktiv(behandling1.id) } returns null

        // Act
        autovedtakFinnmarkstilleggScheduler.triggAutovedtakFinnmarkstillegg()

        // Assert
        verify(exactly = 0) { pdlRestClient.hentBostedsadresseOgDeltBostedForPersoner(any()) }
        verify(exactly = 0) { opprettTaskService.opprettAutovedtakFinnmarkstilleggTask(any()) }
    }

    @Test
    fun `opprettTaskerForAutovedtakFinnmarkstillegg skal sende spørringer til PDL på maks 1000 personer`() {
        // Arrange
        val fagsakIder = (0L until 1000).toList()

        every { fagsakRepository.finnLøpendeFagsakerForFinnmarkstilleggKjøring(any()) } returns
            PageImpl(fagsakIder, Pageable.ofSize(1000), fagsakIder.size.toLong())

        fagsakIder.forEach {
            val behandling = lagBehandling(id = it, fagsak = lagFagsak(id = it))
            val personer = List(5) { lagPerson() }.toTypedArray()
            val persongrunnlag = lagTestPersonopplysningGrunnlag(behandlingId = behandling.id, personer = personer)
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(it) } returns behandling
            every { persongrunnlagService.hentAktiv(it) } returns persongrunnlag
        }

        every { pdlRestClient.hentBostedsadresseOgDeltBostedForPersoner(any()) } answers {
            assertThat(firstArg<List<String>>()).hasSize(1000)
            emptyMap()
        }

        // Act
        autovedtakFinnmarkstilleggScheduler.triggAutovedtakFinnmarkstillegg()

        // Assert
        verify(exactly = 5) { pdlRestClient.hentBostedsadresseOgDeltBostedForPersoner(any()) }
    }
}
