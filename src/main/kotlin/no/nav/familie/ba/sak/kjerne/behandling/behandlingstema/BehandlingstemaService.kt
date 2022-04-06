package no.nav.familie.ba.sak.kjerne.behandling.behandlingstema

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaUtils.bestemKategori
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaUtils.bestemUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaUtils.utledKategori
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaUtils.utledLøpendeUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaUtils.utledUnderKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.TidslinjeService
import org.springframework.stereotype.Service

@Service
class BehandlingstemaService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val loggService: LoggService,
    private val oppgaveService: OppgaveService,
    private val tidslinjeService: TidslinjeService,
    private val featureToggleService: FeatureToggleService
) {

    fun oppdaterBehandlingstema(
        behandling: Behandling,
        nyKategori: BehandlingKategori?,
        nyUnderkategori: BehandlingUnderkategori?,
        manueltOppdatert: Boolean = false
    ): Behandling {
        if (!behandling.skalBehandlesAutomatisk) return behandling
        else if (manueltOppdatert && (nyKategori == null || nyUnderkategori == null)) throw FunksjonellFeil("Du må velge behandlingstema.")

        val utledetKategori = bestemKategori(
            nyBehandlingKategori = nyKategori,
            løpendeBehandlingKategori = hentLøpendeKategori(behandling.fagsak.id),
            kategoriFraInneværendeBehandling = hentKategoriFraInneværendeBehandling(behandling.fagsak.id),
        )

        val utledetUnderkategori = if (manueltOppdatert) {
            nyUnderkategori!!
        } else {
            bestemUnderkategori(
                nyUnderkategori = nyUnderkategori,
                løpendeUnderkategori = hentLøpendeUnderkategori(fagsakId = behandling.fagsak.id),
                underkategoriFraInneværendeBehandling = hentUnderkategoriFraInneværendeBehandling(fagsakId = behandling.fagsak.id)
            )
        }

        val forrigeUnderkategori = behandling.underkategori
        val forrigeKategori = behandling.kategori
        val skalOppdatereKategori = utledetKategori != forrigeKategori
        val skalOppdatereUnderkategori = utledetUnderkategori != forrigeUnderkategori

        if (skalOppdatereKategori) {
            behandling.apply { kategori = utledetKategori }
        }
        if (skalOppdatereUnderkategori) {
            behandling.apply {
                underkategori = utledetUnderkategori
            }
        }

        return behandlingHentOgPersisterService.lagreEllerOppdater(behandling).also { lagretBehandling ->
            oppgaveService.patchOppgaverForBehandling(lagretBehandling) {
                if (it.behandlingstema != lagretBehandling.underkategori.tilOppgaveBehandlingTema().value || it.behandlingstype != lagretBehandling.kategori.tilOppgavebehandlingType().value) {
                    it.copy(
                        behandlingstema = lagretBehandling.underkategori.tilOppgaveBehandlingTema().value,
                        behandlingstype = lagretBehandling.kategori.tilOppgavebehandlingType().value
                    )
                } else null
            }

            if (manueltOppdatert && (skalOppdatereKategori || skalOppdatereUnderkategori)) {
                loggService.opprettEndretBehandlingstema(
                    behandling = lagretBehandling,
                    forrigeKategori = forrigeKategori,
                    forrigeUnderkategori = forrigeUnderkategori,
                    nyKategori = utledetKategori,
                    nyUnderkategori = utledetUnderkategori
                )
            }
        }
    }

    fun hentLøpendeKategori(fagsakId: Long): BehandlingKategori {
        return if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS)) {
            val forrigeIverksattBehandling =
                behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = fagsakId)
                    ?: return BehandlingKategori.NASJONAL

            val barnasTidslinjer =
                tidslinjeService.hentTidslinjer(behandlingId = forrigeIverksattBehandling.id)?.barnasTidslinjer()
            utledKategori(barnasTidslinjer)
        } else {
            BehandlingKategori.NASJONAL
        }
    }

    fun hentKategoriFraInneværendeBehandling(fagsakId: Long): BehandlingKategori {
        return if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS)) {
            val aktivBehandling =
                behandlingHentOgPersisterService.hentAktivOgÅpenForFagsak(fagsakId = fagsakId)
                    ?: return BehandlingKategori.NASJONAL

            val barnasTidslinjer =
                tidslinjeService.hentTidslinjer(behandlingId = aktivBehandling.id)?.barnasTidslinjer()
                    ?: return BehandlingKategori.NASJONAL

            utledKategori(barnasTidslinjer)
        } else {
            BehandlingKategori.NASJONAL
        }
    }

    fun hentLøpendeUnderkategori(fagsakId: Long): BehandlingUnderkategori? {
        val forrigeAndeler = hentForrigeAndeler(fagsakId)
        return if (forrigeAndeler != null) utledLøpendeUnderkategori(forrigeAndeler) else null
    }

    fun hentUnderkategoriFraInneværendeBehandling(fagsakId: Long): BehandlingUnderkategori {
        val aktivBehandling =
            behandlingHentOgPersisterService.hentAktivOgÅpenForFagsak(fagsakId = fagsakId)
                ?: return BehandlingUnderkategori.ORDINÆR

        val søkersTidslinje =
            tidslinjeService.hentTidslinjer(behandlingId = aktivBehandling.id)?.søkersTidslinjer()
                ?: return BehandlingUnderkategori.ORDINÆR

        return utledUnderKategori(søkersTidslinje)
    }

    private fun hentForrigeAndeler(fagsakId: Long): List<AndelTilkjentYtelse>? {
        val forrigeIverksattBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = fagsakId) ?: return null
        return andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = forrigeIverksattBehandling.id)
    }
}
