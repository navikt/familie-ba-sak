package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.filtreringsregler.Fakta
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.filtreringsregler.Filtreringsregler
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.filtreringsregler.evaluerFiltreringsregler
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Evaluering
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.erOppfylt
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FiltreringsreglerUtilTest {

    fun assertIkkeOppfyltFiltreringsregel(evalueringer: List<Evaluering>, filtreringsregler: Filtreringsregler) {
        evalueringer.forEach {
            if (it.evalueringÅrsaker.first().hentIdentifikator() == filtreringsregler.name) {
                assertEquals(Resultat.IKKE_OPPFYLT, it.resultat)
                return
            } else {
                assertEquals(Resultat.OPPFYLT, it.resultat)
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
                evaluerFiltreringsregler(Fakta(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(barn2PersonInfo),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false
                ))
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregler.MOR_ER_OVER_18_ÅR)
    }

    @Test
    fun `Barn med mindre mellomrom enn 5mnd`() {
        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2020-09-23"))

        val evalueringer =
                evaluerFiltreringsregler(Fakta(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(barn2PersonInfo),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false,
                ))
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregler.MER_ENN_5_MND_SIDEN_FORRIGE_BARN)
    }

    @Test
    fun `Mor lever ikke`() {
        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val evalueringer =
                evaluerFiltreringsregler(Fakta(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(barn2PersonInfo),
                        morLever = false,
                        barnaLever = true,
                        morHarVerge = false,
                ))
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregler.MOR_LEVER)
    }

    @Test
    fun `Barnet lever ikke`() {
        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val evalueringer =
                evaluerFiltreringsregler(Fakta(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(barn2PersonInfo),
                        morLever = true,
                        barnaLever = false,
                        morHarVerge = false,
                ))
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregler.BARN_LEVER)
    }

    @Test
    fun `Mor har verge`() {
        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val evalueringer =
                evaluerFiltreringsregler(Fakta(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(barn2PersonInfo),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = true,
                ))
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregler.MOR_HAR_IKKE_VERGE)
    }

    @Test
    fun `Mor er død og er umyndig`() {
        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val evalueringer =
                evaluerFiltreringsregler(Fakta(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(barn2PersonInfo),
                        morLever = false,
                        barnaLever = true,
                        morHarVerge = true,
                ))
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregler.MOR_LEVER)
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
                evaluerFiltreringsregler(Fakta(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person, barn2Person),
                        restenAvBarna = listOf(barn3PersonInfo),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false
                ))
        assertTrue(evalueringer.erOppfylt())
    }

    @Test
    fun `Mor har ugyldig fødselsnummer`() {
        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("23456789111"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("21111777001"))
        val barn3PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val evalueringer =
                evaluerFiltreringsregler(Fakta(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(barn3PersonInfo),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false
                ))
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregler.MOR_GYLDIG_FNR)
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
                evaluerFiltreringsregler(Fakta(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person, barn2Person),
                        restenAvBarna = listOf(),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false
                ))
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregler.BARN_GYLDIG_FNR)
    }

    @Test
    fun `Saken krever etterbetaling pga det er lenge siden fødsel for et barn`() {
        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent(randomFnr()))
        val barn1Person =
                tilfeldigPerson(fødselsdato = LocalDate.parse("2018-09-23"), personIdent = PersonIdent(randomFnr()))

        val evalueringer =
                evaluerFiltreringsregler(Fakta(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false
                ))
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregler.BARNETS_FØDSELSDATO_TRIGGER_IKKE_ETTERBETALING)
    }

    @Test
    fun `Saken er godkjent fordi barnet er født i denne måneden`() {
        val fødselsdatoIDenneMåned = LocalDate.now().withDayOfMonth(1)

        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = fødselsdatoIDenneMåned, personIdent = PersonIdent("23091823456"))

        val evalueringer =
                evaluerFiltreringsregler(Fakta(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false
                ))
        assertTrue(evalueringer.erOppfylt())
    }
}