package no.nav.familie.ba.sak.kjerne.automatiskvurdering


import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VelgFagSystemService(
        private val fagsakService: FagsakService,
        private val infotrygdService: InfotrygdService,
) {


    internal fun morHarSakerMenIkkeLøpendeIInfotrygd(morsIdent: String): Boolean {
        return infotrygdService.harÅpenSakIInfotrygd(mutableListOf(morsIdent)) && !infotrygdService.harLøpendeSakIInfotrygd(
                mutableListOf(morsIdent))
    }

    internal fun morHarLøpendeSakIInfotrygd(morsIdent: String): Boolean {
        return infotrygdService.harLøpendeSakIInfotrygd(mutableListOf(morsIdent))
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