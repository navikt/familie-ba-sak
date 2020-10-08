package no.nav.familie.ba.sak.dokument.domene.maler

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform

data class Innvilget(
        val enhet: String,
        val saksbehandler: String,
        val beslutter: String,
        val hjemmel: String, // "§2, 4 og 11"
        var duFaar: List<DuFårSeksjon> = emptyList(),
        val maalform: String,
        val etterbetalingsbelop: String? = "",
        val erFeilutbetaling: Boolean? = false,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class InnvilgetAutovedtak(
        val navn: String,
        val fodselsnummer: String,
        val fodselsdato: String,
        val virkningstidspunkt: String,
        val belop: String,
        val etterbetalingsbelop: String? = null,
        val antallBarn: Int,
        val erBehandletAutomatisk: Boolean = true,
        val enhet: String
)

data class DuFårSeksjon(
        val fom: String,
        val tom: String?,
        val belop: String,
        val antallBarn: Int,
        val barnasFodselsdatoer: String,
        val begrunnelser: List<String>
)