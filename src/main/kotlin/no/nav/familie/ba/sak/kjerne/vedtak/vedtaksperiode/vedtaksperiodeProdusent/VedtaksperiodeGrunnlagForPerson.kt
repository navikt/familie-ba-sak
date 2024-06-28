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
            this.andeler
                .filter { it.prosent > BigDecimal.ZERO }
                .map { it.type }
                .toSet()
        } else {
            emptySet()
        }

    fun kopier(
        person: Person = this.person,
        vilkårResultaterForVedtaksperiode: List<VilkårResultatForVedtaksperiode> = this.vilkårResultaterForVedtaksperiode,
    ): VedtaksperiodeGrunnlagForPerson =
        when (this) {
            is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget ->
                this.copy(
                    person,
                    vilkårResultaterForVedtaksperiode,
                )

            is VedtaksperiodeGrunnlagForPersonVilkårInnvilgetNy -> this.copy(person, vilkårResultaterForVedtaksperiode)

            is VedtaksperiodeGrunnlagForPersonVilkårInnvilgetGammel -> this.copy(person, vilkårResultaterForVedtaksperiode)
        }
}

sealed class VedtaksperiodeGrunnlagForPersonVilkårInnvilget(
    override val person: Person,
    override val vilkårResultaterForVedtaksperiode: List<VilkårResultatForVedtaksperiode>,
    open val andeler: Iterable<AndelForVedtaksobjekt>,
    open val kompetanse: KompetanseForVedtaksperiode? = null,
    open val endretUtbetalingAndel: IEndretUtbetalingAndelForVedtaksperiode? = null,
    open val utenlandskPeriodebeløp: UtenlandskPeriodebeløpForVedtaksperiode? = null,
    open val valutakurs: ValutakursForVedtaksperiode? = null,
    open val overgangsstønad: OvergangsstønadForVedtaksperiode? = null,
) : VedtaksperiodeGrunnlagForPerson {
    abstract fun erInnvilgetEndretUtbetaling(): Boolean
}

data class VedtaksperiodeGrunnlagForPersonVilkårInnvilgetNy(
    override val person: Person,
    override val vilkårResultaterForVedtaksperiode: List<VilkårResultatForVedtaksperiode>,
    override val andeler: Iterable<AndelForVedtaksperiode>,
    override val kompetanse: KompetanseForVedtaksperiode? = null,
    override val endretUtbetalingAndel: IEndretUtbetalingAndelForVedtaksperiode? = null,
    override val utenlandskPeriodebeløp: UtenlandskPeriodebeløpForVedtaksperiode? = null,
    override val valutakurs: ValutakursForVedtaksperiode? = null,
    override val overgangsstønad: OvergangsstønadForVedtaksperiode? = null,
) : VedtaksperiodeGrunnlagForPersonVilkårInnvilget(
        person,
        vilkårResultaterForVedtaksperiode,
        andeler,
        kompetanse,
        endretUtbetalingAndel,
        utenlandskPeriodebeløp,
        valutakurs,
        overgangsstønad,
    ) {
    override fun erInnvilgetEndretUtbetaling() =
        endretUtbetalingAndel?.prosent != BigDecimal.ZERO || endretUtbetalingAndel?.årsak == Årsak.DELT_BOSTED
}

@Deprecated("Bruk heller tilGrunnlagForPersonTidslinjeNy. Kan fjernes når feature toggle IKKE_SPLITT_VEDTAKSPERIODE_PÅ_ENDRING_I_VALUTAKURS ikke lenger er i bruk.")
data class VedtaksperiodeGrunnlagForPersonVilkårInnvilgetGammel(
    override val person: Person,
    override val vilkårResultaterForVedtaksperiode: List<VilkårResultatForVedtaksperiode>,
    override val andeler: Iterable<AndelForVedtaksbegrunnelse>,
    override val kompetanse: KompetanseForVedtaksperiode? = null,
    override val endretUtbetalingAndel: IEndretUtbetalingAndelForVedtaksperiode? = null,
    override val utenlandskPeriodebeløp: UtenlandskPeriodebeløpForVedtaksperiode? = null,
    override val valutakurs: ValutakursForVedtaksperiode? = null,
    override val overgangsstønad: OvergangsstønadForVedtaksperiode? = null,
) : VedtaksperiodeGrunnlagForPersonVilkårInnvilget(
        person,
        vilkårResultaterForVedtaksperiode,
        andeler,
        kompetanse,
        endretUtbetalingAndel,
        utenlandskPeriodebeløp,
        valutakurs,
        overgangsstønad,
    ) {
    override fun erInnvilgetEndretUtbetaling() =
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

fun List<VilkårResultatForVedtaksperiode>.erLikUtenomTom(other: List<VilkårResultatForVedtaksperiode>): Boolean = this.map { it.copy(tom = null) }.toSet() == other.map { it.copy(tom = null) }.toSet()

fun Iterable<VilkårResultatForVedtaksperiode>.erOppfyltForBarn(): Boolean =
    Vilkår
        .hentOrdinæreVilkårFor(PersonType.BARN)
        .all { vilkår ->
            val vilkårsresutlatet = this.find { it.vilkårType == vilkår }

            vilkårsresutlatet?.resultat == Resultat.OPPFYLT
        }

sealed interface AndelForVedtaksobjekt {
    val kalkulertUtbetalingsbeløp: Int
    val nasjonaltPeriodebeløp: Int?
    val differanseberegnetPeriodebeløp: Int?
    val type: YtelseType
    val prosent: BigDecimal
    val sats: Int
}

data class AndelForVedtaksperiode(
    override val kalkulertUtbetalingsbeløp: Int,
    override val nasjonaltPeriodebeløp: Int?,
    override val differanseberegnetPeriodebeløp: Int?,
    override val type: YtelseType,
    override val prosent: BigDecimal,
    override val sats: Int,
) : AndelForVedtaksobjekt {
    constructor(andelTilkjentYtelse: AndelTilkjentYtelse) : this(
        kalkulertUtbetalingsbeløp = andelTilkjentYtelse.kalkulertUtbetalingsbeløp,
        nasjonaltPeriodebeløp = andelTilkjentYtelse.nasjonaltPeriodebeløp,
        differanseberegnetPeriodebeløp = andelTilkjentYtelse.differanseberegnetPeriodebeløp,
        type = andelTilkjentYtelse.type,
        prosent = andelTilkjentYtelse.prosent,
        sats = andelTilkjentYtelse.sats,
    )

    /**
     * Dette objektet er for å finne ut hvilke splitter vi skal ha på vedtaksperiodene.
     * I utgangspunktet ønsker vi å lage en splitt hver gang det er en endring i andelene.
     * Unntakene er:
     * - Dersom det er to nullutbetalinger etter hverandre ønsker vi ikke at det skal bli nye vedtaksperioder.
     * - Dersom endringen skyldes en valutajustering ønsker vi heller ikke at det skal bli nye vedtaksperioder.
     *
     * equals og hashcode er derfor endret for å reflektere dette.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AndelForVedtaksperiode) return false

        val erBeggeNull = kalkulertUtbetalingsbeløp == 0 && other.kalkulertUtbetalingsbeløp == 0
        val erIngenNull = kalkulertUtbetalingsbeløp != 0 && other.kalkulertUtbetalingsbeløp != 0

        return Objects.equals(type, other.type) &&
            Objects.equals(prosent, other.prosent) &&
            (erBeggeNull || (erIngenNull && sats == other.sats))
    }

    override fun hashCode(): Int =
        if (kalkulertUtbetalingsbeløp == 0) {
            Objects.hash(
                kalkulertUtbetalingsbeløp,
                type,
                prosent,
            )
        } else {
            Objects.hash(
                type,
                prosent,
                sats,
            )
        }
}

data class AndelForVedtaksbegrunnelse(
    override val kalkulertUtbetalingsbeløp: Int,
    override val nasjonaltPeriodebeløp: Int?,
    override val differanseberegnetPeriodebeløp: Int?,
    override val type: YtelseType,
    override val prosent: BigDecimal,
    override val sats: Int,
) : AndelForVedtaksobjekt {
    constructor(andelTilkjentYtelse: AndelTilkjentYtelse) : this(
        kalkulertUtbetalingsbeløp = andelTilkjentYtelse.kalkulertUtbetalingsbeløp,
        nasjonaltPeriodebeløp = andelTilkjentYtelse.nasjonaltPeriodebeløp,
        differanseberegnetPeriodebeløp = andelTilkjentYtelse.differanseberegnetPeriodebeløp,
        type = andelTilkjentYtelse.type,
        prosent = andelTilkjentYtelse.prosent,
        sats = andelTilkjentYtelse.sats,
    )

    override fun equals(other: Any?): Boolean {
        if (other !is AndelForVedtaksbegrunnelse) {
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

    private fun satsErlik(annen: Int): Boolean =
        if (kalkulertUtbetalingsbeløp == 0) {
            true
        } else {
            Objects.equals(sats, annen)
        }

    override fun hashCode(): Int =
        if (kalkulertUtbetalingsbeløp == 0) {
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
