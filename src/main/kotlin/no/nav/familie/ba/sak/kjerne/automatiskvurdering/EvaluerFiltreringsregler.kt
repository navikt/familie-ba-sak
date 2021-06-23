package no.nav.familie.ba.sak.kjerne.automatiskvurdering;

import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.springframework.stereotype.Service


@Service
class EvaluerFiltreringsregler(
    private val personopplysningerService: PersonopplysningerService,
    private val localDateService: LocalDateService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

    ) {
    private fun lagFiltrering(
        morsIndent: String,
        barnasIdenter: Set<String>,
        behandling: Behandling
    ): FiltreringIAutomatiskBehandling {

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
            ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")

        val barnaFraHendelse = personopplysningGrunnlag.barna.filter { barnasIdenter.contains(it.personIdent.ident) }

        val morFnr: Boolean = morsIndent.isEmpty()
        val barnFnr: Boolean = !barnasIdenter.any {
            it.isEmpty()
        }


        val morLever: Boolean = !personopplysningerService.hentDødsfall(Ident(morsIndent)).erDød
        val barnLever: Boolean = !barnasIdenter.any {
            personopplysningerService.hentDødsfall(Ident(it)).erDød
        }


        val restenAvBarna = finnRestenAvBarnasPersonInfo(morsIndent, barnaFraHendelse)

        val toBarnPåFemMnd: Boolean = toBarnPåFemMnd(barnaFraHendelse.toSet(), restenAvBarna)



        personopplysningerService.hentPersoninfoMedRelasjoner(morsIndent).forelderBarnRelasjon
        val morOver18: Boolean = personopplysningerService.hentPersoninfo(morsIndent).fødselsdato.plusYears(18)
            .isBefore(localDateService.now())
        val morHarIkkeVerge: Boolean = !personopplysningerService.hentVergeData(Ident(morsIndent)).harVerge

        return FiltreringIAutomatiskBehandling(
            morFnr,
            barnFnr,
            morLever,
            barnLever,
            toBarnPåFemMnd,
            morOver18,
            morHarIkkeVerge
        )
    }


    fun automatiskBehandlingEvaluering(
        morsIdent: String,
        barnasIdenter: Set<String>,
        behandling: Behandling
    ): Pair<Boolean, String> {
        val filtrering = lagFiltrering(morsIdent, barnasIdenter, behandling)
        return Pair(filtrering.søkerPassererFiltering(), filtrering.hentBegrunnelseFraFiltrering())
    }

    private fun toBarnPåFemMnd(barnaFraHendelse: Set<Person>, restenAvBarna: List<PersonInfo>): Boolean {
        return barnaFraHendelse.all { barn ->
            restenAvBarna.all {
                barn.fødselsdato.isAfter(it.fødselsdato.plusMonths(5)) ||
                        barn.fødselsdato.isBefore(it.fødselsdato.plusDays(6))
            }
        }
    }


    private fun finnRestenAvBarnasPersonInfo(
        morsIndent: String,
        barnaFraHendelse: List<Person>
    ): List<PersonInfo> {
        return personopplysningerService.hentPersoninfoMedRelasjoner(morsIndent).forelderBarnRelasjon.filter {
            it.relasjonsrolle == FORELDERBARNRELASJONROLLE.BARN && barnaFraHendelse.none { barn -> barn.personIdent.ident == it.personIdent.id }
        }.map {
            personopplysningerService.hentPersoninfoMedRelasjoner(it.personIdent.id)
        }
    }
}

