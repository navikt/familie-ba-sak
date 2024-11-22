package no.nav.familie.ba.sak.integrasjoner.sanity

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SanityServiceTest {
    @MockK
    private lateinit var sanityKlient: SanityKlient

    @InjectMockKs
    private lateinit var sanityService: SanityService

    @Test
    fun `hentSanityBegrunnelser - skal hente standardbegrunnelser`() {
        every { sanityKlient.hentBegrunnelser() } returns
            Standardbegrunnelse.entries.map {
                SanityBegrunnelse(
                    apiNavn = it.sanityApiNavn,
                    navnISystem = it.name,
                    fagsakType = null,
                    periodeType = null,
                    tema = null,
                    vilkår = emptySet(),
                    hjemler = emptyList(),
                    hjemlerFolketrygdloven = emptyList(),
                    valgbarhet = null,
                    øvrigeTriggere = emptyList(),
                )
            }

        val begrunnelser = sanityService.hentSanityBegrunnelser()

        assertThat(begrunnelser.keys).isEqualTo(Standardbegrunnelse.entries.toSet())
    }

    @Test
    fun `hentSanityBegrunnelser - skal kaste feil hvis det ikke er noen cache`() {
        every { sanityKlient.hentBegrunnelser() } throws RuntimeException("Feil ved henting av begrunnelser i test")

        val e = assertThrows<RuntimeException> { sanityService.hentSanityBegrunnelser() }

        assertThat(e.message).isEqualTo("Feil ved henting av begrunnelser i test")
    }

    @Test
    fun `hentSanityBegrunnelser - skal kaste bruke cachet versjon hvis det feiler`() {
        every { sanityKlient.hentBegrunnelser() } returns
            Standardbegrunnelse.entries.map {
                SanityBegrunnelse(
                    apiNavn = it.sanityApiNavn,
                    navnISystem = it.name,
                    fagsakType = null,
                    periodeType = null,
                    tema = null,
                    vilkår = emptySet(),
                    hjemler = emptyList(),
                    hjemlerFolketrygdloven = emptyList(),
                    valgbarhet = null,
                    øvrigeTriggere = emptyList(),
                )
            } andThenThrows RuntimeException("Feil får å teste cachet versjon")

        sanityService.hentSanityBegrunnelser().also { begrunnelser ->
            assertThat(begrunnelser.keys).isEqualTo(Standardbegrunnelse.entries.toSet())
        }

        sanityService.hentSanityBegrunnelser().also { begrunnelser ->
            assertThat(begrunnelser.keys).isEqualTo(Standardbegrunnelse.entries.toSet())
        }
    }

    @Test
    fun `hentSanityEØSBegrunnelser - skal ikke filtrere bort nye begrunnelser tilknyttet EØS praksisendring`() {
        every { sanityKlient.hentEØSBegrunnelser() } returns
            EØSStandardbegrunnelse.entries.map {
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

    @Test
    fun `hentSanityEØSBegrunnelser - skal kaste feil hvis det ikke er noen cache`() {
        every { sanityKlient.hentEØSBegrunnelser() } throws RuntimeException("Feil ved henting av EØS-begrunnelser i test")

        val e = assertThrows<RuntimeException> { sanityService.hentSanityEØSBegrunnelser() }

        assertThat(e.message).isEqualTo("Feil ved henting av EØS-begrunnelser i test")
    }

    @Test
    fun `hentSanityEØSBegrunnelser - skal kaste bruke cachet versjon hvis det feiler`() {
        every { sanityKlient.hentEØSBegrunnelser() } returns
            EØSStandardbegrunnelse.entries.map {
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
            } andThenThrows RuntimeException("Feil får å teste cachet versjon")

        sanityService.hentSanityEØSBegrunnelser().also { eøsBegrunnelser ->
            assertThat(eøsBegrunnelser).isNotEmpty()
        }

        sanityService.hentSanityEØSBegrunnelser().also { eøsBegrunnelser ->
            assertThat(eøsBegrunnelser).isNotEmpty()
        }
    }
}
