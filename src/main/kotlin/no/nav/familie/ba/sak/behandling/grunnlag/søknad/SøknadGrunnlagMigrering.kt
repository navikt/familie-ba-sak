package no.nav.familie.ba.sak.behandling.grunnlag.søknad

import no.nav.familie.ba.sak.behandling.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.behandling.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.behandling.restDomene.writeValueAsString
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class SøknadGrunnlagMigrering(private val søknadGrunnlagRepository: SøknadGrunnlagRepository): BaseJavaMigration() {

    override fun migrate(context: Context) {
        søknadGrunnlagRepository.findAll().map {
            val søknadDTOGammel = it.hentSøknadDtoGammel()
            val søknadDTO = SøknadDTO(
                    underkategori = søknadDTOGammel.underkategori,
                    søkerMedOpplysninger = SøkerMedOpplysninger(ident = søknadDTOGammel.søkerMedOpplysninger.ident),
                    barnaMedOpplysninger = søknadDTOGammel.barnaMedOpplysninger.map { barnMedOpplysninger ->
                        BarnMedOpplysninger(ident = barnMedOpplysninger.ident, inkludertISøknaden = barnMedOpplysninger.inkludertISøknaden)
                    }
            )
            it.søknad = søknadDTO.writeValueAsString()
            søknadGrunnlagRepository.save(it)
        }
    }

}