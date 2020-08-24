package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class MalerServiceTest {

    @Test
    fun `Skal returnere malnavn innvilget-tredjelandsborger for medlemskap TREDJELANDSBORGER og resultat INNVILGET`() {

        val malNavn = MalerService.malNavnForMedlemskapOgResultatType(Medlemskap.TREDJELANDSBORGER,
                                                                     BehandlingResultatType.INNVILGET)

        assertEquals(malNavn, "innvilget-tredjelandsborger")
    }

    @Test
    fun `Skal returnere malnavn innvilget for medlemskap NORDEN og resultat INNVILGET`() {

        val malNavn = MalerService.malNavnForMedlemskapOgResultatType(Medlemskap.NORDEN,
                                                                     BehandlingResultatType.INNVILGET)

        assertEquals(malNavn, "innvilget")
    }

    @Test
    fun `Skal returnere malnavn innvilget for resultat INNVILGET når medlemskap er null`() {
        val malNavn = MalerService.malNavnForMedlemskapOgResultatType(null,
                                                                     BehandlingResultatType.INNVILGET)

        assertEquals(malNavn, "innvilget")
    }
}