package no.nav.familie.ba.sak.kjerne.automatiskvurdering


import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class VelgFagSystemService(
        private val fagsakService: FagsakService,
        private val infotrygdService: InfotrygdService,
        private val behandlingService: BehandlingService,
        private val personopplysningerService: PersonopplysningerService,
        private val envService: EnvService,
) {

    internal fun morHarSakerMenIkkeLøpendeIInfotrygd(morsIdent: String): Boolean {
        return infotrygdService.harÅpenSakIInfotrygd(mutableListOf(morsIdent)) && !infotrygdService.harLøpendeSakIInfotrygd(
                mutableListOf(morsIdent))
    }

    internal fun morHarLøpendeSakIInfotrygd(morsIdent: String): Boolean {
        return infotrygdService.harLøpendeSakIInfotrygd(mutableListOf(morsIdent))
    }

    internal fun erDagensFørsteFødselshendelse(): Boolean {
        if (envService.erProd()) {
            return behandlingService.hentDagensFødselshendelser().isEmpty()
        } else return behandlingService.hentDagensFødselshendelser().size <= 1000
    }

    internal fun harMorGyldigNorskstatsborger(morsIdent: Ident): Boolean {
        return personopplysningerService.hentStatsborgerskap(morsIdent).any {
            it.land == "NOR" && it.gyldigFraOgMed?.isBefore(LocalDate.now()) == true && (it.gyldigTilOgMed
                                                                                         ?: LocalDate.MAX).isAfter(
                    LocalDate.now())
        }

    }


    fun velgFagsystem(nyBehandlingHendelse: NyBehandlingHendelse): FagsystemRegelVurdering {
        val morsPersonIdent = PersonIdent(nyBehandlingHendelse.morsIdent)
        val fagsak = fagsakService.hent(morsPersonIdent)

        val (årsak: String, fagsystem: FagsystemRegelVurdering) = when {
            morHarLøpendeUtbetalingerIBA(fagsak) -> Pair("Mor har løpende utbetalinger i ba-sak",
                                                         FagsystemRegelVurdering.SEND_TIL_BA)
            morHarLøpendeSakIInfotrygd(nyBehandlingHendelse.morsIdent) -> Pair("Mor har løpende sak i infotrygd",
                                                                               FagsystemRegelVurdering.SEND_TIL_INFOTRYGD)
            morHarSakerMenIkkeLøpendeUtbetalingerIBA(fagsak) -> Pair("Mor har sak i ba-sak, men ikke løpende utbetalinger",
                                                                     FagsystemRegelVurdering.SEND_TIL_BA)
            morHarSakerMenIkkeLøpendeIInfotrygd(nyBehandlingHendelse.morsIdent) -> Pair("Mor har saker i infotrygd, men ikke løpende utbetalinger",
                                                                                        FagsystemRegelVurdering.SEND_TIL_INFOTRYGD)

            else -> Pair("Mor sin fødselshendelse sendes til ba-sak fordi ingen av filtreringene slo ut",
                         FagsystemRegelVurdering.SEND_TIL_BA)
        }

        secureLogger.info("Sender fødselshendelse for ${nyBehandlingHendelse.morsIdent} til $fagsystem med årsak $årsak")

        return fagsystem
    }

    companion object {

        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

enum class FagsystemRegelVurdering {
    SEND_TIL_BA,
    SEND_TIL_INFOTRYGD
}