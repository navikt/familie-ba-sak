package no.nav.familie.ba.sak.kjerne.personident

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.kjerne.aktørid.AktørId
import no.nav.familie.ba.sak.kjerne.aktørid.AktørIdRepository
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
            AktørId(aktørId = aktørId.aktørId).also {
                it.personidenter.add(Personident(fødselsnummer = personidentAktiv, aktørId = it))
            }
        }

        val response = personidentService.hentOgLagreAktørIder(listOf(personidentAktiv))

        Assertions.assertEquals(personidentAktiv, response.single())

        val slotAktørId = slot<AktørId>()
        verify(exactly = 1) { aktørIdRepository.save(capture(slotAktørId)) }
        Assertions.assertEquals(aktørId.aktørId, slotAktørId.captured.aktørId)
        Assertions.assertEquals(personidentAktiv, slotAktørId.captured.personidenter.single().fødselsnummer)
    }

    @Test
    fun `Test aktør id som som er peristert fra før men ikke personident`() {
        val personidentRepository: PersonidentRepository = mockk()
        val aktørIdRepository: AktørIdRepository = mockk()

        val personidentService = PersonidentService(personidentRepository, aktørIdRepository, personopplysningerService)

        every { personidentRepository.findByIdOrNull(personidentAktiv) }.answers { null }
        every { aktørIdRepository.findByIdOrNull(aktørId.aktørId) }.answers {
            AktørId(
                aktørId.aktørId,
                mutableSetOf(Personident(fødselsnummer = personidentHistorisk, aktørId = aktørId, aktiv = true))
            )
        }

        every { personidentRepository.save(any()) } answers {
            Personident(
                aktørId = aktørId,
                fødselsnummer = personidentAktiv,
                aktiv = true
            )
        }

        val response = personidentService.hentOgLagreAktørIder(listOf(personidentAktiv))

        Assertions.assertEquals(personidentAktiv, response.single())

        verify(exactly = 0) { aktørIdRepository.save(any()) }

        val slotPersonident = mutableListOf<Personident>()
        verify(exactly = 2) { personidentRepository.save(capture(slotPersonident)) }

        Assertions.assertEquals(false, slotPersonident.first().aktiv)
        Assertions.assertEquals(aktørId.aktørId, slotPersonident.first().aktørId.aktørId)
        Assertions.assertEquals(personidentHistorisk, slotPersonident.first().fødselsnummer)
        Assertions.assertEquals(null, slotPersonident.first().gjelderTil)

        Assertions.assertEquals(true, slotPersonident.last().aktiv)
        Assertions.assertEquals(aktørId.aktørId, slotPersonident.last().aktørId.aktørId)
        Assertions.assertEquals(personidentAktiv, slotPersonident.last().fødselsnummer)
        Assertions.assertEquals(null, slotPersonident.last().gjelderTil)
    }

    @Test
    fun `Test aktør id pg personident som som er peristert fra før`() {
        val personidentRepository: PersonidentRepository = mockk()
        val aktørIdRepository: AktørIdRepository = mockk()

        val personidentService = PersonidentService(personidentRepository, aktørIdRepository, personopplysningerService)

        every { personidentRepository.findByIdOrNull(personidentAktiv) }.answers {
            Personident(fødselsnummer = personidentAktiv, aktørId = aktørId, aktiv = true)
        }

        val response = personidentService.hentOgLagreAktørIder(listOf(personidentAktiv))

        Assertions.assertEquals(personidentAktiv, response.single())

        verify(exactly = 0) { aktørIdRepository.save(any()) }
        verify(exactly = 0) { personidentRepository.save(any()) }
    }
}
