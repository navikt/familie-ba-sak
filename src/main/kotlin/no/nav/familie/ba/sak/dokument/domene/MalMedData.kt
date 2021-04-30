package no.nav.familie.ba.sak.dokument.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype

enum class BrevType(val malId: String, val dokumenttype: Dokumenttype, val visningsTekst: String, val genererForside: Boolean) {

    INNHENTE_OPPLYSNINGER("innhente-opplysninger",
                          Dokumenttype.BARNETRYGD_INNHENTE_OPPLYSNINGER,
                          "innhenting av opplysninger",
                          true),
    VARSEL_OM_REVURDERING("varsel-om-revurdering", Dokumenttype.BARNETRYGD_VARSEL_OM_REVURDERING, "varsel om revurdering", true),
    VEDTAK("vedtak", Dokumenttype.BARNETRYGD_VEDTAK, "vedtak", false),
    HENLEGGE_TRUKKET_SØKNAD("henlegge-trukket-soknad",
                            Dokumenttype.BARNETRYGD_HENLEGGE_TRUKKET_SØKNAD,
                            "henlegg trukket søknad",
                            false);

    override fun toString(): String {
        return visningsTekst
    }

    fun tilSanityBrevtype() =
            when (this) {
                INNHENTE_OPPLYSNINGER -> no.nav.familie.ba.sak.brev.domene.maler.EnkelBrevtype.INNHENTE_OPPLYSNINGER
                VARSEL_OM_REVURDERING -> no.nav.familie.ba.sak.brev.domene.maler.EnkelBrevtype.VARSEL_OM_REVURDERING
                HENLEGGE_TRUKKET_SØKNAD -> no.nav.familie.ba.sak.brev.domene.maler.EnkelBrevtype.HENLEGGE_TRUKKET_SØKNAD
                VEDTAK -> throw Feil("Kan ikke oversette gammel brevtype til vedtak")
            }
}