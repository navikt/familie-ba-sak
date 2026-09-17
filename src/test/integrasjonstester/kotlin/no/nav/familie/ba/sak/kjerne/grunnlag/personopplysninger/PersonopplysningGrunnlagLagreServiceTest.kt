package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class PersonopplysningGrunnlagLagreServiceTest(
    @Autowired private val personopplysningGrunnlagLagreService: PersonopplysningGrunnlagLagreService,
    @Autowired private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    @Autowired private val personRepository: PersonRepository,
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `skal lagre grunnlag når behandlingen ikke har grunnlag fra før`() {
        // Arrange
        val aktør = aktørIdRepository.saveAndFlush(lagAktør())
        val behandlingId = opprettBehandling(aktør)
        val nyttGrunnlag = lagGrunnlag(behandlingId, aktør)

        // Act
        val lagretGrunnlag = personopplysningGrunnlagLagreService.lagreOgSlettGammelt(nyttGrunnlag)

        // Assert
        assertThat(lagretGrunnlag.id).isNotZero()
        assertThat(lagretGrunnlag.aktiv).isTrue()
        assertThat(personopplysningGrunnlagRepository.findById(lagretGrunnlag.id)).isPresent()
        assertThat(personRepository.findById(lagretGrunnlag.personer.single().id)).isPresent()
        assertThat(personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)?.id).isEqualTo(lagretGrunnlag.id)
    }

    @Test
    fun `skal slette gammelt grunnlag før nytt grunnlag lagres`() {
        // Arrange
        val aktør = aktørIdRepository.saveAndFlush(lagAktør())
        val behandlingId = opprettBehandling(aktør)
        val gammeltGrunnlag = personopplysningGrunnlagLagreService.lagreOgSlettGammelt(lagGrunnlag(behandlingId, aktør))
        val gammelPersonId = gammeltGrunnlag.personer.single().id

        // Act
        val nyttGrunnlag = personopplysningGrunnlagLagreService.lagreOgSlettGammelt(lagGrunnlag(behandlingId, aktør))

        // Assert
        assertThat(personopplysningGrunnlagRepository.findById(gammeltGrunnlag.id)).isEmpty()
        assertThat(personRepository.findById(gammelPersonId)).isEmpty()
        assertThat(personopplysningGrunnlagRepository.findById(nyttGrunnlag.id)).isPresent()
        assertThat(personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)?.id).isEqualTo(nyttGrunnlag.id)
    }

    @Test
    fun `skal beholde gammelt grunnlag når lagring av nytt grunnlag feiler`() {
        // Arrange
        val aktør = aktørIdRepository.saveAndFlush(lagAktør())
        val behandlingId = opprettBehandling(aktør)
        val gammeltGrunnlag = personopplysningGrunnlagLagreService.lagreOgSlettGammelt(lagGrunnlag(behandlingId, aktør))
        val gammelPersonId = gammeltGrunnlag.personer.single().id
        val ugyldigNyttGrunnlag = lagGrunnlag(behandlingId, lagAktør())

        // Act & Assert
        assertThrows<RuntimeException> {
            personopplysningGrunnlagLagreService.lagreOgSlettGammelt(ugyldigNyttGrunnlag)
        }
        assertThat(personopplysningGrunnlagRepository.findById(gammeltGrunnlag.id)).isPresent()
        assertThat(personRepository.findById(gammelPersonId)).isPresent()
        assertThat(personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)?.id).isEqualTo(gammeltGrunnlag.id)
    }

    private fun opprettBehandling(aktør: Aktør): Long {
        val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
        return behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak)).id
    }

    private fun lagGrunnlag(
        behandlingId: Long,
        aktør: Aktør,
    ): PersonopplysningGrunnlag =
        PersonopplysningGrunnlag(behandlingId = behandlingId).apply {
            personer.add(
                lagPerson(
                    type = PersonType.SØKER,
                    personopplysningGrunnlag = this,
                    aktør = aktør,
                ),
            )
        }
}
