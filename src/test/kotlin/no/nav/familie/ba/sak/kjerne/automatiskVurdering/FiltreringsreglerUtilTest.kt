package no.nav.familie.ba.sak.kjerne.automatiskVurdering

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
        assert(filtreringsResultat == FiltreringsreglerResultat.MOR_HAR_VERGE)
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
        assert(filtreringsResultat == FiltreringsreglerResultat.MOR_ER_DØD)
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

        assert(filtreringsResultat == FiltreringsreglerResultat.GODKJENT)
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

        assert(filtreringsResultat == FiltreringsreglerResultat.MOR_IKKE_GYLDIG_FNR)
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
        assert(filtreringsResultat == FiltreringsreglerResultat.BARN_IKKE_GYLDIG_FNR)
    }

    @Test
    fun `Saken krever etterbetaling`() {
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
        assert(filtreringsResultat == FiltreringsreglerResultat.KREVER_ETTERBETALING)
    }
}