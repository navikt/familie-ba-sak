package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FagsakIntegrationTest(
    @Autowired
    val fagsakService: FagsakService,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `hentMinimalFagsakerForPerson() skal return begge fagsaker for en person`() {
        // Arrange
        val personFnr = randomFnr()
        val fagsakOmsorgperson =
            fagsakService.hentEllerOpprettFagsak(
                personFnr,
            )
        val fagsakInstitusjon =
            fagsakService.hentEllerOpprettFagsak(
                personFnr,
                false,
                FagsakType.INSTITUSJON,
                InstitusjonDto("orgnr", null),
            )
        val fagsakEnsligMindreÅrig =
            fagsakService.hentEllerOpprettFagsak(
                personFnr,
                false,
                FagsakType.BARN_ENSLIG_MINDREÅRIG,
            )

        // Act
        val minimalFagsakList = fagsakService.hentMinimalFagsakerForPerson(fagsakOmsorgperson.aktør)

        // Assert
        assertThat(minimalFagsakList.data)
            .hasSize(3)
            .extracting("id")
            .contains(fagsakInstitusjon.id, fagsakOmsorgperson.id, fagsakEnsligMindreÅrig.id)
    }

    @Test
    fun `hentMinimalFagsakForPerson() skal return riktig fagsak for en person`() {
        // Arrange
        val personFnr = randomFnr()
        val fagsakOmsorgperson =
            fagsakService.hentEllerOpprettFagsak(
                personFnr,
            )
        val fagsakInstitusjon =
            fagsakService.hentEllerOpprettFagsak(
                personFnr,
                false,
                FagsakType.INSTITUSJON,
                InstitusjonDto("orgnr", null),
            )
        val fagsakEnsligMindreÅrig =
            fagsakService.hentEllerOpprettFagsak(
                personFnr,
                false,
                FagsakType.BARN_ENSLIG_MINDREÅRIG,
            )

        // Act
        val defaultMinimalFagsak = fagsakService.hentMinimalFagsakForPerson(fagsakOmsorgperson.aktør)
        // Assert
        assertThat(defaultMinimalFagsak.data!!.id).isEqualTo(fagsakOmsorgperson.id)

        // Act
        val omsorgpersonMinimalFagsak =
            fagsakService.hentMinimalFagsakForPerson(fagsakOmsorgperson.aktør, FagsakType.NORMAL)
        // Assert
        assertThat(omsorgpersonMinimalFagsak.data!!.id).isEqualTo(fagsakOmsorgperson.id)

        // Act
        val institusjonMinimalFagsak =
            fagsakService.hentMinimalFagsakForPerson(fagsakOmsorgperson.aktør, FagsakType.INSTITUSJON)
        // Assert
        assertThat(institusjonMinimalFagsak.data!!.id).isEqualTo(fagsakInstitusjon.id)

        // Act
        val ensligMindreÅrigMinimalFagsak =
            fagsakService.hentMinimalFagsakForPerson(fagsakOmsorgperson.aktør, FagsakType.BARN_ENSLIG_MINDREÅRIG)
        // Assert
        assertThat(ensligMindreÅrigMinimalFagsak.data!!.id).isEqualTo(fagsakEnsligMindreÅrig.id)
    }
}
