package no.nav.familie.ba.sak.kjerne.automatiskvurdering


import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
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

        return when {
            morHarLøpendeUtbetalingerIBA(fagsak) -> FagsystemRegelVurdering.SEND_TIL_BA
            morHarLøpendeSakIInfotrygd(nyBehandlingHendelse.morsIdent) -> FagsystemRegelVurdering.SEND_TIL_INFOTRYGD
            morHarSakerMenIkkeLøpendeUtbetalingerIBA(fagsak) -> FagsystemRegelVurdering.SEND_TIL_BA
            morHarSakerMenIkkeLøpendeIInfotrygd(nyBehandlingHendelse.morsIdent) -> FagsystemRegelVurdering.SEND_TIL_INFOTRYGD

            else -> FagsystemRegelVurdering.SEND_TIL_BA
        }
    }

}

enum class FagsystemRegelVurdering {
    SEND_TIL_BA,
    SEND_TIL_INFOTRYGD
}