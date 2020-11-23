package no.nav.familie.ba.sak.dokument.domene

data class MalMedData(
        val mal: String,
        val fletteFelter: String
)

enum class BrevType(val malId: String, val arkivType: String, val visningsTekst: String) {
    INNHENTE_OPPLYSNINGER("innhente-opplysninger", "BARNETRYGD_INNHENTE_OPPLYSNINGER", "innhenting av opplysninger"),
    VARSEL_OM_REVURDERING("varsel-om-revurdering", "BARNETRYGD_VARSEL_OM_REVURDERING", "varsel om revurdering"),
    VEDTAK("vedtak", "BARNETRYGD_VEDTAK", "vedtak"),
    HENLEGGE_TRUKKET_SØKNAD("henlegge-trukket-søknad", "BARNETRYGD_HENLEGGE_TRUKKET_SØKNAD", "henlegg trukket søknad");

    override fun toString(): String {
        return visningsTekst
    }
}