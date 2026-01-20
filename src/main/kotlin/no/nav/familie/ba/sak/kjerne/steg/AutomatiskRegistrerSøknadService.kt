package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.RegistrerSøknadDto
import no.nav.familie.ba.sak.ekstern.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.tilDto
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import org.springframework.stereotype.Service

@Service
class AutomatiskRegistrerSøknadService(
    private val søknadService: SøknadService,
    private val persongrunnlagService: PersongrunnlagService,
    private val personopplysningerService: PersonopplysningerService,
    private val personidentService: PersonidentService,
) {
    fun lagRestRegistrerSøknad(behandling: Behandling): RegistrerSøknadDto {
        val søknad =
            søknadService.finnSøknad(behandling.id)
                ?: throw Feil("Fant ikke søknad for behandling med id ${behandling.id}")

        val underkategori = søknad.behandlingUnderkategori.tilDto()

        val søker = persongrunnlagService.hentSøker(behandling.id)
        val søkerMedOpplysninger =
            SøkerMedOpplysninger(
                ident = søker.aktør.aktivFødselsnummer(),
                målform = søknad.målform,
            )

        val barnMedRelasjon =
            personopplysningerService
                .hentPersoninfoMedRelasjonerOgRegisterinformasjon(søker.aktør)
                .forelderBarnRelasjon
                .filter { it.relasjonsrolle == FORELDERBARNRELASJONROLLE.BARN }
                .map { barn ->
                    BarnMedOpplysninger(
                        ident = barn.aktør.aktivFødselsnummer(),
                        navn = barn.navn ?: "Mangler navn",
                        fødselsdato = barn.fødselsdato,
                        inkludertISøknaden = søknad.barn.any { it.fnr == barn.aktør.aktivFødselsnummer() },
                    )
                }

        val barnUtenRelasjonFraSøknad =
            søknad.barn
                .filter { barn -> barnMedRelasjon.none { it.ident == barn.fnr } }
                .map {
                    val barnAktør = personidentService.hentAktør(it.fnr)
                    val barn = personopplysningerService.hentPersoninfoEnkel(barnAktør)

                    BarnMedOpplysninger(
                        ident = barnAktør.aktivFødselsnummer(),
                        navn = barn.navn ?: "Mangler navn",
                        fødselsdato = barn.fødselsdato,
                        inkludertISøknaden = true,
                        manueltRegistrert = true,
                    )
                }

        val barnaMedOpplysninger = (barnMedRelasjon + barnUtenRelasjonFraSøknad).distinctBy { it.ident }

        return RegistrerSøknadDto(
            søknad =
                SøknadDTO(
                    underkategori = underkategori,
                    søkerMedOpplysninger = søkerMedOpplysninger,
                    barnaMedOpplysninger = barnaMedOpplysninger,
                    endringAvOpplysningerBegrunnelse = "",
                    erAutomatiskRegistrert = true,
                ),
            bekreftEndringerViaFrontend = false,
        )
    }
}
