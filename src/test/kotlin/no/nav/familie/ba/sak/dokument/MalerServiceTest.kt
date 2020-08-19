package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class MalerServiceTest {

    @Test
    fun `Skal returnere malnavn Innvilget-Tredjelandsborger for medlemskap TREDJELANDSBORGER og resultat INNVILGET`() {

        val malNavn = MalerService.malNavnForMedlemskapOgResultatType(Medlemskap.TREDJELANDSBORGER,
                                                                     BehandlingResultatType.INNVILGET)

        assertEquals(malNavn, "Innvilget-Tredjelandsborger")
    }

    @Test
    fun `Skal returnere malnavn Innvilget for medlemskap NORDEN og resultat INNVILGET`() {

        val malNavn = MalerService.malNavnForMedlemskapOgResultatType(Medlemskap.NORDEN,
                                                                     BehandlingResultatType.INNVILGET)

        assertEquals(malNavn, "Innvilget")
    }

    @Test
    fun `Skal returnere malnavn Innvilget for resultat INNVILGET når medlemskap er null`() {
        val malNavn = MalerService.malNavnForMedlemskapOgResultatType(null,
                                                                     BehandlingResultatType.INNVILGET)

        assertEquals(malNavn, "Innvilget")
    }
}