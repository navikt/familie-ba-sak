package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FILTRERINGSREGLER_SØKNAD
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class MetrikkerTest {
    @Test
    fun `skal ikke kaste feil ved oppdatering av metrikker for alle utfall av filtreringsreglene`() {
        // Arrange
        val metrikker = Metrikker()
        val evalueringer =
            Resultat.entries.flatMap { resultat ->
                FILTRERINGSREGLER_SØKNAD.map {
                    Evaluering(
                        resultat = resultat,
                        evalueringÅrsaker = emptyList(),
                        begrunnelse = "",
                        identifikator = it.identifikator.name,
                    )
                }
            }

        // Act & Assert
        assertDoesNotThrow { metrikker.oppdaterMetrikker(evalueringer) }
    }
}
