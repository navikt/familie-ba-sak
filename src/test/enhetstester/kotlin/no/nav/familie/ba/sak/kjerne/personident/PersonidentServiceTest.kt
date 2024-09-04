package no.nav.familie.ba.sak.kjerne.personident

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersonidentServiceTest {
    private val personidentAleredePersistert = randomFnr()
    private val aktørIdAleredePersistert = tilAktør(personidentAleredePersistert)
    private val personidentAktiv = randomFnr()
    private val aktørIdAktiv = tilAktør(personidentAktiv)
    private val personidentHistorisk = randomFnr()

    private val pdlIdentRestClient: PdlIdentRestClient = mockk(relaxed = true)
    private val personidentRepository: PersonidentRepository = mockk()
    private val aktørIdRepository: AktørIdRepository = mockk()
    private val personIdentSlot = slot<Personident>()
    private val aktørSlot = slot<Aktør>()
    private val taskRepositoryMock = mockk<TaskRepositoryWrapper>(relaxed = true)
    private val fagsakService: FagsakService = mockk()
    private val opprettTaskService: OpprettTaskService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val personopplysningerService: PersonopplysningerService = mockk()
    private val behandlingRepository: BehandlingRepository = mockk()

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

    @BeforeAll
    fun init() {
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

    @BeforeEach
    fun byggRepositoryMocks() {
        clearMocks(answers = true, firstMock = aktørIdRepository)
        clearMocks(answers = true, firstMock = personidentRepository)
        clearMocks(answers = true, firstMock = taskRepositoryMock)

        every { personidentRepository.saveAndFlush(capture(personIdentSlot)) } answers {
            personIdentSlot.captured
        }

        every { aktørIdRepository.saveAndFlush(capture(aktørSlot)) } answers {
            aktørSlot.captured
        }
    }

    @Nested
    inner class HåndterNyIdentTest {
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
                Personident(fødselsnummer = personidentAktiv, aktør = aktørIdSomFinnes, aktiv = true)
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

    @Nested
    inner class HentAktørTest {
        @Test
        fun `Test aktør id som som er persistert fra før`() {
            every { personidentRepository.findByFødselsnummerOrNull(aktørIdAleredePersistert.aktørId) } answers { null }
            every { aktørIdRepository.findByAktørIdOrNull(aktørIdAleredePersistert.aktørId) } answers { aktørIdAleredePersistert }

            val aktør = personidentService.hentAktør(aktørIdAleredePersistert.aktørId)

            verify(exactly = 0) { aktørIdRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
            assertEquals(aktørIdAleredePersistert.aktørId, aktør.aktørId)
            assertEquals(personidentAleredePersistert, aktør.personidenter.single().fødselsnummer)
        }

        @Test
        fun `Test personident som er persistert fra før`() {
            every { personidentRepository.findByFødselsnummerOrNull(personidentAleredePersistert) } answers { aktørIdAleredePersistert.personidenter.first() }

            val aktør = personidentService.hentAktør(personidentAleredePersistert)

            verify(exactly = 0) { aktørIdRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
            assertEquals(aktørIdAleredePersistert.aktørId, aktør.aktørId)
            assertEquals(personidentAleredePersistert, aktør.personidenter.single().fødselsnummer)
        }

        @Test
        fun `Test aktiv personident som er persistert fra før`() {
            every { personidentRepository.findByFødselsnummerOrNull(personidentAktiv) } answers { null }
            every { aktørIdRepository.findByAktørIdOrNull(personidentAktiv) } answers { null }

            every { personidentRepository.findByFødselsnummerOrNull(personidentAktiv) } answers {
                Personident(
                    personidentAktiv,
                    aktørIdAktiv,
                )
            }

            val aktør = personidentService.hentOgLagreAktør(personidentAktiv, false)

            verify(exactly = 0) { aktørIdRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
            assertEquals(aktørIdAktiv.aktørId, aktør.aktørId)
            assertEquals(personidentAktiv, aktør.personidenter.single().fødselsnummer)
        }

        @Test
        fun `Test aktør id som som er persistert fra før men aktiv personident som ikke er persistert`() {
            every { personidentRepository.findByFødselsnummerOrNull(personidentHistorisk) } answers { null }
            every { aktørIdRepository.findByAktørIdOrNull(personidentHistorisk) } answers { null }
            every { personidentRepository.findByFødselsnummerOrNull(personidentAktiv) } answers { null }

            every { aktørIdRepository.findByAktørIdOrNull(aktørIdAktiv.aktørId) } answers { aktørIdAktiv }

            val aktør = personidentService.hentOgLagreAktør(personidentHistorisk, true)

            verify(exactly = 0) { aktørIdRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
            assertEquals(aktørIdAktiv.aktørId, aktør.aktørId)
            assertEquals(personidentAktiv, aktør.personidenter.single().fødselsnummer)
        }

        @Test
        fun `Test hverken aktør id eller aktiv personident som er persistert fra før`() {
            every { personidentRepository.findByFødselsnummerOrNull(personidentAktiv) } answers { null }
            every { aktørIdRepository.findByAktørIdOrNull(personidentAktiv) } answers { null }
            every { personidentRepository.findByFødselsnummerOrNull(personidentAktiv) } answers { null }

            every { aktørIdRepository.findByAktørIdOrNull(aktørIdAktiv.aktørId) } answers { null }

            val aktør = personidentService.hentOgLagreAktør(personidentAktiv, true)

            verify(exactly = 1) { aktørIdRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
            assertEquals(aktørIdAktiv.aktørId, aktør.aktørId)
            assertEquals(personidentAktiv, aktør.personidenter.single().fødselsnummer)
        }

        @Test
        fun `Test hverken aktør id eller aktiv personident som er persistert fra før men som ikke skal persisteres`() {
            every { personidentRepository.findByFødselsnummerOrNull(personidentAktiv) } answers { null }
            every { aktørIdRepository.findByAktørIdOrNull(personidentAktiv) } answers { null }
            every { personidentRepository.findByFødselsnummerOrNull(personidentAktiv) } answers { null }

            every { aktørIdRepository.findByAktørIdOrNull(aktørIdAktiv.aktørId) } answers { null }

            val aktør = personidentService.hentOgLagreAktør(personidentAktiv, false)

            verify(exactly = 0) { aktørIdRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
            assertEquals(aktørIdAktiv.aktørId, aktør.aktørId)
            assertEquals(personidentAktiv, aktør.personidenter.single().fødselsnummer)
        }
    }

    @Nested
    inner class OpprettTaskForIdentHendelseTest {
        @Test
        fun `Skal opprette task for håndtering av ny ident ved ny fnr men samme aktør`() {
            val personIdentSomFinnes = randomFnr()
            val personIdentSomSkalLeggesTil = randomFnr()
            val aktørIdSomFinnes = tilAktør(personIdentSomFinnes)
            aktørIdSomFinnes.personidenter.add(
                Personident(
                    fødselsnummer = personIdentSomFinnes,
                    aktør = aktørIdSomFinnes,
                ),
            )

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
                    IdentInformasjon(personIdentSomFinnes, true, "FOLKEREGISTERIDENT"),
                )
            }
            every { aktørIdRepository.findByAktørIdOrNull(aktørIdSomFinnes.aktørId) }.answers {
                aktørIdSomFinnes
            }

            val slot = slot<Task>()
            every { taskRepositoryMock.save(capture(slot)) } answers { slot.captured }

            val ident = PersonIdent(personIdentSomSkalLeggesTil)
            personidentService.opprettTaskForIdentHendelse(ident)

            verify(exactly = 1) { taskRepositoryMock.save(any()) }
            assertEquals(ident, objectMapper.readValue(slot.captured.payload, PersonIdent::class.java))
        }

        @Test
        fun `Skal opprette task for håndtering av ny ident ved ny fnr og ny aktør`() {
            val personIdentSomFinnes = randomFnr()
            val personIdentSomSkalLeggesTil = randomFnr()
            val aktørIdGammel = tilAktør(personIdentSomFinnes)
            val aktørIdNy = tilAktør(personIdentSomSkalLeggesTil)
            aktørIdGammel.personidenter.add(
                Personident(
                    fødselsnummer = personIdentSomFinnes,
                    aktør = aktørIdGammel,
                ),
            )

            every { pdlIdentRestClient.hentIdenter(personIdentSomFinnes, false) } answers {
                listOf(
                    IdentInformasjon(aktørIdGammel.aktørId, false, "AKTORID"),
                    IdentInformasjon(personIdentSomFinnes, false, "FOLKEREGISTERIDENT"),
                )
            }

            every { pdlIdentRestClient.hentIdenter(personIdentSomSkalLeggesTil, true) } answers {
                listOf(
                    IdentInformasjon(aktørIdGammel.aktørId, false, "AKTORID"),
                    IdentInformasjon(personIdentSomSkalLeggesTil, false, "FOLKEREGISTERIDENT"),
                    IdentInformasjon(aktørIdNy.aktørId, true, "AKTORID"),
                    IdentInformasjon(personIdentSomFinnes, true, "FOLKEREGISTERIDENT"),
                )
            }
            every { aktørIdRepository.findByAktørIdOrNull(aktørIdGammel.aktørId) }.answers {
                aktørIdGammel
            }

            every { aktørIdRepository.findByAktørIdOrNull(aktørIdNy.aktørId) } returns null

            val slot = slot<Task>()
            every { taskRepositoryMock.save(capture(slot)) } answers { slot.captured }

            val ident = PersonIdent(personIdentSomSkalLeggesTil)
            personidentService.opprettTaskForIdentHendelse(ident)

            verify(exactly = 1) { taskRepositoryMock.save(any()) }
            assertEquals(ident, objectMapper.readValue(slot.captured.payload, PersonIdent::class.java))
        }

        @Test
        fun `Skal ikke opprette task for håndtering av ny ident når ident ikke er tilknyttet noen aktører i systemet`() {
            val personIdentSomFinnes = randomFnr()
            val personIdentSomSkalLeggesTil = randomFnr()
            val aktørIdIkkeIBaSak = tilAktør(personIdentSomSkalLeggesTil)
            val aktørIdSomFinnes = tilAktør(personIdentSomFinnes)
            aktørIdSomFinnes.personidenter.add(
                Personident(
                    fødselsnummer = personIdentSomFinnes,
                    aktør = aktørIdSomFinnes,
                ),
            )

            every { pdlIdentRestClient.hentIdenter(personIdentSomFinnes, false) } answers {
                listOf(
                    IdentInformasjon(aktørIdSomFinnes.aktørId, false, "AKTORID"),
                    IdentInformasjon(personIdentSomFinnes, false, "FOLKEREGISTERIDENT"),
                )
            }

            every { pdlIdentRestClient.hentIdenter(personIdentSomSkalLeggesTil, false) } answers {
                listOf(
                    IdentInformasjon(aktørIdIkkeIBaSak.aktørId, false, "AKTORID"),
                    IdentInformasjon(personIdentSomSkalLeggesTil, false, "FOLKEREGISTERIDENT"),
                )
            }
            every { aktørIdRepository.findByAktørIdOrNull(aktørIdIkkeIBaSak.aktørId) }.answers {
                aktørIdIkkeIBaSak
            }

            val slot = slot<Task>()
            every { taskRepositoryMock.save(capture(slot)) } answers { slot.captured }

            val ident = PersonIdent(personIdentSomSkalLeggesTil)
            personidentService.opprettTaskForIdentHendelse(ident)

            verify(exactly = 0) { taskRepositoryMock.save(any()) }
        }
    }
}
