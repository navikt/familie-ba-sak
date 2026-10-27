package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TestVerktøyService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val vilkårService: VilkårService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
) {
    @Transactional
    fun oppdaterVilkårUtenFomTilFødselsdato(
        behandlingId: Long,
        fraDato: LocalDate? = null,
    ) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val vilkårsvurdering = vilkårService.hentVilkårsvurdering(behandlingId)

        val persongrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)

        vilkårsvurdering?.personResultater?.forEach { personResultat ->
            personResultat.vilkårResultater.forEach { vilkårResultat ->
                if (vilkårResultat.resultat == Resultat.IKKE_VURDERT) {
                    val person = persongrunnlag?.personer?.find { it.aktør == personResultat.aktør }

                    vilkårResultat.periodeFom = bestemPeriodeFomForVilkår(vilkår = vilkårResultat.vilkårType, personFødselsdato = person?.fødselsdato, fraDato = fraDato)
                    vilkårResultat.resultat = Resultat.OPPFYLT
                    vilkårResultat.begrunnelse = "Opprettet automatisk fra \"Fyll ut vilkårsvurdering\"-knappen"

                    if (behandling.kategori == BehandlingKategori.EØS) {
                        vilkårResultat.utdypendeVilkårsvurderinger = bestemUtdypendeVilkårsvurderingForVilkårEØS(vilkår = vilkårResultat.vilkårType, personType = person?.type)
                    }
                }
            }
        }
    }

    private fun bestemPeriodeFomForVilkår(
        vilkår: Vilkår,
        personFødselsdato: LocalDate?,
        fraDato: LocalDate?,
    ): LocalDate? =
        when (vilkår) {
            Vilkår.UNDER_18_ÅR,
            Vilkår.GIFT_PARTNERSKAP,
            -> personFødselsdato

            Vilkår.BOR_MED_SØKER,
            Vilkår.BOSATT_I_RIKET,
            Vilkår.LOVLIG_OPPHOLD,
            Vilkår.UTVIDET_BARNETRYGD,
            -> fraDato ?: personFødselsdato
        }

    private fun bestemUtdypendeVilkårsvurderingForVilkårEØS(
        vilkår: Vilkår,
        personType: PersonType?,
    ): List<UtdypendeVilkårsvurdering> =
        when (vilkår) {
            Vilkår.BOSATT_I_RIKET -> {
                when (personType) {
                    PersonType.SØKER -> listOf(UtdypendeVilkårsvurdering.OMFATTET_AV_NORSK_LOVGIVNING)
                    PersonType.BARN -> listOf(UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE)
                    else -> emptyList()
                }
            }

            Vilkår.BOR_MED_SØKER -> {
                listOf(UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE_MED_SØKER)
            }

            else -> {
                emptyList()
            }
        }
}
