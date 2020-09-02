package no.nav.familie.ba.sak.stønadsstatistikk

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.beregnUtbetalingsperioderUtenKlassifisering
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.eksterne.kontrakter.PersonDVH
import no.nav.familie.eksterne.kontrakter.UtbetalingsDetaljDVH
import no.nav.familie.eksterne.kontrakter.UtbetalingsperiodeDVH
import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import org.springframework.stereotype.Service

@Service
class StønadsstatistikkService(private val behandlingService: BehandlingService,
                               private val persongrunnlagService: PersongrunnlagService,
                               private val beregningService: BeregningService,
                               private val vedtakService: VedtakService) {


    fun hentVedtak(behandlingId: Long): VedtakDVH {

        val behandling = behandlingService.hent(behandlingId)


        return VedtakDVH(fagsakId = behandling.fagsak.id.toString(),
                behandlingsId = behandlingId.toString(),
                tidspunktVedtak = vedtakService.hentAktivForBehandling(behandlingId)?.vedtaksdato
                        ?: error("Fant ikke vedtaksdato"),
                personIdent = behandling.fagsak.hentAktivIdent().ident,
                ensligForsørger = utledEnsligForsørger(behandlingId), utbetalingsperioder = hentUtbetalingsperioder(behandlingId)) //TODO implementere støtte for dette
    }


    private fun hentUtbetalingsperioder(behandlingId: Long)
            : List<UtbetalingsperiodeDVH> {

        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId)
        val persongrunnlag = persongrunnlagService.hentAktiv(behandlingId) ?: error("Fant ikke aktivt persongrunnlag")

        if (tilkjentYtelse.andelerTilkjentYtelse.isEmpty()) return emptyList()

        val utbetalingsPerioder = beregnUtbetalingsperioderUtenKlassifisering(tilkjentYtelse.andelerTilkjentYtelse)

        return utbetalingsPerioder.toSegments()
                .sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
                .map { segment ->
                    val andelerForSegment = tilkjentYtelse.andelerTilkjentYtelse.filter {
                        segment.localDateInterval.overlaps(LocalDateInterval(it.stønadFom, it.stønadTom))
                    }
                    mapTilUtbetalingsperiode(segment,
                            andelerForSegment,
                            tilkjentYtelse.behandling,
                            persongrunnlag)
                }
    }


    private fun utledEnsligForsørger(behandlingId: Long): Boolean {

        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId)
        if (tilkjentYtelse.andelerTilkjentYtelse.isEmpty()) return false

        return tilkjentYtelse.andelerTilkjentYtelse.find { it.type == YtelseType.UTVIDET_BARNETRYGD } != null

    }

    private fun mapTilUtbetalingsperiode(segment: LocalDateSegment<Int>,
                                         andelerForSegment: List<AndelTilkjentYtelse>,
                                         behandling: Behandling,
                                         personopplysningGrunnlag: PersonopplysningGrunnlag): UtbetalingsperiodeDVH {
        return UtbetalingsperiodeDVH(
                hjemmel = "Ikke implementert",
                stønadFom = segment.fom,
                stønadTom = segment.tom,
                utbetaltPerMnd = segment.value,
                utbetalingsDetaljer = andelerForSegment.map { andel ->
                    val personForAndel = personopplysningGrunnlag.personer.find { person -> andel.personIdent == person.personIdent.ident }
                            ?: throw IllegalStateException("Fant ikke personopplysningsgrunnlag for andel")
                    UtbetalingsDetaljDVH(
                            person = PersonDVH(
                                    rolle = personForAndel.type.name,
                                    statsborgerskap = emptyList(), // TODO lag liste med statsborgerskap
                                    bostedsland = "NO", //TODO hvor finner vi bostedsland?
                                    primærland = "IKKE IMPLMENTERT",
                                    sekundærland = "IKKE IMPLEMENTERT",
                                    delingsprosentOmsorg = 0, // TODO ikke implementert
                                    delingsprosentYtelse = 0, // TODO ikke implementert
                                    annenpartBostedsland = "Ikke implementert",
                                    annenpartPersonident = "ikke implementert",
                                    annenpartStatsborgerskap = "ikke implementert",
                                    personIdent = personForAndel.personIdent.ident
                            ),
                            klassekode = andel.type.klassifisering,
                            utbetaltPrMnd = andel.beløp
                    )
                }
        )

    }
}