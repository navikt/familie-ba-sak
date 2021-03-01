package no.nav.familie.ba.sak.opplysningsplikt

import no.nav.familie.ba.sak.annenvurdering.AnnenVurderingService
import no.nav.familie.ba.sak.annenvurdering.AnnenVurderingType
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestAnnenVurdering
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestOpplysningsplikt
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/opplysningsplikt")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OpplysningspliktController(
        private val annenVurderingService: AnnenVurderingService,
        private val vilkårsvurderingService: VilkårsvurderingService,
        private val fagsakService: FagsakService
) {

    @PutMapping(path = ["/{fagsakId}/{behandlingId}"])
    fun oppdaterOpplysningsplikt(@PathVariable fagsakId: Long,
                                 @PathVariable behandlingId: Long,
                                 @RequestBody restOpplysningsplikt: RestOpplysningsplikt): ResponseEntity<Ressurs<RestFagsak>> {

        //TODO: Etter at opplysningsplikt blitt ferdigflyttet så skal denne klasssen fjernes.
        val resultat = when (restOpplysningsplikt.status) {
            OpplysningspliktStatus.MOTTATT -> Resultat.OPPFYLT
            OpplysningspliktStatus.IKKE_MOTTATT_FORTSETT -> Resultat.IKKE_OPPFYLT
            OpplysningspliktStatus.IKKE_MOTTATT_AVSLAG -> Resultat.IKKE_OPPFYLT
            OpplysningspliktStatus.IKKE_SATT -> Resultat.IKKE_VURDERT
        }

        vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingId)
                ?.personResultater
                ?.flatMap { it.andreVurderinger }
                ?.filter { it.type == AnnenVurderingType.OPPLYSNINGSPLIKT }
                ?.forEach { annenVurdering ->
                    annenVurderingService.endreAnnenVurdering(
                            restAnnenVurdering = RestAnnenVurdering(
                                    id = annenVurdering.id,
                                    resultat = resultat,
                                    type = AnnenVurderingType.OPPLYSNINGSPLIKT,
                                    begrunnelse = restOpplysningsplikt.begrunnelse
                            ))
                }

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId))
    }
}