package no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.erOppfylt
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FiltreringsregelTest {

    val gyldigFnr = PersonIdent(randomFnr())

    @Test
    fun `Regelevaluering skal resultere i Ja`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evalueringer = evaluerFiltreringsregler(
            FiltreringsreglerFakta(
                mor,
                listOf(barnet),
                restenAvBarna,
                morLever = true,
                barnaLever = true,
                morHarVerge = false
            )
        )

        assertThat(evalueringer.erOppfylt()).isTrue
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når mor er under 18 år`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(17)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evalueringer = evaluerFiltreringsregler(
            FiltreringsreglerFakta(
                mor,
                listOf(barnet),
                restenAvBarna,
                morLever = true,
                barnaLever = true,
                morHarVerge = false
            )
        )

        assertThat(evalueringer.erOppfylt()).isFalse
        assertEnesteRegelMedResultatNei(evalueringer, Filtreringsregel.MOR_ER_OVER_18_ÅR)
    }

    @Test
    fun `Regelevaluering skal resultere i JA når det har gått mer enn 5 måneder siden forrige barn ble født`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet1 = tilfeldigPerson(LocalDate.now().plusMonths(0)).copy(personIdent = gyldigFnr)
        val barnet2 = tilfeldigPerson(LocalDate.now().minusMonths(1)).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf(
            PersonInfo(LocalDate.now().minusMonths(8).minusDays(1)),
            PersonInfo(LocalDate.now().minusMonths(8))
        )

        val evaluering = Filtreringsregel.MER_ENN_5_MND_SIDEN_FORRIGE_BARN.vurder(
            FiltreringsreglerFakta(
                mor,
                listOf(barnet1, barnet2),
                restenAvBarna,
                morLever = true,
                barnaLever = true,
                morHarVerge = false
            )
        )
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det har gått mindre enn 5 måneder siden forrige barn ble født`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet1 = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val barnet2 = tilfeldigPerson(LocalDate.now().minusMonths(1)).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf(
            PersonInfo(LocalDate.now().minusMonths(5).minusDays(1)),
            PersonInfo(LocalDate.now().minusMonths(8))
        )

        val evaluering = Filtreringsregel.MER_ENN_5_MND_SIDEN_FORRIGE_BARN.vurder(
            FiltreringsreglerFakta(
                mor,
                listOf(barnet1, barnet2),
                restenAvBarna,
                morLever = true,
                barnaLever = true,
                morHarVerge = false
            )
        )

        assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det er registrert dødsfall på mor`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evalueringer = evaluerFiltreringsregler(
            FiltreringsreglerFakta(
                mor,
                listOf(barnet),
                restenAvBarna,
                morLever = false,
                barnaLever = true,
                morHarVerge = false
            )
        )

        assertThat(evalueringer.erOppfylt()).isFalse
        assertEnesteRegelMedResultatNei(evalueringer, Filtreringsregel.MOR_LEVER)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det er registrert dødsfall på barnet`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evalueringer = evaluerFiltreringsregler(
            FiltreringsreglerFakta(
                mor,
                listOf(barnet),
                restenAvBarna,
                morLever = true,
                barnaLever = false,
                morHarVerge = false
            )
        )

        assertThat(evalueringer.erOppfylt()).isFalse
        assertEnesteRegelMedResultatNei(evalueringer, Filtreringsregel.BARN_LEVER)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når mor har verge`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(personIdent = gyldigFnr)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evalueringer = evaluerFiltreringsregler(
            FiltreringsreglerFakta(
                mor,
                listOf(barnet),
                restenAvBarna,
                morLever = true,
                barnaLever = true,
                morHarVerge = true
            )
        )

        assertThat(evalueringer.erOppfylt()).isFalse
        assertEnesteRegelMedResultatNei(evalueringer, Filtreringsregel.MOR_HAR_IKKE_VERGE)
    }

    fun assertIkkeOppfyltFiltreringsregel(evalueringer: List<Evaluering>, filtreringsregel: Filtreringsregel) {
        evalueringer.forEach {
            if (it.evalueringÅrsaker.first().hentIdentifikator() == filtreringsregel.name) {
                Assertions.assertEquals(Resultat.IKKE_OPPFYLT, it.resultat)
                return
            } else {
                Assertions.assertEquals(Resultat.OPPFYLT, it.resultat)
            }
        }
    }

    @Test
    fun `Mor er under 18`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2019-10-23"), personIdent = PersonIdent("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2020-09-23"))

        val evalueringer =
            evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(barn2PersonInfo),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = false
                )
            )
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.MOR_ER_OVER_18_ÅR)
    }

    @Test
    fun `Barn med mindre mellomrom enn 5mnd`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2020-09-23"))

        val evalueringer =
            evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(barn2PersonInfo),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = false,
                )
            )
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.MER_ENN_5_MND_SIDEN_FORRIGE_BARN)
    }

    @Test
    fun `Mor lever ikke`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val evalueringer =
            evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(barn2PersonInfo),
                    morLever = false,
                    barnaLever = true,
                    morHarVerge = false,
                )
            )
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.MOR_LEVER)
    }

    @Test
    fun `Barnet lever ikke`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val evalueringer =
            evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(barn2PersonInfo),
                    morLever = true,
                    barnaLever = false,
                    morHarVerge = false,
                )
            )
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.BARN_LEVER)
    }

    @Test
    fun `Mor har verge`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val evalueringer =
            evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(barn2PersonInfo),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = true,
                )
            )
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.MOR_HAR_IKKE_VERGE)
    }

    @Test
    fun `Mor er død og er umyndig`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val evalueringer =
            evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(barn2PersonInfo),
                    morLever = false,
                    barnaLever = true,
                    morHarVerge = true,
                )
            )
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.MOR_LEVER)
    }

    @Test
    fun `Flere barn født`() {
        val nyligFødselsdato = LocalDate.now().minusDays(2)
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = nyligFødselsdato, personIdent = PersonIdent("21111777001"))
        val barn2Person =
            tilfeldigPerson(fødselsdato = nyligFødselsdato, personIdent = PersonIdent("23128438785"))
        val barn3PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val evalueringer =
            evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person, barn2Person),
                    restenAvBarna = listOf(barn3PersonInfo),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = false
                )
            )
        Assertions.assertTrue(evalueringer.erOppfylt())
    }

    @Test
    fun `Mor har ugyldig fødselsnummer`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("23456789111"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("21111777001"))
        val barn3PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val evalueringer =
            evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(barn3PersonInfo),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = false
                )
            )
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.MOR_GYLDIG_FNR)
    }

    @Test
    fun `Barn med ugyldig fødselsnummer`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("23102000000"))
        val barn2Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2018-09-23"), personIdent = PersonIdent("23091823456"))

        val evalueringer =
            evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person, barn2Person),
                    restenAvBarna = listOf(),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = false
                )
            )
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.BARN_GYLDIG_FNR)
    }

    @Test
    fun `Saken er godkjent fordi barnet er født i denne måneden`() {
        val fødselsdatoIDenneMåned = LocalDate.now().withDayOfMonth(1)

        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = fødselsdatoIDenneMåned, personIdent = PersonIdent("23091823456"))

        val evalueringer =
            evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = false
                )
            )
        Assertions.assertTrue(evalueringer.erOppfylt())
    }

    private fun assertEnesteRegelMedResultatNei(evalueringer: List<Evaluering>, filtreringsRegel: Filtreringsregel) {
        assertThat(1).isEqualTo(evalueringer.filter { it.resultat == Resultat.IKKE_OPPFYLT }.size)
        assertThat(filtreringsRegel.name)
            .isEqualTo(evalueringer.filter { it.resultat == Resultat.IKKE_OPPFYLT }[0].identifikator)
    }

    @Test
    fun `Filtreringsreglene skal følge en fagbestemt rekkefølge`() {
        val fagbestemtFiltreringsregelrekkefølge = listOf(
            Filtreringsregel.MOR_GYLDIG_FNR,
            Filtreringsregel.BARN_GYLDIG_FNR,
            Filtreringsregel.MOR_LEVER,
            Filtreringsregel.BARN_LEVER,
            Filtreringsregel.MER_ENN_5_MND_SIDEN_FORRIGE_BARN,
            Filtreringsregel.MOR_ER_OVER_18_ÅR,
            Filtreringsregel.MOR_HAR_IKKE_VERGE,
        )
        assertThat(Filtreringsregel.values().size).isEqualTo(fagbestemtFiltreringsregelrekkefølge.size)
        assertThat(
            Filtreringsregel.values().zip(fagbestemtFiltreringsregelrekkefølge)
                .all { (x, y) -> x == y }
        ).isTrue
    }
}
