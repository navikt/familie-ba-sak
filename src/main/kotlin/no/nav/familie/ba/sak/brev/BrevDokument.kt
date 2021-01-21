package no.nav.familie.ba.sak.brev


enum class BrevType(val malId: String, val arkivType: String, val visningsTekst: String, val genererForside: Boolean) {
    INNHENTE_OPPLYSNINGER("innhente-opplysninger", "BARNETRYGD_INNHENTE_OPPLYSNINGER", "innhenting av opplysninger", true),
    VARSEL_OM_REVURDERING("varsel-om-revurdering", "BARNETRYGD_VARSEL_OM_REVURDERING", "varsel om revurdering", true),
    VEDTAK("vedtak", "BARNETRYGD_VEDTAK", "vedtak", false),
    HENLEGGE_TRUKKET_SØKNAD("henlegge-trukket-soknad", "BARNETRYGD_HENLEGGE_TRUKKET_SØKNAD", "henlegg trukket søknad", false);

    override fun toString(): String {
        return visningsTekst
    }
}