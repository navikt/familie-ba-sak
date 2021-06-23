package no.nav.familie.ba.sak.kjerne.automatiskvurdering;

import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.springframework.stereotype.Service;


@Service
class EvaluerFiltreringsregler(
    private val personopplysningerService: PersonopplysningerService,
    private val localDateService: LocalDateService
) {
    private fun lagFiltrering(morsIndent: String, barnasIdenter: Set<String>): FiltreringIAutomatiskBehandling {
        val barnaFraHendelse = personopplysningerService.hentPersoninfoMedRelasjoner(morsIndent).forelderBarnRelasjon

        val morFnr: Boolean = morsIndent.isEmpty()
        val barnFnr: Boolean = !barnasIdenter.any {
            it.isEmpty()
        }


        val morLever: Boolean = !personopplysningerService.hentDødsfall(Ident(morsIndent)).erDød
        val barnLever: Boolean = !barnasIdenter.any {
            personopplysningerService.hentDødsfall(Ident(it)).erDød
        }


        val restenAvBarna = finnRestenAvBarnasPersonInfo(morsIndent, barnaFraHendelse)

        val barnMindreEnnFemMnd: Boolean = toBarnPåFemMnd(barnaFraHendelse, restenAvBarna)



        personopplysningerService.hentPersoninfoMedRelasjoner(morsIndent).forelderBarnRelasjon
        val morOver18: Boolean = personopplysningerService.hentPersoninfo(morsIndent).fødselsdato.plusYears(18)
            .isBefore(localDateService.now())
        val morHarIkkeVerge: Boolean = !personopplysningerService.hentVergeData(Ident(morsIndent)).harVerge

        return FiltreringIAutomatiskBehandling(
            morFnr,
            barnFnr,
            morLever,
            barnLever,
            barnMindreEnnFemMnd,
            morOver18,
            morHarIkkeVerge
        )
    }


    fun automatiskBehandlingEvaluering(morsIdent: String, barnasIdenter: Set<String>): Pair<Boolean, String> {
        val filtrering = lagFiltrering(morsIdent, barnasIdenter)
        return Pair(filtrering.søkerPassererFiltering(), filtrering.hentBegrunnelseFraFiltrering())
    }

    private fun toBarnPåFemMnd(barnaFraHendelse: Set<ForelderBarnRelasjon>, restenAvBarna: List<PersonInfo>): Boolean {
        return barnaFraHendelse.all { barnFraHendelse ->
            restenAvBarna.all {
                (barnFraHendelse.fødselsdato != null && barnFraHendelse.fødselsdato.isAfter(
                    it.fødselsdato.plusMonths(
                        5
                    )
                )) ||
                        (barnFraHendelse.fødselsdato != null && barnFraHendelse.fødselsdato?.isBefore(
                            it.fødselsdato.plusDays(
                                6
                            )
                        ))
            }
        }
    }

    private fun finnRestenAvBarnasPersonInfo(
        morsIndent: String,
        barnaFraHendelse: Set<ForelderBarnRelasjon>
    ): List<PersonInfo> {
        val barna =
            personopplysningerService.hentPersoninfoMedRelasjoner(morsIndent).forelderBarnRelasjon.filter {
                it.relasjonsrolle == FORELDERBARNRELASJONROLLE.BARN && barnaFraHendelse.none { barn ->
                    barn.personIdent.id == it.personIdent.id
                }
            }
        return barna.map {
            personopplysningerService.hentPersoninfoMedRelasjoner(it.personIdent.id)
        }
    }
}
