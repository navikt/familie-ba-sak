package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.springframework.stereotype.Service

@Service
class PreutfyllVilkårService(
    private val preutfyllLovligOppholdService: PreutfyllLovligOppholdService,
    private val preutfyllBorMedSøkerService: PreutfyllBorMedSøkerService,
    private val preutfyllBosattIRiketService: PreutfyllBosattIRiketService,
    private val preutfyllBosattIRiketForFødselshendelserService: PreutfyllBosattIRiketForFødselshendelserService,
    private val persongrunnlagService: PersongrunnlagService,
) {
    fun preutfyllVilkår(vilkårsvurdering: Vilkårsvurdering) {
        val behandling = vilkårsvurdering.behandling

        if (behandling.kategori == BehandlingKategori.EØS) return
        if (behandling.type != FØRSTEGANGSBEHANDLING) return
        if (behandling.fagsak.type == FagsakType.SKJERMET_BARN) return

        persongrunnlagService.oppdaterRegisteropplysninger(behandling.id)

        preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering)
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering)

        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering)
    }

    fun preutfyllBosattIRiketForFødselshendelseBehandlinger(
        vilkårsvurdering: Vilkårsvurdering,
        barnSomSkalVurderesIFødselshendelse: List<String>? = null,
    ) {
        val identerVilkårSkalPreutfyllesFor =
            barnSomSkalVurderesIFødselshendelse?.let {
                if (vilkårsvurdering.behandling.type == FØRSTEGANGSBEHANDLING) {
                    it +
                        vilkårsvurdering.behandling.fagsak.aktør
                            .aktivFødselsnummer()
                } else {
                    it
                }
            }

        preutfyllBosattIRiketForFødselshendelserService.preutfyllBosattIRiket(
            vilkårsvurdering = vilkårsvurdering,
            identerVilkårSkalPreutfyllesFor = identerVilkårSkalPreutfyllesFor,
        )
    }

    companion object {
        const val PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT = "Fylt ut automatisk fra registerdata i PDL\n"
    }
}
