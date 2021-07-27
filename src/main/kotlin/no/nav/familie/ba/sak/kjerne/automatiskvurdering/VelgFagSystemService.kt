package no.nav.familie.ba.sak.kjerne.automatiskvurdering


import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.Ident
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
            it.land == "NOK" && it.gyldigFraOgMed?.isBefore(LocalDate.now()) == true && (it.gyldigTilOgMed
                                                                                         ?: LocalDate.MAX).isAfter(
                    LocalDate.now())
        }

    }


    fun velgFagsystem(nyBehandlingHendelse: NyBehandlingHendelse): FagsystemRegelVurdering {
        val morsPersonIdent = PersonIdent(nyBehandlingHendelse.morsIdent)
        val fagsak = fagsakService.hent(morsPersonIdent)


        return when {
            morHarLøpendeUtbetalingerIBA(fagsak) -> FagsystemRegelVurdering.SEND_TIL_BA
            morHarLøpendeSakIInfotrygd(nyBehandlingHendelse.morsIdent) -> FagsystemRegelVurdering.SEND_TIL_INFOTRYGD
            morHarSakerMenIkkeLøpendeUtbetalingerIBA(fagsak) -> FagsystemRegelVurdering.SEND_TIL_BA
            morHarSakerMenIkkeLøpendeIInfotrygd(nyBehandlingHendelse.morsIdent) -> FagsystemRegelVurdering.SEND_TIL_INFOTRYGD
            erDagensFørsteFødselshendelse() && harMorGyldigNorskstatsborger(Ident(morsPersonIdent.ident)) -> FagsystemRegelVurdering.SEND_TIL_BA

            else -> FagsystemRegelVurdering.SEND_TIL_INFOTRYGD
        }
    }

}

enum class FagsystemRegelVurdering {
    SEND_TIL_BA,
    SEND_TIL_INFOTRYGD
}