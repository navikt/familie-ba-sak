package no.nav.familie.ba.sak.kjerne.dokument.domene

import no.nav.familie.ba.sak.common.lagRestSanityBegrunnelse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SanityBegrunnelseTest {
    @Test
    fun `skal fjerne ugyldige enumverdier`() {
        val restSanityBegrunnelse = lagRestSanityBegrunnelse(
            ovrigeTriggere = listOf(
                ØvrigTrigger.BARN_MED_6_ÅRS_DAG.name,
                "IKKE_GYLDIG_ØVRIG_TRIGGER"
            )
        )
        Assertions.assertEquals(
            listOf(
                ØvrigTrigger.BARN_MED_6_ÅRS_DAG,
            ),
            restSanityBegrunnelse.tilSanityBegrunnelse().ovrigeTriggere?.toList()
        )
    }
}
