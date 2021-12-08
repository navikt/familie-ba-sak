package no.nav.familie.ba.sak.kjerne.dokument.domene

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.UregistrertBarnEnkel
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.dokument.UtvidetScenario
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.BrevBegrunnelseGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.MinimertEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.MinimertPersonResultat
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.MinimertUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.MinimertVilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.math.BigDecimal
import java.time.LocalDate

data class BrevBegrunnelseTestConfig(
    val beskrivelse: String,

    val fom: LocalDate,
    val tom: LocalDate?,
    val vedtaksperiodetype: Vedtaksperiodetype,
    val begrunnelser: List<BrevBegrunnelseGrunnlagConfig>,
    val fritekster: List<String>,

    val personerPåBehandling: List<PersonPåBehandlingConfig>,

    val utvidetScenario: UtvidetScenario = UtvidetScenario.IKKE_UTVIDET_YTELSE,
    val uregistrerteBarn: List<UregistrertBarnEnkel>,
    val erFørsteVedtaksperiodePåFagsak: Boolean = false,
    val målformSøker: Målform,

    val forventetOutput: BrevPeriodeTestConfig
)

data class PersonPåBehandlingConfig(
    val personIdent: String,
    val fødselsdato: LocalDate,
    val type: PersonType,
    val atypiskeVilkårResultater: List<MinimertVilkårResultat>,
    val andreVurderinger: List<AnnenVurdering>,
    val endredeUtbetalinger: List<EndretUtbetalingAndelPåPerson>,
    val utbetalinger: List<UtbetalingPåPerson>
) {
    fun tilMinimertPerson() = MinimertPerson(personIdent = personIdent, fødselsdato = fødselsdato, type = type)
    fun tilUtbetalingsperiodeDetaljer() = utbetalinger.map {
        it.tilMinimertUtbetalingsperiodeDetalj(this.tilMinimertPerson())
    }

    fun tilMinimerteEndredeUtbetalingAndeler() =
        endredeUtbetalinger.map { it.tilMinimertEndretUtbetalingAndel(this.personIdent) }

    fun tilMinimertePersonResultater(): MinimertPersonResultat {
        return MinimertPersonResultat(
            personIdent = this.personIdent,
            minimerteVilkårResultater = this.atypiskeVilkårResultater + hentTypiskeVilkårForPersontype(),
            andreVurderinger = this.andreVurderinger,
        )
    }

    private fun hentTypiskeVilkårForPersontype() =
        Vilkår.hentVilkårFor(this.type)
            .filter { vilkår -> !this.atypiskeVilkårResultater.any { it.vilkårType == vilkår } }
            .map { vilkår ->
                MinimertVilkårResultat(
                    vilkårType = vilkår,
                    periodeFom = this.fødselsdato,
                    periodeTom = null,
                    resultat = Resultat.OPPFYLT,
                    utdypendeVilkårsvurderinger = emptyList(),
                    erEksplisittAvslagPåSøknad = false,
                )
            }
}

data class UtbetalingPåPerson(
    val ytelseType: YtelseType,
    val utbetaltPerMnd: Int,
    val erPåvirketAvEndring: Boolean = false,
    val prosent: BigDecimal = BigDecimal(100),
) {
    fun tilMinimertUtbetalingsperiodeDetalj(minimertPerson: MinimertPerson) =
        MinimertUtbetalingsperiodeDetalj(
            person = minimertPerson,
            utbetaltPerMnd = this.utbetaltPerMnd,
            prosent = this.prosent,
            erPåvirketAvEndring = this.erPåvirketAvEndring,
            ytelseType = this.ytelseType
        )
}

data class EndretUtbetalingAndelPåPerson(
    val periode: MånedPeriode,
    val årsak: Årsak,
) {
    fun tilMinimertEndretUtbetalingAndel(personIdent: String) =
        MinimertEndretUtbetalingAndel(
            personIdent = personIdent,
            periode = periode,
            årsak = årsak
        )
}

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
    val tom: String?,
    val belop: Int?,
    val antallBarn: String?,
    val barnasFodselsdager: String?,
    val begrunnelser: List<TestBegrunnelse>,
    val type: String,
)

data class BrevBegrunnelseGrunnlagConfig(
    val vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
) {
    fun tilBrevBegrunnelseGrunnlag(sanityBegrunnelser: List<SanityBegrunnelse>) = BrevBegrunnelseGrunnlag(
        vedtakBegrunnelseSpesifikasjon = this.vedtakBegrunnelseSpesifikasjon,
        vedtakBegrunnelseType = this.vedtakBegrunnelseType,
        triggesAv = this.vedtakBegrunnelseSpesifikasjon.tilSanityBegrunnelse(sanityBegrunnelser)!!.tilTriggesAv()
    )
}
