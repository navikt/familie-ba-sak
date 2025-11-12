package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.Søknadsinfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.NyEksternBehandlingRelasjon
import no.nav.familie.ba.sak.kjerne.behandling.domene.Visningsbehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.initStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.kjerne.steg.FØRSTE_STEG
import no.nav.familie.ba.sak.kjerne.steg.StegType
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Bruk denne for enhetstester. Bruk lagBehandlingUtenId for integrasjonstester
 */
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
    id: Long = Random.nextLong(10000000),
    endretTidspunkt: LocalDateTime = LocalDateTime.now(),
    aktiv: Boolean = true,
) = lagBehandlingUtenId(
    fagsak = fagsak,
    skalBehandlesAutomatisk = skalBehandlesAutomatisk,
    behandlingType = behandlingType,
    behandlingKategori = behandlingKategori,
    underkategori = underkategori,
    årsak = årsak,
    resultat = resultat,
    status = status,
    aktivertTid = aktivertTid,
    aktiv = aktiv,
    førsteSteg = førsteSteg,
    endretTidspunkt = endretTidspunkt,
).copy(id = id)

/**
 * Bruk denne for integrasjonstester hvor man ikke kan ha en predefinert id for å kunne lagre til DB. Bruk heller lagBehandling for enhetstester
 */
fun lagBehandlingUtenId(
    fagsak: Fagsak = defaultFagsak().copy(id = 0L),
    behandlingKategori: BehandlingKategori = BehandlingKategori.NASJONAL,
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    skalBehandlesAutomatisk: Boolean = false,
    førsteSteg: StegType = FØRSTE_STEG,
    resultat: Behandlingsresultat = Behandlingsresultat.IKKE_VURDERT,
    underkategori: BehandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
    status: BehandlingStatus = initStatus(),
    aktivertTid: LocalDateTime = LocalDateTime.now(),
    endretTidspunkt: LocalDateTime = LocalDateTime.now(),
    aktiv: Boolean = true,
) = Behandling(
    id = 0,
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
                behandlingStegStatus = BehandlingStegStatus.UTFØRT,
            ),
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
        søknadMottattDato = if (årsak == BehandlingÅrsak.SØKNAD) LocalDate.now().minusYears(18) else null,
        fagsakId = fagsakId,
    )

fun nyRevurdering(
    søkersIdent: String,
    fagsakId: Long,
): NyBehandling =
    NyBehandling(
        søkersIdent = søkersIdent,
        behandlingType = BehandlingType.REVURDERING,
        behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
        kategori = BehandlingKategori.NASJONAL,
        underkategori = BehandlingUnderkategori.ORDINÆR,
        søknadMottattDato = null,
        fagsakId = fagsakId,
    )

fun lagVisningsbehandling(
    behandlingId: Long = 0L,
    opprettetTidspunkt: LocalDateTime = LocalDateTime.now().minusDays(1),
    aktivertTidspunkt: LocalDateTime = LocalDateTime.now().minusDays(1),
    kategori: BehandlingKategori = BehandlingKategori.NASJONAL,
    underkategori: BehandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
    aktiv: Boolean = true,
    opprettetÅrsak: BehandlingÅrsak? = BehandlingÅrsak.SØKNAD,
    type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    status: BehandlingStatus = BehandlingStatus.AVSLUTTET,
    resultat: Behandlingsresultat = Behandlingsresultat.INNVILGET,
    vedtaksdato: LocalDateTime? = LocalDateTime.now(),
): Visningsbehandling =
    Visningsbehandling(
        behandlingId,
        opprettetTidspunkt,
        aktivertTidspunkt,
        kategori,
        underkategori,
        aktiv,
        opprettetÅrsak,
        type,
        status,
        resultat,
        vedtaksdato,
    )

fun lagNyBehandling(
    fagsakId: Long = 0L,
    navIdent: String? = "Z123",
    søkersIdent: String = "10468906606",
    barnasIdenter: List<String> = listOf("08529926074", "27508947807"),
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    behandlingÅrsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    kategori: BehandlingKategori? = BehandlingKategori.NASJONAL,
    underkategori: BehandlingUnderkategori? = BehandlingUnderkategori.ORDINÆR,
    skalBehandlesAutomatisk: Boolean = false,
    nyMigreringsdato: LocalDate? = null,
    søknadMottattDato: LocalDate? = LocalDate.now().minusDays(1),
    søknadsinfo: Søknadsinfo? = null,
    nyEksternBehandlingRelasjon: NyEksternBehandlingRelasjon? = null,
): NyBehandling =
    NyBehandling(
        kategori,
        underkategori,
        søkersIdent,
        behandlingType,
        behandlingÅrsak,
        skalBehandlesAutomatisk,
        navIdent,
        barnasIdenter,
        nyMigreringsdato,
        søknadMottattDato,
        søknadsinfo,
        fagsakId,
        nyEksternBehandlingRelasjon,
    )
