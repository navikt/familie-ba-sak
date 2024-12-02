package no.nav.familie.ba.sak.kjerne.behandling.behandlingstema

import jakarta.transaction.Transactional
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt
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
    private val vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
) {
    @Transactional
    fun oppdaterBehandlingstemaForRegistrerSøknad(
        behandling: Behandling,
        nyUnderkategori: BehandlingUnderkategori,
    ): Behandling = oppdaterBehandlingstemaPåBehandlingHvisNødvendig(behandling, behandling.kategori, nyUnderkategori)

    @Transactional
    fun oppdaterSaksbehandletBehandlingstema(
        behandling: Behandling,
        nyKategori: BehandlingKategori,
        nyUnderkategori: BehandlingUnderkategori,
    ): Behandling {
        val forrigeKategori = behandling.kategori
        val forrigeUnderkategori = behandling.underkategori
        val oppdatertBehanding = oppdaterBehandlingstemaPåBehandlingHvisNødvendig(behandling, nyKategori, nyUnderkategori)
        loggService.opprettEndretBehandlingstema(
            behandling = oppdatertBehanding,
            forrigeKategori = forrigeKategori,
            forrigeUnderkategori = forrigeUnderkategori,
            nyKategori = nyKategori,
            nyUnderkategori = nyUnderkategori,
        )
        return oppdatertBehanding
    }

    @Transactional
    fun oppdaterBehandlingstemaForVilkår(
        behandling: Behandling,
        overstyrtUnderkategori: BehandlingUnderkategori? = null,
    ): Behandling {
        if (behandling.skalBehandlesAutomatisk) {
            return behandling
        }
        val nyKategori = hentKategoriFraInneværendeBehandling(behandling.fagsak.id)
        val nyUnderkategori = overstyrtUnderkategori ?: hentUnderkategoriFraInneværendeBehandling(fagsakId = behandling.fagsak.id)
        return oppdaterBehandlingstemaPåBehandlingHvisNødvendig(behandling, nyKategori, nyUnderkategori)
    }

    fun hentLøpendeKategori(fagsakId: Long): BehandlingKategori {
        val forrigeVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = fagsakId)
        if (forrigeVedtatteBehandling == null) {
            return BehandlingKategori.NASJONAL
        }

        val tidslinjer = vilkårsvurderingTidslinjeService.hentTidslinjer(behandlingId = BehandlingId(forrigeVedtatteBehandling.id))
        if (tidslinjer == null) {
            return BehandlingKategori.NASJONAL
        }

        val etBarnHarMinstEnLøpendeEØSPeriode =
            tidslinjer
                .barnasTidslinjer()
                .values
                .map { it.egetRegelverkResultatTidslinje.innholdForTidspunkt(MånedTidspunkt.nå()) }
                .any { it.innhold?.regelverk == Regelverk.EØS_FORORDNINGEN }

        return if (etBarnHarMinstEnLøpendeEØSPeriode) {
            BehandlingKategori.EØS
        } else {
            BehandlingKategori.NASJONAL
        }
    }

    fun hentKategoriFraInneværendeBehandling(fagsakId: Long): BehandlingKategori {
        val aktivBehandling = behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = fagsakId)
        if (aktivBehandling == null) {
            return BehandlingKategori.NASJONAL
        }

        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = aktivBehandling.id)
        if (vilkårsvurdering == null) {
            return aktivBehandling.kategori
        }

        val erVilkårMedEØSRegelverkBehandlet =
            vilkårsvurdering.personResultater
                .flatMap { it.vilkårResultater }
                .filter { it.sistEndretIBehandlingId == aktivBehandling.id }
                .any { it.vurderesEtter == Regelverk.EØS_FORORDNINGEN }

        return if (erVilkårMedEØSRegelverkBehandlet) {
            BehandlingKategori.EØS
        } else {
            BehandlingKategori.NASJONAL
        }
    }

    fun hentLøpendeUnderkategori(fagsakId: Long): BehandlingUnderkategori? {
        val forrigeVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = fagsakId)
        if (forrigeVedtatteBehandling == null) {
            return null
        }
        val forrigeAndeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = forrigeVedtatteBehandling.id)
        return if (forrigeAndeler.any { it.erUtvidet() && it.erLøpende() }) {
            BehandlingUnderkategori.UTVIDET
        } else {
            BehandlingUnderkategori.ORDINÆR
        }
    }

    fun hentUnderkategoriFraInneværendeBehandling(fagsakId: Long): BehandlingUnderkategori {
        val aktivBehandling = behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = fagsakId)
        if (aktivBehandling == null) {
            return BehandlingUnderkategori.ORDINÆR
        }

        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = aktivBehandling.id)
        if (vilkårsvurdering == null) {
            return BehandlingUnderkategori.ORDINÆR
        }

        val erUtvidetVilkårBehandlet =
            vilkårsvurdering
                .personResultater
                .flatMap { it.vilkårResultater }
                .filter { it.sistEndretIBehandlingId == aktivBehandling.id }
                .any { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

        return if (erUtvidetVilkårBehandlet) {
            BehandlingUnderkategori.UTVIDET
        } else {
            BehandlingUnderkategori.ORDINÆR
        }
    }

    private fun oppdaterBehandlingstemaPåBehandlingHvisNødvendig(
        behandling: Behandling,
        nyKategori: BehandlingKategori,
        nyUnderkategori: BehandlingUnderkategori,
    ): Behandling {
        val skalOppdatereKategori = nyKategori != behandling.kategori
        val skalOppdatereUnderkategori = nyUnderkategori != behandling.underkategori
        if (skalOppdatereKategori || skalOppdatereUnderkategori) {
            behandling.kategori = nyKategori
            behandling.underkategori = nyUnderkategori
            val lagretBehandling = behandlingHentOgPersisterService.lagreEllerOppdater(behandling)
            patchOppgaveForBehandlingHvisNødvendig(lagretBehandling)
            return lagretBehandling
        }
        return behandling
    }

    private fun patchOppgaveForBehandlingHvisNødvendig(
        behandling: Behandling,
    ) {
        oppgaveService.patchOppgaverForBehandling(behandling) {
            val behandlingstemaErEndret = it.behandlingstema != behandling.tilOppgaveBehandlingTema().value
            val behandlingstypeErEndret = it.behandlingstype != behandling.kategori.tilOppgavebehandlingType().value
            if (behandlingstemaErEndret || behandlingstypeErEndret) {
                it.copy(
                    behandlingstema = behandling.tilOppgaveBehandlingTema().value,
                    behandlingstype = behandling.kategori.tilOppgavebehandlingType().value,
                )
            } else {
                null
            }
        }
    }
}
