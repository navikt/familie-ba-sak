import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.UtvidetScenarioForEndringsperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevBegrunnelseGrunnlag
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertAnnenVurdering
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertRestEndretAndel
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertRestPersonResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertVilkårResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.tilTriggesAv
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.math.BigDecimal
import java.time.LocalDate

@Deprecated("Bruk BrevPeriodeTestConfig")
data class BrevPeriodeTestConfigGammel(
    val beskrivelse: String,

    val fom: LocalDate?,
    val tom: LocalDate?,
    val vedtaksperiodetype: Vedtaksperiodetype,
    val begrunnelser: List<BrevBegrunnelseGrunnlagConfig>,
    val fritekster: List<String>,

    val personerPåBehandling: List<BrevPeriodeTestPerson>,

    val utvidetScenarioForEndringsperiode: UtvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
    val uregistrerteBarn: List<MinimertUregistrertBarn>,
    val erFørsteVedtaksperiodePåFagsak: Boolean = false,
    val brevMålform: Målform,

    val forventetOutput: BrevPeriodeOutputGammel?
)

data class BrevPeriodeTestConfig(
    val beskrivelse: String,

    val fom: LocalDate?,
    val tom: LocalDate?,
    val vedtaksperiodetype: Vedtaksperiodetype,
    val begrunnelser: List<BrevBegrunnelseGrunnlagConfig>,
    val fritekster: List<String>,

    val personerPåBehandling: List<BrevPeriodeTestPerson>,

    val utvidetScenarioForEndringsperiode: UtvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
    val uregistrerteBarn: List<MinimertUregistrertBarn>,
    val erFørsteVedtaksperiodePåFagsak: Boolean = false,
    val brevMålform: Målform,

    val forventetOutput: BrevPeriodeOutput?
)

data class BrevPeriodeTestPerson(
    val personIdent: String = randomFnr(),
    val fødselsdato: LocalDate,
    val type: PersonType,
    val overstyrteVilkårresultater: List<MinimertVilkårResultat>,
    val andreVurderinger: List<MinimertAnnenVurdering>,
    val endredeUtbetalinger: List<EndretRestUtbetalingAndelPåPerson>,
    val utbetalinger: List<UtbetalingPåPerson>
) {
    fun tilMinimertPerson() = MinimertRestPerson(personIdent = personIdent, fødselsdato = fødselsdato, type = type)
    fun tilUtbetalingsperiodeDetaljer() = utbetalinger.map {
        it.tilMinimertUtbetalingsperiodeDetalj(this.tilMinimertPerson())
    }

    fun tilMinimerteEndredeUtbetalingAndeler() =
        endredeUtbetalinger.map { it.tilMinimertRestEndretUtbetalingAndel(this.personIdent) }

    fun tilMinimertePersonResultater(): MinimertRestPersonResultat {
        return MinimertRestPersonResultat(
            personIdent = this.personIdent,
            minimerteVilkårResultater = hentVilkårForPerson(),
            minimerteAndreVurderinger = this.andreVurderinger,
        )
    }

    private fun hentVilkårForPerson() =
        this.overstyrteVilkårresultater +
            Vilkår.hentVilkårFor(this.type)
                .filter { vilkår -> !this.overstyrteVilkårresultater.any { it.vilkårType == vilkår } }
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
    fun tilMinimertUtbetalingsperiodeDetalj(minimertRestPerson: MinimertRestPerson) =
        MinimertUtbetalingsperiodeDetalj(
            person = minimertRestPerson,
            utbetaltPerMnd = this.utbetaltPerMnd,
            prosent = this.prosent,
            erPåvirketAvEndring = this.erPåvirketAvEndring,
            ytelseType = this.ytelseType
        )
}

data class EndretRestUtbetalingAndelPåPerson(
    val periode: MånedPeriode,
    val årsak: Årsak,
) {
    fun tilMinimertRestEndretUtbetalingAndel(personIdent: String) =
        MinimertRestEndretAndel(
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

@Deprecated("Bruk BrevPeriodeOutput")
data class BrevPeriodeOutputGammel(
    val fom: String?,
    val tom: String?,
    val belop: Int?,
    val antallBarn: String?,
    val barnasFodselsdager: String?,
    val begrunnelser: List<TestBegrunnelse>,
    val type: String,
)

data class BrevPeriodeOutput(
    val fom: String,
    val tom: String,
    val belop: Int?,
    val antallBarn: String,
    val barnasFodselsdager: String,
    val begrunnelser: List<TestBegrunnelse>,
    val antallBarnMedUtbetaling: String,
    val antallBarnUtenUtbetaling: String,
    val fodselsdagerBarnMedUtbetaling: String,
    val fodselsdagerBarnUtenUtbetaling: String,
    val apiNavn: String,
)

data class BrevBegrunnelseGrunnlagConfig(
    val vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon,
) {
    fun tilBrevBegrunnelseGrunnlag(sanityBegrunnelser: List<SanityBegrunnelse>) = BrevBegrunnelseGrunnlag(
        vedtakBegrunnelseSpesifikasjon = this.vedtakBegrunnelseSpesifikasjon,
        triggesAv = this.vedtakBegrunnelseSpesifikasjon.tilSanityBegrunnelse(sanityBegrunnelser)!!.tilTriggesAv()
    )
}
