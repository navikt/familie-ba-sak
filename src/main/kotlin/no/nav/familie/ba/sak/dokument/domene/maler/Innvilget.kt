package no.nav.familie.ba.sak.dokument.domene.maler

data class Innvilget(
        val enhet: String,
        val saksbehandler: String,
        val beslutter: String,
        val barnasFodselsdatoer: String, // "24.12.19, 24.12.18 og 24.12.17"
        val belop: Int,
        val virkningsdato: String, // "februar 2020"
        val vilkårsdato: String, // "24.12.19"
        val vedtaksdato: String, // "24.12.19"
        val antallBarn: Int,
        val flereBarn: Boolean,
        val hjemmel: String // "§2, 4 og 11"
)