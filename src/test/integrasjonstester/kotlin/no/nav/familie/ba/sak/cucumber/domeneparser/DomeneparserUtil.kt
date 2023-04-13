package no.nav.familie.ba.sak.cucumber.domeneparser

import io.cucumber.datatable.DataTable

interface Domenenøkkel {
    val nøkkel: String
}

enum class Domenebegrep(override val nøkkel: String) : Domenenøkkel {
    BEHANDLING_ID("BehandlingId"),
    FRA_DATO("Fra dato"),
    TIL_DATO("Til dato"),
}

object DomeneparserUtil {
    fun DataTable.groupByBehandlingId() =
        this.asMaps().groupBy {
            it.getValue(Domenebegrep.BEHANDLING_ID.nøkkel).toLong()
        }
}
