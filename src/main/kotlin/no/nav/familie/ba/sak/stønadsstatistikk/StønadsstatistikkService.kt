package no.nav.familie.ba.sak.stønadsstatistikk

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.beregnUtbetalingsperioderUtenKlassifisering
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.eksterne.kontrakter.PersonDVH
import no.nav.familie.eksterne.kontrakter.UtbetalingsDetaljDVH
import no.nav.familie.eksterne.kontrakter.UtbetalingsperiodeDVH
import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import org.springframework.stereotype.Service

@Service
class StønadsstatistikkService(private val behandlingRepository: BehandlingRepository,
                               private val persongrunnlagService: PersongrunnlagService,
                               private val beregningService: BeregningService,
                               private val loggService: LoggService,
                               private val vedtakService: VedtakService) {


    fun hentVedtak(behandlingId: Long): VedtakDVH {

        val behandling = behandlingRepository.getOne(behandlingId)
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId)
        val persongrunnlag = persongrunnlagService.hentAktiv(behandlingId)


        return VedtakDVH(fagsakId = behandling.fagsak.id.toString(),
                behandlingsId = behandlingId.toString(),
                tidspunktVedtak = vedtakService.hentAktivForBehandling(behandlingId)!!.vedtaksdato!!,
                personIdent = behandling.fagsak.hentAktivIdent().ident,
                ensligForsørger = true, utbetalingsperioder = hentUtbetalingsperioder(tilkjentYtelse, persongrunnlag!!))
    }


    private fun hentUtbetalingsperioder(tilkjentYtelseForBehandling: TilkjentYtelse, personopplysningGrunnlag: PersonopplysningGrunnlag)
            : List<UtbetalingsperiodeDVH> {
        if (tilkjentYtelseForBehandling.andelerTilkjentYtelse.isEmpty()) return emptyList()

        val utbetalingsPerioder = beregnUtbetalingsperioderUtenKlassifisering(tilkjentYtelseForBehandling.andelerTilkjentYtelse)

        return utbetalingsPerioder.toSegments()
                .sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
                .map { segment ->
                    val andelerForSegment = tilkjentYtelseForBehandling.andelerTilkjentYtelse.filter {
                        segment.localDateInterval.overlaps(LocalDateInterval(it.stønadFom, it.stønadTom))
                    }
                    mapTilUtbetalingsPeriode(segment,
                            andelerForSegment,
                            tilkjentYtelseForBehandling.behandling,
                            personopplysningGrunnlag)
                }
    }


    private fun mapTilUtbetalingsPeriode(segment: LocalDateSegment<Int>,
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
                                    bostedsland = "Norge", //TODO hvor finner vi bostedsland?
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