package no.nav.familie.ba.sak.behandling.filtreringsregler

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.nare.core.evaluations.Resultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FiltreringsreglerTest {
    val dnummer = PersonIdent("42345678910")
    val gyldigFnr = PersonIdent("12345678910")

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
        assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }.size).isEqualTo(1)
        assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }[0].identifikator).isEqualTo(
                Filtreringsregler.MOR_HAR_GYLDIG_FOEDSELSNUMMER.spesifikasjon.identifikator)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når barnet har D-nummer`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = dnummer)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }.size).isEqualTo(1)
        assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }[0].identifikator).isEqualTo(
                Filtreringsregler.BARNET_HAR_GYLDIG_FOEDSELSNUMMER.spesifikasjon.identifikator)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når barnet er over 6 måneder`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now().minusYears(1)).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }.size).isEqualTo(1)
        assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }[0].identifikator).isEqualTo(
                Filtreringsregler.BARNET_ER_UNDER_6_MND.spesifikasjon.identifikator)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når mor er under 18 år`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(17)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }.size).isEqualTo(1)
        assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }[0].identifikator).isEqualTo(
                Filtreringsregler.MOR_ER_OVER_18_AAR.spesifikasjon.identifikator)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det har gått mindre enn 5 måneder siden forrige barn ble født`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf(PersonInfo(LocalDate.now().minusMonths(4)))

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }.size).isEqualTo(1)
        assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }[0].identifikator).isEqualTo(
                Filtreringsregler.MER_ENN_5_MND_SIDEN_FORRIGE_BARN.spesifikasjon.identifikator)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det er registrert dødsfall på mor`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = false, barnetLever = true, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }.size).isEqualTo(1)
        assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }[0].identifikator).isEqualTo(
                Filtreringsregler.MOR_LEVER.spesifikasjon.identifikator)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det er registrert dødsfall på barnet`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = true, barnetLever = false, morHarVerge = false))

        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }.size).isEqualTo(1)
        assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }[0].identifikator).isEqualTo(
                Filtreringsregler.BARNET_LEVER.spesifikasjon.identifikator)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når mor har verge`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barnet, restenAvBarna, morLever = true, barnetLever = true, morHarVerge = true))

        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }.size).isEqualTo(1)
        assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }[0].identifikator).isEqualTo(
                Filtreringsregler.MOR_HAR_IKKE_VERGE.spesifikasjon.identifikator)
    }
}