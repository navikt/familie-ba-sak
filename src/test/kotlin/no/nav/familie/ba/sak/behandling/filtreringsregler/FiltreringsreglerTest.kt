package no.nav.familie.ba.sak.behandling.filtreringsregler

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.nare.core.evaluations.Resultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FiltreringsreglerTest {

    @Test
    fun `Regelevaluering skal resultere i Ja`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = PersonIdent("12345678910"))
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = PersonIdent("12345678910"))
        val restenAvBarna: List<Personinfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.JA)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når mor har D-nummer`() {
        val dNummer = "42345678910"
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = PersonIdent(dNummer))
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = PersonIdent("12345678910"))
        val restenAvBarna: List<Personinfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.JA)
    }


}