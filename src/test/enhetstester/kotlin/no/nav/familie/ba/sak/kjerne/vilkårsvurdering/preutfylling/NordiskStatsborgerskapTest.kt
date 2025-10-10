package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.net.URI
import java.time.LocalDate

class NordiskStatsborgerskapTest {
    @Test
    fun `Duplikate statsborgerskap fra PDL skal filtreres bort før vi lager tidslinje`() {
        val pdlRestClient = PdlRestClient(URI("test"), mockk(), mockk())
        val pdlRestClientSpyk = spyk(pdlRestClient)
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

        every { pdlRestClientSpyk.hentStatsborgerskap(any(), any()) } returns listOf(statsborgerskap1, statsborgerskap2)
        every { personResultat.aktør } returns aktør

        assertDoesNotThrow {
            pdlRestClientSpyk.lagErNordiskStatsborgerTidslinje(personResultat)
        }
    }

    @Test
    fun `Statsborgerskap med null i fom og tom skal fjernes dersom det eksisteres andre innslag med dato`() {
        val pdlRestClient = PdlRestClient(URI("test"), mockk(), mockk())
        val pdlRestClientSpyk = spyk(pdlRestClient)
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

        every { pdlRestClientSpyk.hentStatsborgerskap(any(), any()) } returns listOf(statsborgerskap1, statsborgerskap2)
        every { personResultat.aktør } returns aktør

        val statsborgerskapTidslinje = pdlRestClientSpyk.lagErNordiskStatsborgerTidslinje(personResultat)
        assertThat(statsborgerskapTidslinje.tilPerioder().single().fom).isEqualTo(LocalDate.of(2020, 1, 1))
    }
}
