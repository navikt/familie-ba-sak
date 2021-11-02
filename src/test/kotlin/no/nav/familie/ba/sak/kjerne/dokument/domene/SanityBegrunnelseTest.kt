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

    @Test
    fun `skal konverdere string til enumverdi dersom det finnes og null ellers`() {
        Assertions.assertEquals(null, finnEnumverdi("IKKE_GYLDIG_VERDI", ØvrigTrigger.values(), ""))
        Assertions.assertEquals(
            ØvrigTrigger.BARN_MED_6_ÅRS_DAG,
            finnEnumverdi("BARN_MED_6_ÅRS_DAG", ØvrigTrigger.values(), "")
        )
    }
}
