package no.nav.familie.ba.sak.behandling.søknad

import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlag
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.writeValueAsString
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.randomFnr
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("dev")
class SøknadGrunnlagTest(
        @Autowired
        private val søknadGrunnlagService: SøknadGrunnlagService
) {

    @Test
    fun `Skal lagre ned og hente søknadsgrunnlag`() {
        val behandlingId = 1L
        val søkerIdent = randomFnr()
        val barnIdent = randomFnr()
        val søknadDTO = lagSøknadDTO(søkerIdent = søkerIdent, barnasIdenter = listOf(barnIdent))
        søknadGrunnlagService.lagreOgDeaktiverGammel(SøknadGrunnlag(
                behandlingId = behandlingId,
                søknad = søknadDTO.writeValueAsString()
        ))

        val søknadGrunnlag = søknadGrunnlagService.hentAktiv(behandlingId)
        Assertions.assertNotNull(søknadGrunnlag)
        Assertions.assertEquals(behandlingId, søknadGrunnlag?.behandlingId)
        Assertions.assertEquals(true, søknadGrunnlag?.aktiv)
        Assertions.assertEquals(søkerIdent, søknadGrunnlag?.hentSøknadDtoGammel()?.søkerMedOpplysninger?.ident)
    }

    @Test
    fun `Skal sjekke at det kun kan være et aktivt grunnlag for en behandling`() {
        val behandlingId = 2L
        val søkerIdent = randomFnr()
        val annenPartIdent = randomFnr()
        val barnIdent = randomFnr()
        val søknadDTO = lagSøknadDTO(søkerIdent = søkerIdent, barnasIdenter = listOf(barnIdent))

        val søkerIdent2 = randomFnr()
        val annenPartIdent2 = randomFnr()
        val barnIdent2 = randomFnr()
        val søknadDTO2 = lagSøknadDTO(søkerIdent = søkerIdent2, barnasIdenter = listOf(barnIdent2))

        søknadGrunnlagService.lagreOgDeaktiverGammel(SøknadGrunnlag(
                behandlingId = behandlingId,
                søknad = søknadDTO.writeValueAsString()
        ))

        søknadGrunnlagService.lagreOgDeaktiverGammel(SøknadGrunnlag(
                behandlingId = behandlingId,
                søknad = søknadDTO2.writeValueAsString()
        ))
        val søknadsGrunnlag = søknadGrunnlagService.hentAlle(behandlingId)
        Assertions.assertEquals(2, søknadsGrunnlag.size)

        val aktivSøknadGrunnlag = søknadGrunnlagService.hentAktiv(behandlingId)
        Assertions.assertNotNull(aktivSøknadGrunnlag)
    }
}