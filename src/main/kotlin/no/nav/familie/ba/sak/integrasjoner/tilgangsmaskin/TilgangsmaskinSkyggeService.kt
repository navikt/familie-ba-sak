package no.nav.familie.ba.sak.integrasjoner.tilgangsmaskin

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.tilgangsmaskin.Avvisningskode
import no.nav.familie.tilgangsmaskin.Regeltype
import no.nav.familie.tilgangsmaskin.TilgangsmaskinException
import no.nav.familie.tilgangsmaskin.TilgangsmaskinKlient
import no.nav.familie.tilgangsmaskin.TilgangsmaskinResultat
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class TilgangsmaskinSkyggeService(
    private val tilgangsmaskinKlient: TilgangsmaskinKlient,
    private val featureToggleService: FeatureToggleService,
) {
    private val sammenlignetTeller = Metrics.counter("tilgangsmaskin.skygge.sammenlignet")
    private val manglendeSvarTeller = Metrics.counter("tilgangsmaskin.skygge.manglende.svar")

    fun skyggeSjekkTilgangTilPersoner(
        personIdenter: List<String>,
        tilgangerFraIntegrasjoner: Map<String, Tilgang>,
    ) {
        try {
            if (SikkerhetContext.erSystemKontekst()) return
            if (!featureToggleService.isEnabled(FeatureToggle.SKAL_SKYGGEKJØRE_TILGANGSMASKINEN)) return

            val (manglendeSvar, resultaterFraTilgangsmaskinen) =
                tilgangsmaskinKlient
                    .sjekkTilgangTilPersoner(personIdenter.toSet(), Regeltype.KJERNE_REGELTYPE)
                    .partition { it.erManglendeSvar() }
            if (manglendeSvar.isNotEmpty()) {
                manglendeSvarTeller.increment(manglendeSvar.size.toDouble())
                logger.warn(
                    "Tilgangsmaskin-skygge: fikk ikke svar for ${manglendeSvar.size} av " +
                        "${manglendeSvar.size + resultaterFraTilgangsmaskinen.size} identer, disse sammenlignes ikke.",
                )
            }
            sammenlignetTeller.increment(resultaterFraTilgangsmaskinen.size.toDouble())

            val avvik =
                resultaterFraTilgangsmaskinen.mapNotNull { nyttResultat ->
                    val gammelTilgang = tilgangerFraIntegrasjoner[nyttResultat.personIdent] ?: return@mapNotNull null
                    if (gammelTilgang.harTilgang != nyttResultat.harTilgang) gammelTilgang to nyttResultat else null
                }
            if (avvik.isEmpty()) {
                logger.info("Tilgangsmaskin-skygge: sammenlignet ${resultaterFraTilgangsmaskinen.size} identer, ingen avvik.")
                return
            }

            avvik.forEach { (gammelTilgang, nyttResultat) -> loggAvvik(gammelTilgang, nyttResultat) }

            val avvisningskoder = avvik.mapNotNull { (_, nyttResultat) -> nyttResultat.avvisningskode }.groupingBy { it }.eachCount()
            val traceIder = avvik.mapNotNull { (_, nyttResultat) -> nyttResultat.traceId }

            logger.warn(
                "Tilgangsmaskin-skygge: ${avvik.size} av ${resultaterFraTilgangsmaskinen.size} identer hadde avvik. " +
                    "Avvisningskoder=$avvisningskoder, traceIder=$traceIder. Se securelogs for detaljer.",
            )
        } catch (exception: Exception) {
            // Skyggingen skal aldri påvirke den gjeldende tilgangskontrollen.
            val rotårsak = NestedExceptionUtils.getMostSpecificCause(exception)
            val httpStatus = (exception as? TilgangsmaskinException)?.httpStatus
            Metrics
                .counter(
                    "tilgangsmaskin.skygge.feilet",
                    "feiltype",
                    rotårsak.javaClass.simpleName,
                    "httpStatus",
                    httpStatus?.toString() ?: "INGEN",
                ).increment()
            logger.warn("Tilgangsmaskin-skygge feilet: ${rotårsak.javaClass.simpleName}${httpStatus?.let { " (HTTP $it)" } ?: ""}")
            secureLogger.warn("Tilgangsmaskin-skygge feilet", exception)
        }
    }

    private fun TilgangsmaskinResultat.erManglendeSvar(): Boolean =
        !harTilgang &&
            httpStatus == HttpStatus.INTERNAL_SERVER_ERROR.value() &&
            avvisningskode == Avvisningskode.UKJENT

    private fun loggAvvik(
        gammelTilgang: Tilgang,
        nyttResultat: TilgangsmaskinResultat,
    ) {
        val retning = if (nyttResultat.harTilgang) Avviksretning.NY_MILDERE else Avviksretning.NY_STRENGERE
        Metrics
            .counter(
                "tilgangsmaskin.skygge.avvik",
                "retning",
                retning.tag,
                "avvisningskode",
                nyttResultat.avvisningskode?.name ?: "INGEN",
            ).increment()
        secureLogger.warn(
            "Tilgangsmaskin-skygge avvik (${retning.tag}) for ident ${nyttResultat.personIdent}: " +
                "integrasjoner harTilgang=${gammelTilgang.harTilgang} (begrunnelse=${gammelTilgang.begrunnelse}), " +
                "tilgangsmaskinen harTilgang=${nyttResultat.harTilgang} (avvisningskode=${nyttResultat.avvisningskode}, " +
                "begrunnelse=${nyttResultat.begrunnelse}, kanOverstyres=${nyttResultat.kanOverstyres}, " +
                "httpStatus=${nyttResultat.httpStatus}, traceId=${nyttResultat.traceId})",
        )
    }

    private enum class Avviksretning(
        val tag: String,
    ) {
        NY_MILDERE("ny-mildere"),
        NY_STRENGERE("ny-strengere"),
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TilgangsmaskinSkyggeService::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
