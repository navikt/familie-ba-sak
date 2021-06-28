package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedFritekster
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakBegrunnelseRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilVedtaksperiodeType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.net.InetAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

// TODO: Fjern etter migrering
@Component
class VedtaksperioderMedBegrunnelserMigrering(
        private val behandlingRepository: BehandlingRepository,
        private val envService: EnvService,
        private val vedtakRepository: VedtakRepository,
        private val vedtakBegrunnelseRepository: VedtakBegrunnelseRepository,
        private val vedtaksperiodeService: VedtaksperiodeService,
        private val vedtaksperiodeRepository: VedtaksperiodeRepository,
) {

    /**
     * Idempotent migrering fra vedtakBegrunnelse tabell til vedtaksperiode med begrunnelser.
     * Denne migreringen går gjennom alle behandlinger sine vedtak og oppretter/legger til begrunnelser på vedtaksperioder.
     *
     * Den vil alltid prøve å legge til på en eksisterende vedtaksperiode, men oppretter om det ikke finnes. I tillegg legger den alltid til
     * begrunnelsene fra den persisterte perioden og vedtakBegrunnelse ved hver kjøring. Ettersom funksjonene som blir brukt erstatter
     * listene så vil vi alltid ha elementene fra den gamle modellen og den nye.
     */
    @Transactional
    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)
    fun migrer() {
        val erLeader = if (envService.erDev()) true else {
            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:4040"))
                    .GET()
                    .build()

            val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
            val hostname: String = InetAddress.getLocalHost().hostName
            logger.info("Respons=${response.body()}, hostname=$hostname")

            response.body().contains(hostname)
        }

        logger.info("Er leader: $erLeader")

        if (erLeader) {
            logger.info("Migrerer behandlinger opprettet før behandling initierer vedtak")
            val behandlinger = behandlingRepository.findAll()

            var vellykkedeMigreringer = 0
            var mislykkedeMigreringer = 0
            behandlinger.forEach { behandling ->
                Result.runCatching {
                    val vedtak = vedtakRepository.finnVedtakForBehandling(behandlingId = behandling.id)

                    // Per vedtak lager vi vedtaksperioder som lagres på hvert vedtak
                    vedtak.forEach { vedtakEntry ->

                        /**
                         * For å kunne ta vare på begrunnelsene fra ny modell og populere fra gammel
                         * må vi først hente de persisterte periodene, generere nye perioder også
                         * populere fra både den gamle modellen og de gamle persisterte periodene.
                         */
                        val persisterteVedtaksperioderMedForrigeBegrunnelser =
                                vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak = vedtakEntry)
                        vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(vedtak = vedtakEntry)
                        val persisterteVedtaksperioderMedBegrunnelserOgOppdaterteIder =
                                vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak = vedtakEntry)

                        val begrunnelser = vedtakBegrunnelseRepository.findByVedtakId(vedtakId = vedtakEntry.id)

                        // Gruppere begrunnelsene som er lagret på vedtaket med periode som nøkkel
                        val begrunnelserPåPeriode =
                                begrunnelser.fold(mutableMapOf<Periode, MutableList<VedtakBegrunnelse>>()) { acc, vedtakBegrunnelse ->
                                    val periode = Periode(fom = vedtakBegrunnelse.fom ?: TIDENES_MORGEN,
                                                          tom = vedtakBegrunnelse.tom ?: TIDENES_ENDE)

                                    if (acc[periode] == null) {
                                        acc[periode] = mutableListOf(vedtakBegrunnelse)
                                    } else {
                                        acc[periode]?.add(vedtakBegrunnelse)
                                    }

                                    acc
                                }

                        begrunnelserPåPeriode.forEach { (periode, vedtakBegrunnelser) ->
                            // Siden begrunnelsene kan ha forskjellig periodetype må vi generere vedtaksperioder per periodetype
                            val vedtaksperiodeTyper =
                                    vedtakBegrunnelser.fold(mutableSetOf<Vedtaksperiodetype>()) { acc, vedtaksbegrunnelse ->
                                        acc.add(vedtaksbegrunnelse.begrunnelse.vedtakBegrunnelseType.tilVedtaksperiodeType())
                                        acc
                                    }

                            vedtaksperiodeTyper.forEach { vedtaksperiodetype ->
                                val fom = if (periode.fom == TIDENES_MORGEN) null else periode.fom
                                val tom = if (periode.tom == TIDENES_ENDE) null else periode.tom

                                val persistertVedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser? =
                                        persisterteVedtaksperioderMedForrigeBegrunnelser.find { it.fom == fom && it.tom == tom && it.type == vedtaksperiodetype }

                                val persistertVedtaksperiodeMedBegrunnelserMedOppdatertId: VedtaksperiodeMedBegrunnelser? =
                                        persisterteVedtaksperioderMedBegrunnelserOgOppdaterteIder.find { it.fom == fom && it.tom == tom && it.type == vedtaksperiodetype }

                                val vedtaksperiodeMedBegrunnelser =
                                        persistertVedtaksperiodeMedBegrunnelserMedOppdatertId
                                        ?: vedtaksperiodeRepository.save(VedtaksperiodeMedBegrunnelser(
                                                fom = fom,
                                                tom = tom,
                                                type = vedtaksperiodetype,
                                                vedtak = vedtakEntry
                                        ))

                                vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
                                        vedtaksperiodeId = vedtaksperiodeMedBegrunnelser.id,
                                        restPutVedtaksperiodeMedStandardbegrunnelser = RestPutVedtaksperiodeMedStandardbegrunnelser(
                                                standardbegrunnelser = (
                                                        vedtakBegrunnelser.filter {
                                                            !it.begrunnelse.erFritekstBegrunnelse() &&
                                                            it.begrunnelse.vedtakBegrunnelseType.tilVedtaksperiodeType() == vedtaksperiodetype
                                                        }.map { vedtakBegrunnelse ->
                                                            vedtakBegrunnelse.begrunnelse
                                                        } + (persistertVedtaksperiodeMedBegrunnelser?.begrunnelser?.map { it.vedtakBegrunnelseSpesifikasjon }
                                                             ?: emptyList())).distinct()
                                        )
                                )

                                vedtaksperiodeService.oppdaterVedtaksperiodeMedFritekster(
                                        vedtaksperiodeId = vedtaksperiodeMedBegrunnelser.id,
                                        restPutVedtaksperiodeMedFritekster = RestPutVedtaksperiodeMedFritekster(
                                                fritekster = (
                                                        vedtakBegrunnelser
                                                                .filter {
                                                                    it.begrunnelse.erFritekstBegrunnelse() &&
                                                                    it.begrunnelse.vedtakBegrunnelseType.tilVedtaksperiodeType() == vedtaksperiodetype &&
                                                                    it.brevBegrunnelse != null
                                                                }
                                                                .map { vedtakBegrunnelse ->
                                                                    vedtakBegrunnelse.brevBegrunnelse
                                                                    ?: error("Brevbegrunnelse er ikke satt for fritekst på vedtakBegrunnelse med id ${vedtakBegrunnelse.id}")
                                                                } + (persistertVedtaksperiodeMedBegrunnelser?.fritekster?.map { it.fritekst }
                                                                     ?: emptyList())).distinct()
                                        )
                                )

                                val vedtaksperiodeMedBegrunnelserOppdatert = vedtaksperiodeRepository.hentVedtaksperiode(
                                        vedtaksperiodeId = vedtaksperiodeMedBegrunnelser.id
                                )
                            }
                        }
                    }
                }.onSuccess {
                    logger.info("Vellykket migrering for behandling ${behandling.id}")
                    vellykkedeMigreringer++
                }.onFailure {
                    logger.info("Migrering feilet for behandling ${behandling.id}")
                    secureLogger.info("Migrering feilet for behandling ${behandling.id}", it)
                    mislykkedeMigreringer++
                }


            }

            logger.info("Migrering av vedtaksbegrunnelser ferdig.\n" +
                        "Antall behandlinger=${behandlinger.size}\n" +
                        "Vellykede migreringer=$vellykkedeMigreringer\n" +
                        "Mislykkede migreringer=$mislykkedeMigreringer\n")
        }
    }

    companion object {

        val logger = LoggerFactory.getLogger(VedtaksperioderMedBegrunnelserMigrering::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}