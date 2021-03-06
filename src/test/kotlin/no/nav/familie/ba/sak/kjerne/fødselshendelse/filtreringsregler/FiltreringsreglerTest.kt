package no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.common.sisteDagIForrigeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Evaluering
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.util.FnrGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FiltreringsreglerTest {

    val gyldigFnr = PersonIdent(FnrGenerator.generer())

    @Test
    fun `Regelevaluering skal resultere i Ja`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, listOf(barnet), restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når mor er under 18 år`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(17)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, listOf(barnet), restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
        assertEnesteRegelMedResultatNei(evaluering.children, Filtreringsregler.MOR_ER_OVER_18_AAR)
    }

    @Test
    fun `Regelevaluering skal resultere i JA når det har gått mer enn 5 måneder siden forrige barn ble født`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet1 = tilfeldigPerson(LocalDate.now().plusMonths(0)).copy(personIdent = gyldigFnr)
        val barnet2 = tilfeldigPerson(LocalDate.now().minusMonths(1)).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf(PersonInfo(LocalDate.now().minusMonths(8).minusDays(1)),
                                                     PersonInfo(LocalDate.now().minusMonths(8)))


        val evaluering = Filtreringsregler.MER_ENN_5_MND_SIDEN_FORRIGE_BARN.spesifikasjon.evaluer(
                Fakta(mor,
                      listOf(barnet1, barnet2),
                      restenAvBarna,
                      morLever = true,
                      barnetLever = true,
                      morHarVerge = false)
        )
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det har gått mindre enn 5 måneder siden forrige barn ble født`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet1 = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val barnet2 = tilfeldigPerson(LocalDate.now().minusMonths(1)).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf(PersonInfo(LocalDate.now().minusMonths(5).minusDays(1)),
                                                     PersonInfo(LocalDate.now().minusMonths(8)))

        val evaluering = Filtreringsregler.MER_ENN_5_MND_SIDEN_FORRIGE_BARN.spesifikasjon.evaluer(
                Fakta(mor,
                      listOf(barnet1, barnet2),
                      restenAvBarna,
                      morLever = true,
                      barnetLever = true,
                      morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det er registrert dødsfall på mor`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, listOf(barnet), restenAvBarna, morLever = false, barnetLever = true, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
        assertEnesteRegelMedResultatNei(evaluering.children, Filtreringsregler.MOR_LEVER)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det er registrert dødsfall på barnet`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, listOf(barnet), restenAvBarna, morLever = true, barnetLever = false, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
        assertEnesteRegelMedResultatNei(evaluering.children, Filtreringsregler.BARNET_LEVER)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når mor har verge`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, listOf(barnet), restenAvBarna, morLever = true, barnetLever = true, morHarVerge = true))

        assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
        assertEnesteRegelMedResultatNei(evaluering.children, Filtreringsregler.MOR_HAR_IKKE_VERGE)
    }

    @Test
    fun `Regelevaluering i forhold til den 21 i hver måned og barnets fødselsdato med hensyn til etterbetaling`() {
        val now = LocalDate.now()
        assertRegelBasertPåDato(dagensDato = now.withDayOfMonth(20),
                                fødselsdatoForBarn = now.minusMonths(2).sisteDagIMåned(),
                                forventetResultat = Resultat.IKKE_OPPFYLT)
        assertRegelBasertPåDato(dagensDato = now.withDayOfMonth(20),
                                fødselsdatoForBarn = now.minusMonths(1).withDayOfMonth(1),
                                forventetResultat = Resultat.OPPFYLT)
        assertRegelBasertPåDato(dagensDato = now.withDayOfMonth(21),
                                fødselsdatoForBarn = now.sisteDagIForrigeMåned(),
                                forventetResultat = Resultat.IKKE_OPPFYLT)
        assertRegelBasertPåDato(dagensDato = now.withDayOfMonth(21),
                                fødselsdatoForBarn = now.withDayOfMonth(1),
                                forventetResultat = Resultat.OPPFYLT)
    }

    private fun assertRegelBasertPåDato(dagensDato: LocalDate, fødselsdatoForBarn: LocalDate, forventetResultat: Resultat) {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(fødselsdatoForBarn).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()
        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, listOf(barnet), restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false,
                               dagensDato))

        assertThat(forventetResultat).isEqualTo(evaluering.resultat)
        if (forventetResultat == Resultat.IKKE_OPPFYLT)
            assertEnesteRegelMedResultatNei(evaluering.children, Filtreringsregler.BARNETS_FØDSELSDATO_TRIGGER_IKKE_ETTERBETALING)
    }

    private fun assertEnesteRegelMedResultatNei(evalueringer: List<Evaluering>, filtreringsRegel: Filtreringsregler) {
        assertThat(1).isEqualTo(evalueringer.filter { it.resultat == Resultat.IKKE_OPPFYLT }.size)
        assertThat(filtreringsRegel.spesifikasjon.identifikator)
                .isEqualTo(evalueringer.filter { it.resultat == Resultat.IKKE_OPPFYLT }[0].identifikator)
    }

    @Test
    fun `Filtreringsreglene skal følge en fagbestemt rekkefølge`() {
        val fagbestemtFiltreringsregelrekkefølge = listOf(
                Filtreringsregler.BARNET_LEVER,
                Filtreringsregler.MOR_LEVER,
                Filtreringsregler.MOR_ER_OVER_18_AAR,
                Filtreringsregler.MOR_HAR_IKKE_VERGE,
                Filtreringsregler.MER_ENN_5_MND_SIDEN_FORRIGE_BARN,
                Filtreringsregler.BARNETS_FØDSELSDATO_TRIGGER_IKKE_ETTERBETALING
        )
        assertThat(Filtreringsregler.values().size).isEqualTo(fagbestemtFiltreringsregelrekkefølge.size)
        assertThat(Filtreringsregler.values().zip(fagbestemtFiltreringsregelrekkefølge)
                           .all { (x, y) -> x == y }
        ).isTrue
    }
}