package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class KompetanseRepositoryTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val kompetanseRepository: KompetanseRepository
) : AbstractSpringIntegrationTest() {

    @Test
    fun `Skal lagre ned kompetanse med flere aktører`() {
        val barn1 = aktørIdRepository.save(randomAktørId())
        val barn2 = aktørIdRepository.save(randomAktørId())

        val kompetanse = kompetanseRepository.save(
            lagKompetanse(
                barnAktører = setOf(barn1, barn2)
            )
        )

        print(kompetanse)
    }
}