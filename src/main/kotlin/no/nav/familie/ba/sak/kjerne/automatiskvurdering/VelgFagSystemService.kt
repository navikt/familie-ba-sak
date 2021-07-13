package no.nav.familie.ba.sak.kjerne.automatiskvurdering


import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import org.springframework.stereotype.Service

@Service
class VelgFagSystemService(
        private val fagsakService: FagsakService,
        private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
) {

    internal fun morHarLøpendeUtbetalingerIBA(fagsak: Fagsak?): Boolean {
        return fagsak?.status == FagsakStatus.LØPENDE

    }


    internal fun morHarSakerMenIkkeLøpendeUtbetalingerIBA(fagsak: Fagsak?): Boolean {
        return fagsak != null && fagsak.status != FagsakStatus.LØPENDE
    }

    internal fun morHarSakerMenIkkeLøpendeIInfotrygd(personIdent: PersonIdent): Boolean {
        return infotrygdBarnetrygdClient.harÅpenSakIInfotrygd(mutableListOf(personIdent.ident)) && !infotrygdBarnetrygdClient.harLøpendeSakIInfotrygd(
                mutableListOf(personIdent.ident))
    }


    fun velgFagsystem(nyBehandlingHendelse: NyBehandlingHendelse): FagsystemRegelVurdering {
        val morsPersonIdent = PersonIdent(nyBehandlingHendelse.morsIdent)
        val fagsak = fagsakService.hent(morsPersonIdent)

        return when {
            morHarLøpendeUtbetalingerIBA(fagsak) -> FagsystemRegelVurdering.SEND_TIL_BA
            infotrygdBarnetrygdClient.harLøpendeSakIInfotrygd(mutableListOf(morsPersonIdent.ident)) -> FagsystemRegelVurdering.SEND_TIL_INFOTRYGD
            morHarSakerMenIkkeLøpendeUtbetalingerIBA(fagsak) -> FagsystemRegelVurdering.SEND_TIL_BA
            morHarSakerMenIkkeLøpendeIInfotrygd(morsPersonIdent) -> FagsystemRegelVurdering.SEND_TIL_INFOTRYGD
            //enFødselsHendelsePerDag(fødselshendelse) -> FagsystemRegelVurdering.SEND_TIL_BA

            else -> FagsystemRegelVurdering.SEND_TIL_INFOTRYGD
        }
    }

}

enum class FagsystemRegelVurdering {
    SEND_TIL_BA,
    SEND_TIL_INFOTRYGD,
}