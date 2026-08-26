package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.lagGrArbeidsforhold
import no.nav.familie.ba.sak.datagenerator.lagGrOpphold
import no.nav.familie.ba.sak.datagenerator.lagGrSivilstand
import no.nav.familie.ba.sak.datagenerator.lagGrStatsborgerskap
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresseBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresseDeltBosted
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresseOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class PersonopplysningGrunnlagSlettingTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    @Autowired private val entityManager: EntityManager,
) : AbstractSpringIntegrationTest() {
    @Test
    @Transactional
    fun `skal slette person og registeropplysninger når grunnlag slettes via JPA`() {
        // Arrange
        val (grunnlag, person) = opprettOgLagreGrunnlagMedFullstendigPerson()

        assertThat(grunnlagFinnes(grunnlag.id)).isTrue()
        assertThat(personFinnes(person.id)).isTrue()
        assertThat(tellRegisteropplysningerRader(person.id).values).allSatisfy { assertThat(it).isEqualTo(1) }

        // Act
        personopplysningGrunnlagRepository.delete(grunnlag)
        personopplysningGrunnlagRepository.flush()

        // Assert
        assertThat(grunnlagFinnes(grunnlag.id)).isFalse()
        assertThat(personFinnes(person.id)).isFalse()
        assertThat(tellRegisteropplysningerRader(person.id).values).allSatisfy { assertThat(it).isEqualTo(0) }
    }

    @Test
    @Transactional
    fun `skal slette person og registeropplysninger ved native sletting av grunnlag`() {
        // Arrange
        val (grunnlag, person) = opprettOgLagreGrunnlagMedFullstendigPerson()

        assertThat(grunnlagFinnes(grunnlag.id)).isTrue()
        assertThat(personFinnes(person.id)).isTrue()
        assertThat(tellRegisteropplysningerRader(person.id).values).allSatisfy { assertThat(it).isEqualTo(1) }

        // Act
        entityManager
            .createNativeQuery("DELETE FROM gr_personopplysninger WHERE id = :id")
            .setParameter("id", grunnlag.id)
            .executeUpdate()

        // Assert
        assertThat(grunnlagFinnes(grunnlag.id)).isFalse()
        assertThat(personFinnes(person.id)).isFalse()
        assertThat(tellRegisteropplysningerRader(person.id).values).allSatisfy { assertThat(it).isEqualTo(0) }
    }

    @Test
    @Transactional
    fun `skal ikke slette aktør og personident når grunnlag slettes`() {
        // Arrange
        val (grunnlag, person) = opprettOgLagreGrunnlagMedFullstendigPerson()

        // Act
        personopplysningGrunnlagRepository.delete(grunnlag)
        personopplysningGrunnlagRepository.flush()

        // Assert
        assertThat(grunnlagFinnes(grunnlag.id)).isFalse()
        assertThat(personFinnes(person.id)).isFalse()
        assertThat(aktørIdRepository.findByAktørIdOrNull(person.aktør.aktørId)).isNotNull()
        assertThat(tellRader("personident", "fk_aktoer_id", person.aktør.aktørId)).isEqualTo(1)
    }

    private fun opprettOgLagreGrunnlagMedFullstendigPerson(): Pair<PersonopplysningGrunnlag, Person> {
        val aktør = aktørIdRepository.saveAndFlush(lagAktør())
        val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
        val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

        val grunnlag =
            PersonopplysningGrunnlag(behandlingId = behandling.id).apply {
                personer.add(
                    lagPerson(
                        type = PersonType.BARN,
                        personopplysningGrunnlag = this,
                        aktør = aktør,
                        bostedsadresser = { p -> listOf(lagGrVegadresseBostedsadresse(person = p)) },
                        oppholdsadresser = { p -> listOf(lagGrVegadresseOppholdsadresse(person = p)) },
                        deltBosted = { p -> listOf(lagGrVegadresseDeltBosted(person = p)) },
                        statsborgerskap = { p -> listOf(lagGrStatsborgerskap(person = p)) },
                        opphold = { p -> listOf(lagGrOpphold(person = p)) },
                        arbeidsforhold = { p -> listOf(lagGrArbeidsforhold(person = p)) },
                        sivilstander = { p -> listOf(lagGrSivilstand(person = p)) },
                        dødsfall = { p -> lagDødsfall(person = p, dødsfallDato = LocalDate.now().minusDays(1)) },
                    ),
                )
            }

        val lagretGrunnlag = personopplysningGrunnlagRepository.saveAndFlush(grunnlag)
        val lagretPerson = lagretGrunnlag.personer.single()
        return lagretGrunnlag to lagretPerson
    }

    private fun grunnlagFinnes(grunnlagId: Long): Boolean = tellRader("gr_personopplysninger", "id", grunnlagId) == 1L

    private fun personFinnes(personId: Long): Boolean = tellRader("po_person", "id", personId) == 1L

    private fun tellRegisteropplysningerRader(personId: Long): Map<String, Long> = REGISTEROPPLYSNINGER_TABELLER.associateWith { tabell -> tellRader(tabell, "fk_po_person_id", personId) }

    private fun tellRader(
        tabell: String,
        kolonne: String,
        id: Any,
    ): Long =
        entityManager
            .createNativeQuery("SELECT count(*) FROM $tabell WHERE $kolonne = :id")
            .setParameter("id", id)
            .singleResult as Long

    companion object {
        private val REGISTEROPPLYSNINGER_TABELLER =
            listOf(
                "po_statsborgerskap",
                "po_opphold",
                "po_arbeidsforhold",
                "po_sivilstand",
                "po_bostedsadresse",
                "po_doedsfall",
                "po_oppholdsadresse",
                "po_delt_bosted",
            )
    }
}
