package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import jakarta.persistence.EntityManager
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
import no.nav.familie.ba.sak.datagenerator.lagPersonopplysningGrunnlagUtenId
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

/**
 * Merk: bevisst ikke @Transactional på klassenivå. slettBatchMedInaktiveGrunnlag bruker bulk-JPQL
 * (@Modifying), som omgår persistence-konteksten. Med en omsluttende testtransaksjon ville
 * førstenivåcachen kunne gi stale svar på findById-oppslag etter sletting.
 */
class SlettInaktivePersonopplysningsgrunnlagServiceIntegrationTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    @Autowired private val personRepository: PersonRepository,
    @Autowired private val slettInaktivePersonopplysningsgrunnlagService: SlettInaktivePersonopplysningsgrunnlagService,
    @Autowired private val entityManager: EntityManager,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `slettBatchMedInaktiveGrunnlag skal slette batchstørrelsen i stigende id-rekkefølge etter gitt id og returnere id-ene`() {
        // Arrange
        val behandlingId = opprettBehandling()
        personopplysningGrunnlagRepository.saveAndFlush(lagPersonopplysningGrunnlagUtenId(behandlingId, aktiv = true))
        val inaktiveIder =
            (1..3)
                .map {
                    personopplysningGrunnlagRepository.saveAndFlush(lagPersonopplysningGrunnlagUtenId(behandlingId, aktiv = false)).id
                }.sorted()

        // Act
        val slettedeIder =
            slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(
                etterId = inaktiveIder.first() - 1,
                batchStørrelse = 2,
            )

        // Assert
        assertThat(slettedeIder).containsExactly(inaktiveIder[0], inaktiveIder[1])
        assertThat(personopplysningGrunnlagRepository.findById(inaktiveIder[0])).isEmpty()
        assertThat(personopplysningGrunnlagRepository.findById(inaktiveIder[1])).isEmpty()
        assertThat(personopplysningGrunnlagRepository.findById(inaktiveIder[2])).isPresent()
    }

    @Test
    fun `skal slette tilhørende person og registeropplysninger via ON DELETE CASCADE`() {
        // Arrange
        val behandlingId = opprettBehandling()
        personopplysningGrunnlagRepository.saveAndFlush(lagPersonopplysningGrunnlagUtenId(behandlingId, aktiv = true))
        val (inaktivtGrunnlag, person) = opprettOgLagreInaktivtGrunnlagMedFullstendigPerson(behandlingId)

        assertThat(tellRader("gr_personopplysninger", "id", inaktivtGrunnlag.id)).isEqualTo(1L)
        assertThat(tellRader("po_person", "id", person.id)).isEqualTo(1L)
        assertThat(tellRegisteropplysningerRader(person.id).values).allSatisfy { assertThat(it).isEqualTo(1) }

        // Act
        val slettedeIder =
            slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(
                etterId = inaktivtGrunnlag.id - 1,
                batchStørrelse = 10,
            )

        // Assert
        assertThat(slettedeIder).containsExactly(inaktivtGrunnlag.id)
        assertThat(tellRader("gr_personopplysninger", "id", inaktivtGrunnlag.id)).isEqualTo(0L)
        assertThat(tellRader("po_person", "id", person.id)).isEqualTo(0L)
        assertThat(tellRegisteropplysningerRader(person.id).values).allSatisfy { assertThat(it).isEqualTo(0) }
    }

    @Test
    fun `skal ikke slette det aktive grunnlaget på behandlingen`() {
        // Arrange
        val behandlingId = opprettBehandling()
        val aktivtGrunnlag = personopplysningGrunnlagRepository.saveAndFlush(lagPersonopplysningGrunnlagUtenId(behandlingId, aktiv = true))
        val inaktivtGrunnlag = personopplysningGrunnlagRepository.saveAndFlush(lagPersonopplysningGrunnlagUtenId(behandlingId, aktiv = false))

        // Act
        val slettedeIder =
            slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(
                etterId = inaktivtGrunnlag.id - 1,
                batchStørrelse = 10,
            )

        // Assert
        assertThat(slettedeIder).containsExactly(inaktivtGrunnlag.id)
        assertThat(personopplysningGrunnlagRepository.findById(aktivtGrunnlag.id)).isPresent()
        assertThat(personopplysningGrunnlagRepository.findById(inaktivtGrunnlag.id)).isEmpty()
    }

    @Test
    fun `skal ikke slette inaktivt grunnlag når behandlingen ikke har et aktivt grunnlag`() {
        // Arrange
        val behandlingId = opprettBehandling()
        val inaktivtGrunnlagUtenAktivtGrunnlagPåSammeBehandling =
            personopplysningGrunnlagRepository.saveAndFlush(lagPersonopplysningGrunnlagUtenId(behandlingId, aktiv = false))

        // Act
        val slettedeIder =
            slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(
                etterId = inaktivtGrunnlagUtenAktivtGrunnlagPåSammeBehandling.id - 1,
                batchStørrelse = 10,
            )

        // Assert
        assertThat(slettedeIder).isEmpty()
        assertThat(personopplysningGrunnlagRepository.findById(inaktivtGrunnlagUtenAktivtGrunnlagPåSammeBehandling.id)).isPresent()
    }

    @Test
    fun `tellInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling skal telle grunnlag på behandlinger uten aktivt grunnlag`() {
        // Arrange
        val antallFør = slettInaktivePersonopplysningsgrunnlagService.tellInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling()

        val behandlingIdUtenAktivt = opprettBehandling()
        personopplysningGrunnlagRepository.saveAndFlush(lagPersonopplysningGrunnlagUtenId(behandlingIdUtenAktivt, aktiv = false))

        val behandlingIdMedAktivt = opprettBehandling()
        personopplysningGrunnlagRepository.saveAndFlush(lagPersonopplysningGrunnlagUtenId(behandlingIdMedAktivt, aktiv = true))
        personopplysningGrunnlagRepository.saveAndFlush(lagPersonopplysningGrunnlagUtenId(behandlingIdMedAktivt, aktiv = false))

        // Act
        val antallEtter = slettInaktivePersonopplysningsgrunnlagService.tellInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling()

        // Assert
        assertThat(antallEtter - antallFør).isEqualTo(1L)
    }

    @Test
    fun `skal ikke slette aktør eller personident når grunnlag slettes`() {
        // Arrange
        val behandlingId = opprettBehandling()
        personopplysningGrunnlagRepository.saveAndFlush(lagPersonopplysningGrunnlagUtenId(behandlingId, aktiv = true))
        val (inaktivtGrunnlag, person) = opprettOgLagreInaktivtGrunnlagMedFullstendigPerson(behandlingId)

        // Act
        slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(
            etterId = inaktivtGrunnlag.id - 1,
            batchStørrelse = 10,
        )

        // Assert
        assertThat(aktørIdRepository.findByAktørIdOrNull(person.aktør.aktørId)).isNotNull()
        assertThat(tellRader("personident", "fk_aktoer_id", person.aktør.aktørId)).isEqualTo(1)
    }

    private fun opprettBehandling(): Long {
        val aktør = aktørIdRepository.saveAndFlush(lagAktør())
        val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
        return behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak)).id
    }

    private fun opprettOgLagreInaktivtGrunnlagMedFullstendigPerson(behandlingId: Long): Pair<PersonopplysningGrunnlag, Person> {
        val aktør = aktørIdRepository.saveAndFlush(lagAktør())
        val grunnlag =
            lagPersonopplysningGrunnlagUtenId(behandlingId, aktiv = false).apply {
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
