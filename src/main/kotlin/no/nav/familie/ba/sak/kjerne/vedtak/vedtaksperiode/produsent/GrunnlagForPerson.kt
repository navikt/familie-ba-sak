package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.IUtfyltEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.SøkersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.math.BigDecimal
import java.time.LocalDate

sealed interface GrunnlagForPerson {
    val person: Person
    val vilkårResultaterForVedtaksperiode: List<VilkårResultatForVedtaksperiode>

    fun erEksplisittAvslag(): Boolean = this is GrunnlagForPersonIkkeInnvilget && this.erEksplisittAvslag

    fun kopier(
        person: Person = this.person,
        vilkårResultaterForVedtaksperiode: List<VilkårResultatForVedtaksperiode> = this.vilkårResultaterForVedtaksperiode,
    ): GrunnlagForPerson {
        return when (this) {
            is GrunnlagForPersonIkkeInnvilget -> this.copy(person, vilkårResultaterForVedtaksperiode)
            is GrunnlagForPersonInnvilget -> this.copy(person, vilkårResultaterForVedtaksperiode)
        }
    }
}

data class GrunnlagForPersonInnvilget(
    override val person: Person,
    override val vilkårResultaterForVedtaksperiode: List<VilkårResultatForVedtaksperiode>,
    val andeler: Iterable<AndelForVedtaksperiode>,
    val kompetanse: KompetanseForVedtaksperiode? = null,
    val endretUtbetalingAndel: EndretUtbetalingAndelForVedtaksperiode? = null,
    val overgangsstønad: OvergangsstønadForVedtaksperiode? = null,
) : GrunnlagForPerson

data class GrunnlagForPersonIkkeInnvilget(
    override val person: Person,
    override val vilkårResultaterForVedtaksperiode: List<VilkårResultatForVedtaksperiode>,
) : GrunnlagForPerson {
    val erEksplisittAvslag: Boolean = vilkårResultaterForVedtaksperiode.inneholderEksplisittAvslag()

    fun List<VilkårResultatForVedtaksperiode>.inneholderEksplisittAvslag() =
        this.any { it.erEksplisittAvslagPåSøknad == true }
}

data class VilkårResultatForVedtaksperiode(
    val vilkårType: Vilkår,
    val resultat: Resultat,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering>,
    val vurderesEtter: Regelverk?,
    val erEksplisittAvslagPåSøknad: Boolean?,
    val standardbegrunnelser: List<IVedtakBegrunnelse>,
    val aktørId: AktørId,
    val fom: LocalDate?,
    val tom: LocalDate?,
) {
    constructor(vilkårResultat: VilkårResultat) : this(
        vilkårType = vilkårResultat.vilkårType,
        resultat = vilkårResultat.resultat,
        utdypendeVilkårsvurderinger = vilkårResultat.utdypendeVilkårsvurderinger,
        vurderesEtter = vilkårResultat.vurderesEtter,
        erEksplisittAvslagPåSøknad = vilkårResultat.erEksplisittAvslagPåSøknad,
        standardbegrunnelser = vilkårResultat.standardbegrunnelser,
        fom = vilkårResultat.periodeFom,
        tom = vilkårResultat.periodeTom,
        aktørId = vilkårResultat.personResultat?.aktør?.aktørId
            ?: throw Feil("$vilkårResultat er ikke knyttet til personResultat"),
    )
}

data class EndretUtbetalingAndelForVedtaksperiode(
    val prosent: BigDecimal,
    val årsak: Årsak,
    val standardbegrunnelse: List<IVedtakBegrunnelse>,
) {
    constructor(endretUtbetalingAndel: IUtfyltEndretUtbetalingAndel) : this(
        prosent = endretUtbetalingAndel.prosent,
        årsak = endretUtbetalingAndel.årsak,
        standardbegrunnelse = endretUtbetalingAndel.standardbegrunnelser,
    )
}

data class AndelForVedtaksperiode(
    val kalkulertUtbetalingsbeløp: Int,
    val type: YtelseType,
) {
    constructor(andelTilkjentYtelse: AndelTilkjentYtelse) : this(
        kalkulertUtbetalingsbeløp = andelTilkjentYtelse.kalkulertUtbetalingsbeløp,
        type = andelTilkjentYtelse.type,
    )
}

data class KompetanseForVedtaksperiode(
    val søkersAktivitet: SøkersAktivitet,
    val annenForeldersAktivitet: AnnenForeldersAktivitet,
    val annenForeldersAktivitetsland: String?,
    val søkersAktivitetsland: String,
    val barnetsBostedsland: String,
    val resultat: KompetanseResultat,
) {
    constructor(kompetanse: UtfyltKompetanse) : this(
        søkersAktivitet = kompetanse.søkersAktivitet,
        annenForeldersAktivitet = kompetanse.annenForeldersAktivitet,
        annenForeldersAktivitetsland = kompetanse.annenForeldersAktivitetsland,
        søkersAktivitetsland = kompetanse.søkersAktivitetsland,
        barnetsBostedsland = kompetanse.barnetsBostedsland,
        resultat = kompetanse.resultat,
    )
}

data class OvergangsstønadForVedtaksperiode(
    val fom: LocalDate,
    val tom: LocalDate,
) {
    constructor(periodeOvergangsstønad: InternPeriodeOvergangsstønad) : this(
        fom = periodeOvergangsstønad.fomDato,
        tom = periodeOvergangsstønad.tomDato,
    )
}
