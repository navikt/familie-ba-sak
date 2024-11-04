package no.nav.familie.ba.sak.cucumber.domeneparser

import io.cucumber.datatable.DataTable

interface Domenenøkkel {
    val nøkkel: String
}

enum class Domenebegrep(
    override val nøkkel: String,
) : Domenenøkkel {
    ID("Id"),
    FAGSAK_ID("FagsakId"),
    FAGSAK_TYPE("Fagsaktype"),
    STATUS("Status"),
    BEHANDLING_ID("BehandlingId"),
    FORRIGE_BEHANDLING_ID("ForrigeBehandlingId"),
    FRA_DATO("Fra dato"),
    TIL_DATO("Til dato"),
    FRA_MÅNED("Fra måned"),
    TIL_MÅNED("Til måned"),
    ENDRET_MIGRERINGSDATO("Endret migreringsdato"),
    ENDRET_TIDSPUNKT("Endret tidspunkt"),
    BEHANDLINGSÅRSAK("Behandlingsårsak"),
    BEHANDLINGSRESULTAT("Behandlingsresultat"),
    SKAL_BEHANLDES_AUTOMATISK("Skal behandles automatisk"),
    SØKNADSTIDSPUNKT("Søknadstidspunkt"),
    BEHANDLINGSKATEGORI("Behandlingskategori"),
    BEHANDLINGSSTATUS("Behandlingsstatus"),
    BEHANDLINGSTYPE("Behandlingstype"),
    BEHANDLINGSSTEG("Behandlingssteg"),
    UNDERKATEGORI("Underkategori"),
    FEATURE_TOGGLE_ID("FeatureToggleId"),
    ER_FEATURE_TOGGLE_TOGGLET_PÅ("Er togglet på"),
}

object DomeneparserUtil {
    fun DataTable.groupByBehandlingId(): Map<Long, List<Map<String, String>>> =
        this.asMaps().groupBy { rad -> parseLong(Domenebegrep.BEHANDLING_ID, rad) }
}
