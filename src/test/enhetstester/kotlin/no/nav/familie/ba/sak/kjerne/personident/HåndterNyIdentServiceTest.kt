package no.nav.familie.ba.sak.kjerne.personident

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.tilPersonEnkel
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.ekstern.restDomene.FagsakDeltagerRolle.FORELDER
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsakDeltager
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

internal class HåndterNyIdentServiceTest {
    private val aktørIdRepository: AktørIdRepository = mockk()
    private val fagsakService: FagsakService = mockk()
    private val opprettTaskService: OpprettTaskService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val pdlRestClient: PdlRestClient = mockk()

    @Nested
    inner class OpprettMergeIdentTaskTest {
        private val personIdentService: PersonidentService = mockk()
        private val håndterNyIdentService =
            HåndterNyIdentService(
                aktørIdRepository = aktørIdRepository,
                fagsakService = fagsakService,
                opprettTaskService = opprettTaskService,
                persongrunnlagService = persongrunnlagService,
                behandlinghentOgPersisterService = behandlingHentOgPersisterService,
                pdlRestClient = pdlRestClient,
                personIdentService = personIdentService,
            )

        val gammelFødselsdato = LocalDate.of(2000, 1, 1)
        val gammeltFnr = randomFnr(gammelFødselsdato)
        val gammelAktør = tilAktør(gammeltFnr)
        val gammelPerson = lagPerson(aktør = gammelAktør, fødselsdato = gammelFødselsdato)

        val nyFødselsdato = LocalDate.of(2000, 2, 2)
        val nyttFnr = randomFnr(nyFødselsdato)
        val nyAktør = tilAktør(nyttFnr)

        val gammelBehandling = lagBehandling()

        val identInformasjonFraPdl =
            listOf(
                IdentInformasjon(nyAktør.aktørId, false, "AKTORID"),
                IdentInformasjon(nyttFnr, false, "FOLKEREGISTERIDENT"),
                IdentInformasjon(gammelAktør.aktørId, true, "AKTORID"),
                IdentInformasjon(gammeltFnr, true, "FOLKEREGISTERIDENT"),
            )

        @BeforeEach
        fun init() {
            clearMocks(answers = true, firstMock = fagsakService)
            every { persongrunnlagService.hentSøkerOgBarnPåFagsak(any()) } returns setOf(gammelPerson.tilPersonEnkel())
            every { personIdentService.hentIdenter(any(), true) } returns identInformasjonFraPdl
            every { aktørIdRepository.findByAktørIdOrNull(nyAktør.aktørId) } returns null
            every { aktørIdRepository.findByAktørIdOrNull(gammelAktør.aktørId) } returns gammelAktør
            every { opprettTaskService.opprettTaskForÅPatcheMergetIdent(any()) } returns Task("", "")
            every { fagsakService.hentFagsakDeltager(any()) } returns listOf(RestFagsakDeltager(rolle = FORELDER, fagsakId = 0))
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns gammelBehandling
            every { persongrunnlagService.hentAktiv(any()) } returns
                PersonopplysningGrunnlag(
                    behandlingId = gammelBehandling.id,
                    personer = mutableSetOf(gammelPerson),
                )
        }

        @Test
        fun `håndterNyIdent dropper merging av identer når det ikke eksisterer en fagsak for identer`() {
            // arrange
            every { fagsakService.hentFagsakDeltager(any()) } returns emptyList()

            // act
            val aktør = håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))

            // assert
            verify(exactly = 0) { opprettTaskService.opprettTaskForÅPatcheMergetIdent(any()) }
            assertThat(aktør).isNull()
        }

        @Test
        fun `håndterNyIdent dropper merging av identer når det eksisterer en fagsak uten fagsakId for identer`() {
            // arrange
            every { fagsakService.hentFagsakDeltager(any()) } returns listOf(RestFagsakDeltager(rolle = FORELDER))

            // act
            val aktør = håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))

            // assert
            verify(exactly = 0) { opprettTaskService.opprettTaskForÅPatcheMergetIdent(any()) }
            assertThat(aktør).isNull()
        }

        @Test
        fun `håndterNyIdent kaster Feil når det eksisterer flere fagsaker for identer`() {
            // arrange
            every { fagsakService.hentFagsakDeltager(any()) } returns
                listOf(
                    RestFagsakDeltager(rolle = FORELDER, fagsakId = 1),
                    RestFagsakDeltager(rolle = FORELDER, fagsakId = 2),
                )

            // act & assert
            val feil =
                assertThrows<Feil> {
                    håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))
                }

            assertThat(feil.message).startsWith("Det eksisterer flere fagsaker på identer som skal merges")
        }

        @Test
        fun `håndterNyIdent kaster Feil når fødselsdato er endret for identer`() {
            // arrange
            every { pdlRestClient.hentPerson(any<String>(), any()) } returns PersonInfo(fødselsdato = nyFødselsdato)

            // act & assert
            val feil =
                assertThrows<Feil> {
                    håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))
                }

            assertThat(feil.message).startsWith("Fødselsdato er forskjellig fra forrige behandling. Må patche ny ident manuelt.")
        }

        // behandlinghentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId)

        @Test
        fun `håndterNyIdent lager en PatchMergetIdent task ved endret fødselsdato, hvis det ikke er en vedtatt behandling`() {
            // arrange
            every { pdlRestClient.hentPerson(any<String>(), any()) } returns PersonInfo(fødselsdato = nyFødselsdato)
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns null

            // act & assert
            val aktør = håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))

            assertThat(aktør).isNull()
            verify(exactly = 1) { opprettTaskService.opprettTaskForÅPatcheMergetIdent(any()) }
        }

        @Test
        fun `håndterNyIdent lager en PatchMergetIdent task ved endret fødselsdato, hvis aktør ikke er med i forrige vedtatte behandling`() {
            // arrange
            every { pdlRestClient.hentPerson(any<String>(), any()) } returns PersonInfo(fødselsdato = nyFødselsdato)
            every { persongrunnlagService.hentAktiv(any()) } returns PersonopplysningGrunnlag(behandlingId = gammelBehandling.id)

            // act & assert
            val aktør = håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))

            assertThat(aktør).isNull()
            verify(exactly = 1) { opprettTaskService.opprettTaskForÅPatcheMergetIdent(any()) }
        }

        @Test
        fun `håndterNyIdent lager en PatchMergetIdent task hvis fødselsdato er uendret`() {
            // arrange
            every { pdlRestClient.hentPerson(any<String>(), any()) } returns PersonInfo(fødselsdato = gammelFødselsdato)

            // act & assert
            val aktør = håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))

            assertThat(aktør).isNull()
            verify(exactly = 1) { opprettTaskService.opprettTaskForÅPatcheMergetIdent(any()) }
        }
    }

    @Nested
    inner class HåndterNyIdentTest {
        private val pdlIdentRestClient: PdlIdentRestClient = mockk(relaxed = true)
        private val personidentRepository: PersonidentRepository = mockk()
        private val taskRepositoryMock = mockk<TaskRepositoryWrapper>(relaxed = true)

        private val personidentAktiv = randomFnr()
        private val aktørIdAktiv = tilAktør(personidentAktiv)
        private val personidentHistorisk = randomFnr()

        private val personIdentSlot = slot<Personident>()
        private val aktørSlot = slot<Aktør>()

        private val personidentService =
            PersonidentService(
                personidentRepository = personidentRepository,
                aktørIdRepository = aktørIdRepository,
                pdlIdentRestClient = pdlIdentRestClient,
                taskRepository = taskRepositoryMock,
            )

        private val håndterNyIdentService =
            HåndterNyIdentService(
                aktørIdRepository = aktørIdRepository,
                fagsakService = fagsakService,
                opprettTaskService = opprettTaskService,
                persongrunnlagService = persongrunnlagService,
                behandlinghentOgPersisterService = behandlingHentOgPersisterService,
                pdlRestClient = pdlRestClient,
                personIdentService = personidentService,
            )

        @BeforeEach
        fun init() {
            clearMocks(answers = true, firstMock = aktørIdRepository)
            clearMocks(answers = true, firstMock = personidentRepository)
            clearMocks(answers = true, firstMock = taskRepositoryMock)

            every { fagsakService.hentFagsakDeltager(any()) } returns listOf(RestFagsakDeltager(rolle = FORELDER, fagsakId = 0))

            every { personidentRepository.saveAndFlush(capture(personIdentSlot)) } answers {
                personIdentSlot.captured
            }

            every { aktørIdRepository.saveAndFlush(capture(aktørSlot)) } answers {
                aktørSlot.captured
            }

            every { pdlIdentRestClient.hentIdenter(personidentAktiv, false) } answers {
                listOf(
                    IdentInformasjon(aktørIdAktiv.aktørId, false, "AKTORID"),
                    IdentInformasjon(personidentAktiv, false, "FOLKEREGISTERIDENT"),
                )
            }
            every { pdlIdentRestClient.hentIdenter(personidentHistorisk, false) } answers {
                listOf(
                    IdentInformasjon(aktørIdAktiv.aktørId, false, "AKTORID"),
                    IdentInformasjon(personidentAktiv, false, "FOLKEREGISTERIDENT"),
                )
            }
        }

        @Test
        fun `Skal legge til ny ident på aktør som finnes i systemet`() {
            val personIdentSomFinnes = randomFnr()
            val personIdentSomSkalLeggesTil = randomFnr()
            val historiskIdent = randomFnr()
            val historiskAktør = tilAktør(historiskIdent)
            val aktørIdSomFinnes = tilAktør(personIdentSomFinnes)

            every { pdlIdentRestClient.hentIdenter(personIdentSomFinnes, false) } answers {
                listOf(
                    IdentInformasjon(aktørIdSomFinnes.aktørId, false, "AKTORID"),
                    IdentInformasjon(personIdentSomFinnes, false, "FOLKEREGISTERIDENT"),
                )
            }

            every { pdlIdentRestClient.hentIdenter(personIdentSomSkalLeggesTil, true) } answers {
                listOf(
                    IdentInformasjon(aktørIdSomFinnes.aktørId, false, "AKTORID"),
                    IdentInformasjon(personIdentSomSkalLeggesTil, false, "FOLKEREGISTERIDENT"),
                    IdentInformasjon(historiskAktør.aktørId, true, "AKTORID"),
                    IdentInformasjon(historiskIdent, true, "FOLKEREGISTERIDENT"),
                )
            }

            every { personidentRepository.findByFødselsnummerOrNull(personIdentSomFinnes) }.answers {
                Personident(fødselsnummer = randomFnr(), aktør = aktørIdSomFinnes, aktiv = true)
            }

            every { aktørIdRepository.findByAktørIdOrNull(aktørIdSomFinnes.aktørId) }.answers {
                aktørIdSomFinnes
            }

            every { aktørIdRepository.findByAktørIdOrNull(historiskAktør.aktørId) }.answers {
                null
            }
            every { personidentRepository.findByFødselsnummerOrNull(personIdentSomSkalLeggesTil) }.answers {
                null
            }

            val aktør = håndterNyIdentService.håndterNyIdent(nyIdent = PersonIdent(personIdentSomSkalLeggesTil))

            assertThat(aktør?.personidenter?.size).isEqualTo(2)
            assertThat(personIdentSomSkalLeggesTil).isEqualTo(aktør!!.aktivFødselsnummer())
            assertThat(
                aktør.personidenter
                    .first { !it.aktiv }
                    .gjelderTil!!
                    .isBefore(LocalDateTime.now()),
            )
            assertThat(
                aktør.personidenter
                    .first { !it.aktiv }
                    .gjelderTil!!
                    .isBefore(LocalDateTime.now()),
            )
            verify(exactly = 2) { aktørIdRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
        }

        @Test
        fun `Skal ikke legge til ny ident på aktør som allerede har denne identen registert i systemet`() {
            val personIdentSomFinnes = randomFnr()
            val aktørIdSomFinnes = tilAktør(personIdentSomFinnes)

            every { pdlIdentRestClient.hentIdenter(personIdentSomFinnes, true) } answers {
                listOf(
                    IdentInformasjon(aktørIdSomFinnes.aktørId, false, "AKTORID"),
                    IdentInformasjon(personIdentSomFinnes, false, "FOLKEREGISTERIDENT"),
                )
            }

            every { aktørIdRepository.findByAktørIdOrNull(aktørIdSomFinnes.aktørId) }.answers { aktørIdSomFinnes }
            every { personidentRepository.findByFødselsnummerOrNull(personIdentSomFinnes) }.answers {
                tilAktør(
                    personIdentSomFinnes,
                ).personidenter.first()
            }

            val aktør = håndterNyIdentService.håndterNyIdent(nyIdent = PersonIdent(personIdentSomFinnes))

            assertThat(aktørIdSomFinnes.aktørId).isEqualTo(aktør?.aktørId)
            assertThat(aktør?.personidenter?.size).isEqualTo(1)
            assertThat(personIdentSomFinnes).isEqualTo(aktør?.personidenter?.single()?.fødselsnummer)
            verify(exactly = 0) { aktørIdRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
        }

        @Test
        fun `Hendelse på en ident hvor gammel ident1 er merget med ny ident2 skal ikke kaste feil når bruker har alt bruker ny ident`() {
            val fnrIdent1 = randomFnr()
            val aktørIdent1 = tilAktør(fnrIdent1)
            val aktivFnrIdent2 = randomFnr()
            val aktivAktørIdent2 = tilAktør(aktivFnrIdent2)

            secureLogger.info("gammelIdent=$fnrIdent1,${aktørIdent1.aktørId}   nyIdent=$aktivFnrIdent2,${aktivAktørIdent2.aktørId}")

            every { pdlIdentRestClient.hentIdenter(aktivFnrIdent2, true) } answers {
                listOf(
                    IdentInformasjon(aktivAktørIdent2.aktørId, false, "AKTORID"),
                    IdentInformasjon(aktivFnrIdent2, false, "FOLKEREGISTERIDENT"),
                    IdentInformasjon(aktørIdent1.aktørId, true, "AKTORID"),
                    IdentInformasjon(fnrIdent1, true, "FOLKEREGISTERIDENT"),
                )
            }

            every { aktørIdRepository.findByAktørIdOrNull(aktivAktørIdent2.aktørId) }.answers {
                aktivAktørIdent2
            }
            every { aktørIdRepository.findByAktørIdOrNull(aktørIdent1.aktørId) }.answers {
                null
            }

            val aktør = håndterNyIdentService.håndterNyIdent(nyIdent = PersonIdent(aktivFnrIdent2))
            assertThat(aktivAktørIdent2.aktørId).isEqualTo(aktør?.aktørId)
            assertThat(aktør?.personidenter?.size).isEqualTo(1)
            assertThat(aktivFnrIdent2).isEqualTo(aktør?.personidenter?.single()?.fødselsnummer)
            verify(exactly = 0) { aktørIdRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
        }
    }
}
