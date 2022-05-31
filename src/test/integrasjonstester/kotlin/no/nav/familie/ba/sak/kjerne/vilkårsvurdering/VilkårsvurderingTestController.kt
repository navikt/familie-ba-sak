package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.writeValueAsString
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/test/vilkaarsvurdering")
// @ProtectedWithClaims(issuer = "azuread")
@Unprotected
@Validated
@Profile("!prod")
class VilkårsvurderingTestController(
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val persongrunnlagService: PersongrunnlagService,
    private val vilkårService: VilkårService,
    private val vilkårsvurderingService: VilkårsvurderingService
) {

    @PostMapping()
    fun opprettBehandlingMedVilkårsvurdering(
        @RequestBody vilkårsvurdering: TestVilkårsvurdering
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn = (1..vilkårsvurdering.antallBarn).map {
            tilfeldigPerson(personType = PersonType.BARN)
        }

        val fagsak = fagsakService.hentEllerOpprettFagsak(søker.aktør.aktivFødselsnummer())

        val behandling = behandlingService.opprettBehandling(
            NyBehandling(
                kategori = BehandlingKategori.EØS,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                søkersIdent = søker.aktør.aktivFødselsnummer(),
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                søknadMottattDato = LocalDate.now(),
                barnasIdenter = barn.map { it.aktør.aktivFødselsnummer() }
            )
        )

        val søknadDTO = SøknadDTO(
            BehandlingUnderkategori.ORDINÆR,
            SøkerMedOpplysninger(søker.aktør.aktivFødselsnummer()),
            barn.map { person -> BarnMedOpplysninger(person.aktør.aktivFødselsnummer(), person.navn, person.fødselsdato) },
            ""
        )

        // Lagre søknad
        val søknadGrunnlag = søknadGrunnlagService.lagreOgDeaktiverGammel(
            SøknadGrunnlag(
                behandlingId = behandling.id,
                søknad = søknadDTO.writeValueAsString()
            )
        )

        // Registrere persongrunnlag fra søknad
        persongrunnlagService.registrerBarnFraSøknad(
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = null,
            søknadDTO = søknadDTO
        )

        // Opprett og lagre vilkårsvurdering
        val vilkårsvurdering = vilkårService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true
        )

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }
}

data class TestVilkårsvurdering(
    val antallBarn: Int

)
