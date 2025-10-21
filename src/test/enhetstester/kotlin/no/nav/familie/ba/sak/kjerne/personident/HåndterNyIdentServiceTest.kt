package no.nav.familie.ba.sak.kjerne.personident

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.datagenerator.tilPersonEnkel
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonInfoQuery
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent as PersonopplysningerPersonIdent

internal class HåndterNyIdentServiceTest {
    private val aktørIdRepository: AktørIdRepository = mockk()
    private val fagsakService: FagsakService = mockk()
    private val opprettTaskService: OpprettTaskService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val pdlRestKlient: PdlRestKlient = mockk()

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
                pdlRestKlient = pdlRestKlient,
                personIdentService = personIdentService,
            )

        val gammelFødselsdato = LocalDate.now().minusYears(1)
        val gammeltFnr = randomFnr(gammelFødselsdato)
        val gammelAktør = lagAktør(gammeltFnr)
        val gammelPerson = lagPerson(aktør = gammelAktør, fødselsdato = gammelFødselsdato)

        val nyFødselsdato = LocalDate.now().minusYears(1).minusMonths(3)
        val nyttFnr = randomFnr(nyFødselsdato)
        val nyAktør = lagAktør(nyttFnr)

        var gammelBehandling = lagBehandling()

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
            every { fagsakService.hentFagsakerPåPerson(any()) } returns listOf(Fagsak(id = 0, aktør = randomAktør()))
            every { fagsakService.hentAlleFagsakerForAktør(any()) } returns emptyList()
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
            every { fagsakService.hentFagsakerPåPerson(any()) } returns emptyList()

            // act
            val aktør = håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))

            // assert
            verify(exactly = 0) { opprettTaskService.opprettTaskForÅPatcheMergetIdent(any()) }
            assertThat(aktør).isNull()
        }

        @Test
        fun `håndterNyIdent kaster ikke Feil når det eksisterer flere fagsaker for identer`() {
            // arrange
            every { fagsakService.hentFagsakerPåPerson(any()) } returns
                listOf(
                    Fagsak(id = 1, aktør = gammelAktør),
                    Fagsak(id = 2, aktør = gammelAktør),
                )

            // act & assert
            val aktør = håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))

            assertThat(aktør).isNull()
            verify(exactly = 1) { opprettTaskService.opprettTaskForÅPatcheMergetIdent(any()) }
        }

        @Test
        fun `håndterNyIdent kaster Feil når fødselsdato er endret for identer`() {
            // arrange
            every { pdlRestKlient.hentPerson(any<String>(), any()) } returns PersonInfo(fødselsdato = nyFødselsdato)

            // act & assert
            val feil =
                assertThrows<Feil> {
                    håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))
                }

            assertThat(feil.message).isEqualTo(
                """Fødselsdato er forskjellig fra forrige behandling. 
   Ny fødselsdato $nyFødselsdato, forrige fødselsdato $gammelFødselsdato
   Fagsak: 1 

Du MÅ først patche fnr med PatchMergetIdentTask og etterpå sende saken til en fagressurs.
Info om gammel og nytt fnr finner man i loggmelding med level WARN i securelogs.
Se https://github.com/navikt/familie/blob/main/doc/ba-sak/manuellt-patche-akt%C3%B8r-sak.md#manuell-patching-av-akt%C3%B8r-for-en-behandling for mer info.""",
            )
        }

        @Test
        fun `håndterNyIdent kaster ikke Feil når fødselsdato er endret innenfor samme måned`() {
            // arrange
            every { pdlRestKlient.hentPerson(any<String>(), any()) } returns PersonInfo(fødselsdato = gammelFødselsdato.sisteDagIMåned())

            // act & assert
            assertDoesNotThrow {
                håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))
            }
        }

        @Test
        fun `håndterNyIdent kaster ikke Feil når fødselsdato er endret for søker`() {
            // arrange
            every { pdlRestKlient.hentPerson(any<String>(), any()) } returns PersonInfo(fødselsdato = nyFødselsdato)
            every { fagsakService.hentFagsakerPåPerson(any()) } returns
                listOf(
                    Fagsak(id = 1, aktør = gammelAktør),
                )

            // act & assert
            assertDoesNotThrow {
                håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))
            }
        }

        @Test
        fun `håndterNyIdent kaster Feil når fødselsdato er endret for søker i en enslig-mindreårig-sak`() {
            // arrange
            every { pdlRestKlient.hentPerson(any<String>(), any()) } returns PersonInfo(fødselsdato = nyFødselsdato)
            every { fagsakService.hentFagsakerPåPerson(any()) } returns
                listOf(
                    Fagsak(id = 1, aktør = gammelAktør, type = FagsakType.BARN_ENSLIG_MINDREÅRIG),
                )

            // act & assert
            val feil =
                assertThrows<Feil> {
                    håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))
                }

            assertThat(feil.message).startsWith("Fødselsdato er forskjellig fra forrige behandling.")
        }

        @Test
        fun `håndterNyIdent lager en PatchMergetIdent task ved endret fødselsdato, hvis det ikke er en vedtatt behandling`() {
            // arrange
            every { pdlRestKlient.hentPerson(any<String>(), any()) } returns PersonInfo(fødselsdato = nyFødselsdato)
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns null

            // act & assert
            val aktør = håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))

            assertThat(aktør).isNull()
            verify(exactly = 1) { opprettTaskService.opprettTaskForÅPatcheMergetIdent(any()) }
        }

        @Test
        fun `håndterNyIdent lager en PatchMergetIdent task ved endret fødselsdato, hvis aktør ikke er med i forrige vedtatte behandling`() {
            // arrange
            every { pdlRestKlient.hentPerson(any<String>(), any()) } returns PersonInfo(fødselsdato = nyFødselsdato)
            every { persongrunnlagService.hentAktiv(any()) } returns PersonopplysningGrunnlag(behandlingId = gammelBehandling.id)

            // act & assert
            val aktør = håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))

            assertThat(aktør).isNull()
            verify(exactly = 1) { opprettTaskService.opprettTaskForÅPatcheMergetIdent(any()) }
        }

        @Test
        fun `håndterNyIdent lager en PatchMergetIdent task hvis fødselsdato er uendret`() {
            // arrange
            every { pdlRestKlient.hentPerson(any<String>(), any()) } returns PersonInfo(fødselsdato = gammelFødselsdato)

            // act & assert
            val aktør = håndterNyIdentService.håndterNyIdent(PersonIdent(nyttFnr))

            assertThat(aktør).isNull()
            verify(exactly = 1) { opprettTaskService.opprettTaskForÅPatcheMergetIdent(any()) }
        }
    }

    @Nested
    inner class HåndterNyIdentTest {
        private val pdlIdentRestKlient: PdlIdentRestKlient = mockk(relaxed = true)
        private val personidentRepository: PersonidentRepository = mockk()
        private val taskRepositoryMock = mockk<TaskRepositoryWrapper>(relaxed = true)

        private val personidentAktiv = randomFnr()
        private val aktørIdAktiv = lagAktør(personidentAktiv)
        private val personidentHistorisk = randomFnr()

        private val personIdentSlot = slot<Personident>()
        private val aktørSlot = slot<Aktør>()

        private val personidentService =
            PersonidentService(
                personidentRepository = personidentRepository,
                aktørIdRepository = aktørIdRepository,
                pdlIdentRestKlient = pdlIdentRestKlient,
                taskRepository = taskRepositoryMock,
            )

        private val håndterNyIdentService =
            HåndterNyIdentService(
                aktørIdRepository = aktørIdRepository,
                fagsakService = fagsakService,
                opprettTaskService = opprettTaskService,
                persongrunnlagService = persongrunnlagService,
                behandlinghentOgPersisterService = behandlingHentOgPersisterService,
                pdlRestKlient = pdlRestKlient,
                personIdentService = personidentService,
            )

        @BeforeEach
        fun init() {
            clearMocks(answers = true, firstMock = aktørIdRepository)
            clearMocks(answers = true, firstMock = personidentRepository)
            clearMocks(answers = true, firstMock = taskRepositoryMock)

            every { fagsakService.hentFagsakerPåPerson(any()) } returns listOf(Fagsak(id = 0, aktør = aktørIdAktiv))
            every { fagsakService.hentAlleFagsakerForAktør(any()) } returns emptyList()

            every { personidentRepository.saveAndFlush(capture(personIdentSlot)) } answers {
                personIdentSlot.captured
            }

            every { aktørIdRepository.saveAndFlush(capture(aktørSlot)) } answers {
                aktørSlot.captured
            }

            every { pdlIdentRestKlient.hentIdenter(personidentAktiv, false) } answers {
                listOf(
                    IdentInformasjon(aktørIdAktiv.aktørId, false, "AKTORID"),
                    IdentInformasjon(personidentAktiv, false, "FOLKEREGISTERIDENT"),
                )
            }
            every { pdlIdentRestKlient.hentIdenter(personidentHistorisk, false) } answers {
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
            val historiskAktør = lagAktør(historiskIdent)
            val aktørIdSomFinnes = lagAktør(personIdentSomFinnes)
            val fødselsdato = LocalDate.now().minusYears(4)

            every {
                pdlRestKlient.hentPerson(personIdentSomSkalLeggesTil, PersonInfoQuery.ENKEL)
            } returns PersonInfo(fødselsdato = fødselsdato)

            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns lagBehandling()

            every { persongrunnlagService.hentAktiv(any()) } returns
                PersonopplysningGrunnlag(
                    behandlingId = 1L,
                    personer =
                        mutableSetOf(
                            lagPerson(
                                personIdent = PersonopplysningerPersonIdent(personIdentSomFinnes),
                                aktør = aktørIdSomFinnes,
                                fødselsdato = fødselsdato,
                            ),
                        ),
                )

            every { pdlIdentRestKlient.hentIdenter(personIdentSomFinnes, false) } answers {
                listOf(
                    IdentInformasjon(aktørIdSomFinnes.aktørId, false, "AKTORID"),
                    IdentInformasjon(personIdentSomFinnes, false, "FOLKEREGISTERIDENT"),
                )
            }

            every { pdlIdentRestKlient.hentIdenter(personIdentSomSkalLeggesTil, true) } answers {
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
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
        }

        @Test
        fun `Skal legge til ny ident på aktør som finnes i systemet, og som kun har fagsak og ingen behandling`() {
            val personIdentSomFinnes = randomFnr()
            val personIdentSomSkalLeggesTil = randomFnr()
            val historiskIdent = randomFnr()
            val historiskAktør = lagAktør(historiskIdent)
            val aktørIdSomFinnes = lagAktør(personIdentSomFinnes)
            val fødselsdato = LocalDate.now().minusYears(4)

            every {
                pdlRestKlient.hentPerson(personIdentSomSkalLeggesTil, PersonInfoQuery.ENKEL)
            } returns PersonInfo(fødselsdato = fødselsdato)

            every { fagsakService.hentFagsakerPåPerson(any()) } returns emptyList()
            every { fagsakService.hentAlleFagsakerForAktør(any()) } returns listOf(Fagsak(id = 0, aktør = aktørIdAktiv))

            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns null

            every { persongrunnlagService.hentAktiv(any()) } returns
                PersonopplysningGrunnlag(
                    behandlingId = 1L,
                    personer =
                        mutableSetOf(
                            lagPerson(
                                personIdent = PersonopplysningerPersonIdent(personIdentSomFinnes),
                                aktør = aktørIdSomFinnes,
                                fødselsdato = fødselsdato,
                            ),
                        ),
                )

            every { pdlIdentRestKlient.hentIdenter(personIdentSomFinnes, false) } answers {
                listOf(
                    IdentInformasjon(aktørIdSomFinnes.aktørId, false, "AKTORID"),
                    IdentInformasjon(personIdentSomFinnes, false, "FOLKEREGISTERIDENT"),
                )
            }

            every { pdlIdentRestKlient.hentIdenter(personIdentSomSkalLeggesTil, true) } answers {
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
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
        }

        @Test
        fun `Skal kaste feil når vi prøver legge til ny ident på aktør som finnes i systemet og som har endret fødselsdato`() {
            val personIdentSomFinnes = randomFnr()
            val personIdentSomSkalLeggesTil = randomFnr()
            val historiskIdent = randomFnr()
            val historiskAktør = lagAktør(historiskIdent)
            val aktørIdSomFinnes = lagAktør(personIdentSomFinnes)
            val fødselsdato = LocalDate.now().minusYears(4)

            every {
                pdlRestKlient.hentPerson(personIdentSomSkalLeggesTil, PersonInfoQuery.ENKEL)
            } returns PersonInfo(fødselsdato = fødselsdato.minusMonths(2))

            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns lagBehandling()

            every { persongrunnlagService.hentAktiv(any()) } returns
                PersonopplysningGrunnlag(
                    behandlingId = 1L,
                    personer =
                        mutableSetOf(
                            lagPerson(
                                personIdent = PersonopplysningerPersonIdent(personIdentSomFinnes),
                                aktør = aktørIdSomFinnes,
                                fødselsdato = fødselsdato,
                            ),
                        ),
                )

            every { pdlIdentRestKlient.hentIdenter(personIdentSomSkalLeggesTil, true) } answers {
                listOf(
                    IdentInformasjon(aktørIdSomFinnes.aktørId, false, "AKTORID"),
                    IdentInformasjon(personIdentSomSkalLeggesTil, false, "FOLKEREGISTERIDENT"),
                    IdentInformasjon(historiskAktør.aktørId, true, "AKTORID"),
                    IdentInformasjon(historiskIdent, true, "FOLKEREGISTERIDENT"),
                )
            }

            every { aktørIdRepository.findByAktørIdOrNull(aktørIdSomFinnes.aktørId) }.answers {
                aktørIdSomFinnes
            }

            every { aktørIdRepository.findByAktørIdOrNull(historiskAktør.aktørId) }.answers {
                null
            }

            val exception =
                assertThrows<Feil> {
                    håndterNyIdentService.håndterNyIdent(nyIdent = PersonIdent(personIdentSomSkalLeggesTil))
                }

            assertThat(exception.message).startsWith("Fødselsdato er forskjellig fra forrige behandling")
        }

        @Test
        fun `Skal ikke legge til ny ident på aktør som allerede har denne identen registert i systemet`() {
            val personIdentSomFinnes = randomFnr()
            val aktørIdSomFinnes = lagAktør(personIdentSomFinnes)

            every { pdlIdentRestKlient.hentIdenter(personIdentSomFinnes, true) } answers {
                listOf(
                    IdentInformasjon(aktørIdSomFinnes.aktørId, false, "AKTORID"),
                    IdentInformasjon(personIdentSomFinnes, false, "FOLKEREGISTERIDENT"),
                )
            }

            every { aktørIdRepository.findByAktørIdOrNull(aktørIdSomFinnes.aktørId) }.answers { aktørIdSomFinnes }
            every { personidentRepository.findByFødselsnummerOrNull(personIdentSomFinnes) }.answers {
                lagAktør(
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
            val aktørIdent1 = lagAktør(fnrIdent1)
            val aktivFnrIdent2 = randomFnr()
            val aktivAktørIdent2 = lagAktør(aktivFnrIdent2)

            secureLogger.info("gammelIdent=$fnrIdent1,${aktørIdent1.aktørId}   nyIdent=$aktivFnrIdent2,${aktivAktørIdent2.aktørId}")

            every { pdlIdentRestKlient.hentIdenter(aktivFnrIdent2, true) } answers {
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
