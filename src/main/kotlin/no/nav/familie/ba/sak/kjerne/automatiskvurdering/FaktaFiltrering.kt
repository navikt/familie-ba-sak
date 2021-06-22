package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import java.time.LocalDate

data class FaktaFiltrering(private val morFnr: String,private val morLever: Boolean,private val barnLever: Boolean, private val barnMindreEnnFemMnd: Boolean, private val morOver18: Boolean, private val morHarIkkeVerge: Boolean)
// har ignorert roed regel
//@JsonIgnore val dagensDato: LocalDate = LocalDate.now())
{


    /*val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                                   ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")
    // private val barnaFraHendelse = personopplysningGrunnlag.barna.filter { barnasIdenter.contains(it.personIdent.ident) }


    private val morFnr: String = personopplysningGrunnlag.søker.personIdent.ident
    private val morLever: Boolean = !personopplysningerService.hentDødsfall(Ident(morFnr)).erDød
    private val barnLever: Boolean = !barnasIdenter.any { personopplysningerService.hentDødsfall(Ident(it)).erDød }

    /* val barnMindreEnnFemMnd: Boolean = barnaFraHendelse.all { barnFraHendelse ->
         restenAvBarna.all {
             barnFraHendelse.fødselsdato.isAfter(it.fødselsdato.plusMonths(5)) ||
             barnFraHendelse.fødselsdato.isBefore(it.fødselsdato.plusDays(6))
         }
     }*/
    private val morOver18: Boolean = personopplysningGrunnlag.søker.fødselsdato.plusYears(18).isBefore(dagensDato)
    private val morHarIkkeVerge: Boolean = !personopplysningerService.hentVergeData(Ident(morFnr)).harVerge*/


    fun søkerPassererFiltering(): Boolean {
        return (morLever && barnLever && barnMindreEnnFemMnd && morOver18 && morHarIkkeVerge)
    }
}

//fun toJson(): String =
//      objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)