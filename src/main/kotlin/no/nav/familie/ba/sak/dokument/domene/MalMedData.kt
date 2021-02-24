package no.nav.familie.ba.sak.dokument.domene

@Deprecated("Data tilhørende gammel brevløsning. Migrering til ny brevløsning i pakken brev")
data class MalMedData(
        val mal: String,
        val fletteFelter: String
)

@Deprecated("Migrering til ny brevløsning i pakken brev")
enum class BrevType(val malId: String, val arkivType: String, val visningsTekst: String, val genererForside: Boolean) {

    INNHENTE_OPPLYSNINGER("innhente-opplysninger", "BARNETRYGD_INNHENTE_OPPLYSNINGER", "innhenting av opplysninger", true),
    VARSEL_OM_REVURDERING("varsel-om-revurdering", "BARNETRYGD_VARSEL_OM_REVURDERING", "varsel om revurdering", true),
    VEDTAK("vedtak", "BARNETRYGD_VEDTAK", "vedtak", false),
    HENLEGGE_TRUKKET_SØKNAD("henlegge-trukket-soknad", "BARNETRYGD_HENLEGGE_TRUKKET_SØKNAD", "henlegg trukket søknad", false);

    override fun toString(): String {
        return visningsTekst
    }
}