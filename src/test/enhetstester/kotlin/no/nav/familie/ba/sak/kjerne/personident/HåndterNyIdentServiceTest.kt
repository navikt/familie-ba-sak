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
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.ekstern.restDomene.FagsakDeltagerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsakDeltager
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.PatchMergetIdentTask
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
    private val personopplysningerService: PersonopplysningerService = mockk()
    private val behandlingRepository: BehandlingRepository = mockk()

    @Nested
    inner class MergeIdentOgRekjørSenereTest {
        private val personIdentService: PersonidentService = mockk()
        private val håndterNyIdentService =
            HåndterNyIdentService(
                aktørIdRepository = aktørIdRepository,
                fagsakService = fagsakService,
                opprettTaskService = opprettTaskService,
                persongrunnlagService = persongrunnlagService,
                personopplysningerService = personopplysningerService,
                behandlingRepository = behandlingRepository,
                personIdentService = personIdentService,
            )

        val fnrGammel = randomFnr(LocalDate.of(2000, 1, 1))
        val aktørGammel = tilAktør(fnrGammel)

        val fnrNy = randomFnr(LocalDate.of(2000, 1, 1))
        val aktørNy = tilAktør(fnrGammel)

        val identInformasjonFraPdl =
            listOf(
                IdentInformasjon(aktørNy.aktørId, false, "AKTORID"),
                IdentInformasjon(fnrNy, false, "FOLKEREGISTERIDENT"),
                IdentInformasjon(aktørGammel.aktørId, true, "AKTORID"),
                IdentInformasjon(fnrGammel, true, "FOLKEREGISTERIDENT"),
            )

        @BeforeEach
        fun init() {
            every { aktørIdRepository.findByAktørIdOrNull(aktørGammel.aktørId) } returns aktørGammel
            every { personIdentService.hentIdenter(any(), true) } returns identInformasjonFraPdl
        }

        @Test
        fun `håndterNyIdent kaster Feil når det eksisterer flere fagsaker for identer`() {
            // arrange
            every { fagsakService.hentFagsakDeltager(any()) } returns
                listOf(
                    RestFagsakDeltager(rolle = FagsakDeltagerRolle.BARN, fagsakId = 1),
                    RestFagsakDeltager(rolle = FagsakDeltagerRolle.FORELDER, fagsakId = 2),
                )

            // act
            val feil =
                assertThrows<Feil> {
                    håndterNyIdentService.håndterNyIdent(PersonIdent(fnrNy))
                }

            // assert
            assertThat(feil.message).startsWith("Det eksisterer flere fagsaker på identer som skal merges")
        }

        @Test
        fun `håndterNyIdent kaster Feil når det ikke eksisterer en fagsak for identer`() {
            // arrange
            every { fagsakService.hentFagsakDeltager(any()) } returns emptyList()

            // act
            val feil =
                assertThrows<Feil> {
                    håndterNyIdentService.håndterNyIdent(PersonIdent(fnrNy))
                }

            // assert
            assertThat(feil.message).startsWith("Fant ingen fagsaker på identer som skal merges")
        }

        @Test
        fun `håndterNyIdent kaster Feil når det eksisterer en fagsak uten fagsakId for identer`() {
            // arrange
            every { fagsakService.hentFagsakDeltager(any()) } returns listOf(RestFagsakDeltager(rolle = FagsakDeltagerRolle.FORELDER))

            // act
            val feil =
                assertThrows<Feil> {
                    håndterNyIdentService.håndterNyIdent(PersonIdent(fnrNy))
                }

            // assert
            assertThat(feil.message).startsWith("Fant ingen fagsakId på fagsak for identer som skal merges")
        }

        @Test
        fun `håndterNyIdent kaster Feil når fødselsdato er endret for identer`() {
            // arrange
            every { fagsakService.hentFagsakDeltager(any()) } returns listOf(RestFagsakDeltager(rolle = FagsakDeltagerRolle.FORELDER, fagsakId = 0))

            every { personopplysningerService.hentPersoninfoEnkel(any()) } returns
                PersonInfo(
                    fødselsdato = LocalDate.of(2000, 2, 2),
                )
            val gammelBehandling = lagBehandling()
            every { behandlingRepository.finnSisteIverksatteBehandling(any()) } returns gammelBehandling

            every { persongrunnlagService.hentAktiv(any()) } returns
                PersonopplysningGrunnlag(
                    behandlingId = gammelBehandling.id,
                    personer =
                        mutableSetOf(
                            lagPerson(
                                aktør = aktørGammel,
                                fødselsdato = LocalDate.of(2000, 1, 1),
                            ),
                        ),
                )

            // act
            val feil =
                assertThrows<Feil> {
                    håndterNyIdentService.håndterNyIdent(PersonIdent(fnrNy))
                }

            // assert
            assertThat(feil.message).startsWith("Fødselsdato er forskjellig fra forrige behandling. Må patche ny ident manuelt. Se")
        }

        @Test
        fun `håndterNyIdent lager en PatchMergetIdent task`() {
            // arrange
            every { fagsakService.hentFagsakDeltager(any()) } returns listOf(RestFagsakDeltager(rolle = FagsakDeltagerRolle.FORELDER, fagsakId = 0))
            every { opprettTaskService.opprettTaskForÅPatcheMergetIdent(any()) } returns
                Task(
                    type = PatchMergetIdentTask.TASK_STEP_TYPE,
                    payload = "",
                )

            every { personopplysningerService.hentPersoninfoEnkel(any()) } returns
                PersonInfo(
                    fødselsdato = LocalDate.of(2000, 1, 1),
                )
            val gammelBehandling = lagBehandling()
            every { behandlingRepository.finnSisteIverksatteBehandling(any()) } returns gammelBehandling

            every { persongrunnlagService.hentAktiv(any()) } returns
                PersonopplysningGrunnlag(
                    behandlingId = gammelBehandling.id,
                    personer =
                        mutableSetOf(
                            lagPerson(
                                aktør = aktørGammel,
                                fødselsdato = LocalDate.of(2000, 1, 1),
                            ),
                        ),
                )

            // act
            val feil =
                assertThrows<RekjørSenereException> {
                    håndterNyIdentService.håndterNyIdent(PersonIdent(fnrNy))
                }

            // assert
            verify(exactly = 1) { opprettTaskService.opprettTaskForÅPatcheMergetIdent(any()) }
            assertThat(feil.årsak).startsWith("Mottok identhendelse som blir forsøkt patchet automatisk")
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
                personopplysningerService = personopplysningerService,
                behandlingRepository = behandlingRepository,
                personIdentService = personidentService,
            )

        @BeforeEach
        fun init() {
            clearMocks(answers = true, firstMock = aktørIdRepository)
            clearMocks(answers = true, firstMock = personidentRepository)
            clearMocks(answers = true, firstMock = taskRepositoryMock)

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

            assertEquals(2, aktør?.personidenter?.size)
            assertEquals(personIdentSomSkalLeggesTil, aktør!!.aktivFødselsnummer())
            assertTrue(
                aktør.personidenter
                    .first { !it.aktiv }
                    .gjelderTil!!
                    .isBefore(LocalDateTime.now()),
            )
            assertTrue(
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

            assertEquals(aktørIdSomFinnes.aktørId, aktør?.aktørId)
            assertEquals(1, aktør?.personidenter?.size)
            assertEquals(personIdentSomFinnes, aktør?.personidenter?.single()?.fødselsnummer)
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
            assertEquals(aktivAktørIdent2.aktørId, aktør?.aktørId)
            assertEquals(1, aktør?.personidenter?.size)
            assertEquals(aktivFnrIdent2, aktør?.personidenter?.single()?.fødselsnummer)
            verify(exactly = 0) { aktørIdRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
        }
    }
}
