package no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.common.sisteDagIForrigeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.util.FnrGenerator
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FiltreringsreglerTest {
    val dnummer = PersonIdent(FnrGenerator.generer(erDnummer = true))
    val gyldigFnr = PersonIdent(FnrGenerator.generer())

    @Test
    fun `Regelevaluering skal resultere i Ja`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.JA)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når mor har D-nummer`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = dnummer)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        assertEnesteRegelMedResultatNei(evaluering.children, Filtreringsregler.MOR_HAR_GYLDIG_FOEDSELSNUMMER)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når barnet har D-nummer`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = dnummer)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        assertEnesteRegelMedResultatNei(evaluering.children, Filtreringsregler.BARNET_HAR_GYLDIG_FOEDSELSNUMMER)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når barnet er over 6 måneder`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now().minusYears(1)).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        val resultaterMedNei = evaluering.children.filter { it.resultat == Resultat.NEI }
        assertThat(2).isEqualTo(resultaterMedNei.size)
        assertThat(resultaterMedNei.map {it.identifikator}.containsAll(listOf(
                Filtreringsregler.BARNET_ER_UNDER_6_MND.spesifikasjon.identifikator,
                Filtreringsregler.BARNETS_FØDSELSDATO_TRIGGER_IKKE_ETTERBETALING.spesifikasjon.identifikator)
        ))
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når mor er under 18 år`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(17)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        assertEnesteRegelMedResultatNei(evaluering.children, Filtreringsregler.MOR_ER_OVER_18_AAR)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det har gått mindre enn 5 måneder siden forrige barn ble født`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf(PersonInfo(LocalDate.now().minusMonths(4)))

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        assertEnesteRegelMedResultatNei(evaluering.children, Filtreringsregler.MER_ENN_5_MND_SIDEN_FORRIGE_BARN)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det er registrert dødsfall på mor`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = false, barnetLever = true, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        assertEnesteRegelMedResultatNei(evaluering.children, Filtreringsregler.MOR_LEVER)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det er registrert dødsfall på barnet`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = true, barnetLever = false, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        assertEnesteRegelMedResultatNei(evaluering.children, Filtreringsregler.BARNET_LEVER)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når mor har verge`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = true, barnetLever = true, morHarVerge = true))

        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        assertEnesteRegelMedResultatNei(evaluering.children, Filtreringsregler.MOR_HAR_IKKE_VERGE)
    }

    @Test
    fun `Regelevaluering i forhold til den 21 i hver måned og barnets fødselsdato med hensyn til etterbetaling`() {
        if (LocalDate.now().dayOfMonth < 21) {
            assertRegelBasertPåDagensDato(
                    LocalDate.now().minusMonths(2).sisteDagIMåned(), Resultat.NEI)
            assertRegelBasertPåDagensDato(
                    LocalDate.now().minusMonths(1).withDayOfMonth(1), Resultat.JA)
        } else {
            assertRegelBasertPåDagensDato(
                    LocalDate.now().sisteDagIForrigeMåned(), Resultat.NEI)
            assertRegelBasertPåDagensDato(
                    LocalDate.now().withDayOfMonth(1), Resultat.JA)
        }
    }

    private fun assertRegelBasertPåDagensDato(fødselsdatoForBarn: LocalDate, forventetResultat: Resultat) {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(fødselsdatoForBarn).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()
        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false))

        assertThat(forventetResultat).isEqualTo(evaluering.resultat)
        if (forventetResultat == Resultat.NEI)
            assertEnesteRegelMedResultatNei(evaluering.children, Filtreringsregler.BARNETS_FØDSELSDATO_TRIGGER_IKKE_ETTERBETALING)
    }

    private fun assertEnesteRegelMedResultatNei(evalueringer: List<Evaluering>, filtreringsRegel: Filtreringsregler) {
        assertThat(1).isEqualTo(evalueringer.filter { it.resultat == Resultat.NEI }.size)
        assertThat(filtreringsRegel.spesifikasjon.identifikator)
                .isEqualTo(evalueringer.filter { it.resultat == Resultat.NEI }[0].identifikator)
    }

    @Test
    fun `Filtreringsreglene skal følge en fagbestemt rekkefølge`() {
        val fagbestemtFiltreringsregelrekkefølge = listOf(
                Filtreringsregler.MOR_HAR_GYLDIG_FOEDSELSNUMMER,
                Filtreringsregler.BARNET_HAR_GYLDIG_FOEDSELSNUMMER,
                Filtreringsregler.BARNET_ER_UNDER_6_MND,
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
        ).isTrue()
    }
}