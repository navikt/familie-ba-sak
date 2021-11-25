package no.nav.familie.ba.sak.kjerne.personident

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.data.repository.findByIdOrNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersonidentServiceTest() {

    private val personidentAktiv = randomFnr()
    private val personidentHistorisk = randomFnr()
    private val aktørId = randomAktørId()
    private val personopplysningerService: PersonopplysningerService = mockk(relaxed = true)

    @BeforeAll
    fun init() {

        every { personopplysningerService.hentIdenter(personidentAktiv, false) } answers {
            listOf(
                IdentInformasjon(aktørId.aktørId, false, "AKTORID"),
                IdentInformasjon(personidentAktiv, false, "FOLKEREGISTERIDENT"),
            )
        }
    }

    @Test
    fun `Test aktør id som som ikke er peristert fra før`() {
        val personidentRepository: PersonidentRepository = mockk()
        val aktørIdRepository: AktørIdRepository = mockk()

        val personidentService = PersonidentService(personidentRepository, aktørIdRepository, personopplysningerService)

        every { personidentRepository.findByIdOrNull(personidentAktiv) }.answers { null }
        every { aktørIdRepository.findByIdOrNull(aktørId.aktørId) }.answers { null }

        every { aktørIdRepository.save(any()) }.answers {
            Aktør(aktørId = aktørId.aktørId).also {
                it.personidenter.add(Personident(fødselsnummer = personidentAktiv, aktør = it))
            }
        }

        val response = personidentService.hentOgLagreAktørIder(listOf(personidentAktiv))

        Assertions.assertEquals(aktørId.aktørId, response.single().aktørId)

        val slotAktør = slot<Aktør>()
        verify(exactly = 1) { aktørIdRepository.save(capture(slotAktør)) }
        Assertions.assertEquals(aktørId.aktørId, slotAktør.captured.aktørId)
        Assertions.assertEquals(personidentAktiv, slotAktør.captured.personidenter.single().fødselsnummer)
    }

    @Test
    fun `Test aktør id som som er peristert fra før men ikke personident`() {
        val personidentRepository: PersonidentRepository = mockk()
        val aktørIdRepository: AktørIdRepository = mockk()

        val personidentService = PersonidentService(personidentRepository, aktørIdRepository, personopplysningerService)

        every { personidentRepository.findByIdOrNull(personidentAktiv) }.answers { null }
        every { aktørIdRepository.findByIdOrNull(aktørId.aktørId) }.answers {
            Aktør(
                aktørId.aktørId,
                mutableSetOf(Personident(fødselsnummer = personidentHistorisk, aktør = aktørId, aktiv = true))
            )
        }

        every { personidentRepository.save(any()) } answers {
            Personident(
                aktør = aktørId,
                fødselsnummer = personidentAktiv,
                aktiv = true
            )
        }

        val response = personidentService.hentOgLagreAktørIder(listOf(personidentAktiv))

        Assertions.assertEquals(aktørId.aktørId, response.single().aktørId)

        verify(exactly = 0) { aktørIdRepository.save(any()) }

        val slotPersonident = mutableListOf<Personident>()
        verify(exactly = 2) { personidentRepository.save(capture(slotPersonident)) }

        Assertions.assertEquals(false, slotPersonident.first().aktiv)
        Assertions.assertEquals(aktørId.aktørId, slotPersonident.first().aktør.aktørId)
        Assertions.assertEquals(personidentHistorisk, slotPersonident.first().fødselsnummer)
        Assertions.assertEquals(null, slotPersonident.first().gjelderTil)

        Assertions.assertEquals(true, slotPersonident.last().aktiv)
        Assertions.assertEquals(aktørId.aktørId, slotPersonident.last().aktør.aktørId)
        Assertions.assertEquals(personidentAktiv, slotPersonident.last().fødselsnummer)
        Assertions.assertEquals(null, slotPersonident.last().gjelderTil)
    }

    @Test
    fun `Test aktør id pg personident som som er peristert fra før`() {
        val personidentRepository: PersonidentRepository = mockk()
        val aktørIdRepository: AktørIdRepository = mockk()

        val personidentService = PersonidentService(personidentRepository, aktørIdRepository, personopplysningerService)

        every { personidentRepository.findByIdOrNull(personidentAktiv) }.answers {
            Personident(fødselsnummer = personidentAktiv, aktør = aktørId, aktiv = true)
        }

        val response = personidentService.hentOgLagreAktørIder(listOf(personidentAktiv))

        Assertions.assertEquals(aktørId.aktørId, response.single().aktørId)

        verify(exactly = 0) { aktørIdRepository.save(any()) }
        verify(exactly = 0) { personidentRepository.save(any()) }
    }
}
