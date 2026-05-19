package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType.REVURDERING
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.springframework.stereotype.Service

@Service
class PreutfyllVilkårService(
    private val preutfyllLovligOppholdService: PreutfyllLovligOppholdService,
    private val preutfyllBorMedSøkerService: PreutfyllBorMedSøkerService,
    private val preutfyllBosattIRiketService: PreutfyllBosattIRiketService,
    private val preutfyllBosattIRiketForFødselshendelserService: PreutfyllBosattIRiketForFødselshendelserService,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) {
    fun preutfyllVilkår(vilkårsvurdering: Vilkårsvurdering) {
        val behandling = vilkårsvurdering.behandling

        if (behandling.kategori == BehandlingKategori.EØS) return
        if (behandling.fagsak.type == FagsakType.SKJERMET_BARN) return

        val aktørerSomViSkalPreutfylleFor =
            when (behandling.type) {
                FØRSTEGANGSBEHANDLING -> {
                    hentSøkerOgBarnIBehandling(behandling)
                }

                REVURDERING -> {
                    if (behandling.opprettetÅrsak != BehandlingÅrsak.SØKNAD) return

                    finnNyeAktørerIBehandling(behandling)
                }

                else -> {
                    return
                }
            }.ifEmpty { return }

        persongrunnlagService.oppdaterRegisteropplysninger(behandling.id)

        preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering, aktørerSomViSkalPreutfylleFor)
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering, aktørerSomViSkalPreutfylleFor)
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, aktørerSomViSkalPreutfylleFor)
    }

    private fun finnNyeAktørerIBehandling(behandling: Behandling): List<Aktør> {
        val aktørerIInneværendeBehandling = hentSøkerOgBarnIBehandling(behandling)

        val aktørerIForrigeBehandling =
            behandlingHentOgPersisterService
                .hentForrigeBehandlingSomErVedtatt(behandling)
                ?.let { hentSøkerOgBarnIBehandling(it) }
                ?: emptyList()

        return aktørerIInneværendeBehandling - aktørerIForrigeBehandling.toSet()
    }

    private fun hentSøkerOgBarnIBehandling(behandling: Behandling): List<Aktør> =
        persongrunnlagService
            .hentAktivThrows(behandling.id)
            .søkerOgBarn
            .map { it.aktør }

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
