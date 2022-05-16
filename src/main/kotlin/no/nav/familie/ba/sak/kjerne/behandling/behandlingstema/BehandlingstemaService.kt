package no.nav.familie.ba.sak.kjerne.behandling.behandlingstema

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.TidslinjeService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.springframework.stereotype.Service

@Service
class BehandlingstemaService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val loggService: LoggService,
    private val oppgaveService: OppgaveService,
    private val tidslinjeService: TidslinjeService,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val featureToggleService: FeatureToggleService
) {

    fun oppdaterBehandlingstema(
        behandling: Behandling,
        overstyrtKategori: BehandlingKategori? = null,
        overstyrtUnderkategori: BehandlingUnderkategori? = null,
        manueltOppdatert: Boolean = false
    ): Behandling {
        if (behandling.skalBehandlesAutomatisk) return behandling
        else if (manueltOppdatert && (overstyrtKategori == null || overstyrtUnderkategori == null)) throw FunksjonellFeil(
            "Du må velge behandlingstema."
        )

        val utledetKategori = bestemKategori(
            overstyrtKategori = overstyrtKategori,
            kategoriFraInneværendeBehandling = hentKategoriFraInneværendeBehandling(behandling.fagsak.id),
        )

        val utledetUnderkategori = bestemUnderkategori(
            overstyrtUnderkategori = overstyrtUnderkategori,
            underkategoriFraLøpendeBehandling = hentLøpendeUnderkategori(fagsakId = behandling.fagsak.id),
            underkategoriFraInneværendeBehandling = hentUnderkategoriFraInneværendeBehandling(fagsakId = behandling.fagsak.id)
        )

        val forrigeUnderkategori = behandling.underkategori
        val forrigeKategori = behandling.kategori
        val skalOppdatereKategori = utledetKategori != forrigeKategori
        val skalOppdatereUnderkategori = utledetUnderkategori != forrigeUnderkategori
        val skalOppdatereKategoriEllerUnderkategori = skalOppdatereKategori || skalOppdatereUnderkategori

        return if (skalOppdatereKategoriEllerUnderkategori) {
            behandling.apply {
                kategori = utledetKategori
                underkategori = utledetUnderkategori
            }

            behandlingHentOgPersisterService.lagreEllerOppdater(behandling).also { lagretBehandling ->
                oppgaveService.patchOppgaverForBehandling(lagretBehandling) {
                    if (it.behandlingstema != lagretBehandling.underkategori.tilOppgaveBehandlingTema().value || it.behandlingstype != lagretBehandling.kategori.tilOppgavebehandlingType().value) {
                        it.copy(
                            behandlingstema = lagretBehandling.underkategori.tilOppgaveBehandlingTema().value,
                            behandlingstype = lagretBehandling.kategori.tilOppgavebehandlingType().value
                        )
                    } else null
                }

                if (manueltOppdatert && skalOppdatereKategoriEllerUnderkategori) {
                    loggService.opprettEndretBehandlingstema(
                        behandling = lagretBehandling,
                        forrigeKategori = forrigeKategori,
                        forrigeUnderkategori = forrigeUnderkategori,
                        nyKategori = utledetKategori,
                        nyUnderkategori = utledetUnderkategori
                    )
                }
            }
        } else behandling
    }

    fun hentLøpendeKategori(fagsakId: Long): BehandlingKategori {
        return if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS)) {
            val forrigeIverksattBehandling =
                behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = fagsakId)
                    ?: return BehandlingKategori.NASJONAL

            val barnasTidslinjer =
                tidslinjeService.hentTidslinjer(behandlingId = forrigeIverksattBehandling.id)?.barnasTidslinjer()
            utledLøpendeKategori(barnasTidslinjer)
        } else {
            BehandlingKategori.NASJONAL
        }
    }

    fun hentKategoriFraInneværendeBehandling(fagsakId: Long): BehandlingKategori {
        return if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS)) {
            val aktivBehandling =
                behandlingHentOgPersisterService.hentAktivOgÅpenForFagsak(fagsakId = fagsakId)
                    ?: return BehandlingKategori.NASJONAL
            val erVilkårMedEØSRegelverkBehandlet =
                vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = aktivBehandling.id)
                    ?.personResultater
                    ?.flatMap { it.vilkårResultater }
                    ?.filter { it.behandlingId == aktivBehandling.id }
                    ?.any { it.vurderesEtter == Regelverk.EØS_FORORDNINGEN }

            return if (erVilkårMedEØSRegelverkBehandlet == true) {
                BehandlingKategori.EØS
            } else {
                BehandlingKategori.NASJONAL
            }
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

        val erUtvidetVilkårBehandlet =
            vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = aktivBehandling.id)
                ?.personResultater
                ?.flatMap { it.vilkårResultater }
                ?.filter { it.behandlingId == aktivBehandling.id }
                ?.any { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

        return if (erUtvidetVilkårBehandlet == true) {
            BehandlingUnderkategori.UTVIDET
        } else {
            BehandlingUnderkategori.ORDINÆR
        }
    }

    private fun hentForrigeAndeler(fagsakId: Long): List<AndelTilkjentYtelse>? {
        val forrigeIverksattBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = fagsakId) ?: return null
        return andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = forrigeIverksattBehandling.id)
    }
}
