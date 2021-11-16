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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersonidentServiceTest() {

    private val personidentAktiv = randomFnr()
    private val personidentInaktiv = randomFnr()
    private val aktørId = randomAktørId()
    private val personopplysningerService: PersonopplysningerService = mockk(relaxed = true)

    @BeforeAll
    fun init() {

        every { personopplysningerService.hentIdenter(personidentAktiv, false) } answers {
            listOf(
                IdentInformasjon(aktørId.id, false, "AKTORID"),
                IdentInformasjon(personidentAktiv, false, "FOLKEREGISTERIDENT"),
            )
        }

        every { personopplysningerService.hentIdenter(personidentInaktiv, false) } answers {
            listOf(
                IdentInformasjon(aktørId.id, false, "AKTORID"),
                IdentInformasjon(personidentAktiv, false, "FOLKEREGISTERIDENT"),
            )
        }
    }

    @Test
    fun `Test aktiv ident som ikke er peristert fra før`() {
        val personidentRepository: PersonidentRepository = mockk()
        val personidentService = PersonidentService(personidentRepository, personopplysningerService)

        every { personidentRepository.hentAktivIdentForAktørId(aktørId.id) } answers {
            null
        }
        every { personidentRepository.save(any()) } answers {
            Personident(
                aktørId = aktørId,
                fødselsnummer = personidentAktiv,
                aktiv = true
            )
        }

        val response = personidentService.hentOgLagreAktiveIdenterMedAktørId(listOf(personidentAktiv))

        Assertions.assertEquals(personidentAktiv, response.single())

        val slot = slot<Personident>()

        verify(exactly = 1) { personidentRepository.save(capture(slot)) }
        Assertions.assertEquals(true, slot.captured.aktiv)
        Assertions.assertEquals(aktørId, slot.captured.aktørId)
        Assertions.assertEquals(personidentAktiv, slot.captured.fødselsnummer)
        Assertions.assertEquals(null, slot.captured.gjelderTil)
    }

    @Test
    fun `Test aktiv ident som er peristert fra før`() {
        val personidentRepository: PersonidentRepository = mockk()
        val personidentService = PersonidentService(personidentRepository, personopplysningerService)

        every { personidentRepository.hentAktivIdentForAktørId(aktørId.id) } answers {
            Personident(
                aktørId = aktørId,
                fødselsnummer = personidentAktiv,
                aktiv = true
            )
        }

        val response = personidentService.hentOgLagreAktiveIdenterMedAktørId(listOf(personidentAktiv))

        Assertions.assertEquals(personidentAktiv, response.single())

        verify(exactly = 0) { personidentRepository.save(any()) }
    }

    @Test
    fun `Test aktiv ident som har en tideligere ident peristert`() {
        val personidentRepository: PersonidentRepository = mockk()
        val personidentService = PersonidentService(personidentRepository, personopplysningerService)

        every { personidentRepository.hentAktivIdentForAktørId(aktørId.id) } answers {
            Personident(
                aktørId = aktørId,
                fødselsnummer = personidentInaktiv,
                aktiv = true
            )
        }
        every { personidentRepository.save(any()) } answers {
            Personident(
                aktørId = aktørId,
                fødselsnummer = personidentAktiv,
                aktiv = true
            )
        }

        val response = personidentService.hentOgLagreAktiveIdenterMedAktørId(listOf(personidentAktiv))

        Assertions.assertEquals(personidentAktiv, response.single())

        val slot = mutableListOf<Personident>()

        verify(exactly = 2) { personidentRepository.save(capture(slot)) }
        Assertions.assertEquals(true, slot[0].aktiv)
        Assertions.assertEquals(aktørId, slot[0].aktørId)
        Assertions.assertEquals(personidentAktiv, slot[0].fødselsnummer)
        Assertions.assertEquals(null, slot[0].gjelderTil)
        Assertions.assertEquals(false, slot[1].aktiv)
        Assertions.assertEquals(aktørId, slot[1].aktørId)
        Assertions.assertEquals(personidentInaktiv, slot[1].fødselsnummer)
        Assertions.assertTrue(slot[1].gjelderTil != null)
    }
}
