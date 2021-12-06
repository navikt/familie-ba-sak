package no.nav.familie.ba.sak.kjerne.dokument.domene

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.ekstern.restDomene.RestPerson
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.UregistrertBarnEnkel
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelsePerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.RestVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import java.math.BigDecimal
import java.time.LocalDate

data class UtbetalingsperiodeDetaljEnkel(
    val personIdent: String,
    val ytelseType: YtelseType,
    val utbetaltPerMnd: Int,
    val erPåvirketAvEndring: Boolean = false,
    val prosent: BigDecimal = BigDecimal(100),
) {
    fun tilUtbetalingsperiodeDetalj(restPerson: RestPerson) =
        UtbetalingsperiodeDetalj(
            person = restPerson,
            utbetaltPerMnd = this.utbetaltPerMnd,
            prosent = this.prosent,
            erPåvirketAvEndring = this.erPåvirketAvEndring,
            ytelseType = this.ytelseType
        )
}

data class BrevBegrunnelseTestConfig(
    val beskrivelse: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val vedtaksperiodetype: Vedtaksperiodetype,
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetaljEnkel>,
    val standardbegrunnelser: List<RestVedtaksbegrunnelse>,
    val fritekster: List<String>,
    val begrunnelsepersoner: List<BegrunnelsePerson>,
    val målform: Målform,
    val uregistrerteBarn: List<UregistrertBarnEnkel>,
    val forventetOutput: BrevPeriodeTestConfig
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = BegrunnelseDataTestConfig::class
)
@JsonSubTypes(value = [JsonSubTypes.Type(value = FritekstBegrunnelseTestConfig::class, name = "fritekst")])
interface TestBegrunnelse

data class FritekstBegrunnelseTestConfig(val fritekst: String) : TestBegrunnelse

data class BegrunnelseDataTestConfig(
    val gjelderSoker: Boolean,
    val barnasFodselsdatoer: String,
    val antallBarn: Int,
    val maanedOgAarBegrunnelsenGjelderFor: String?,
    val maalform: String,
    val apiNavn: String,
    val belop: Int,
) : TestBegrunnelse {

    fun tilBegrunnelseData() = BegrunnelseData(
        belop = Utils.formaterBeløp(this.belop),
        gjelderSoker = this.gjelderSoker,
        barnasFodselsdatoer = this.barnasFodselsdatoer,
        antallBarn = this.antallBarn,
        maanedOgAarBegrunnelsenGjelderFor = this.maanedOgAarBegrunnelsenGjelderFor,
        maalform = this.maalform,
        apiNavn = this.apiNavn,
    )
}

data class BrevPeriodeTestConfig(
    val fom: String,
    val tom: String,
    val belop: Int,
    val antallBarn: String,
    val barnasFodselsdager: String,
    val begrunnelser: List<TestBegrunnelse>,
    val type: String,
)
