package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FILTRERINGSREGLER_SØKNAD
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FiltreringsregelEvaluator
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene.FiltreringResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene.FiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.erOppfylt
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsreglerFødselshendelseService.Companion.logger
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.steg.FiltrerAutomatiskBehandlingData
import org.springframework.stereotype.Service

@Service
class FiltreringsreglerSøknadService(
    private val filtreringResultatRepository: FiltreringResultatRepository,
    private val filtreringsregelEvaluator: FiltreringsregelEvaluator,
    private val faktaOppretter: FaktaOppretter,
    private val metrikker: Metrikker,
) {
    fun kjørFiltreringsregler(
        filtrerAutomatiskBehandlingData: FiltrerAutomatiskBehandlingData,
        behandling: Behandling,
    ): List<FiltreringResultat> {
        val fakta = faktaOppretter.opprettFakta(filtrerAutomatiskBehandlingData, behandling)

        val evalueringer = filtreringsregelEvaluator.evaluerFiltreringsregler(FILTRERINGSREGLER_SØKNAD, fakta)

        logger.info("Resultater fra filtreringsregler på behandling $behandling: ${evalueringer.map { "${it.identifikator}: ${it.resultat}" }}")
        if (!evalueringer.erOppfylt()) {
            secureLogger.info("Resultater fra filtreringsregler på behandling $behandling: (Fakta: ${fakta.convertDataClassToJson()}): ${evalueringer.map { "${it.identifikator}: ${it.resultat}" }}")
        }

        metrikker.oppdaterMetrikker(evalueringer)

        val filtreringsresultater = evalueringer.map { FiltreringResultat.opprett(behandling.id, fakta, it) }

        return filtreringResultatRepository.saveAll(filtreringsresultater)
    }
}
