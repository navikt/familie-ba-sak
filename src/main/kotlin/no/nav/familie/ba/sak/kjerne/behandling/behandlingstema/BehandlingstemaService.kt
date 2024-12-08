package no.nav.familie.ba.sak.kjerne.behandling.behandlingstema

import jakarta.transaction.Transactional
import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.FunksjonellFeil
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
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class BehandlingstemaService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val loggService: LoggService,
    private val oppgaveService: OppgaveService,
    private val vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val clockProvider: ClockProvider,
) {
    @Transactional
    fun oppdaterBehandlingstemaForRegistrereSøknad(
        behandling: Behandling,
        nyUnderkategori: BehandlingUnderkategori,
    ): Behandling {
        if (behandling.skalBehandlesAutomatisk) {
            return behandling
        }
        return oppdaterBehandlingstemaPåBehandlingHvisNødvendig(behandling, behandling.kategori, nyUnderkategori)
    }

    @Transactional
    fun oppdaterSaksbehandletBehandlingstema(
        behandling: Behandling,
        nyKategori: BehandlingKategori,
        nyUnderkategori: BehandlingUnderkategori,
    ): Behandling {
        if (behandling.skalBehandlesAutomatisk) {
            throw FunksjonellFeil("Kan ikke oppdatere behandlingstema på behandlinger som skal behandles automatisk.")
        }
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
        val nyKategori = finnKategori(behandling.fagsak.id)
        val nyUnderkategori = overstyrtUnderkategori ?: finnUnderkategoriFraInneværendeBehandling(fagsakId = behandling.fagsak.id)
        return oppdaterBehandlingstemaPåBehandlingHvisNødvendig(behandling, nyKategori, nyUnderkategori)
    }

    fun finnKategori(fagsakId: Long): BehandlingKategori {
        val aktivBehandling = behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = fagsakId)
        val sisteVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = fagsakId)

        if (aktivBehandling == null) {
            return sisteVedtatteBehandling?.kategori ?: BehandlingKategori.NASJONAL
        }

        val tidslinjer = vilkårsvurderingTidslinjeService.hentTidslinjer(behandlingId = BehandlingId(aktivBehandling.id))
        if (tidslinjer == null) {
            return sisteVedtatteBehandling?.kategori ?: BehandlingKategori.NASJONAL
        }

        val alleBarnasTidslinjerSomHarLøpendePeriode =
            tidslinjer
                .barnasTidslinjer()
                .values
                .map { it.egetRegelverkResultatTidslinje.innholdForTidspunkt(YearMonth.now(clockProvider.get()).tilTidspunkt()) }

        val etBarnHarMinstEnLøpendeEØSPeriode = alleBarnasTidslinjerSomHarLøpendePeriode.any { it.innhold?.regelverk == Regelverk.EØS_FORORDNINGEN }
        if (etBarnHarMinstEnLøpendeEØSPeriode) {
            return BehandlingKategori.EØS
        }

        val etBarnHarMinstEnLøpendeNasjonalPeriode = alleBarnasTidslinjerSomHarLøpendePeriode.any { it.innhold?.regelverk == Regelverk.NASJONALE_REGLER }
        if (etBarnHarMinstEnLøpendeNasjonalPeriode) {
            return BehandlingKategori.NASJONAL
        }

        return sisteVedtatteBehandling?.kategori ?: BehandlingKategori.NASJONAL
    }

    fun finnLøpendeUnderkategori(fagsakId: Long): BehandlingUnderkategori? {
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

    fun finnUnderkategoriFraInneværendeBehandling(fagsakId: Long): BehandlingUnderkategori {
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
            patchBehandlingstemaPåOppgaveForBehandlingHvisNødvendig(lagretBehandling)
            return lagretBehandling
        }
        return behandling
    }

    private fun patchBehandlingstemaPåOppgaveForBehandlingHvisNødvendig(
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
