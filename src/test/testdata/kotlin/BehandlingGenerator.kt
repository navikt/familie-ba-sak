import no.nav.familie.ba.sak.common.nesteBehandlingId
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.initStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.kjerne.steg.FØRSTE_STEG
import no.nav.familie.ba.sak.kjerne.steg.StegType
import java.time.LocalDate
import java.time.LocalDateTime

fun lagBehandling(
    fagsak: Fagsak = defaultFagsak(),
    behandlingKategori: BehandlingKategori = BehandlingKategori.NASJONAL,
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    skalBehandlesAutomatisk: Boolean = false,
    førsteSteg: StegType = FØRSTE_STEG,
    resultat: Behandlingsresultat = Behandlingsresultat.IKKE_VURDERT,
    underkategori: BehandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
    status: BehandlingStatus = initStatus(),
    aktivertTid: LocalDateTime = LocalDateTime.now(),
    id: Long = nesteBehandlingId(),
    endretTidspunkt: LocalDateTime = LocalDateTime.now(),
    aktiv: Boolean = true,
) = Behandling(
    id = id,
    fagsak = fagsak,
    skalBehandlesAutomatisk = skalBehandlesAutomatisk,
    type = behandlingType,
    kategori = behandlingKategori,
    underkategori = underkategori,
    opprettetÅrsak = årsak,
    resultat = resultat,
    status = status,
    aktivertTidspunkt = aktivertTid,
    aktiv = aktiv,
).also {
    it.endretTidspunkt = endretTidspunkt
    val tidligereSteg =
        StegType.entries
            .filter { it.rekkefølge < førsteSteg.rekkefølge }
            .filter { it != StegType.HENLEGG_BEHANDLING }

    tidligereSteg.forEach { steg ->
        it.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                0,
                it,
                steg,
                behandlingStegStatus = BehandlingStegStatus.UTFØRT
            )
        )
    }

    it.behandlingStegTilstand.add(BehandlingStegTilstand(0, it, førsteSteg))
}

fun nyOrdinærBehandling(
    søkersIdent: String,
    årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    fagsakId: Long,
): NyBehandling =
    NyBehandling(
        søkersIdent = søkersIdent,
        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        kategori = BehandlingKategori.NASJONAL,
        underkategori = BehandlingUnderkategori.ORDINÆR,
        behandlingÅrsak = årsak,
        søknadMottattDato = if (årsak == BehandlingÅrsak.SØKNAD) LocalDate.now() else null,
        fagsakId = fagsakId,
    )

fun nyRevurdering(
    søkersIdent: String,
    fagsakId: Long,
): NyBehandling =
    NyBehandling(
        søkersIdent = søkersIdent,
        behandlingType = BehandlingType.REVURDERING,
        kategori = BehandlingKategori.NASJONAL,
        underkategori = BehandlingUnderkategori.ORDINÆR,
        søknadMottattDato = LocalDate.now(),
        fagsakId = fagsakId,
    )