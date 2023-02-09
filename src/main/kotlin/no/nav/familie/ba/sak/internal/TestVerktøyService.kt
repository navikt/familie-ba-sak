package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TestVerktøyService(
    private val vilkårService: VilkårService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository
) {

    @Transactional
    fun oppdaterVilkårUtenFomTilFødselsdato(behandlingId: Long) {
        val vilkårsvurdering = vilkårService.hentVilkårsvurdering(behandlingId)

        val persongrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)

        vilkårsvurdering?.personResultater?.forEach { personResultat ->
            personResultat.vilkårResultater.forEach { vilkårResultat ->
                if (vilkårResultat.periodeFom == null && vilkårResultat.resultat != Resultat.OPPFYLT) {
                    vilkårResultat.periodeFom =
                        persongrunnlag?.personer?.find { it.aktør == personResultat.aktør }?.fødselsdato
                    vilkårResultat.resultat = Resultat.OPPFYLT
                }
            }
        }
    }
}