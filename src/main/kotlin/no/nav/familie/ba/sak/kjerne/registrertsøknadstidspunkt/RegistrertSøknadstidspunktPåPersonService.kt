package no.nav.familie.ba.sak.kjerne.registrertsøknadstidspunkt

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RegistrertSøknadstidspunktPåPersonDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilRegistrertSøknadstidspunktPåPersonDto
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class RegistrertSøknadstidspunktPåPersonService(
    private val registrertSøknadstidspunktRepository: RegistrertSøknadstidspunktPåPersonRepository,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val featureToggleService: FeatureToggleService,
) {
    fun hentForBehandling(behandlingId: Long): List<RegistrertSøknadstidspunktPåPersonDto> =
        registrertSøknadstidspunktRepository
            .findByBehandlingId(behandlingId)
            .map { it.tilRegistrertSøknadstidspunktPåPersonDto() }

    /**
     * For søknadsbehandlinger settes søknad mottatt-dato som default registrert søknadstidspunkt per barn,
     * slik at etterbetaling beregnes ut fra denne datoen. Saksbehandler kan korrigere via menyvalget
     * «Endre søknadstidspunkt» (for EØS er søknad mottatt-dato dagens dato og må typisk korrigeres).
     * Setter kun for barn det er framstilt krav for (krysset av i søknaden) og som ikke allerede har et
     * registrert søknadstidspunkt, slik at korrigeringer består dersom steget kjøres på nytt.
     */
    @Transactional
    fun settDefaultSøknadstidspunktForBarn(behandling: Behandling) {
        if (!featureToggleService.isEnabled(FeatureToggle.KAN_REGISTRERE_SØKNADSTIDSPUNKT)) return
        if (behandling.opprettetÅrsak != BehandlingÅrsak.SØKNAD) return

        val søknadMottattDato = behandlingSøknadsinfoService.hentSøknadMottattDato(behandling.id)?.toLocalDate() ?: return

        val aktørerFremstiltKravFor =
            søknadGrunnlagService.finnPersonerFremstiltKravFor(behandling = behandling, forrigeBehandling = null).toSet()

        val aktørerMedRegistrertSøknadstidspunktPåPerson =
            registrertSøknadstidspunktRepository.findByBehandlingId(behandling.id).map { it.aktør }.toSet()

        val nyeRegistrerteSøknadstidspunkt =
            persongrunnlagService
                .hentBarna(behandling)
                .filter { it.aktør in aktørerFremstiltKravFor && it.aktør !in aktørerMedRegistrertSøknadstidspunktPåPerson }
                .map {
                    RegistrertSøknadstidspunktPåPerson(
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
        søknadstidspunktPerPerson: List<RegistrertSøknadstidspunktPåPersonDto>,
    ) {
        if (søknadstidspunktPerPerson.isEmpty()) {
            throw FunksjonellFeil("Må sette søknadstidspunkt for minst én person.")
        }
        if (søknadstidspunktPerPerson.distinctBy { it.personIdent }.size != søknadstidspunktPerPerson.size) {
            throw FunksjonellFeil("Kan ikke sette søknadstidspunkt flere ganger for samme person.")
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
                RegistrertSøknadstidspunktPåPerson(
                    behandlingId = behandling.id,
                    aktør =
                        aktørPerIdent[dto.personIdent]
                            ?: throw FunksjonellFeil("Fant ikke en av de oppgitte personene på behandling ${behandling.id}"),
                    søknadstidspunkt = dto.søknadstidspunkt,
                )
            },
        )
    }
}
