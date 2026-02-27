package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.springframework.stereotype.Service

@Service
class PreutfyllVilkårService(
    private val preutfyllLovligOppholdService: PreutfyllLovligOppholdService,
    private val preutfyllBorMedSøkerService: PreutfyllBorMedSøkerService,
    private val preutfyllBosattIRiketService: PreutfyllBosattIRiketService,
    private val preutfyllBosattIRiketForFødselshendelserService: PreutfyllBosattIRiketForFødselshendelserService,
    private val gammelPreutfyllBosattIRiketService: GammelPreutfyllBosattIRiketService,
    private val persongrunnlagService: PersongrunnlagService,
    private val featureToggleService: FeatureToggleService,
) {
    fun preutfyllVilkår(vilkårsvurdering: Vilkårsvurdering) {
        if (vilkårsvurdering.behandling.kategori == BehandlingKategori.EØS) return
        if (vilkårsvurdering.behandling.type != BehandlingType.FØRSTEGANGSBEHANDLING) return

        if (featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_PERSONOPPLYSNIGSGRUNNLAG)) {
            persongrunnlagService.oppdaterRegisteropplysninger(vilkårsvurdering.behandling.id)
        }

        if (featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_VILKÅR)) {
            if (featureToggleService.isEnabled(FeatureToggle.OPPDATERT_PREUTFYLLING_BOSATT_I_RIKET)) {
                preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering)
            } else {
                gammelPreutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering)
            }
        }
        if (featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_VILKÅR_LOVLIG_OPPHOLD)) {
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering)
        }
        if (featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_BOR_HOS_SØKER)) {
            preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering)
        }
    }

    fun preutfyllBosattIRiketForFødselshendelseBehandlinger(
        vilkårsvurdering: Vilkårsvurdering,
        barnSomSkalVurderesIFødselshendelse: List<String>,
    ) {
        val identerVilkårSkalPreutfyllesFor =
            if (vilkårsvurdering.behandling.type == FØRSTEGANGSBEHANDLING) {
                barnSomSkalVurderesIFødselshendelse +
                    vilkårsvurdering.behandling.fagsak.aktør
                        .aktivFødselsnummer()
            } else {
                barnSomSkalVurderesIFødselshendelse
            }

        if (featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_BOSATT_I_RIKET_FOR_FØDSELSHENDELSE)) {
            preutfyllBosattIRiketForFødselshendelserService.preutfyllBosattIRiket(
                vilkårsvurdering = vilkårsvurdering,
                identerVilkårSkalPreutfyllesFor = identerVilkårSkalPreutfyllesFor,
            )
        } else {
            gammelPreutfyllBosattIRiketService.preutfyllBosattIRiket(
                vilkårsvurdering = vilkårsvurdering,
                identerVilkårSkalPreutfyllesFor = identerVilkårSkalPreutfyllesFor,
            )
        }
    }

    companion object {
        const val PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT = "Fylt ut automatisk fra registerdata i PDL\n"
    }
}
