package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.lagPersonopplysningGrunnlagUtenId
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PersonopplysningGrunnlagRepositoryTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
) : AbstractSpringIntegrationTest() {
    @Nested
    inner class HentAktivForBehandlinger {
        @Test
        fun `skal hente grunnlag per behandling`() {
            // Arrange
            val aktør1 = aktørIdRepository.save(randomAktør())
            val aktør2 = aktørIdRepository.save(randomAktør())

            val fagsak1 = fagsakRepository.save(lagFagsakUtenId(aktør = aktør1))
            val fagsak2 = fagsakRepository.save(lagFagsakUtenId(aktør = aktør2))

            val behandling1 = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak1))
            val behandling2 = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak2))
            val behandlinger = setOf(behandling1.id, behandling2.id)

            val grunnlag1 = personopplysningGrunnlagRepository.save(lagPersonopplysningGrunnlagUtenId(behandlingId = behandling1.id))
            val grunnlag2 = personopplysningGrunnlagRepository.save(lagPersonopplysningGrunnlagUtenId(behandlingId = behandling2.id))

            // Act
            val grunnlag = personopplysningGrunnlagRepository.hentAktivForBehandlinger(behandlinger)

            // Assert
            assertThat(grunnlag).anySatisfy { assertThat(it.id).isEqualTo(grunnlag1.id) }
            assertThat(grunnlag).anySatisfy { assertThat(it.id).isEqualTo(grunnlag2.id) }
        }
    }
}
