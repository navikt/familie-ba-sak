package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.junit.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.net.URI

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
}
