package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.FINNMARKSTILLEGG
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.BARN_ENSLIG_MINDREÅRIG
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.INSTITUSJON
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.NORMAL
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.SKJERMET_BARN
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.lagForskjøvetTidslinjeForOppfylteVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import java.math.BigDecimal

object FinnmarkstilleggGenerator {
    fun lagFinnmarkstilleggAndeler(
        behandling: Behandling,
        vilkårsvurdering: Vilkårsvurdering,
        barnasAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelse> {
        val søkersPersonResultat =
            vilkårsvurdering.personResultater.find { it.erSøkersResultater() }
                ?: return emptyList()

        val søkerBosattIFinnmarkTidslinje =
            søkersPersonResultat.vilkårResultater
                .filter { it.vilkårType == BOSATT_I_RIKET && BOSATT_I_FINNMARK_NORD_TROMS in it.utdypendeVilkårsvurderinger }
                .lagForskjøvetTidslinjeForOppfylteVilkår(BOSATT_I_RIKET)

        if (søkerBosattIFinnmarkTidslinje.erTom()) {
            return emptyList()
        }

        val finnmarkstilleggSatsTidslinje = satstypeTidslinje(SatsType.FINNMARKSTILLEGG)

        return when (behandling.fagsak.type) {
            SKJERMET_BARN -> {
                throw Feil("Finnmarkstillegg er ikke implementert for skjermet barn")
            }

            INSTITUSJON, BARN_ENSLIG_MINDREÅRIG -> {
                lagAndeler(
                    barnetsAndelerTidslinje = barnasAndeler.tilTidslinje(),
                    barnHarRettTilFinnmarkstilleggTidslinje = søkerBosattIFinnmarkTidslinje.mapVerdi { it != null },
                    finnmarkstilleggSatsTidslinje = finnmarkstilleggSatsTidslinje,
                    personResultat = søkersPersonResultat,
                    tilkjentYtelse = tilkjentYtelse,
                )
            }

            NORMAL -> {
                vilkårsvurdering
                    .personResultater
                    .filterNot { it.erSøkersResultater() }
                    .flatMap { personResultat ->
                        val barnBosattIFinnmarkTidslinje =
                            personResultat.vilkårResultater
                                .filter { it.vilkårType == BOSATT_I_RIKET && BOSATT_I_FINNMARK_NORD_TROMS in it.utdypendeVilkårsvurderinger }
                                .lagForskjøvetTidslinjeForOppfylteVilkår(BOSATT_I_RIKET)

                        val barnHarRettTilFinnmarkstilleggTidslinje =
                            barnBosattIFinnmarkTidslinje
                                .kombinerMed(annen = søkerBosattIFinnmarkTidslinje) { barnBosattIFinnmark, søkerBosattIFinnmark ->
                                    barnBosattIFinnmark != null && søkerBosattIFinnmark != null
                                }

                        val barnetsAndelerTidslinje = barnasAndeler.filter { it.aktør == personResultat.aktør }.tilTidslinje()

                        lagAndeler(
                            barnetsAndelerTidslinje = barnetsAndelerTidslinje,
                            barnHarRettTilFinnmarkstilleggTidslinje = barnHarRettTilFinnmarkstilleggTidslinje,
                            finnmarkstilleggSatsTidslinje = finnmarkstilleggSatsTidslinje,
                            personResultat = personResultat,
                            tilkjentYtelse = tilkjentYtelse,
                        )
                    }
            }
        }
    }

    private fun lagAndeler(
        barnetsAndelerTidslinje: Tidslinje<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        barnHarRettTilFinnmarkstilleggTidslinje: Tidslinje<Boolean>,
        finnmarkstilleggSatsTidslinje: Tidslinje<Int>,
        personResultat: PersonResultat,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelse> =
        barnetsAndelerTidslinje
            .kombinerMed(
                barnHarRettTilFinnmarkstilleggTidslinje,
                finnmarkstilleggSatsTidslinje,
            ) { andel, barnHarRettTilFinnmarkstillegg, sats ->
                if (barnHarRettTilFinnmarkstillegg == true && andel != null && andel.prosent > BigDecimal.ZERO && sats != null) {
                    AndelTilkjentYtelseForTidslinje(
                        aktør = personResultat.aktør,
                        beløp = sats.avrundetHeltallAvProsent(andel.prosent),
                        sats = sats,
                        ytelseType = FINNMARKSTILLEGG,
                        prosent = andel.prosent,
                    )
                } else {
                    null
                }
            }.tilAndelerTilkjentYtelse(tilkjentYtelse)
}
