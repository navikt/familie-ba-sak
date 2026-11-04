package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FILTRERINGSREGLER_SØKNAD
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.Filtreringsregel
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FiltreringsregelEvaluator
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene.FiltreringResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene.FiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene.slåSammenIkkeOppfylteBegrunnelser
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
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

    fun hentBegrunnelseForStoppIFiltreringsreglene(behandlingId: Long): String? {
        // Oppgaven fordeles før barna er med i persongrunnlaget, så den kan havne hos en enhet uten tilgang til adressebeskyttede personer
        val ikkeOppfylteResultaterUtenAdressebeskyttelse =
            filtreringResultatRepository
                .finnFiltreringResultater(behandlingId)
                .filter { it.resultat == Resultat.IKKE_OPPFYLT && it.filtreringsregel !in FILTRERINGSREGLER_OM_ADRESSEBESKYTTELSE }

        return if (ikkeOppfylteResultaterUtenAdressebeskyttelse.isEmpty()) {
            null
        } else {
            ikkeOppfylteResultaterUtenAdressebeskyttelse.slåSammenIkkeOppfylteBegrunnelser(behandlingId)
        }
    }

    companion object {
        private val FILTRERINGSREGLER_OM_ADRESSEBESKYTTELSE =
            setOf(
                Filtreringsregel.Identifikator.SØKER_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19,
                Filtreringsregel.Identifikator.BARN_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19,
            )
    }
}
