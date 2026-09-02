package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PersonopplysningGrunnlagLagreService(
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
) {
    @Transactional
    fun lagreOgSlettGammelt(personopplysningGrunnlag: PersonopplysningGrunnlag): PersonopplysningGrunnlag {
        val aktivtPersonopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(personopplysningGrunnlag.behandlingId)

        if (aktivtPersonopplysningGrunnlag != null) {
            personopplysningGrunnlagRepository.delete(aktivtPersonopplysningGrunnlag)
            personopplysningGrunnlagRepository.flush()
        }

        secureLogger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter persongrunnlag $personopplysningGrunnlag")
        return personopplysningGrunnlagRepository.saveAndFlush(personopplysningGrunnlag)
    }
}
