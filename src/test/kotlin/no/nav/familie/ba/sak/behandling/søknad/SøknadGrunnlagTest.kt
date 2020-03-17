package no.nav.familie.ba.sak.behandling.søknad

import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.*
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

    fun lagSøknadDTO(behandlingId: Long, annenPartIdent: String): SøknadDTO {
        val søkerIdent = randomFnr()
        val barnIdent = randomFnr()

        return SøknadDTO(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                annenPartIdent = annenPartIdent,
                søkerMedOpplysninger = PartMedOpplysninger(
                        ident = søkerIdent,
                        personType = PersonType.SØKER,
                        opphold = Opphold()
                ),
                barnaMedOpplysninger = listOf(PartMedOpplysninger(
                        ident = barnIdent,
                        personType = PersonType.BARN,
                        opphold = Opphold()
                ))
        )
    }


    @Test
    fun `Skal lagre ned og hente søknadsgrunnlag`() {
        val behandlingId = 1L
        val annenPartIdent = randomFnr()
        val søknadDTO = lagSøknadDTO(behandlingId, annenPartIdent)
        søknadGrunnlagService.lagreOgDeaktiverGammel(SøknadGrunnlag(
                behandlingId = behandlingId,
                søknad = søknadDTO.writeValueAsString()
        ))

        val søknadGrunnlag = søknadGrunnlagService.hent(behandlingId)
        Assertions.assertNotNull(søknadGrunnlag)
        Assertions.assertEquals(behandlingId, søknadGrunnlag.behandlingId)
        Assertions.assertEquals(true, søknadGrunnlag.aktiv)

        Assertions.assertEquals(annenPartIdent, søknadGrunnlag.hentSøknadDto().annenPartIdent)
    }

    @Test
    fun `Skal sjekke at det kun kan være et aktivt grunnlag for en behandling`() {
        val behandlingId = 2L
        val annenPartIdent = randomFnr()
        val søknadDTO = lagSøknadDTO(behandlingId, annenPartIdent)
        val annenPartIdent2 = randomFnr()
        val søknadDTO2 = lagSøknadDTO(behandlingId, annenPartIdent2)

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
        Assertions.assertEquals(annenPartIdent2, aktivSøknadGrunnlag?.hentSøknadDto()?.annenPartIdent)
    }
}