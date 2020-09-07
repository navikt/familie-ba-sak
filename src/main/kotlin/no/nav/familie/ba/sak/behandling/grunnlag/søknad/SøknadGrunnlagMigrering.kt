package no.nav.familie.ba.sak.behandling.grunnlag.søknad;

import no.nav.familie.ba.sak.behandling.restDomene.*
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SøknadGrunnlagMigrering(private val søknadGrunnlagRepository: SøknadGrunnlagRepository) {

    @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE)
    private fun migrer() {
        LOG.info("Migrerer søknadGrunnlagData")
        var migrerteSøknader = 0
        val søknadGrunnlagListe = søknadGrunnlagRepository.findAll()
        søknadGrunnlagListe.map { søknadGrunnlag ->

            val søknadDTO = Result.runCatching { søknadGrunnlag.hentSøknadDto() }.fold(
                    onSuccess = { it },
                    onFailure = { null }
            )
            if(søknadDTO == null){
                val søknadDTOGammel = Result.runCatching { søknadGrunnlag.hentSøknadDtoGammel() }.fold(
                        onSuccess = { it },
                        onFailure = { null }
                )
                if (søknadDTOGammel != null) {
                    søknadGrunnlag.søknad = søknadDTOGammel.toSøknadDTO().writeValueAsString()
                    søknadGrunnlagRepository.save(søknadGrunnlag)
                    migrerteSøknader++
                }
            }
        }
        LOG.info("Fant ${søknadGrunnlagListe.size} søknadGrunnlag, og migrerte $migrerteSøknader søknader")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(this::class.java)
    }
}