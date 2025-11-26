package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.SVALBARDTILLEGG
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.BARN_ENSLIG_MINDREÅRIG
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.INSTITUSJON
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.NORMAL
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.SKJERMET_BARN
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.lagForskjøvetTidslinjeForOppfylteVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import java.math.BigDecimal

object SvalbardtilleggGenerator {
    fun lagSvalbardtilleggAndeler(
        behandling: Behandling,
        vilkårsvurdering: Vilkårsvurdering,
        barnasAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelse> {
        val søkersPersonResultat =
            vilkårsvurdering.personResultater.find { it.erSøkersResultater() }
                ?: return emptyList()

        val søkerBosattISvalbardTidslinje =
            søkersPersonResultat.vilkårResultater
                .filter { it.vilkårType == BOSATT_I_RIKET && BOSATT_PÅ_SVALBARD in it.utdypendeVilkårsvurderinger }
                .lagForskjøvetTidslinjeForOppfylteVilkår(BOSATT_I_RIKET)

        if (søkerBosattISvalbardTidslinje.erTom()) {
            return emptyList()
        }

        val svalbardtilleggSatsTidslinje = satstypeTidslinje(SatsType.SVALBARDTILLEGG)

        return when (behandling.fagsak.type) {
            SKJERMET_BARN -> {
                throw Feil("Svalbardtillegg er ikke implementert for skjermet barn")
            }

            INSTITUSJON, BARN_ENSLIG_MINDREÅRIG -> {
                lagAndeler(
                    barnetsAndelerTidslinje = barnasAndeler.tilTidslinje(),
                    barnHarRettTilSvalbardtilleggTidslinje = søkerBosattISvalbardTidslinje.mapVerdi { it != null },
                    svalbardtilleggSatsTidslinje = svalbardtilleggSatsTidslinje,
                    personResultat = søkersPersonResultat,
                    tilkjentYtelse = tilkjentYtelse,
                )
            }

            NORMAL -> {
                vilkårsvurdering
                    .personResultater
                    .filterNot { it.erSøkersResultater() }
                    .flatMap { personResultat ->
                        val barnBosattISvalbardTidslinje =
                            personResultat.vilkårResultater
                                .filter { it.vilkårType == BOSATT_I_RIKET && BOSATT_PÅ_SVALBARD in it.utdypendeVilkårsvurderinger }
                                .lagForskjøvetTidslinjeForOppfylteVilkår(BOSATT_I_RIKET)

                        val barnHarRettTilSvalbardtilleggTidslinje =
                            barnBosattISvalbardTidslinje
                                .kombinerMed(annen = søkerBosattISvalbardTidslinje) { barnBosattISvalbard, søkerBosattISvalbard ->
                                    barnBosattISvalbard != null && søkerBosattISvalbard != null
                                }

                        val barnetsAndelerTidslinje = barnasAndeler.filter { it.aktør == personResultat.aktør }.tilTidslinje()

                        lagAndeler(
                            barnetsAndelerTidslinje = barnetsAndelerTidslinje,
                            barnHarRettTilSvalbardtilleggTidslinje = barnHarRettTilSvalbardtilleggTidslinje,
                            svalbardtilleggSatsTidslinje = svalbardtilleggSatsTidslinje,
                            personResultat = personResultat,
                            tilkjentYtelse = tilkjentYtelse,
                        )
                    }
            }
        }
    }

    private fun lagAndeler(
        barnetsAndelerTidslinje: Tidslinje<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        barnHarRettTilSvalbardtilleggTidslinje: Tidslinje<Boolean>,
        svalbardtilleggSatsTidslinje: Tidslinje<Int>,
        personResultat: PersonResultat,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelse> =
        barnetsAndelerTidslinje
            .kombinerMed(
                barnHarRettTilSvalbardtilleggTidslinje,
                svalbardtilleggSatsTidslinje,
            ) { andel, barnHarRettTilSvalbardtillegg, sats ->
                if (barnHarRettTilSvalbardtillegg == true && andel != null && andel.prosent > BigDecimal.ZERO && sats != null) {
                    AndelTilkjentYtelseForTidslinje(
                        aktør = personResultat.aktør,
                        beløp = sats.avrundetHeltallAvProsent(andel.prosent),
                        sats = sats,
                        ytelseType = SVALBARDTILLEGG,
                        prosent = andel.prosent,
                    )
                } else {
                    null
                }
            }.tilAndelerTilkjentYtelse(tilkjentYtelse)
}
