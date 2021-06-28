package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import org.springframework.stereotype.Service

@Service
class VelgFagSystemService(
        private val fagsakRepository: FagsakRepository,
        private val fagsakService: FagsakService,
        private val infotrygd: InfotrygdService,

        ) {


    fun velgFagsystem(nyBehandlingHendelse: NyBehandlingHendelse): FagsystemRegelVurdering {

        val personIdent = PersonIdent(ident = nyBehandlingHendelse.morsIdent)

        return when {
            morHarLøpendeUtbetalingerIBA(personIdent) -> FagsystemRegelVurdering.SEND_TIL_BA
            morHarLøpendeUtbetalingerIInfotrygd(personIdent) -> FagsystemRegelVurdering.SEND_TIL_INFOTRYGD
            morHarSakerMenIkkeLøpendeUtbetalingerIBA(personIdent) -> FagsystemRegelVurdering.SEND_TIL_BA
            morHarSakerMenIkkeLøpendeIInfotrygd(personIdent) -> FagsystemRegelVurdering.SEND_TIL_INFOTRYGD
            morHarBarnDerFarHarLøpendeUtbetalingIInfotrygd(personIdent) -> FagsystemRegelVurdering.SEND_TIL_INFOTRYGD

            else -> FagsystemRegelVurdering.SEND_TIL_INFOTRYGD
        }
    }

    internal fun morHarLøpendeUtbetalingerIBA(personIdent: PersonIdent): Boolean {
        // Denne funksjonen er true i det tilfellet FagsakStatus er LØPENDE. Må simulere stegprosess slik at fagsaken får LØPENDE.
        return true
        // return fagsakService.hent(personIdent)?.status?.equals(FagsakStatus.LØPENDE) ?: false
    }

    internal fun morHarLøpendeUtbetalingerIInfotrygd(personIdent: PersonIdent): Boolean {
        return false
        //return infotrygd.harLøpendeSakIInfotrygd(mutableListOf(personIdent.ident))
    }

    internal fun morHarSakerMenIkkeLøpendeUtbetalingerIBA(personIdent: PersonIdent): Boolean {
        return false
        //return fagsakService.hent(personIdent)?.status ?: FagsakStatus.LØPENDE != FagsakStatus.LØPENDE
    }

    internal fun morHarSakerMenIkkeLøpendeIInfotrygd(personIdent: PersonIdent): Boolean {
        return false
    }

    internal fun morHarBarnDerFarHarLøpendeUtbetalingIInfotrygd(personIdent: PersonIdent): Boolean {
        return false
    }

    enum class FagsystemRegelVurdering {
        SEND_TIL_BA,
        SEND_TIL_INFOTRYGD,
    }

}