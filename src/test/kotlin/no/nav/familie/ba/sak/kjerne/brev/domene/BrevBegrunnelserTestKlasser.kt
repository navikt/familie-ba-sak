package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.UtvidetScenarioForEndringsperiode
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.MinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.MinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class BrevBegrunnelserTestConfig(
    val beskrivelse: String,

    val fom: LocalDate?,
    val tom: LocalDate?,
    val vedtaksperiodetype: Vedtaksperiodetype,

    // Brukes for å se om det er en reduksjon i småbarnstillegg eller utvidet
    val ytelserForSøkerForrigeMåned: List<YtelseType>,

    val utvidetScenarioForEndringsperiode: UtvidetScenarioForEndringsperiode,

    val ytelserForBarnOgSøkerIPeriode: List<YtelseType>,
    val erFørsteVedtaksperiodePåFagsak: Boolean,
    val personerPåBehandling: List<BrevbegrunnelserTestPerson>,

    val forventetOutput: List<Standardbegrunnelse>
) {
    fun hentMinimertVedtaksperiode() = MinimertVedtaksperiode(
        fom = this.fom,
        tom = this.tom,
        type = this.vedtaksperiodetype,
        ytelseTyperForPeriode = ytelserForBarnOgSøkerIPeriode.toSet(),
        utbetalingsperioder = emptyList()
    )

    fun hentMinimertePersonResultater() = this.personerPåBehandling.map { it.tilMinimertePersonResultater() }
    fun hentMinimertePersoner() = this.personerPåBehandling.map { it.tilMinimertPerson() }
    fun hentAktørIderMedUtbetaling() = this.personerPåBehandling.map { it.aktørId }
    fun hentEndretUtbetalingAndeler() = this.personerPåBehandling.flatMap { it.tilMinimerteEndredeUtbetalingAndeler() }
}

data class BrevbegrunnelserTestPerson(
    val personIdent: String = randomFnr(),
    val aktørId: String = randomAktørId(personIdent).aktørId,
    val type: PersonType,
    val fødselsdato: LocalDate,
    val overstyrteVilkårresultater: List<MinimertVilkårResultat>,
    val andreVurderinger: List<MinimertAnnenVurdering>,
    val endredeUtbetalinger: List<EndretUtbetalingAndelPåPerson>,
) {
    fun tilMinimertPerson() = MinimertPerson(
        aktivPersonIdent = this.personIdent,
        aktørId = this.aktørId,
        type = this.type,
        fødselsdato = this.fødselsdato,
    )

    fun tilMinimerteEndredeUtbetalingAndeler() =
        endredeUtbetalinger.map { it.tilMinimertEndretUtbetalingAndel(aktørId = this.aktørId) }

    fun tilMinimertePersonResultater() = MinimertRestPersonResultat(
        personIdent = this.personIdent,
        minimerteVilkårResultater = hentVilkårForPerson(),
        minimerteAndreVurderinger = this.andreVurderinger,
    )

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

data class EndretUtbetalingAndelPåPerson(
    val fom: YearMonth,
    val tom: YearMonth,
    val årsak: Årsak,
    val prosent: BigDecimal,
) {
    fun tilMinimertEndretUtbetalingAndel(aktørId: String) =
        MinimertEndretAndel(
            aktørId = aktørId,
            fom = fom,
            tom = tom,
            årsak = årsak,
            prosent = prosent
        )
}
