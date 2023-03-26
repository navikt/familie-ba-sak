package no.nav.familie.ba.sak.kjerne.grunnlag.søknad

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SøknadGrunnlagService(
    private val søknadGrunnlagRepository: SøknadGrunnlagRepository
) {

    @Transactional
    fun lagreOgDeaktiverGammel(søknadGrunnlag: SøknadGrunnlag): SøknadGrunnlag {
        val aktivSøknadGrunnlag = søknadGrunnlagRepository.hentAktiv(søknadGrunnlag.behandlingId.id)

        if (aktivSøknadGrunnlag != null) {
            søknadGrunnlagRepository.saveAndFlush(aktivSøknadGrunnlag.also { it.aktiv = false })
        }

        return søknadGrunnlagRepository.save(søknadGrunnlag)
    }

    fun hentAlle(behandlingId: BehandlingId): List<SøknadGrunnlag> {
        return søknadGrunnlagRepository.hentAlle(behandlingId.id)
    }

    fun hentAktiv(behandlingId: BehandlingId): SøknadGrunnlag? {
        return søknadGrunnlagRepository.hentAktiv(behandlingId.id)
    }
}
