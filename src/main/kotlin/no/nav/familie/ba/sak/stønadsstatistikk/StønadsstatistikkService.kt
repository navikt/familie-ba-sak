package no.nav.familie.ba.sak.stønadsstatistikk

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.beregnUtbetalingsperioderUtenKlassifisering
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.eksterne.kontrakter.*
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class StønadsstatistikkService(private val behandlingService: BehandlingService,
                               private val persongrunnlagService: PersongrunnlagService,
                               private val beregningService: BeregningService,
                               private val vedtakService: VedtakService,
                               private val personopplysningerService: PersonopplysningerService) {

    fun hentVedtak(behandlingId: Long): VedtakDVH {

        val behandling = behandlingService.hent(behandlingId)


        return VedtakDVH(fagsakId = behandling.fagsak.id.toString(),
                         behandlingsId = behandlingId.toString(),
                         tidspunktVedtak = vedtakService.hentAktivForBehandling(behandlingId)?.vedtaksdato
                                           ?: error("Fant ikke vedtaksdato"),
                         personIdent = behandling.fagsak.hentAktivIdent().ident,
                         ensligForsørger = utledEnsligForsørger(behandlingId), //TODO implementere støtte for dette
                         kategori = Kategori.valueOf(behandling.kategori.name),
                         underkategori = Underkategori.valueOf(behandling.underkategori.name),
                         behandlingType = BehandlingType.valueOf(behandling.type.name),
                         behandlingOpprinnelse = when (behandling.opprettetÅrsak) {
                             BehandlingÅrsak.SØKNAD -> BehandlingOpprinnelse.MANUELL
                             BehandlingÅrsak.FØDSELSHENDELSE -> BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE
                             else -> BehandlingOpprinnelse.MANUELL
                         },
                         utbetalingsperioder = hentUtbetalingsperioder(behandlingId))
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
                    val personForAndel =
                            personopplysningGrunnlag.personer.find { person -> andel.personIdent == person.personIdent.ident }
                            ?: throw IllegalStateException("Fant ikke personopplysningsgrunnlag for andel")
                    UtbetalingsDetaljDVH(
                            person = PersonDVH(
                                    rolle = personForAndel.type.name,
                                    statsborgerskap = personForAndel.statsborgerskap.map { grStatsborgerskap: GrStatsborgerskap -> grStatsborgerskap.landkode },
                                    bostedsland = hentLandkode(personForAndel),
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
                            utbetaltPrMnd = andel.beløp,
                            delytelseId = behandling.fagsak.id.toString() + andel.periodeOffset
                    )
                }
        )

    }

    private fun hentLandkode(person: Person): String {
        return if (person.bostedsadresse != null) "NO" else {
            val landKode = personopplysningerService.hentLandkodeUtenlandskBostedsadresse(
                    person.personIdent.ident)
            if (landKode == PersonopplysningerService.UKJENT_LANDKODE) {
                LOG.error("Sender landkode ukjent til DVH. Bør undersøke om hvorfor. Ident i securelogger")
                secureLogger.error("Ukjent land sendt til DVH for person ${person.personIdent.ident}")
            }
            landKode
        }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
