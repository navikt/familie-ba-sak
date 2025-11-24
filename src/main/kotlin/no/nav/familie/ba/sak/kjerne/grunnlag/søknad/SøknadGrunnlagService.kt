package no.nav.familie.ba.sak.kjerne.grunnlag.søknad

import no.nav.familie.ba.sak.ekstern.restDomene.BehandlingUnderkategoriDTO
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SøknadGrunnlagService(
    private val søknadGrunnlagRepository: SøknadGrunnlagRepository,
    private val personidentService: PersonidentService,
    private val persongrunnlagService: PersongrunnlagService,
) {
    @Transactional
    fun lagreOgDeaktiverGammel(søknadGrunnlag: SøknadGrunnlag): SøknadGrunnlag {
        val aktivSøknadGrunnlag = søknadGrunnlagRepository.hentAktiv(søknadGrunnlag.behandlingId)

        if (aktivSøknadGrunnlag != null) {
            søknadGrunnlagRepository.saveAndFlush(aktivSøknadGrunnlag.also { it.aktiv = false })
        }

        return søknadGrunnlagRepository.save(søknadGrunnlag)
    }

    fun hentAlle(behandlingId: Long): List<SøknadGrunnlag> = søknadGrunnlagRepository.hentAlle(behandlingId)

    fun hentAktiv(behandlingId: Long): SøknadGrunnlag? = søknadGrunnlagRepository.hentAktiv(behandlingId)

    fun hentAktivSøknadDto(behandlingId: Long): SøknadDTO? = hentAktiv(behandlingId)?.hentSøknadDto()

    internal fun finnPersonerFremstiltKravFor(
        behandling: Behandling,
        forrigeBehandling: Behandling?,
    ): List<Aktør> {
        val søknadDTO = hentAktivSøknadDto(behandlingId = behandling.id)
        val personerFremstiltKravFor =
            when {
                behandling.opprettetÅrsak == BehandlingÅrsak.SØKNAD -> {
                    // alle barna som er krysset av på søknad
                    val barnFraSøknad =
                        søknadDTO
                            ?.barnaMedOpplysninger
                            ?.filter { it.erFolkeregistrert && it.inkludertISøknaden }
                            ?.map { personidentService.hentAktør(it.ident) }
                            ?: emptyList()

                    // hvis det søkes om utvidet skal søker med
                    val utvidetBarnetrygdSøker =
                        if (søknadDTO?.underkategori == BehandlingUnderkategoriDTO.UTVIDET) listOf(behandling.fagsak.aktør) else emptyList()

                    barnFraSøknad + utvidetBarnetrygdSøker
                }

                behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE -> {
                    persongrunnlagService.finnNyeBarn(behandling, forrigeBehandling).map { it.aktør }
                }

                behandling.erManuellMigrering() || behandling.opprettetÅrsak == BehandlingÅrsak.KLAGE -> {
                    persongrunnlagService.hentAktivThrows(behandling.id).personer.map { it.aktør }
                }

                else -> {
                    emptyList()
                }
            }

        return personerFremstiltKravFor.distinct()
    }
}
