package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fødselshendelse.FødselshendelseService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import org.springframework.stereotype.Service

@Service
class VelgFagSystemService(
        private val fødselshendelseService: FødselshendelseService,
        private val fagsakRepository: FagsakRepository,
        private val personopplysningerService: PersonopplysningerService,
        private val infotrygd: InfotrygdService,
) {

    fun velgFagsystem(nyBehandlingHendelse: NyBehandlingHendelse): RegelVurdering {

        val personIdent = PersonIdent(ident = nyBehandlingHendelse.morsIdent)

        if (morHarLøpendeUtbetalingerIBA(personIdent)) {
            return RegelVurdering.SEND_TIL_BA
        }

        if (morHarLøpendeUtbetalingerIInfotrygd(personIdent)) {
            return RegelVurdering.SEND_TIL_INFOTRYGD
        }

        if (morHarSakerMenIkkeLøpendeUtbetalingerIBA(personIdent)) {
            return RegelVurdering.SEND_TIL_BA
        }

        if (morHarSakerMenIkkeLøpendeUtbetalingerIBA(personIdent)) {
            return RegelVurdering.SEND_TIL_BA
        }

        if (morHarSakerMenIkkeLøpendeIInfotrygd(personIdent)) {
            return RegelVurdering.SEND_TIL_INFOTRYGD
        }

        if (morHarBarnDerFarHarLøpendeUtbetalingIInfotrygd(personIdent)) {
            return RegelVurdering.SEND_TIL_INFOTRYGD
        }

        return RegelVurdering.SEND_TIL_INFOTRYGD
    }

    internal fun morHarLøpendeUtbetalingerIBA(personIdent: PersonIdent): Boolean {
        return fagsakRepository.finnFagsakForPersonIdent(personIdent)?.status?.equals(FagsakStatus.LØPENDE) ?: false
    }

    internal fun morHarLøpendeUtbetalingerIInfotrygd(personIdent: PersonIdent): Boolean {
        return infotrygd.harLøpendeSakIInfotrygd(mutableListOf(personIdent.ident))
    }

    internal fun morHarSakerMenIkkeLøpendeUtbetalingerIBA(personIdent: PersonIdent): Boolean {
        return fagsakRepository.finnFagsakForPersonIdent(personIdent)?.status ?: FagsakStatus.LØPENDE != FagsakStatus.LØPENDE
    }

    internal fun morHarSakerMenIkkeLøpendeIInfotrygd(personIdent: PersonIdent): Boolean {
        return false
    }

    internal fun morHarBarnDerFarHarLøpendeUtbetalingIInfotrygd(personIdent: PersonIdent): Boolean {
        return false
    }

    enum class RegelVurdering {
        SEND_TIL_BA,
        SEND_TIL_INFOTRYGD,
    }

}