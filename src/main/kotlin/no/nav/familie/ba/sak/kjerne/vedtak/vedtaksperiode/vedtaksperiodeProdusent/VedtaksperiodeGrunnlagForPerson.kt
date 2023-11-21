package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtfyltUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.UtfyltValutakurs
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Objects

sealed interface VedtaksperiodeGrunnlagForPerson {
    val person: Person
    val vilkårResultaterForVedtaksperiode: List<VilkårResultatForVedtaksperiode>

    fun erEksplisittAvslag(): Boolean =
        this is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget && this.erEksplisittAvslag

    fun erInnvilget() = this is VedtaksperiodeGrunnlagForPersonVilkårInnvilget && this.erInnvilgetEndretUtbetaling()

    fun hentInnvilgedeYtelsestyper() =
        if (this is VedtaksperiodeGrunnlagForPersonVilkårInnvilget) {
            this.andeler.filter { it.prosent > BigDecimal.ZERO }
                .map { it.type }.toSet()
        } else {
            emptySet()
        }

    fun kopier(
        person: Person = this.person,
        vilkårResultaterForVedtaksperiode: List<VilkårResultatForVedtaksperiode> = this.vilkårResultaterForVedtaksperiode,
    ): VedtaksperiodeGrunnlagForPerson {
        return when (this) {
            is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget ->
                this.copy(
                    person,
                    vilkårResultaterForVedtaksperiode,
                )

            is VedtaksperiodeGrunnlagForPersonVilkårInnvilget -> this.copy(person, vilkårResultaterForVedtaksperiode)
        }
    }
}

data class VedtaksperiodeGrunnlagForPersonVilkårInnvilget(
    override val person: Person,
    override val vilkårResultaterForVedtaksperiode: List<VilkårResultatForVedtaksperiode>,
    val andeler: Iterable<AndelForVedtaksperiode>,
    val kompetanse: KompetanseForVedtaksperiode? = null,
    val endretUtbetalingAndel: IEndretUtbetalingAndelForVedtaksperiode? = null,
    val utenlandskPeriodebeløp: UtenlandskPeriodebeløpForVedtaksperiode? = null,
    val valutakurs: ValutakursForVedtaksperiode? = null,
    val overgangsstønad: OvergangsstønadForVedtaksperiode? = null,
) : VedtaksperiodeGrunnlagForPerson {
    fun erInnvilgetEndretUtbetaling() =
        endretUtbetalingAndel?.prosent != BigDecimal.ZERO || endretUtbetalingAndel?.årsak == Årsak.DELT_BOSTED
}

data class VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget(
    override val person: Person,
    override val vilkårResultaterForVedtaksperiode: List<VilkårResultatForVedtaksperiode>,
) : VedtaksperiodeGrunnlagForPerson {
    val erEksplisittAvslag: Boolean = vilkårResultaterForVedtaksperiode.inneholderEksplisittAvslag()

    fun List<VilkårResultatForVedtaksperiode>.inneholderEksplisittAvslag() =
        this.any { it.erEksplisittAvslagPåSøknad }
}

data class VilkårResultatForVedtaksperiode(
    val vilkårType: Vilkår,
    val resultat: Resultat,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering>,
    val vurderesEtter: Regelverk?,
    val erEksplisittAvslagPåSøknad: Boolean,
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
        erEksplisittAvslagPåSøknad = vilkårResultat.erEksplisittAvslagPåSøknad == true,
        standardbegrunnelser = vilkårResultat.standardbegrunnelser,
        fom = vilkårResultat.periodeFom,
        tom = vilkårResultat.periodeTom,
        aktørId =
            vilkårResultat.personResultat?.aktør?.aktørId
                ?: throw Feil("$vilkårResultat er ikke knyttet til personResultat"),
    )
}

fun List<VilkårResultatForVedtaksperiode>.erLikUtenFomOgTom(other: List<VilkårResultatForVedtaksperiode>): Boolean {
    return this.map { it.copy(fom = null, tom = null) }.toSet() == other.map { it.copy(fom = null, tom = null) }.toSet()
}

fun Iterable<VilkårResultatForVedtaksperiode>.erOppfyltForBarn(): Boolean =
    Vilkår.hentOrdinæreVilkårFor(PersonType.BARN)
        .all { vilkår ->
            val vilkårsresutlatet = this.find { it.vilkårType == vilkår }

            vilkårsresutlatet?.resultat == Resultat.OPPFYLT
        }

data class AndelForVedtaksperiode(
    val kalkulertUtbetalingsbeløp: Int,
    val nasjonaltPeriodebeløp: Int?,
    val type: YtelseType,
    val prosent: BigDecimal,
    val sats: Int,
) {
    constructor(andelTilkjentYtelse: AndelTilkjentYtelse) : this(
        kalkulertUtbetalingsbeløp = andelTilkjentYtelse.kalkulertUtbetalingsbeløp,
        nasjonaltPeriodebeløp = andelTilkjentYtelse.nasjonaltPeriodebeløp,
        type = andelTilkjentYtelse.type,
        prosent = andelTilkjentYtelse.prosent,
        sats = andelTilkjentYtelse.sats,
    )

    override fun equals(other: Any?): Boolean {
        if (other !is AndelForVedtaksperiode) {
            return false
        } else if (this === other) {
            return true
        }

        val annen = other
        return Objects.equals(kalkulertUtbetalingsbeløp, annen.kalkulertUtbetalingsbeløp) &&
            Objects.equals(type, annen.type) &&
            Objects.equals(prosent, annen.prosent) &&
            satsErlik(annen.sats)
    }

    private fun satsErlik(annen: Int): Boolean {
        return if (kalkulertUtbetalingsbeløp == 0) {
            true
        } else {
            Objects.equals(sats, annen)
        }
    }

    override fun hashCode(): Int {
        return if (kalkulertUtbetalingsbeløp == 0) {
            Objects.hash(
                kalkulertUtbetalingsbeløp,
                type,
                prosent,
            )
        } else {
            Objects.hash(
                kalkulertUtbetalingsbeløp,
                type,
                prosent,
                sats,
            )
        }
    }
}

data class KompetanseForVedtaksperiode(
    val søkersAktivitet: KompetanseAktivitet,
    val annenForeldersAktivitet: KompetanseAktivitet,
    val annenForeldersAktivitetsland: String?,
    val søkersAktivitetsland: String,
    val barnetsBostedsland: String,
    val resultat: KompetanseResultat,
    val barnAktører: Set<Aktør>,
) {
    constructor(kompetanse: UtfyltKompetanse) : this(
        søkersAktivitet = kompetanse.søkersAktivitet,
        annenForeldersAktivitet = kompetanse.annenForeldersAktivitet,
        annenForeldersAktivitetsland = kompetanse.annenForeldersAktivitetsland,
        søkersAktivitetsland = kompetanse.søkersAktivitetsland,
        barnetsBostedsland = kompetanse.barnetsBostedsland,
        resultat = kompetanse.resultat,
        barnAktører = kompetanse.barnAktører,
    )
}

data class ValutakursForVedtaksperiode(
    val barnAktører: Set<Aktør>,
    val valutakursdato: LocalDate,
    val valutakode: String,
    val kurs: BigDecimal,
) {
    constructor(valutakurs: UtfyltValutakurs) : this(
        barnAktører = valutakurs.barnAktører,
        valutakursdato = valutakurs.valutakursdato,
        valutakode = valutakurs.valutakode,
        kurs = valutakurs.kurs,
    )
}

data class UtenlandskPeriodebeløpForVedtaksperiode(
    val barnAktører: Set<Aktør>,
    val beløp: BigDecimal,
    val valutakode: String,
    val intervall: Intervall,
    val utbetalingsland: String,
) {
    constructor(utenlandskPeriodebeløp: UtfyltUtenlandskPeriodebeløp) : this(
        barnAktører = utenlandskPeriodebeløp.barnAktører,
        beløp = utenlandskPeriodebeløp.beløp,
        valutakode = utenlandskPeriodebeløp.valutakode,
        intervall = utenlandskPeriodebeløp.intervall,
        utbetalingsland = utenlandskPeriodebeløp.utbetalingsland,
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
