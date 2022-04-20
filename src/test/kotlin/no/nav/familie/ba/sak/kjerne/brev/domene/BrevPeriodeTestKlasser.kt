import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
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
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.math.BigDecimal
import java.time.LocalDate

data class BrevPeriodeTestConfig(
    val beskrivelse: String,

    val fom: LocalDate?,
    val tom: LocalDate?,
    val vedtaksperiodetype: Vedtaksperiodetype,
    val begrunnelser: List<BrevBegrunnelseGrunnlagConfig>,
    val fritekster: List<String>,

    val personerPåBehandling: List<BrevPeriodeTestPerson>,

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
    val søknadstidspunkt: LocalDate = LocalDate.now(),
    val avtaletidspunktDeltBosted: LocalDate? = null
) {
    fun tilMinimertRestEndretUtbetalingAndel(personIdent: String) =
        MinimertRestEndretAndel(
            personIdent = personIdent,
            periode = periode,
            årsak = årsak,
            søknadstidspunkt = søknadstidspunkt,
            avtaletidspunktDeltBosted = avtaletidspunktDeltBosted
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
    val fodselsdatoerBarnOppfyllerTriggereOgHarUtbetaling: String,
    val fodselsdatoerBarnOppfyllerTriggereOgHarNullutbetaling: String,
    val antallBarn: Int,
    val antallBarnOppfyllerTriggereOgHarUtbetaling: Int,
    val antallBarnOppfyllerTriggereOgHarNullutbetaling: Int,
    val maanedOgAarBegrunnelsenGjelderFor: String?,
    val maalform: String,
    val apiNavn: String,
    val belop: Int,
    val soknadstidspunkt: String?,
    val avtaletidspunktDeltBosted: String?
) : TestBegrunnelse {

    fun tilBegrunnelseData() = BegrunnelseData(
        belop = Utils.formaterBeløp(this.belop),
        gjelderSoker = this.gjelderSoker,
        barnasFodselsdatoer = this.barnasFodselsdatoer,
        fodselsdatoerBarnOppfyllerTriggereOgHarUtbetaling = this.fodselsdatoerBarnOppfyllerTriggereOgHarUtbetaling,
        fodselsdatoerBarnOppfyllerTriggereOgHarNullutbetaling = this.fodselsdatoerBarnOppfyllerTriggereOgHarNullutbetaling,
        antallBarn = this.antallBarn,
        antallBarnOppfyllerTriggereOgHarUtbetaling = this.antallBarnOppfyllerTriggereOgHarUtbetaling,
        antallBarnOppfyllerTriggereOgHarNullutbetaling = this.antallBarnOppfyllerTriggereOgHarNullutbetaling,
        maanedOgAarBegrunnelsenGjelderFor = this.maanedOgAarBegrunnelsenGjelderFor,
        maalform = this.maalform,
        apiNavn = this.apiNavn,
        soknadstidspunkt = this.soknadstidspunkt ?: "",
        avtaletidspunktDeltBosted = this.avtaletidspunktDeltBosted ?: ""
    )
}

data class BrevPeriodeOutput(
    val fom: String?,
    val tom: String?,
    val belop: Int?,
    val antallBarn: String?,
    val barnasFodselsdager: String?,
    val begrunnelser: List<TestBegrunnelse>,
    val type: String,
)

data class BrevBegrunnelseGrunnlagConfig(
    val standardbegrunnelse: Standardbegrunnelse,
) {
    fun tilBrevBegrunnelseGrunnlag(sanityBegrunnelser: List<SanityBegrunnelse>) = BrevBegrunnelseGrunnlag(
        standardbegrunnelse = this.standardbegrunnelse,
        triggesAv = this.standardbegrunnelse.tilSanityBegrunnelse(sanityBegrunnelser)!!.tilTriggesAv()
    )
}
