package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.tilDto
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import org.springframework.stereotype.Service

@Service
class AutomatiskRegistrerSøknadService(
    private val søknadService: SøknadService,
    private val persongrunnlagService: PersongrunnlagService,
    private val personidentService: PersonidentService,
    private val personopplysningerService: PersonopplysningerService,
) {
    fun lagRestRegistrerSøknad(behandling: Behandling): RestRegistrerSøknad {
        val søknad =
            søknadService.hentSøknad(behandling.id)
                ?: throw IllegalStateException("Fant ikke søknad for behandling med id ${behandling.id}")

        val underkategori = søknad.behandlingUnderkategori.tilDto()

        val søker = persongrunnlagService.hentSøker(behandling.id)
        val søkerMedOpplysninger =
            SøkerMedOpplysninger(
                ident = søker.aktør.aktivFødselsnummer(),
                målform = søknad.målform,
            )

        val barnaMedOpplysninger =
            søknad.barn.map {
                val barnAktør = personidentService.hentOgLagreAktør(it.fnr, true)
                val barnPersonInfo = personopplysningerService.hentPersoninfoEnkel(barnAktør)

                BarnMedOpplysninger(
                    ident = barnAktør.aktivFødselsnummer(),
                    navn = barnPersonInfo.navn ?: "",
                    fødselsdato = barnPersonInfo.fødselsdato,
                )
            }

        return RestRegistrerSøknad(
            søknad =
                SøknadDTO(
                    underkategori = underkategori,
                    søkerMedOpplysninger = søkerMedOpplysninger,
                    barnaMedOpplysninger = barnaMedOpplysninger,
                    endringAvOpplysningerBegrunnelse = "",
                ),
            bekreftEndringerViaFrontend = false,
        )
    }
}
