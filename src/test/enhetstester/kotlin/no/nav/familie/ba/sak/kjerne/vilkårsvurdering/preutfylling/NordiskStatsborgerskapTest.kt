package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestKlient
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.kontrakter.felles.personopplysning.Folkeregistermetadata
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.net.URI
import java.time.LocalDate

class NordiskStatsborgerskapTest {
    @Test
    fun `Duplikate statsborgerskap fra PDL skal filtreres bort før vi lager tidslinje`() {
        val pdlRestKlient = PdlRestKlient(URI("test"), mockk(), mockk())
        val pdlRestKlientSpyk = spyk(pdlRestKlient)
        val aktør = lagAktør()
        val personResultat = mockk<PersonResultat>()

        val statsborgerskap1 =
            Statsborgerskap(
                land = "UKR",
                gyldigFraOgMed = null,
                gyldigTilOgMed = null,
                bekreftelsesdato = null,
            )

        val statsborgerskap2 =
            Statsborgerskap(
                land = "UKR",
                gyldigFraOgMed = null,
                gyldigTilOgMed = null,
                bekreftelsesdato = null,
            )

        every { pdlRestKlientSpyk.hentStatsborgerskap(any(), any()) } returns listOf(statsborgerskap1, statsborgerskap2)
        every { personResultat.aktør } returns aktør

        assertDoesNotThrow {
            pdlRestKlientSpyk.lagErNordiskStatsborgerTidslinje(personResultat)
        }
    }

    @Test
    fun `Statsborgerskap med null i fom og tom skal fjernes dersom det eksisteres andre innslag med dato`() {
        val pdlRestKlient = PdlRestKlient(URI("test"), mockk(), mockk())
        val pdlRestKlientSpyk = spyk(pdlRestKlient)
        val aktør = lagAktør()
        val personResultat = mockk<PersonResultat>()

        val statsborgerskap1 =
            Statsborgerskap(
                land = "NOR",
                gyldigFraOgMed = null,
                gyldigTilOgMed = null,
                bekreftelsesdato = null,
            )

        val statsborgerskap2 =
            Statsborgerskap(
                land = "NOR",
                gyldigFraOgMed = LocalDate.of(2020, 1, 1),
                gyldigTilOgMed = null,
                bekreftelsesdato = null,
            )

        every { pdlRestKlientSpyk.hentStatsborgerskap(any(), any()) } returns listOf(statsborgerskap1, statsborgerskap2)
        every { personResultat.aktør } returns aktør

        val statsborgerskapTidslinje = pdlRestKlientSpyk.lagErNordiskStatsborgerTidslinje(personResultat)
        assertThat(statsborgerskapTidslinje.tilPerioder().single().fom).isEqualTo(LocalDate.of(2020, 1, 1))
    }

    @Test
    fun `Statsborgerskap med opphørstidspunkt skal filtreres bort `() {
        val pdlRestKlient = PdlRestKlient(URI("test"), mockk(), mockk())
        val pdlRestKlientSpyk = spyk(pdlRestKlient)
        val aktør = lagAktør()
        val personResultat = mockk<PersonResultat>()

        val gyldigFraOgMed = LocalDate.now().minusYears(5)

        val statsborgerskap1 =
            Statsborgerskap(
                land = "NOR",
                gyldigFraOgMed = gyldigFraOgMed,
                gyldigTilOgMed = null,
                bekreftelsesdato = null,
                folkeregistermetadata = Folkeregistermetadata(opphoerstidspunkt = null, kilde = null, aarsak = null),
            )

        val statsborgerskap2 =
            Statsborgerskap(
                land = "AL",
                gyldigFraOgMed = gyldigFraOgMed,
                gyldigTilOgMed = null,
                bekreftelsesdato = null,
                folkeregistermetadata = Folkeregistermetadata(opphoerstidspunkt = LocalDate.now().minusYears(1), kilde = null, aarsak = null),
            )

        every { pdlRestKlientSpyk.hentStatsborgerskap(any(), any()) } returns listOf(statsborgerskap1, statsborgerskap2)
        every { personResultat.aktør } returns aktør

        val statsborgerskapTidslinje = pdlRestKlientSpyk.lagErNordiskStatsborgerTidslinje(personResultat)
        assertThat(statsborgerskapTidslinje.tilPerioder().single().fom).isEqualTo(gyldigFraOgMed)
    }
}
