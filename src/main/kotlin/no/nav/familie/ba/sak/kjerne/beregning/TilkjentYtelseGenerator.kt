package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseMedEndretUtbetalingGenerator.lagAndelerMedEndretUtbetalingAndeler
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseUtils.oppdaterTilkjentYtelseMedEndretUtbetalingAndelerGammel
import no.nav.familie.ba.sak.kjerne.beregning.UtvidetBarnetrygdUtil.finnUtvidetVilkår
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

object TilkjentYtelseGenerator {
    fun beregnTilkjentYtelse(
        vilkårsvurdering: Vilkårsvurdering,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> = emptyList(),
        fagsakType: FagsakType,
        skalBrukeNyVersjonAvOppdaterAndelerMedEndringer: Boolean = true,
        hentPerioderMedFullOvergangsstønad: (aktør: Aktør) -> List<InternPeriodeOvergangsstønad> = { _ -> emptyList() },
    ): TilkjentYtelse {
        val tilkjentYtelse =
            TilkjentYtelse(
                behandling = vilkårsvurdering.behandling,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now(),
            )

        val (endretUtbetalingAndelerSøker, endretUtbetalingAndelerBarna) = endretUtbetalingAndeler.partition { it.person?.type == PersonType.SØKER }

        val andelerTilkjentYtelseBarnaUtenEndringer =
            OrdinærBarnetrygdUtil
                .beregnAndelerTilkjentYtelseForBarna(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    personResultater = vilkårsvurdering.personResultater,
                    fagsakType = fagsakType,
                ).map {
                    if (it.person.type != PersonType.BARN) throw Feil("Prøver å generere ordinær andel for person av typen ${it.person.type}. Forventet ${PersonType.BARN}")

                    AndelTilkjentYtelse(
                        behandlingId = vilkårsvurdering.behandling.id,
                        tilkjentYtelse = tilkjentYtelse,
                        aktør = it.person.aktør,
                        stønadFom = it.stønadFom,
                        stønadTom = it.stønadTom,
                        kalkulertUtbetalingsbeløp = it.beløp,
                        nasjonaltPeriodebeløp = it.beløp,
                        type = YtelseType.ORDINÆR_BARNETRYGD,
                        sats = it.sats,
                        prosent = it.prosent,
                    )
                }

        val barnasAndelerInkludertEtterbetaling3ÅrEller3MndEndringer =
            if (skalBrukeNyVersjonAvOppdaterAndelerMedEndringer) {
                lagAndelerMedEndretUtbetalingAndeler(
                    andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseBarnaUtenEndringer,
                    endretUtbetalingAndeler = endretUtbetalingAndelerBarna.filter { it.årsak in listOf(Årsak.ETTERBETALING_3ÅR, Årsak.ETTERBETALING_3MND) },
                    tilkjentYtelse = tilkjentYtelse,
                )
            } else {
                oppdaterTilkjentYtelseMedEndretUtbetalingAndelerGammel(
                    andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseBarnaUtenEndringer,
                    endretUtbetalingAndeler = endretUtbetalingAndelerBarna.filter { it.årsak in listOf(Årsak.ETTERBETALING_3ÅR, Årsak.ETTERBETALING_3MND) },
                )
            }

        val andelerTilkjentYtelseUtvidetMedAlleEndringer =
            UtvidetBarnetrygdUtil.beregnTilkjentYtelseUtvidet(
                utvidetVilkår = finnUtvidetVilkår(vilkårsvurdering),
                tilkjentYtelse = tilkjentYtelse,
                andelerTilkjentYtelseBarnaMedEtterbetaling3ÅrEller3MndEndringer = barnasAndelerInkludertEtterbetaling3ÅrEller3MndEndringer,
                endretUtbetalingAndelerSøker = endretUtbetalingAndelerSøker,
                personResultater = vilkårsvurdering.personResultater,
                skalBrukeNyVersjonAvOppdaterAndelerMedEndringer = skalBrukeNyVersjonAvOppdaterAndelerMedEndringer,
            )

        val småbarnstilleggErMulig =
            erSmåbarnstilleggMulig(
                utvidetAndeler = andelerTilkjentYtelseUtvidetMedAlleEndringer,
                barnasAndeler = barnasAndelerInkludertEtterbetaling3ÅrEller3MndEndringer,
            )

        val andelerTilkjentYtelseSmåbarnstillegg =
            if (småbarnstilleggErMulig) {
                SmåbarnstilleggBarnetrygdGenerator(
                    behandlingId = vilkårsvurdering.behandling.id,
                    tilkjentYtelse = tilkjentYtelse,
                ).lagSmåbarnstilleggAndeler(
                    perioderMedFullOvergangsstønad =
                        hentPerioderMedFullOvergangsstønad(
                            personopplysningGrunnlag.søker.aktør,
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
            if (skalBrukeNyVersjonAvOppdaterAndelerMedEndringer) {
                lagAndelerMedEndretUtbetalingAndeler(
                    andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseBarnaUtenEndringer,
                    endretUtbetalingAndeler = endretUtbetalingAndelerBarna,
                    tilkjentYtelse = tilkjentYtelse,
                )
            } else {
                oppdaterTilkjentYtelseMedEndretUtbetalingAndelerGammel(
                    andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseBarnaUtenEndringer,
                    endretUtbetalingAndeler = endretUtbetalingAndelerBarna,
                )
            }

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelseBarnaMedAlleEndringer.map { it.andel } + andelerTilkjentYtelseUtvidetMedAlleEndringer.map { it.andel } + andelerTilkjentYtelseSmåbarnstillegg.map { it.andel })

        return tilkjentYtelse
    }

    private fun erSmåbarnstilleggMulig(
        utvidetAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        barnasAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    ): Boolean = utvidetAndeler.isNotEmpty() && barnasAndeler.isNotEmpty()
}
