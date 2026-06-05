package no.nav.familie.ba.sak.kjerne.registrertsøknadstidspunkt

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
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
    fun hentForBehandling(behandlingId: Long): List<RegistrertSøknadstidspunktPåPerson> = registrertSøknadstidspunktRepository.findByBehandlingId(behandlingId)

    @Transactional
    fun settSøknadstidspunktForBarn(behandling: Behandling) {
        if (!featureToggleService.isEnabled(FeatureToggle.KAN_REGISTRERE_SØKNADSTIDSPUNKT_PÅ_PERSON)) return
        if (behandling.opprettetÅrsak != BehandlingÅrsak.SØKNAD) return

        val søknadMottattDato = behandlingSøknadsinfoService.hentSøknadMottattDato(behandling.id)?.toLocalDate() ?: return
        val barnFremstiltKravFor = søknadGrunnlagService.finnPersonerFremstiltKravFor(behandling = behandling, forrigeBehandling = null).toSet()
        val aktørerMedRegistrertSøknadstidspunkt = registrertSøknadstidspunktRepository.findByBehandlingId(behandling.id).map { it.aktør }.toSet()

        val nyeRegistrerteSøknadstidspunkt =
            persongrunnlagService
                .hentBarna(behandling)
                .filter { it.aktør in barnFremstiltKravFor && it.aktør !in aktørerMedRegistrertSøknadstidspunkt }
                .map {
                    RegistrertSøknadstidspunktPåPerson(
                        behandlingId = behandling.id,
                        aktør = it.aktør,
                        søknadstidspunkt = søknadMottattDato,
                    )
                }

        registrertSøknadstidspunktRepository.saveAll(nyeRegistrerteSøknadstidspunkt)
    }

    @Transactional
    fun lagreSøknadstidspunkterPåPersoner(
        behandling: Behandling,
        søknadstidspunktPerPerson: List<RegistrertSøknadstidspunkt>,
    ) {
        validerSøknadstidspunktFørLagring(søknadstidspunktPerPerson)

        val aktørPerIdent =
            persongrunnlagService
                .hentPersonerPåBehandling(søknadstidspunktPerPerson.map { it.personIdent }, behandling)
                .associate { it.aktør.aktivFødselsnummer() to it.aktør }

        registrertSøknadstidspunktRepository.deleteByBehandlingId(behandling.id)

        registrertSøknadstidspunktRepository.saveAll(
            søknadstidspunktPerPerson.map { registrertSøknadstidspunkt ->
                RegistrertSøknadstidspunktPåPerson(
                    behandlingId = behandling.id,
                    aktør =
                        aktørPerIdent[registrertSøknadstidspunkt.personIdent]
                            ?: throw FunksjonellFeil("Fant ikke en av de oppgitte personene på behandling ${behandling.id}"),
                    søknadstidspunkt = registrertSøknadstidspunkt.søknadstidspunkt,
                )
            },
        )
    }
}

private fun validerSøknadstidspunktFørLagring(søknadstidspunktPerPerson: List<RegistrertSøknadstidspunkt>) {
    if (søknadstidspunktPerPerson.isEmpty()) {
        throw FunksjonellFeil("Må sette søknadstidspunkt for minst én person.")
    }
    if (søknadstidspunktPerPerson.distinctBy { it.personIdent }.size != søknadstidspunktPerPerson.size) {
        throw FunksjonellFeil("Kan ikke sette søknadstidspunkt flere ganger for samme person.")
    }
    if (søknadstidspunktPerPerson.any { it.søknadstidspunkt.isAfter(LocalDate.now()) }) {
        throw FunksjonellFeil("Søknadstidspunkt kan ikke være frem i tid.")
    }
}
