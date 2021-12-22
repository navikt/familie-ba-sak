package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype

enum class BrevType(
    val malId: String,
    val dokumenttype: Dokumenttype,
    val visningsTekst: String,
    val genererForside: Boolean
) {

    INNHENTE_OPPLYSNINGER(
        "innhente-opplysninger",
        Dokumenttype.BARNETRYGD_INNHENTE_OPPLYSNINGER,
        "innhenting av opplysninger",
        true
    ),
    INFORMASJONSBREV_DELT_BOSTED(
        "informasjonsbrev-delt-bosted",
        Dokumenttype.BARNETRYGD_INFORMASJONSBREV_DELT_BOSTED,
        "Informasjonsbrev delt bosted",
        false
    ),
    VARSEL_OM_REVURDERING(
        "varsel-om-revurdering",
        Dokumenttype.BARNETRYGD_VARSEL_OM_REVURDERING,
        "varsel om revurdering",
        true
    ),
    VEDTAK("vedtak", Dokumenttype.BARNETRYGD_VEDTAK, "vedtak", false),
    HENLEGGE_TRUKKET_SØKNAD(
        "henlegge-trukket-soknad",
        Dokumenttype.BARNETRYGD_HENLEGGE_TRUKKET_SØKNAD,
        "henlegg trukket søknad",
        false
    ),
    SVARTIDSBREV(
        "svartidsbrev",
        Dokumenttype.BARNETRYGD_SVARTIDSBREV,
        "svartidsbrev",
        false
    ),
    INFORMASJONSBREV_FØDSEL_UMYNDIG(
        "informasjonsbrev-fodsel-umyndig",
        Dokumenttype.BARNETRYGD_INFORMASJONSBREV_FØDSEL_UMYNDIG,
        "informasjonsbrev fødsel umyndig",
        false
    ),
    INFORMASJONSBREV_FØDSEL_MINDREÅRIG(
        "informasjonsbrev-fodsel-mindreaarig",
        Dokumenttype.BARNETRYGD_INFORMASJONSBREV_FØDSEL_MINDREÅRIG,
        "informasjonsbrev fødsel mindreårig",
        false
    );

    override fun toString(): String {
        return visningsTekst
    }

    fun tilSanityBrevtype() =
        when (this) {
            INNHENTE_OPPLYSNINGER -> Brevmal.INNHENTE_OPPLYSNINGER
            INFORMASJONSBREV_DELT_BOSTED -> Brevmal.INFORMASJONSBREV_DELT_BOSTED
            VARSEL_OM_REVURDERING -> Brevmal.VARSEL_OM_REVURDERING
            HENLEGGE_TRUKKET_SØKNAD -> Brevmal.HENLEGGE_TRUKKET_SØKNAD
            SVARTIDSBREV -> Brevmal.SVARTIDSBREV
            INFORMASJONSBREV_FØDSEL_MINDREÅRIG -> Brevmal.INFORMASJONSBREV_FØDSEL_MINDREÅRIG
            INFORMASJONSBREV_FØDSEL_UMYNDIG -> Brevmal.INFORMASJONSBREV_FØDSEL_UMYNDIG
            VEDTAK -> throw Feil("Kan ikke oversette gammel brevtype til vedtak")
        }
}
