package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus

fun mockBehandlingHentOgPersisterService(
    forrigeBehandling: Behandling?,
    dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition,
    idForNyBehandling: Long,
): BehandlingHentOgPersisterService {
    val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns forrigeBehandling
    every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(any()) } returns forrigeBehandling
    every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(any()) } answers {
        val fagsakId = firstArg<Long>()
        dataFraCucumber.behandlinger.values.singleOrNull { it.fagsak.id == fagsakId && it.status != BehandlingStatus.AVSLUTTET }
    }
    every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(any()) } answers {
        val fagsakId = firstArg<Long>()
        dataFraCucumber.tilkjenteYtelser.values
            .filter { it.behandling.fagsak.id == fagsakId && it.behandling.status == BehandlingStatus.AVSLUTTET && it.utbetalingsoppdrag != null }
            .maxByOrNull { it.behandling.aktivertTidspunkt }
            ?.behandling
    }
    every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(any()) } answers {
        val behandling = firstArg<Behandling>()
        dataFraCucumber.behandlinger.values
            .filter { it.fagsak.id == behandling.fagsak.id && it.id != behandling.id && it.status == BehandlingStatus.AVSLUTTET }
            .maxByOrNull { it.aktivertTidspunkt }
    }
    every { behandlingHentOgPersisterService.hent(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.behandlinger[behandlingId]!!
    }
    every { behandlingHentOgPersisterService.finnAktivForFagsak(any()) } answers {
        forrigeBehandling
    }
    every { behandlingHentOgPersisterService.lagreOgFlush(any()) } answers {
        val behandling = firstArg<Behandling>()
        oppdaterEllerLagreBehandling(dataFraCucumber, behandling, idForNyBehandling)
    }
    every { behandlingHentOgPersisterService.lagreEllerOppdater(any(), any()) } answers {
        val behandling = firstArg<Behandling>()
        oppdaterEllerLagreBehandling(dataFraCucumber, behandling, idForNyBehandling)
    }
    every { behandlingHentOgPersisterService.hentBehandlinger(any()) } answers {
        val fagsakId = firstArg<Long>()
        dataFraCucumber.behandlinger.values.filter { it.fagsak.id == fagsakId }
    }
    every { behandlingHentOgPersisterService.hentBehandlinger(any(), any()) } answers {
        val fagsakId = firstArg<Long>()
        val status = secondArg<BehandlingStatus>()
        dataFraCucumber.behandlinger.values.filter { it.fagsak.id == fagsakId && it.status == status }
    }
    return behandlingHentOgPersisterService
}

fun oppdaterEllerLagreBehandling(
    dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition,
    behandlingSomSkalLagres: Behandling,
    idForNyBehandling: Long,
): Behandling {
    val behandling = if (behandlingSomSkalLagres.id == 0L) behandlingSomSkalLagres.copy(id = idForNyBehandling) else behandlingSomSkalLagres

    dataFraCucumber.behandlinger[behandling.id] = behandling
    return behandling
}
