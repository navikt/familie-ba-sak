package no.nav.familie.ba.sak.kjerne.grunnlag.søknad

import no.nav.familie.ba.sak.kjerne.behandlingsresultat.tilMinimertUregisrertBarn
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SøknadGrunnlagService(
    private val søknadGrunnlagRepository: SøknadGrunnlagRepository
) {

    @Transactional
    fun lagreOgDeaktiverGammel(søknadGrunnlag: SøknadGrunnlag): SøknadGrunnlag {
        val aktivSøknadGrunnlag = søknadGrunnlagRepository.hentAktiv(søknadGrunnlag.behandlingId)

        if (aktivSøknadGrunnlag != null) {
            søknadGrunnlagRepository.saveAndFlush(aktivSøknadGrunnlag.also { it.aktiv = false })
        }

        return søknadGrunnlagRepository.save(søknadGrunnlag)
    }

    fun hentAlle(behandlingId: Long): List<SøknadGrunnlag> {
        return søknadGrunnlagRepository.hentAlle(behandlingId)
    }

    fun hentAktiv(behandlingId: Long): SøknadGrunnlag? {
        return søknadGrunnlagRepository.hentAktiv(behandlingId)
    }

    fun hentMinimerteUregistrerteBarn(behandlingId: Long) =
        hentAktiv(behandlingId)
            ?.hentUregistrerteBarn()
            ?.map { uregistrertBarn -> uregistrertBarn.tilMinimertUregisrertBarn() }
            ?: emptyList()
}
