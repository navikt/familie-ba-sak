package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.NullableMånedPeriode
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.lagOgValiderPeriodeFraVilkår
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.time.LocalDate

data class BrevGrunnlag(
    val personerPåBehandling: List<MinimertPerson>,
    val minimertePersonResultater: List<MinimertPersonResultat>,
    val minimerteEndredeUtbetalingAndeler: List<MinimertEndretUtbetalingAndel>,
)

data class MinimertPersonResultat(
    val personIdent: String,
    val minimerteVilkårResultater: List<MinimertVilkårResultat>,
    val andreVurderinger: List<AnnenVurdering>
)

fun PersonResultat.tilMinimertPersonResultat() =
    MinimertPersonResultat(
        personIdent = this.personIdent,
        minimerteVilkårResultater = this.vilkårResultater.map { it.tilMinimertVilkårResultat() },
        andreVurderinger = this.andreVurderinger.toList()
    )

data class MinimertVilkårResultat(
    val vilkårType: Vilkår,
    val periodeFom: LocalDate?,
    val periodeTom: LocalDate?,
    val resultat: Resultat,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering>,
    val erEksplisittAvslagPåSøknad: Boolean?,
) {

    fun toPeriode(): Periode = lagOgValiderPeriodeFraVilkår(
        this.periodeFom,
        this.periodeTom,
        this.erEksplisittAvslagPåSøknad
    )
}

fun VilkårResultat.tilMinimertVilkårResultat() =
    MinimertVilkårResultat(
        vilkårType = this.vilkårType,
        periodeFom = this.periodeFom,
        periodeTom = this.periodeTom,
        resultat = this.resultat,
        utdypendeVilkårsvurderinger = this.utdypendeVilkårsvurderinger,
        erEksplisittAvslagPåSøknad = this.erEksplisittAvslagPåSøknad,
    )

fun List<MinimertPersonResultat>.harPersonerSomManglerOpplysninger(): Boolean =
    this.any { personResultat ->
        personResultat.andreVurderinger.any {
            it.type == AnnenVurderingType.OPPLYSNINGSPLIKT && it.resultat == Resultat.IKKE_OPPFYLT
        }
    }

data class MinimertEndretUtbetalingAndel(
    val periode: MånedPeriode,
    val personIdent: String,
    val årsak: Årsak,
) {
    fun erOverlappendeMed(nullableMånedPeriode: NullableMånedPeriode): Boolean {
        if (nullableMånedPeriode.fom == null) {
            throw Feil("Fom ble null ved sjekk av overlapp av periode til endretUtbetalingAndel")
        }

        return MånedPeriode(
            this.periode.fom,
            this.periode.tom
        ).overlapperHeltEllerDelvisMed(
            MånedPeriode(
                nullableMånedPeriode.fom,
                nullableMånedPeriode.tom ?: TIDENES_ENDE.toYearMonth()
            )
        )
    }
}

fun List<MinimertEndretUtbetalingAndel>.somOverlapper(nullableMånedPeriode: NullableMånedPeriode) =
    this.filter { it.erOverlappendeMed(nullableMånedPeriode) }

fun EndretUtbetalingAndel.tilMinimertEndretUtbetalingAndel() = MinimertEndretUtbetalingAndel(
    periode = this.periode,
    personIdent = this.person?.personIdent?.ident ?: throw Feil(
        "Har ikke ident på endretUtbetalingsandel ${this.id} " +
            "ved konvertering til minimertEndretUtbetalingsandel"
    ),
    årsak = this.årsak ?: throw Feil(
        "Har ikke årsak på endretUtbetalingsandel ${this.id} " +
            "ved konvertering til minimertEndretUtbetalingsandel"
    )
)