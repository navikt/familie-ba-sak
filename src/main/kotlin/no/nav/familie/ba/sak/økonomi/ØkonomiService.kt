package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.Beregning
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.vedtak.PersonBeregningType
import no.nav.familie.ba.sak.behandling.domene.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakResultat
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.fpsak.tidsserie.LocalDateSegment
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import java.math.BigDecimal

@Service
class ØkonomiService(
        private val økonomiKlient: ØkonomiKlient,
        private val beregning: Beregning,
        private val behandlingService: BehandlingService
) {

    fun iverksettVedtak(behandlingsId: Long, vedtakId: Long, saksbehandlerId: String) {
        val vedtak = behandlingService.hentVedtak(vedtakId)
                     ?: throw Error("Fant ikke vedtak med id $vedtakId i forbindelse med iverksetting mot oppdrag")

        val erOpphør = vedtak.resultat==VedtakResultat.OPPHØRT

        val personberegninger = if(erOpphør)
                behandlingService.hentPersonerForVedtak(vedtak.forrigeVedtakId!!)
                else behandlingService.hentPersonerForVedtak(vedtak.id)

        val tidslinjeMap = beregning.beregnUtbetalingsperioder(personberegninger,::betalingstypeTilKlassifisering)
        val utbetalingsperioder = tidslinjeMap.flatMap {
            (klassifisering,tidslinje) -> tidslinje.toSegments()
                // Må forsikre oss om at tidslinjesegmentene er i samme rekkefølge for å få konsekvent periodeId
                // . Sorter etter fraDato, sats, og evt til dato
                .sortedWith(compareBy<LocalDateSegment<Int>>({it.fom},{it.value},{it.tom}))
                .mapIndexed { indeks, segment->
                    Utbetalingsperiode(
                            erEndringPåEksisterendePeriode = erOpphør,
                            opphør = vedtak.opphørsdato?.let { Opphør(it) },
                            datoForVedtak = vedtak.vedtaksdato,
                            klassifisering = klassifisering,
                            vedtakdatoFom = segment.fom,
                            vedtakdatoTom = segment.tom,
                            sats = BigDecimal(segment.value),
                            satsType = Utbetalingsperiode.SatsType.MND,
                            utbetalesTil = vedtak.behandling.fagsak.personIdent.ident.toString(),
                            behandlingId = vedtak.behandling.id!!,
                            // Denne måten å sette periodeId på krever at vedtak.id inkrementeres i store nok steg, f.eks 50 og 50
                            // Og at måten segmentene bygges opp på ikke endrer seg, dvs det kommer ALLTID i samme rekkefølge
                            periodeId = (if(!erOpphør) vedtakId else vedtak.forrigeVedtakId!!) + indeks.toLong()
                    )
        }}

        val utbetalingsoppdrag = Utbetalingsoppdrag(
                saksbehandlerId = saksbehandlerId,
                kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
                fagSystem = FAGSYSTEM,
                saksnummer = vedtak.behandling.fagsak.id.toString(),
                aktoer = vedtak.behandling.fagsak.personIdent.ident.toString(),
                utbetalingsperiode = utbetalingsperioder
        )

        Result.runCatching { økonomiKlient.iverksettOppdrag(utbetalingsoppdrag) }
                .fold(
                        onSuccess = {
                            Assert.notNull(it.body, "Finner ikke ressurs")
                            Assert.notNull(it.body?.data, "Ressurs mangler data")
                            Assert.isTrue(it.body?.status == Ressurs.Status.SUKSESS,
                                          String.format("Ressurs returnerer %s men har http status kode %s",
                                                        it.body?.status,
                                                        it.statusCode))

                            behandlingService.oppdaterStatusPåBehandling(behandlingsId, BehandlingStatus.SENDT_TIL_IVERKSETTING)
                        },
                        onFailure = {
                            throw Exception("Iverksetting mot oppdrag feilet", it)
                        }
                )
    }

    private fun betalingstypeTilKlassifisering(type: PersonBeregningType) : String {
        return "BATR"
    }

    fun hentStatus(statusFraOppdragDTO: StatusFraOppdragDTO): OppdragProtokollStatus {
        Result.runCatching { økonomiKlient.hentStatus(statusFraOppdragDTO) }
                .fold(
                        onSuccess = {
                            Assert.notNull(it.body, "Finner ikke ressurs")
                            Assert.notNull(it.body?.data, "Ressurs mangler data")
                            Assert.isTrue(it.body?.status == Ressurs.Status.SUKSESS,
                                          String.format("Ressurs returnerer %s men har http status kode %s",
                                                        it.body?.status,
                                                        it.statusCode))

                            return objectMapper.convertValue(it.body?.data, OppdragProtokollStatus::class.java)
                        },
                        onFailure = {
                            throw Exception("Henting av status mot oppdrag feilet", it)
                        }
                )
    }

}
