package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentÅrsak
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
    VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14(
        "varsel-om-revurdering-delt-bosted-paragraf-14",
        Dokumenttype.BARNETRYGD_VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14,
        "Varsel om revurdering delt bosted § 14",
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
    ),
    INFORMASJONSBREV_KAN_SØKE(
        "informasjonsbrev-kan-soke",
        Dokumenttype.BARNETRYGD_INFORMASJONSBREV_KAN_SØKE,
        "informasjonsbrev kan søke",
        false
    ),
    INFORMASJONSBREV_FØDSEL_GENERELL(
        "informasjonsbrev-fodsel-generell",
        Dokumenttype.BARNETRYGD_INFORMASJONSBREV_FØDSEL_GENERELL,
        "informasjonsbrev fødsel generell",
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
            VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14 -> Brevmal.VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14
            HENLEGGE_TRUKKET_SØKNAD -> Brevmal.HENLEGGE_TRUKKET_SØKNAD
            SVARTIDSBREV -> Brevmal.SVARTIDSBREV
            INFORMASJONSBREV_FØDSEL_MINDREÅRIG -> Brevmal.INFORMASJONSBREV_FØDSEL_MINDREÅRIG
            INFORMASJONSBREV_FØDSEL_UMYNDIG -> Brevmal.INFORMASJONSBREV_FØDSEL_UMYNDIG
            INFORMASJONSBREV_KAN_SØKE -> Brevmal.INFORMASJONSBREV_KAN_SØKE
            INFORMASJONSBREV_FØDSEL_GENERELL -> Brevmal.INFORMASJONSBREV_FØDSEL_GENERELL
            VEDTAK -> throw Feil("Kan ikke oversette gammel brevtype til vedtak")
        }

    fun setterBehandlingPåVent(): Boolean =
        when (this) {
            INNHENTE_OPPLYSNINGER,
            VARSEL_OM_REVURDERING,
            VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14 -> true
            else -> false
        }

    fun ventefristDager(): Long =
        when (this) {
            INNHENTE_OPPLYSNINGER,
            VARSEL_OM_REVURDERING -> 3 * 7
            else -> throw Feil("Ventefrist ikke definert for brevtype $this")
        }

    fun venteårsak() =
        when (this) {
            INNHENTE_OPPLYSNINGER,
            VARSEL_OM_REVURDERING -> SettPåVentÅrsak.AVVENTER_DOKUMENTASJON
            else -> throw Feil("Venteårsak ikke definert for brevtype $this")
        }
}
