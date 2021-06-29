package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.springframework.stereotype.Service


@Service
class FiltreringsreglerService(
        private val personopplysningerService: PersonopplysningerService,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        ) {

    fun hentDataOgKjørFiltreringsregler(
            morsIdent: String,
            barnasIdenter: Set<String>,
            behandling: Behandling,
    ): FiltreringsreglerResultat {

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                                       ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")
        val barnaFraHendelse = personopplysningGrunnlag.barna.filter { barnasIdenter.contains(it.personIdent.ident) }

        return evaluerFiltreringsregler(
                mor = personopplysningGrunnlag.søker,
                barnaFraHendelse = barnaFraHendelse,
                restenAvBarna = finnRestenAvBarnasPersonInfo(morsIdent, barnaFraHendelse),
                morLever = !personopplysningerService.hentDødsfall(Ident(morsIdent)).erDød,
                barnaLever = !barnasIdenter.any { personopplysningerService.hentDødsfall(Ident(it)).erDød },
                morHarVerge = !personopplysningerService.harVerge(morsIdent).harVerge,
        )
    }

    internal fun finnRestenAvBarnasPersonInfo(morsIndent: String, barnaFraHendelse: List<Person>): List<PersonInfo> {
        return personopplysningerService.hentPersoninfoMedRelasjoner(morsIndent).forelderBarnRelasjon.filter {
            it.relasjonsrolle == FORELDERBARNRELASJONROLLE.BARN && barnaFraHendelse.none { barn -> barn.personIdent.ident == it.personIdent.id }
        }.map {
            personopplysningerService.hentPersoninfoMedRelasjoner(it.personIdent.id)
        }
    }
}

