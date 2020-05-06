package no.nav.familie.ba.sak.dokument

import org.junit.jupiter.api.Assertions.*

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.TypeSøker

import org.junit.jupiter.api.Test


class MalerServiceTest {

    @Test
    fun `Skal returnere malnavn Innvilget-Tredjelandsborger for typeSøker TREDJELANDSBORGER og resultat INNVILGET`() {

        val malNavn = MalerService.malNavnForTypeSøkerOgResultatType(TypeSøker.TREDJELANDSBORGER,
                                                                     BehandlingResultatType.INNVILGET)

        assertEquals(malNavn, "Innvilget-Tredjelandsborger")
    }

    @Test
    fun `Skal returnere malnavn Innvilget for typeSøker ORDINÆR og resultat INNVILGET`() {

        val malNavn = MalerService.malNavnForTypeSøkerOgResultatType(TypeSøker.ORDINÆR,
            BehandlingResultatType.INNVILGET)

        assertEquals(malNavn, "Innvilget")
    }

    @Test
    fun `Skal returnere malnavn Innvilget for resultat INNVILGET når typeSøker er null`() {
        val malNavn = MalerService.malNavnForTypeSøkerOgResultatType(null,
            BehandlingResultatType.INNVILGET)

        assertEquals(malNavn, "Innvilget")
    }
}