package no.nav.familie.ba.sak.dokument.domene.maler

data class Innvilget(
        val enhet: String,
        val saksbehandler: String,
        val beslutter: String,
        val hjemmel: String, // "§2, 4 og 11"
        var duFaar: List<DuFårSeksjon> = emptyList()
)

data class DuFårSeksjon(
        val fom: String,
        val tom: String,
        val belop: String,
        val antallBarn: Int,
        val barnasFodselsdatoer: String,
        val begrunnelser: Map<String, String>
)