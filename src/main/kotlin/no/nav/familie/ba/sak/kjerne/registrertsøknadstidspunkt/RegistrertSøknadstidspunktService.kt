package no.nav.familie.ba.sak.kjerne.registrertsøknadstidspunkt

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.ekstern.restDomene.RegistrertSøknadstidspunktDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilRegistrertSøknadstidspunktDto
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class RegistrertSøknadstidspunktService(
    private val registrertSøknadstidspunktRepository: RegistrertSøknadstidspunktRepository,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService,
) {
    fun hentForBehandling(behandlingId: Long): List<RegistrertSøknadstidspunktDto> =
        registrertSøknadstidspunktRepository
            .findByBehandlingId(behandlingId)
            .map { it.tilRegistrertSøknadstidspunktDto() }

    /**
     * For søknadsbehandlinger settes søknad mottatt-dato som default registrert søknadstidspunkt per barn,
     * slik at etterbetaling beregnes ut fra denne datoen. Saksbehandler kan korrigere via menyvalget
     * «Endre søknadstidspunkt» (for EØS er søknad mottatt-dato dagens dato og må typisk korrigeres).
     * Setter kun for barn som ikke allerede har et registrert søknadstidspunkt, slik at korrigeringer
     * består dersom steget kjøres på nytt.
     */
    @Transactional
    fun settDefaultSøknadstidspunktForBarn(behandling: Behandling) {
        if (behandling.opprettetÅrsak != BehandlingÅrsak.SØKNAD) return

        val søknadMottattDato = behandlingSøknadsinfoService.hentSøknadMottattDato(behandling.id)?.toLocalDate() ?: return

        val aktørerMedRegistrertSøknadstidspunkt =
            registrertSøknadstidspunktRepository.findByBehandlingId(behandling.id).map { it.aktør }.toSet()

        val nyeRegistrerteSøknadstidspunkt =
            persongrunnlagService
                .hentBarna(behandling)
                .filter { it.aktør !in aktørerMedRegistrertSøknadstidspunkt }
                .map {
                    RegistrertSøknadstidspunkt(
                        behandlingId = behandling.id,
                        aktør = it.aktør,
                        søknadstidspunkt = søknadMottattDato,
                    )
                }

        registrertSøknadstidspunktRepository.saveAll(nyeRegistrerteSøknadstidspunkt)
    }

    /**
     * Lagrer søknadstidspunkt per person slik at det forhåndsutfylles ved gjenåpning av modalen,
     * også for personer som ikke gir en etterbetalingsandel. Erstatter eksisterende rader for behandlingen.
     */
    @Transactional
    fun lagre(
        behandling: Behandling,
        søknadstidspunktPerPerson: List<RegistrertSøknadstidspunktDto>,
    ) {
        if (søknadstidspunktPerPerson.isEmpty()) {
            throw FunksjonellFeil("Må sette søknadstidspunkt for minst én person.")
        }
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
