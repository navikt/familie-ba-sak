package no.nav.familie.ba.sak.kjerne.personident

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

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

    private val personidentService =
        PersonidentService(
            personidentRepository = personidentRepository,
            aktørIdRepository = aktørIdRepository,
            pdlIdentRestClient = pdlIdentRestClient,
            taskRepository = taskRepositoryMock,
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
