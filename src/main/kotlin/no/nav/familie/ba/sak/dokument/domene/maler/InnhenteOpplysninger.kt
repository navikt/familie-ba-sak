package no.nav.familie.ba.sak.dokument.domene.maler


data class InnhenteOpplysninger(
        val fritekst: String,
        val saksbehandler: String,
        val enhet: String,
        val dokumenter: List<String>,
        val maalform: String
)