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

    fun erEksplisittAvslag(personerFremstiltKravFor: List<Aktør>): Boolean = this is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget && this.vilkårResultaterForVedtaksperiode.inneholderEksplisittAvslag(personerFremstiltKravFor)

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
            is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget -> {
                this.copy(
                    person,
                    vilkårResultaterForVedtaksperiode,
                )
            }

            is VedtaksperiodeGrunnlagForPersonVilkårInnvilget -> {
                this.copy(person, vilkårResultaterForVedtaksperiode)
            }
        }
}

data class VedtaksperiodeGrunnlagForPersonVilkårInnvilget(
    override val person: Person,
    override val vilkårResultaterForVedtaksperiode: List<VilkårResultatForVedtaksperiode>,
    val andeler: Iterable<AndelForVedtaksobjekt>,
    val kompetanse: KompetanseForVedtaksperiode? = null,
    val endretUtbetalingAndel: IEndretUtbetalingAndelForVedtaksperiode? = null,
    val utenlandskPeriodebeløp: UtenlandskPeriodebeløpForVedtaksperiode? = null,
    val valutakurs: ValutakursForVedtaksperiode? = null,
    val overgangsstønad: OvergangsstønadForVedtaksperiode? = null,
) : VedtaksperiodeGrunnlagForPerson {
    fun erInnvilgetEndretUtbetaling() = endretUtbetalingAndel?.prosent != BigDecimal.ZERO || endretUtbetalingAndel?.årsak == Årsak.DELT_BOSTED
}

data class VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget(
    override val person: Person,
    override val vilkårResultaterForVedtaksperiode: List<VilkårResultatForVedtaksperiode>,
) : VedtaksperiodeGrunnlagForPerson {
    fun List<VilkårResultatForVedtaksperiode>.inneholderEksplisittAvslag(personerFremstiltKravFor: List<Aktør>) = this.any { it.erEksplisittAvslagPåSøknad } && (personerFremstiltKravFor.contains(person.aktør) || person.type == PersonType.SØKER)
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

        val satsEndretOgDifferanseberegnetPeriodebeløpErNegativt = sats != other.sats && differanseberegnetPeriodebeløp != null && differanseberegnetPeriodebeløp < 0

        return Objects.equals(type, other.type) &&
            Objects.equals(prosent, other.prosent) &&
            ((erBeggeNull && !satsEndretOgDifferanseberegnetPeriodebeløpErNegativt) || (erIngenNull && sats == other.sats))
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
    val erAnnenForelderOmfattetAvNorskLovgivning: Boolean,
) {
    constructor(kompetanse: UtfyltKompetanse) : this(
        søkersAktivitet = kompetanse.søkersAktivitet,
        annenForeldersAktivitet = kompetanse.annenForeldersAktivitet,
        annenForeldersAktivitetsland = kompetanse.annenForeldersAktivitetsland,
        søkersAktivitetsland = kompetanse.søkersAktivitetsland,
        barnetsBostedsland = kompetanse.barnetsBostedsland,
        resultat = kompetanse.resultat,
        barnAktører = kompetanse.barnAktører,
        erAnnenForelderOmfattetAvNorskLovgivning = kompetanse.erAnnenForelderOmfattetAvNorskLovgivning,
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
