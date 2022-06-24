package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FagsakIntegrationTest(
    @Autowired
    val fagsakService: FagsakService
) : AbstractSpringIntegrationTest() {

    @Test
    fun `hentMinimalFagsakerForPerson() skal return begge fagsaker for en person`() {
        val personFnr = randomFnr()
        val fagsakOmsorgperson = fagsakService.hentEllerOpprettFagsak(personFnr)
        val fagsakBarn = fagsakService.hentEllerOpprettFagsak(personFnr, false, FagsakEier.BARN, FagsakType.INSTITUSJON)

        val minimalFagsakList = fagsakService.hentMinimalFagsakerForPerson(fagsakOmsorgperson.aktør)

        assertThat(minimalFagsakList.data).hasSize(2).extracting("id").contains(fagsakBarn.id, fagsakOmsorgperson.id)
    }

    @Test
    fun `hentMinimalFagsakForPerson() skal return riktig fagsak for en person`() {
        val personFnr = randomFnr()
        val fagsakOmsorgperson = fagsakService.hentEllerOpprettFagsak(personFnr)
        val fagsakBarn = fagsakService.hentEllerOpprettFagsak(personFnr, false, FagsakEier.BARN, FagsakType.INSTITUSJON)

        val defaultMinimalFagsak = fagsakService.hentMinimalFagsakForPerson(fagsakOmsorgperson.aktør)
        assertThat(defaultMinimalFagsak.data!!.id).isEqualTo(fagsakOmsorgperson.id)

        val omsorgpersonMinimalFagsak =
            fagsakService.hentMinimalFagsakForPerson(fagsakOmsorgperson.aktør, FagsakEier.OMSORGSPERSON)
        assertThat(omsorgpersonMinimalFagsak.data!!.id).isEqualTo(fagsakOmsorgperson.id)

        val barnMinimalFagsak =
            fagsakService.hentMinimalFagsakForPerson(fagsakOmsorgperson.aktør, FagsakEier.BARN)
        assertThat(barnMinimalFagsak.data!!.id).isEqualTo(fagsakBarn.id)
    }
}
