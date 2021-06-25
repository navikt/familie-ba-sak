package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import io.mockk.mockk
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FiltreringIAutomatiskBehandling
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FiltreringIAutomatiskBehandlingTest {
    val mockPdlRestClient: PdlRestClient = mockk(relaxed = true)

    @Test
    fun `Mor er under 18`() {
        val søkerPerson = tilfeldigSøker(fødselsdato = LocalDate.parse("2020-10-23"))
        val barn1Person = tilfeldigPerson(fødselsdato = LocalDate.parse("2019-10-23"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2020-09-23"))
        val filtrering =
            FiltreringIAutomatiskBehandling(søkerPerson, listOf(barn1Person), listOf(barn2PersonInfo), true, true, true)
        val (evaluering, begrunnelse) = filtrering.evaluerData()
        assert(!evaluering && begrunnelse == "Fødselshendelse: Mor under 18 år") { "Mottatt begrunnelse: $begrunnelse" }
    }

    @Test
    fun `Barn med mindre mellomrom enn 5mnd`() {
        val søkerPerson = tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"))
        val barn1Person = tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2020-09-23"))

        val filtrering =
            FiltreringIAutomatiskBehandling(søkerPerson, listOf(barn1Person), listOf(barn2PersonInfo), true, true, true)
        val (evaluering, begrunnelse) = filtrering.evaluerData()
        assert(!evaluering && begrunnelse == "Fødselshendelse: Mor har barn med mindre enn fem måneders mellomrom") { "Mottatt begrunnelse: $begrunnelse" }
    }

    @Test
    fun `Mor lever ikke`() {
        val søkerPerson = tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"))
        val barn1Person = tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val filtrering =
            FiltreringIAutomatiskBehandling(
                søkerPerson,
                listOf(barn1Person),
                listOf(barn2PersonInfo),
                false,
                true,
                true
            )
        val (evaluering, begrunnelse) = filtrering.evaluerData()

        assert(!evaluering && begrunnelse == "Fødselshendelse: Registrert dødsdato på mor") { "Mottatt begrunnelse: $begrunnelse" }
    }

    @Test
    fun `Barnet lever ikke`() {
        val søkerPerson = tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"))
        val barn1Person = tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val filtrering =
            FiltreringIAutomatiskBehandling(
                søkerPerson,
                listOf(barn1Person),
                listOf(barn2PersonInfo),
                true,
                false,
                true
            )
        val (evaluering, begrunnelse) = filtrering.evaluerData()

        assert(!evaluering && begrunnelse == "Fødselshendelse: Registrert dødsdato på barnet") { "Mottatt begrunnelse: $begrunnelse" }
    }

    @Test
    fun `Mor har verge`() {
        val søkerPerson = tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"))
        val barn1Person = tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val filtrering =
            FiltreringIAutomatiskBehandling(
                søkerPerson,
                listOf(barn1Person),
                listOf(barn2PersonInfo),
                true,
                true,
                false
            )
        val (evaluering, begrunnelse) = filtrering.evaluerData()

        assert(!evaluering && begrunnelse == "Fødselshendelse: Mor er umyndig") { "Mottatt begrunnelse: $begrunnelse" }
    }

    @Test
    fun `Mor er død og er umyndig`() {
        val søkerPerson = tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"))
        val barn1Person = tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val filtrering =
            FiltreringIAutomatiskBehandling(
                søkerPerson,
                listOf(barn1Person),
                listOf(barn2PersonInfo),
                false,
                true,
                false
            )
        val (evaluering, begrunnelse) = filtrering.evaluerData()

        assert(!evaluering && begrunnelse == "Fødselshendelse: Registrert dødsdato på mor") { "Mottatt begrunnelse: $begrunnelse" }
    }

    @Test
    fun `Flere barn født`() {
        val søkerPerson = tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"))
        val barn1Person = tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"))
        val barn2Person = tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"))
        val barn3PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val filtrering =
            FiltreringIAutomatiskBehandling(
                søkerPerson,
                listOf(barn1Person, barn2Person),
                listOf(barn3PersonInfo),
                true,
                true,
                true
            )
        val (evaluering, begrunnelse) = filtrering.evaluerData()

        assert(evaluering && begrunnelse == "Blir sendt til BA-SAK") { "Mottatt begrunnelse: $begrunnelse" }
    }
}