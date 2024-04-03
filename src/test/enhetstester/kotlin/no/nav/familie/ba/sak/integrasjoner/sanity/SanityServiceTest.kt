package no.nav.familie.ba.sak.integrasjoner.sanity

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SanityServiceTest {
    @MockK
    private lateinit var sanityKlient: SanityKlient

    @MockK
    private lateinit var envService: EnvService

    @InjectMockKs
    private lateinit var sanityService: SanityService

    @Test
    fun `hentSanityEØSBegrunnelser - skal ikke filtrere bort nye begrunnelser tilknyttet EØS praksisendring`() {
        every { sanityKlient.hentEØSBegrunnelser() } returns
            EØSStandardbegrunnelse.values().map {
                SanityEØSBegrunnelse(
                    apiNavn = it.sanityApiNavn,
                    navnISystem = it.name,
                    fagsakType = null,
                    periodeType = null,
                    tema = null,
                    vilkår = emptySet(),
                    annenForeldersAktivitet = emptyList(),
                    barnetsBostedsland = emptyList(),
                    kompetanseResultat = emptyList(),
                    hjemler = emptyList(),
                    hjemlerFolketrygdloven = emptyList(),
                    hjemlerEØSForordningen883 = emptyList(),
                    hjemlerEØSForordningen987 = emptyList(),
                    hjemlerSeperasjonsavtalenStorbritannina = emptyList(),
                    valgbarhet = null,
                    øvrigeTriggere = emptyList(),
                )
            }

        val eøsBegrunnelser = sanityService.hentSanityEØSBegrunnelser()

        assertThat(eøsBegrunnelser.keys).isEqualTo(EØSStandardbegrunnelse.entries.toSet())
    }
}
