package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.IntegrasjonClientMock
import no.nav.familie.ba.sak.config.IntegrasjonClientMock.Companion.FOM_1990
import no.nav.familie.ba.sak.config.IntegrasjonClientMock.Companion.FOM_2008
import no.nav.familie.ba.sak.config.IntegrasjonClientMock.Companion.TOM_2010
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.sisteStatsborgerskap
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class StatsborgerskapServiceTest {

    private val integrasjonClient = mockk<IntegrasjonClient>()

    private val personopplysningerService = mockk<PersonopplysningerService>()

    private lateinit var statsborgerskapService: StatsborgerskapService

    @BeforeEach
    fun setUp() {
        statsborgerskapService = StatsborgerskapService(integrasjonClient, personopplysningerService)
        IntegrasjonClientMock.initEuKodeverk(integrasjonClient)
    }

    @Test
    fun `Skal returnere siste statsborgerskap`() {
        val statsborgerskapMedGyldigFom = listOf(
            Statsborgerskap(
                "POL",
                bekreftelsesdato = null,
                gyldigFraOgMed = FOM_1990,
                gyldigTilOgMed = TOM_2010
            ),
            Statsborgerskap(
                "DNK",
                bekreftelsesdato = null,
                gyldigFraOgMed = FOM_2008,
                gyldigTilOgMed = null
            )
        )

        assertEquals("DNK", statsborgerskapMedGyldigFom.sisteStatsborgerskap()?.land)

        val statsborgerskapMedBekreftelsesdato = listOf(
            Statsborgerskap(
                "POL",
                bekreftelsesdato = FOM_1990,
                gyldigFraOgMed = null,
                gyldigTilOgMed = TOM_2010
            ),
            Statsborgerskap(
                "DNK",
                bekreftelsesdato = FOM_2008,
                gyldigFraOgMed = null,
                gyldigTilOgMed = null
            )
        )

        assertEquals("DNK", statsborgerskapMedBekreftelsesdato.sisteStatsborgerskap()?.land)

        val statsborgerskapUtenGyldigFomEllerBekreftelsesdato = listOf(
            Statsborgerskap(
                "POL",
                bekreftelsesdato = null,
                gyldigFraOgMed = null,
                gyldigTilOgMed = TOM_2010
            ),
            Statsborgerskap(
                "DNK",
                bekreftelsesdato = null,
                gyldigFraOgMed = null,
                gyldigTilOgMed = null
            )
        )

        assertThrows<Feil> { statsborgerskapUtenGyldigFomEllerBekreftelsesdato.sisteStatsborgerskap() }
    }
}
