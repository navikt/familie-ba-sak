package no.nav.familie.ba.sak.kjerne.registrertsøknadstidspunkt

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.ekstern.restDomene.RegistrertSøknadstidspunktDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilRegistrertSøknadstidspunktDto
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class RegistrertSøknadstidspunktService(
    private val registrertSøknadstidspunktRepository: RegistrertSøknadstidspunktRepository,
    private val persongrunnlagService: PersongrunnlagService,
) {
    fun hentForBehandling(behandlingId: Long): List<RegistrertSøknadstidspunktDto> =
        registrertSøknadstidspunktRepository
            .findByBehandlingId(behandlingId)
            .map { it.tilRegistrertSøknadstidspunktDto() }

    /**
     * Lagrer søknadstidspunkt per person slik at det forhåndsutfylles ved gjenåpning av modalen,
     * også for personer som ikke gir en etterbetalingsandel. Erstatter eksisterende rader for behandlingen.
     */
    @Transactional
    fun lagre(
        behandling: Behandling,
        søknadstidspunktPerPerson: List<RegistrertSøknadstidspunktDto>,
    ) {
        if (søknadstidspunktPerPerson.any { it.søknadstidspunkt.isAfter(LocalDate.now()) }) {
            throw FunksjonellFeil("Søknadstidspunkt kan ikke være frem i tid.")
        }

        val aktørPerIdent =
            persongrunnlagService
                .hentPersonerPåBehandling(søknadstidspunktPerPerson.map { it.personIdent }, behandling)
                .associate { it.aktør.aktivFødselsnummer() to it.aktør }

        registrertSøknadstidspunktRepository.deleteByBehandlingId(behandling.id)

        registrertSøknadstidspunktRepository.saveAll(
            søknadstidspunktPerPerson.map { dto ->
                RegistrertSøknadstidspunkt(
                    behandlingId = behandling.id,
                    aktør =
                        aktørPerIdent[dto.personIdent]
                            ?: throw FunksjonellFeil("Fant ikke person ${dto.personIdent} på behandling ${behandling.id}"),
                    søknadstidspunkt = dto.søknadstidspunkt,
                )
            },
        )
    }
}
