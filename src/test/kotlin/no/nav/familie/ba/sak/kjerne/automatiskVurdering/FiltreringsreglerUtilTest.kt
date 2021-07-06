package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import io.mockk.every
import io.mockk.mockkStatic
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FiltreringsreglerResultat
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.evaluerFiltreringsregler
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FiltreringsreglerUtilTest {

    @Test
    fun `Mor er under 18`() {
        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = LocalDate.parse("2019-10-23"), personIdent = PersonIdent("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2020-09-23"))

        val filtreringsResultat =
                evaluerFiltreringsregler(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(barn2PersonInfo),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false
                )
        Assertions.assertEquals(FiltreringsreglerResultat.MOR_ER_IKKE_OVER_18, filtreringsResultat)
    }

    @Test
    fun `Barn med mindre mellomrom enn 5mnd`() {
        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2020-09-23"))

        val filtreringsResultat =
                evaluerFiltreringsregler(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(barn2PersonInfo),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false,
                )
        Assertions.assertEquals(FiltreringsreglerResultat.MINDRE_ENN_5_MND_SIDEN_FORRIGE_BARN, filtreringsResultat)

    }

    @Test
    fun `Mor lever ikke`() {
        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val filtreringsResultat =
                evaluerFiltreringsregler(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(barn2PersonInfo),
                        morLever = false,
                        barnaLever = true,
                        morHarVerge = false,
                )
        Assertions.assertEquals(FiltreringsreglerResultat.MOR_ER_DØD, filtreringsResultat)
    }

    @Test
    fun `Barnet lever ikke`() {
        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val filtreringsResultat =
                evaluerFiltreringsregler(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(barn2PersonInfo),
                        morLever = true,
                        barnaLever = false,
                        morHarVerge = false,
                )
        Assertions.assertEquals(FiltreringsreglerResultat.DØDT_BARN, filtreringsResultat)
    }

    @Test
    fun `Mor har verge`() {
        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val filtreringsResultat =
                evaluerFiltreringsregler(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(barn2PersonInfo),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = true
                )
        Assertions.assertEquals(FiltreringsreglerResultat.MOR_HAR_VERGE, filtreringsResultat)
    }

    @Test
    fun `Mor er død og er umyndig`() {
        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val filtreringsResultat =
                evaluerFiltreringsregler(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(barn2PersonInfo),
                        morLever = false,
                        barnaLever = true,
                        morHarVerge = true
                )
        Assertions.assertEquals(FiltreringsreglerResultat.MOR_ER_DØD, filtreringsResultat)
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

        val filtreringsResultat =
                evaluerFiltreringsregler(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person, barn2Person),
                        restenAvBarna = listOf(barn3PersonInfo),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false
                )

        Assertions.assertEquals(FiltreringsreglerResultat.GODKJENT, filtreringsResultat)
    }

    @Test
    fun `Mor har ugyldig fødselsnummer`() {
        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("23456789111"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("21111777001"))
        val barn3PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val filtreringsResultat =
                evaluerFiltreringsregler(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(barn3PersonInfo),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false
                )

        Assertions.assertEquals(FiltreringsreglerResultat.MOR_IKKE_GYLDIG_FNR, filtreringsResultat)
    }

    @Test
    fun `Barn med ugyldig fødselsnummer`() {
        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), personIdent = PersonIdent("23102000000"))
        val barn2Person =
                tilfeldigPerson(fødselsdato = LocalDate.parse("2018-09-23"), personIdent = PersonIdent("23091823456"))

        val filtreringsResultat =
                evaluerFiltreringsregler(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person, barn2Person),
                        restenAvBarna = listOf(),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false
                )
        Assertions.assertEquals(FiltreringsreglerResultat.BARN_IKKE_GYLDIG_FNR, filtreringsResultat)
    }

    @Test
    fun `Saken krever etterbetaling pga det er lenge siden fødsel for et barn`() {
        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = LocalDate.parse("2018-09-23"), personIdent = PersonIdent("23091823456"))

        val filtreringsResultat =
                evaluerFiltreringsregler(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false
                )
        Assertions.assertEquals(FiltreringsreglerResultat.KREVER_ETTERBETALING, filtreringsResultat)
    }

    @Test
    fun `Saken krever etterbetaling pga barnet er født forrige måned når dato dagensdato er 22 i måneden`() {
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns LocalDate.parse("2020-03-22")
        val fødselsdatoForrigeMåned = LocalDate.parse("2020-04-29")

        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = fødselsdatoForrigeMåned, personIdent = PersonIdent("23091823456"))

        val filtreringsResultat =
                evaluerFiltreringsregler(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false
                )
        Assertions.assertEquals(FiltreringsreglerResultat.KREVER_ETTERBETALING, filtreringsResultat)
    }


    @Test
    fun `Saken er godkjent selv om barnet er født forrige måned fordi dato dagensdato er 15 i måneden`() {
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns LocalDate.parse("2020-03-15")
        val fødselsdatoForrigeMåned = LocalDate.parse("2020-02-28")

        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = fødselsdatoForrigeMåned, personIdent = PersonIdent("23091823456"))

        val filtreringsResultat =
                evaluerFiltreringsregler(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false
                )
        Assertions.assertEquals(FiltreringsreglerResultat.GODKJENT, filtreringsResultat)
    }

    @Test
    fun `Saken er godkjent fordi barnet er født i denne måneden`() {
        val fødselsdatoIDenneMåned = LocalDate.now().withDayOfMonth(1)

        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = fødselsdatoIDenneMåned, personIdent = PersonIdent("23091823456"))

        val filtreringsResultat =
                evaluerFiltreringsregler(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person),
                        restenAvBarna = listOf(),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false
                )
        Assertions.assertEquals(FiltreringsreglerResultat.GODKJENT, filtreringsResultat)
    }

    @Test
    fun `Saken krever etterbetaling pga én tvilling er født forrige måned og én denne måned når dato dagensdato er 22 i måneden`() {
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns LocalDate.parse("2020-03-22")
        val fødselsdatoForrigeMåned = LocalDate.parse("2020-04-29")
        val fødselsdatoinnenforDenneMåned = LocalDate.parse("2020-03-01")

        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = fødselsdatoForrigeMåned, personIdent = PersonIdent("23091823456"))
        val barn2Person =
                tilfeldigPerson(fødselsdato = fødselsdatoinnenforDenneMåned, personIdent = PersonIdent("23091823456"))

        val filtreringsResultat =
                evaluerFiltreringsregler(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person, barn2Person),
                        restenAvBarna = listOf(),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false
                )
        Assertions.assertEquals(FiltreringsreglerResultat.KREVER_ETTERBETALING, filtreringsResultat)
    }

    @Test
    fun `Saken krever etterbetaling pga begge tvillingene er født forrige måned når dato dagensdato er 22 i måneden`() {
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns LocalDate.parse("2020-03-22")
        val fødselsdatoForrigeMåned = LocalDate.parse("2020-04-29")

        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = fødselsdatoForrigeMåned, personIdent = PersonIdent("23091823456"))
        val barn2Person =
                tilfeldigPerson(fødselsdato = fødselsdatoForrigeMåned, personIdent = PersonIdent("23091823456"))

        val filtreringsResultat =
                evaluerFiltreringsregler(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person, barn2Person),
                        restenAvBarna = listOf(),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false
                )
        Assertions.assertEquals(FiltreringsreglerResultat.KREVER_ETTERBETALING, filtreringsResultat)
    }

    @Test
    fun `Saken er godkjent selv om én tvilling er født forrige måned fordi dato dagensdato er 15 i måneden`() {
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns LocalDate.parse("2020-03-15")
        val fødselsdatoForrigeMåned = LocalDate.parse("2020-02-28")
        val fødselsdatoinnenforDenneMåned = LocalDate.parse("2020-03-01")

        val søkerPerson =
                tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), personIdent = PersonIdent("04086226621"))
        val barn1Person =
                tilfeldigPerson(fødselsdato = fødselsdatoForrigeMåned, personIdent = PersonIdent("23091823456"))
        val barn2Person =
                tilfeldigPerson(fødselsdato = fødselsdatoinnenforDenneMåned, personIdent = PersonIdent("23091823456"))

        val filtreringsResultat =
                evaluerFiltreringsregler(
                        mor = søkerPerson,
                        barnaFraHendelse = listOf(barn1Person, barn2Person),
                        restenAvBarna = listOf(),
                        morLever = true,
                        barnaLever = true,
                        morHarVerge = false
                )
        Assertions.assertEquals(FiltreringsreglerResultat.GODKJENT, filtreringsResultat)
    }
}