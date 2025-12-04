package no.nav.familie.ba.sak.kjerne.behandling.behandlingstema

import jakarta.transaction.Transactional
import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori.EØS
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori.NASJONAL
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.EØS_FORORDNINGEN
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.NASJONALE_REGLER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.familie.tidslinje.utvidelser.verdiPåTidspunkt
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BehandlingstemaService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val loggService: LoggService,
    private val oppgaveService: OppgaveService,
    private val vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val clockProvider: ClockProvider,
    private val integrasjonKlient: IntegrasjonKlient,
) {
    @Transactional
    fun oppdaterBehandlingstemaFraRegistrereSøknadSteg(
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
            throw FunksjonellFeil("Kan ikke oppdatere behandlingstema manuelt på behandlinger som skal behandles automatisk.")
        }
        val forrigeKategori = behandling.kategori
        val forrigeUnderkategori = behandling.underkategori
        val oppdatertBehanding = oppdaterBehandlingstemaPåBehandlingHvisNødvendig(behandling, nyKategori, nyUnderkategori)
        lagLogginnslagHvisNødvendig(oppdatertBehanding, forrigeKategori, forrigeUnderkategori, nyKategori, nyUnderkategori)
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
        val nyKategori = finnBehandlingKategori(behandling.fagsak.id)
        val nyUnderkategori = overstyrtUnderkategori ?: finnUnderkategoriFraAktivBehandling(fagsakId = behandling.fagsak.id)
        return oppdaterBehandlingstemaPåBehandlingHvisNødvendig(behandling, nyKategori, nyUnderkategori)
    }

    fun finnBehandlingKategori(fagsakId: Long): BehandlingKategori {
        val aktivBehandling = behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = fagsakId)
        val sisteVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = fagsakId)

        if (aktivBehandling == null) {
            return sisteVedtatteBehandling?.kategori ?: NASJONAL
        }

        val tidslinjer = vilkårsvurderingTidslinjeService.hentTidslinjer(behandlingId = BehandlingId(aktivBehandling.id))

        if (tidslinjer == null) {
            return sisteVedtatteBehandling?.kategori ?: NASJONAL
        }

        val nå = LocalDate.now(clockProvider.get())
        val alleBarnasRegelverkResultatNesteMåned =
            tidslinjer
                .barnasTidslinjer()
                .values
                .mapNotNull {
                    it.egetRegelverkResultatTidslinje.verdiPåTidspunkt(nå.plusMonths(1))
                }

        val alleBarnasRegelverkResultatInneværendeMåned =
            tidslinjer
                .barnasTidslinjer()
                .values
                .mapNotNull { it.egetRegelverkResultatTidslinje.verdiPåTidspunkt(nå) }

        return when {
            alleBarnasRegelverkResultatNesteMåned.any { it.regelverk == EØS_FORORDNINGEN } -> EØS
            alleBarnasRegelverkResultatNesteMåned.any { it.regelverk == NASJONALE_REGLER } -> NASJONAL
            alleBarnasRegelverkResultatInneværendeMåned.any { it.regelverk == EØS_FORORDNINGEN } -> EØS
            alleBarnasRegelverkResultatInneværendeMåned.any { it.regelverk == NASJONALE_REGLER } -> NASJONAL
            else -> sisteVedtatteBehandling?.kategori ?: aktivBehandling.kategori
        }
    }

    fun finnLøpendeUnderkategoriFraForrigeVedtatteBehandling(fagsakId: Long): BehandlingUnderkategori? {
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

    fun finnUnderkategoriFraAktivBehandling(fagsakId: Long): BehandlingUnderkategori {
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
                val endretAvEnhetsnr =
                    SikkerhetContext.hentSaksbehandler().takeIf { it != SYSTEM_FORKORTELSE }?.let {
                        integrasjonKlient.hentSaksbehandler(it).enhet
                    }

                it.copy(
                    behandlingstema = behandling.tilOppgaveBehandlingTema().value,
                    behandlingstype = behandling.kategori.tilOppgavebehandlingType().value,
                    endretAvEnhetsnr = endretAvEnhetsnr ?: it.endretAvEnhetsnr,
                )
            } else {
                null
            }
        }
    }

    private fun lagLogginnslagHvisNødvendig(
        behandling: Behandling,
        forrigeKategori: BehandlingKategori,
        forrigeUnderkategori: BehandlingUnderkategori,
        nyKategori: BehandlingKategori,
        nyUnderkategori: BehandlingUnderkategori,
    ) {
        val skalOppdatereKategori = nyKategori != forrigeKategori
        val skalOppdatereUnderkategori = nyUnderkategori != forrigeUnderkategori
        if (skalOppdatereKategori || skalOppdatereUnderkategori) {
            loggService.opprettEndretBehandlingstema(
                behandling = behandling,
                forrigeKategori = forrigeKategori,
                forrigeUnderkategori = forrigeUnderkategori,
                nyKategori = nyKategori,
                nyUnderkategori = nyUnderkategori,
            )
        }
    }
}
