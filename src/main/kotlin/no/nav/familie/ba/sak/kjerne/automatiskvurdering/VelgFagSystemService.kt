package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import org.springframework.stereotype.Service

@Service
class VelgFagSystemService(
        private val fagsakService: FagsakService,
        private val personopplysningerService: PersonopplysningerService,
) {


    fun velgFagsystem(nyBehandlingHendelse: NyBehandlingHendelse): FagsystemRegelVurdering {

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(nyBehandlingHendelse.morsIdent)
        val relasjon = personopplysningerService.hentPersoninfoMedRelasjoner(nyBehandlingHendelse.morsIdent)

        return when {
            morHarLøpendeUtbetalingerIBA(fagsak) -> FagsystemRegelVurdering.SEND_TIL_BA
            //morHarLøpendeUtbetalingerIInfotrygd(infotrygdsak) -> FagsystemRegelVurdering.SEND_TIL_INFOTRYGD
            //morHarSakerMenIkkeLøpendeUtbetalingerIBA(fagsak) -> FagsystemRegelVurdering.SEND_TIL_BA
            //morHarSakerMenIkkeLøpendeIInfotrygd(fagsak) -> FagsystemRegelVurdering.SEND_TIL_INFOTRYGD
            //morHarBarnDerFarHarLøpendeUtbetalingIInfotrygd(fagsak) -> FagsystemRegelVurdering.SEND_TIL_INFOTRYGD

            else -> FagsystemRegelVurdering.SEND_TIL_INFOTRYGD
        }
    }

    enum class FagsystemRegelVurdering {
        SEND_TIL_BA,
        SEND_TIL_INFOTRYGD,
    }

}