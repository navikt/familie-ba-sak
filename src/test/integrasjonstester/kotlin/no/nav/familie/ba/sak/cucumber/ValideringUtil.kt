package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import no.nav.familie.ba.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.ba.sak.cucumber.domeneparser.parseLong

object ValideringUtil {

    fun assertSjekkBehandlingIder(dataTable: DataTable, eksisterendeBehandlingId: Set<Long>) {
        val forventedeBehandlingId = dataTable.asMaps().map { parseLong(Domenebegrep.BEHANDLING_ID, it) }.toSet()
        val ukontrollerteBehandlingId = eksisterendeBehandlingId.filterNot { forventedeBehandlingId.contains(it) }
        if (ukontrollerteBehandlingId.isNotEmpty()) {
            error("Har ikke kontrollert behandlingene:$ukontrollerteBehandlingId")
        }
    }
}
