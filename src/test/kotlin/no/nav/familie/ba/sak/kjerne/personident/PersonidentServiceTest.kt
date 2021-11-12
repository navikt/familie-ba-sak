package no.nav.familie.ba.sak.kjerne.personident

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class PersonidentServiceTest() {

    private val personidentAktiv = randomFnr()
    private val personidentInaktiv = randomFnr()

    // private val personidentNpidInaktiv = randomFnr()
    private val aktørId = randomAktørId()

    private val personopplysningerService: PersonopplysningerService = mockk(relaxed = true)
    private val personidentRepository: PersonidentRepository = mockk()
    private val personidentService = PersonidentService(personidentRepository, personopplysningerService)

    @BeforeAll
    fun init() {
        every { personopplysningerService.hentIdenter(personidentAktiv, true) } answers {
            listOf(
                IdentInformasjon(personidentInaktiv, true, "FOLKEREGISTERIDENT"),
                // IdentInformasjon("222", true, "NPID"),
                IdentInformasjon(aktørId.id, false, "AKTORID"),
                IdentInformasjon(personidentAktiv, false, "FOLKEREGISTERIDENT"),
            )
        }

        every { personopplysningerService.hentIdenter(personidentInaktiv, true) } answers {
            listOf(
                IdentInformasjon(personidentInaktiv, true, "FOLKEREGISTERIDENT"),
                // IdentInformasjon("222", true, "NPID"),
                IdentInformasjon(aktørId.id, false, "AKTORID"),
                IdentInformasjon(personidentAktiv, false, "FOLKEREGISTERIDENT"),
            )
        }
    }

    @Test
    fun `vedtaksperioder sorteres korrekt til brev`() {

        personidentService.hentOgLagreIdenterForSøkerOgBarn(listOf("444"))
    }
}
