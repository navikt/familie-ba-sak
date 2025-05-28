package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.tilDto
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import org.springframework.stereotype.Service

@Service
class AutomatiskRegistrerSøknadService(
    private val søknadService: SøknadService,
    private val persongrunnlagService: PersongrunnlagService,
    private val personopplysningerService: PersonopplysningerService,
) {
    fun lagRestRegistrerSøknad(behandling: Behandling): RestRegistrerSøknad {
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

        val barnaMedOpplysninger =
            personopplysningerService
                .hentPersoninfoMedRelasjonerOgRegisterinformasjon(søker.aktør)
                .forelderBarnRelasjon
                .filter { it.relasjonsrolle == FORELDERBARNRELASJONROLLE.BARN }
                .mapNotNull { barn ->
                    // Filtrer bort barn som har adressebeskyttelse som ikke er i søknaden
                    if (barn.adressebeskyttelseGradering == null ||
                        barn.adressebeskyttelseGradering == ADRESSEBESKYTTELSEGRADERING.UGRADERT ||
                        søknad.barn.any { it.fnr == barn.aktør.aktivFødselsnummer() }
                    ) {
                        BarnMedOpplysninger(
                            ident = barn.aktør.aktivFødselsnummer(),
                            navn = barn.navn ?: "Mangler navn",
                            fødselsdato = barn.fødselsdato,
                            inkludertISøknaden = søknad.barn.any { it.fnr == barn.aktør.aktivFødselsnummer() },
                        )
                    } else {
                        null
                    }
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
