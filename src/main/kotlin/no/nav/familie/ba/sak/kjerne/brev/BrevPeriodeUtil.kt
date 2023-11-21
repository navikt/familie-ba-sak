package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertKompetanse
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertKompetanse
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertPersonResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertRestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSkjemaer
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerUendeligSent
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerUendeligTidlig
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjær
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilMinimertPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate
import java.time.YearMonth

fun List<LocalDate>.tilSammenslåttKortString(): String = Utils.slåSammen(this.sorted().map { it.tilKortString() })

fun hentRestBehandlingsgrunnlagForBrev(
    persongrunnlag: PersonopplysningGrunnlag,
    vilkårsvurdering: Vilkårsvurdering,
    endredeUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>,
): RestBehandlingsgrunnlagForBrev {
    return RestBehandlingsgrunnlagForBrev(
        personerPåBehandling = persongrunnlag.søkerOgBarn.map { it.tilMinimertPerson() },
        minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
        minimerteEndredeUtbetalingAndeler = endredeUtbetalingAndeler.map { it.tilMinimertRestEndretUtbetalingAndel() },
        fagsakType = vilkårsvurdering.behandling.fagsak.type,
    )
}

fun hentMinimerteKompetanserForPeriode(
    kompetanser: List<Kompetanse>,
    fom: YearMonth?,
    tom: YearMonth?,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    landkoderISO2: Map<String, String>,
): List<MinimertKompetanse> {
    val minimerteKompetanser =
        kompetanser.hentIPeriode(fom, tom)
            .filter { it.erObligatoriskeFelterSatt() }
            .map {
                it.tilMinimertKompetanse(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    landkoderISO2 = landkoderISO2,
                )
            }

    return minimerteKompetanser
}

fun hentKompetanserSomStopperRettFørPeriode(
    kompetanser: List<Kompetanse>,
    periodeFom: YearMonth?,
) = kompetanser.filter { it.tom?.plusMonths(1) == periodeFom }

fun Collection<Kompetanse>.hentIPeriode(
    fom: YearMonth?,
    tom: YearMonth?,
): Collection<Kompetanse> =
    tilSeparateTidslinjerForBarna().mapValues { (_, tidslinje) ->
        tidslinje.beskjær(
            fraOgMed = fom.tilTidspunktEllerUendeligTidlig(tom),
            tilOgMed = tom.tilTidspunktEllerUendeligSent(fom),
        )
    }.tilSkjemaer()
