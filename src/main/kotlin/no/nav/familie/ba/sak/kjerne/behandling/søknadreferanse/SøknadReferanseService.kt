package no.nav.familie.ba.sak.kjerne.behandling.søknadreferanse

import org.springframework.stereotype.Service

@Service
class SøknadReferanseService(
    private val søknadReferanseRepository: SøknadReferanseRepository,
) {
    fun hentSøknadReferanse(behandlingId: Long): SøknadReferanse? = søknadReferanseRepository.findByBehandlingId(behandlingId)

    fun lagreSøknadReferanse(
        behandlingId: Long,
        journalpostId: String,
    ) = søknadReferanseRepository.save(
        SøknadReferanse(
            behandlingId = behandlingId,
            journalpostId = journalpostId,
        ),
    )
}
