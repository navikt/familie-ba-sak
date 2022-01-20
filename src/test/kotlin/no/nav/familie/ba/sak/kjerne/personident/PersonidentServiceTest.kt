package no.nav.familie.ba.sak.kjerne.personident

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
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
    private val personidentService = PersonidentService(
        personidentRepository, aktørIdRepository, pdlIdentRestClient, mockk()
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

        every { personidentRepository.saveAndFlush(capture(personIdentSlot)) } answers {
            personIdentSlot.captured
        }

        every { aktørIdRepository.saveAndFlush(capture(aktørSlot)) } answers {
            aktørSlot.captured
        }
    }

    @Test
    fun `Skal legge til ny ident på aktør som finnes i systemet`() {
        val personIdentSomFinnes = randomFnr()
        val personIdentSomSkalLeggesTil = randomFnr()
        val aktørIdSomFinnes = tilAktør(personIdentSomFinnes)
        aktørIdSomFinnes.personidenter.add(
            Personident(
                fødselsnummer = personIdentSomFinnes,
                aktør = aktørIdSomFinnes
            )
        )

        every { pdlIdentRestClient.hentIdenter(personIdentSomFinnes, false) } answers {
            listOf(
                IdentInformasjon(aktørIdSomFinnes.aktørId, false, "AKTORID"),
                IdentInformasjon(personIdentSomFinnes, false, "FOLKEREGISTERIDENT"),
            )
        }

        every { pdlIdentRestClient.hentIdenter(personIdentSomSkalLeggesTil, false) } answers {
            listOf(
                IdentInformasjon(aktørIdSomFinnes.aktørId, false, "AKTORID"),
                IdentInformasjon(personIdentSomSkalLeggesTil, false, "FOLKEREGISTERIDENT"),
            )
        }

        every { personidentRepository.findByFødselsnummerOrNull(personIdentSomFinnes) }.answers {
            Personident(fødselsnummer = personidentAktiv, aktør = aktørIdSomFinnes, aktiv = true)
        }

        every { aktørIdRepository.findByAktørIdOrNull(aktørIdSomFinnes.aktørId) }.answers {
            aktørIdSomFinnes
        }
        every { personidentRepository.findByFødselsnummerOrNull(personIdentSomSkalLeggesTil) }.answers {
            null
        }

        val personidentService = PersonidentService(
            personidentRepository, aktørIdRepository, pdlIdentRestClient, mockk()
        )

        val aktør = personidentService.håndterNyIdent(nyIdent = PersonIdent(personIdentSomSkalLeggesTil))

        assertEquals(2, aktør?.personidenter?.size)
        assertEquals(personIdentSomSkalLeggesTil, aktør!!.aktivFødselsnummer())
        assertTrue(aktør.personidenter.first { !it.aktiv }.gjelderTil!!.isBefore(LocalDateTime.now()))
        verify(exactly = 2) { aktørIdRepository.saveAndFlush(any()) }
        verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
    }

    @Test
    fun `Skal opprette task for håndtering av ny ident`() {
        val taskRepositoryMock = mockk<TaskRepositoryWrapper>(relaxed = true)
        val personidentService = PersonidentService(
            personidentRepository, aktørIdRepository, pdlIdentRestClient, taskRepositoryMock
        )

        val slot = slot<Task>()
        every { taskRepositoryMock.save(capture(slot)) } answers { slot.captured }

        val ident = PersonIdent("123")
        personidentService.opprettTaskForIdentHendelse(ident)

        verify(exactly = 1) { taskRepositoryMock.save(any()) }
        assertEquals(ident, objectMapper.readValue(slot.captured.payload, PersonIdent::class.java))
    }

    @Test
    fun `Skal ikke legge til ny ident på aktør som ikke finnes i systemet`() {
        val personIdentSomSkalLeggesTil = randomFnr()
        val aktørIdSomIkkeFinnes = randomAktørId()

        every { pdlIdentRestClient.hentIdenter(personIdentSomSkalLeggesTil, false) } answers {
            listOf(
                IdentInformasjon(aktørIdSomIkkeFinnes.aktørId, false, "AKTORID"),
                IdentInformasjon(personIdentSomSkalLeggesTil, false, "FOLKEREGISTERIDENT"),
            )
        }

        every { personidentRepository.findByFødselsnummerOrNull(personIdentSomSkalLeggesTil) }.answers { null }

        every { aktørIdRepository.findByAktørIdOrNull(aktørIdSomIkkeFinnes.aktørId) }.answers { null }

        val personidentService = PersonidentService(
            personidentRepository, aktørIdRepository, pdlIdentRestClient, mockk()
        )

        val aktør = personidentService.håndterNyIdent(nyIdent = PersonIdent(personIdentSomSkalLeggesTil))

        assertNull(aktør)
        verify(exactly = 0) { aktørIdRepository.save(any()) }
        verify(exactly = 0) { personidentRepository.save(any()) }
    }

    @Test
    fun `Skal ikke legge til ny ident på aktør som allerede har denne identen registert i systemet`() {
        val personIdentSomFinnes = randomFnr()
        val aktørIdSomFinnes = tilAktør(personIdentSomFinnes)
        aktørIdSomFinnes.personidenter.add(
            Personident(
                fødselsnummer = personIdentSomFinnes,
                aktør = aktørIdSomFinnes
            )
        )

        every { pdlIdentRestClient.hentIdenter(personIdentSomFinnes, false) } answers {
            listOf(
                IdentInformasjon(aktørIdSomFinnes.aktørId, false, "AKTORID"),
                IdentInformasjon(personIdentSomFinnes, false, "FOLKEREGISTERIDENT"),
            )
        }

        every { aktørIdRepository.findByAktørIdOrNull(aktørIdSomFinnes.aktørId) }.answers { aktørIdSomFinnes }
        every { personidentRepository.findByFødselsnummerOrNull(personIdentSomFinnes) }.answers {
            tilAktør(
                personIdentSomFinnes
            ).personidenter.first()
        }

        val personidentService = PersonidentService(
            personidentRepository, aktørIdRepository, pdlIdentRestClient, mockk()
        )

        val aktør = personidentService.håndterNyIdent(nyIdent = PersonIdent(personIdentSomFinnes))

        assertEquals(aktørIdSomFinnes.aktørId, aktør?.aktørId)
        assertEquals(1, aktør?.personidenter?.size)
        assertEquals(personIdentSomFinnes, aktør?.personidenter?.single()?.fødselsnummer)
        verify(exactly = 0) { aktørIdRepository.saveAndFlush(any()) }
        verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
    }

    @Test
    fun `Test aktør id som som er persistert fra før`() {
        every { personidentRepository.findByFødselsnummerOrNull(aktørIdAleredePersistert.aktørId) } answers { null }
        every { aktørIdRepository.findByAktørIdOrNull(aktørIdAleredePersistert.aktørId) } answers { aktørIdAleredePersistert }

        val aktør = personidentService.hentOgLagreAktør(aktørIdAleredePersistert.aktørId)

        verify(exactly = 0) { aktørIdRepository.saveAndFlush(any()) }
        verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
        assertEquals(aktørIdAleredePersistert.aktørId, aktør.aktørId)
        assertEquals(personidentAleredePersistert, aktør.personidenter.single().fødselsnummer)
    }

    @Test
    fun `Test personident som er persistert fra før`() {
        every { personidentRepository.findByFødselsnummerOrNull(personidentAleredePersistert) } answers { aktørIdAleredePersistert.personidenter.first() }

        val aktør = personidentService.hentOgLagreAktør(personidentAleredePersistert)

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
                aktørIdAktiv
            )
        }

        val aktør = personidentService.hentOgLagreAktør(personidentAktiv)

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

        verify(exactly = 2) { aktørIdRepository.saveAndFlush(any()) }
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

    @Test
    fun `Ident som allerede er persistert på annen aktør id er en merget id og skal derfor feile`() {
        val mergedPersonident = randomFnr()
        val mergetTilAktørId = tilAktør(mergedPersonident)

        every { pdlIdentRestClient.hentIdenter(mergedPersonident, false) } answers {
            listOf(
                IdentInformasjon(mergetTilAktørId.aktørId, false, "AKTORID"),
                IdentInformasjon(mergedPersonident, false, "FOLKEREGISTERIDENT"),
            )
        }

        every { aktørIdRepository.findByAktørIdOrNull(mergetTilAktørId.aktørId) }.answers { null }
        every { personidentRepository.findByFødselsnummerOrNull(mergedPersonident) }.answers {
            tilAktør(
                mergedPersonident,
                "11"
            ).personidenter.first()
        }

        val personidentService = PersonidentService(
            personidentRepository, aktørIdRepository, pdlIdentRestClient, mockk()
        )

        assertThrows<Feil> { personidentService.håndterNyIdent(nyIdent = PersonIdent(mergedPersonident)) }
    }
}
