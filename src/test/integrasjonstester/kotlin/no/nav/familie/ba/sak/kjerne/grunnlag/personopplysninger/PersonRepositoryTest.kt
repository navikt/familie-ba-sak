package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PersonRepositoryTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    @Autowired private val personRepository: PersonRepository,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `skal kun hente personer fra aktive grunnlag`() {
        // Arrange
        val aktør = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
        val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

        val inaktivtGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = aktør.aktivFødselsnummer(),
                barnasIdenter = emptyList(),
                søkerAktør = aktør,
                barnAktør = emptyList(),
            ).also { it.aktiv = false }
        personopplysningGrunnlagRepository.save(inaktivtGrunnlag)

        val aktivtGrunnlag =
            personopplysningGrunnlagRepository.save(
                lagTestPersonopplysningGrunnlag(
                    behandlingId = behandling.id,
                    søkerPersonIdent = aktør.aktivFødselsnummer(),
                    barnasIdenter = emptyList(),
                    søkerAktør = aktør,
                    barnAktør = emptyList(),
                ),
            )

        // Act
        val personer = personRepository.finnPersonerIAktiveGrunnlag(aktør)

        // Assert
        assertThat(personer).hasSize(1)
        assertThat(personer.single().personopplysningGrunnlag.id).isEqualTo(aktivtGrunnlag.id)
    }

    @Test
    fun `skal returnere tom liste når aktør kun finnes i inaktivt grunnlag`() {
        // Arrange
        val aktør = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
        val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))
        personopplysningGrunnlagRepository.save(
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = aktør.aktivFødselsnummer(),
                barnasIdenter = emptyList(),
                søkerAktør = aktør,
                barnAktør = emptyList(),
            ).also { it.aktiv = false },
        )

        // Act
        val personer = personRepository.finnPersonerIAktiveGrunnlag(aktør)

        // Assert
        assertThat(personer).isEmpty()
    }

    @Test
    fun `skal returnere personer fra nyeste aktive grunnlag først`() {
        // Arrange
        val aktør = aktørIdRepository.save(randomAktør())
        val førsteFagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
        val andreFagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktørIdRepository.save(randomAktør())))
        val førsteBehandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = førsteFagsak))
        val andreBehandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = andreFagsak))

        val førsteGrunnlag =
            personopplysningGrunnlagRepository.save(
                lagTestPersonopplysningGrunnlag(
                    behandlingId = førsteBehandling.id,
                    søkerPersonIdent = aktør.aktivFødselsnummer(),
                    barnasIdenter = emptyList(),
                    søkerAktør = aktør,
                    barnAktør = emptyList(),
                ),
            )
        val andreGrunnlag =
            personopplysningGrunnlagRepository.save(
                lagTestPersonopplysningGrunnlag(
                    behandlingId = andreBehandling.id,
                    søkerPersonIdent = aktør.aktivFødselsnummer(),
                    barnasIdenter = emptyList(),
                    søkerAktør = aktør,
                    barnAktør = emptyList(),
                ),
            )

        // Act
        val personer = personRepository.finnPersonerIAktiveGrunnlag(aktør)

        // Assert
        assertThat(personer.map { it.personopplysningGrunnlag.id }).containsExactly(andreGrunnlag.id, førsteGrunnlag.id)
    }
}
