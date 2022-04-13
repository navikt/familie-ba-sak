package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.YearMonth

class KompetanseRepositoryTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val kompetanseRepository: KompetanseRepository
) : AbstractSpringIntegrationTest() {

    @Test
    fun `Skal lagre flere kompetanser med gjenbruk av flere aktører`() {
        val søker = aktørIdRepository.save(randomAktørId())
        val barn1 = aktørIdRepository.save(randomAktørId())
        val barn2 = aktørIdRepository.save(randomAktørId())

        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandling(fagsak))

        val kompetanse = kompetanseRepository.save(
            lagKompetanse(
                barnAktører = setOf(barn1, barn2),
            ).also { it.behandlingId = behandling.id }
        )

        val kompetanse2 = kompetanseRepository.save(
            lagKompetanse(
                barnAktører = setOf(barn1, barn2)
            ).also { it.behandlingId = behandling.id }
        )

        print(kompetanse)
        assertEquals(kompetanse.barnAktører, kompetanse2.barnAktører)
    }

    @Test
    fun `Skal lagre skjema-feltene`() {
        val søker = aktørIdRepository.save(randomAktørId())
        val barn1 = aktørIdRepository.save(randomAktørId())

        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandling(fagsak))

        val kompetanse = kompetanseRepository.save(
            lagKompetanse(
                behandlingId = behandling.id,
                barnAktører = setOf(barn1),
                fom = YearMonth.of(2020, 1),
                tom = YearMonth.of(2021, 12),
                søkersAktivitet = SøkersAktivitet.ARBEIDER_I_NORGE,
                annenForeldersAktivitet = AnnenForeldersAktivitet.MOTTAR_PENSJON,
                annenForeldersAktivitetsland = "pl",
                barnetsBostedsland = "sl"
            )
        )

        val hentedeKompetanser = kompetanseRepository.findByBehandlingId(behandlingId = behandling.id)

        assertEquals(1, hentedeKompetanser.size)
        assertEquals(kompetanse, hentedeKompetanser.first())
    }
}
