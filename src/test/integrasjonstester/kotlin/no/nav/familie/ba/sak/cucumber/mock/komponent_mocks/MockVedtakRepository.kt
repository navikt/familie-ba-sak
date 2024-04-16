﻿package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.cucumber.BegrunnelseTeksterStepDefinition
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository

fun mockVedtakRepository(dataFraCucumber: BegrunnelseTeksterStepDefinition): VedtakRepository {
    val vedtakRepository = mockk<VedtakRepository>()
    every { vedtakRepository.findByBehandlingAndAktiv(any()) } answers {
        val behandlingId = firstArg<Long>()
        opprettEllerHentVedtak(dataFraCucumber, behandlingId)
    }
    every { vedtakRepository.getReferenceById(any()) } answers {
        val vedtakId = firstArg<Long>()
        dataFraCucumber.vedtaksliste.first { it.id == vedtakId }
    }
    every { vedtakRepository.findByBehandlingAndAktivOptional(any()) } answers {
        val behandlingId = firstArg<Long>()
        opprettEllerHentVedtak(dataFraCucumber, behandlingId)
    }
    every { vedtakRepository.save(any()) } answers {
        val oppdatertVedtak = firstArg<Vedtak>()
        lagreVedtak(dataFraCucumber, oppdatertVedtak)
    }
    every { vedtakRepository.saveAndFlush(any()) } answers {
        val oppdatertVedtak = firstArg<Vedtak>()
        lagreVedtak(dataFraCucumber, oppdatertVedtak)
    }
    return vedtakRepository
}

private fun lagreVedtak(
    dataFraCucumber: BegrunnelseTeksterStepDefinition,
    oppdatertVedtak: Vedtak,
): Vedtak {
    dataFraCucumber.vedtaksliste = dataFraCucumber.vedtaksliste.map { if (it.id == oppdatertVedtak.id) oppdatertVedtak else it }.toMutableList()
    if (oppdatertVedtak.id !in dataFraCucumber.vedtaksliste.map { it.id }) {
        dataFraCucumber.vedtaksliste.add(oppdatertVedtak)
    }
    return oppdatertVedtak
}

private fun opprettEllerHentVedtak(
    dataFraCucumber: BegrunnelseTeksterStepDefinition,
    behandlingId: Long,
): Vedtak {
    val vedtakForBehandling =
        dataFraCucumber.vedtaksliste.find { it.behandling.id == behandlingId }
            ?: lagVedtak(dataFraCucumber.behandlinger[behandlingId]!!)

    if (vedtakForBehandling !in dataFraCucumber.vedtaksliste) {
        dataFraCucumber.vedtaksliste.add(vedtakForBehandling)
    }

    return vedtakForBehandling
}
