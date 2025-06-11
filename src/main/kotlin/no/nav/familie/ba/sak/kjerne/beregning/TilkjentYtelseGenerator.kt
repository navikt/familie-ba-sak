package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseMedEndretUtbetalingGenerator.lagAndelerMedEndretUtbetalingAndeler
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.overgangsstønad.OvergangsstønadService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class TilkjentYtelseGenerator(
    private val overgangsstønadService: OvergangsstønadService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val unleashService: UnleashNextMedContextService,
) {
    fun genererTilkjentYtelse(
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> = emptyList(),
    ): TilkjentYtelse {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandlingThrows(behandlingId = behandling.id)

        val tilkjentYtelse =
            TilkjentYtelse(
                behandling = behandling,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now(),
            )

        val (endretUtbetalingAndelerSøker, endretUtbetalingAndelerBarna) = endretUtbetalingAndeler.partition { it.personer.any { person -> person.type == PersonType.SØKER } }

        val andelerTilkjentYtelseBarnaUtenEndringer =
            OrdinærBarnetrygdUtil
                .beregnAndelerTilkjentYtelseForBarna(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    personResultater = vilkårsvurdering.personResultater,
                    fagsakType = behandling.fagsak.type,
                ).map {
                    if (it.person.type != PersonType.BARN) throw Feil("Prøver å generere ordinær andel for person av typen ${it.person.type}. Forventet ${PersonType.BARN}")

                    AndelTilkjentYtelse(
                        behandlingId = behandling.id,
                        tilkjentYtelse = tilkjentYtelse,
                        aktør = it.person.aktør,
                        stønadFom = it.stønadFom,
                        stønadTom = it.stønadTom,
                        kalkulertUtbetalingsbeløp = it.beløp,
                        nasjonaltPeriodebeløp = it.beløp,
                        beløpUtenEndretUtbetaling = it.beløp,
                        type = YtelseType.ORDINÆR_BARNETRYGD,
                        sats = it.sats,
                        prosent = it.prosent,
                    )
                }

        val skalBeholdeSplittI0krAndeler = unleashService.isEnabled(FeatureToggle.SKAL_BRUKE_NY_DIFFERANSEBEREGNING)

        val barnasAndelerInkludertEtterbetaling3ÅrEller3MndEndringer =
            lagAndelerMedEndretUtbetalingAndeler(
                andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseBarnaUtenEndringer,
                endretUtbetalingAndeler = endretUtbetalingAndelerBarna.filter { it.årsak in listOf(Årsak.ETTERBETALING_3ÅR, Årsak.ETTERBETALING_3MND) },
                tilkjentYtelse = tilkjentYtelse,
                skalBeholdeSplittI0krAndeler = skalBeholdeSplittI0krAndeler,
            )

        val andelerTilkjentYtelseUtvidetMedAlleEndringer =
            UtvidetBarnetrygdUtil.beregnTilkjentYtelseUtvidet(
                tilkjentYtelse = tilkjentYtelse,
                andelerTilkjentYtelseBarnaMedEtterbetaling3ÅrEller3MndEndringer = barnasAndelerInkludertEtterbetaling3ÅrEller3MndEndringer,
                endretUtbetalingAndelerSøker = endretUtbetalingAndelerSøker,
                personResultater = vilkårsvurdering.personResultater,
                skalBeholdeSplittI0krAndeler = skalBeholdeSplittI0krAndeler,
            )

        val småbarnstilleggErMulig =
            erSmåbarnstilleggMulig(
                utvidetAndeler = andelerTilkjentYtelseUtvidetMedAlleEndringer,
                barnasAndeler = barnasAndelerInkludertEtterbetaling3ÅrEller3MndEndringer,
            )

        val andelerTilkjentYtelseSmåbarnstillegg =
            if (småbarnstilleggErMulig) {
                SmåbarnstilleggGenerator(
                    tilkjentYtelse = tilkjentYtelse,
                ).lagSmåbarnstilleggAndeler(
                    perioderMedFullOvergangsstønad =
                        hentPerioderMedFullOvergangsstønad(
                            søkerAktør = personopplysningGrunnlag.søker.aktør,
                            behandling = behandling,
                        ),
                    utvidetAndeler = andelerTilkjentYtelseUtvidetMedAlleEndringer,
                    barnasAndeler = barnasAndelerInkludertEtterbetaling3ÅrEller3MndEndringer,
                    barnasAktørerOgFødselsdatoer =
                        personopplysningGrunnlag.barna.map {
                            Pair(
                                it.aktør,
                                it.fødselsdato,
                            )
                        },
                )
            } else {
                emptyList()
            }

        val andelerTilkjentYtelseBarnaMedAlleEndringer =
            lagAndelerMedEndretUtbetalingAndeler(
                andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseBarnaUtenEndringer,
                endretUtbetalingAndeler = endretUtbetalingAndelerBarna,
                tilkjentYtelse = tilkjentYtelse,
                skalBeholdeSplittI0krAndeler = skalBeholdeSplittI0krAndeler,
            )

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelseBarnaMedAlleEndringer.map { it.andel } + andelerTilkjentYtelseUtvidetMedAlleEndringer.map { it.andel } + andelerTilkjentYtelseSmåbarnstillegg.map { it.andel })

        return tilkjentYtelse
    }

    private fun hentPerioderMedFullOvergangsstønad(
        søkerAktør: Aktør,
        behandling: Behandling,
    ): List<InternPeriodeOvergangsstønad> {
        overgangsstønadService.hentOgLagrePerioderMedOvergangsstønadForBehandling(
            søkerAktør = søkerAktør,
            behandling = behandling,
        )

        return overgangsstønadService.hentPerioderMedFullOvergangsstønad(behandling)
    }

    private fun erSmåbarnstilleggMulig(
        utvidetAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        barnasAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    ): Boolean = utvidetAndeler.isNotEmpty() && barnasAndeler.isNotEmpty()
}
