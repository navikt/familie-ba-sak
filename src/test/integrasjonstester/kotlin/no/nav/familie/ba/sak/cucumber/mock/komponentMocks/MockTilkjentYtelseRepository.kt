package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import kotlin.random.Random

fun mockTilkjentYtelseRepository(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): TilkjentYtelseRepository {
    val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    every { tilkjentYtelseRepository.findByBehandling(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.tilkjenteYtelser[behandlingId]!!
    }
    every { tilkjentYtelseRepository.findByBehandlingOptional(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.tilkjenteYtelser[behandlingId]
    }
    every { tilkjentYtelseRepository.slettTilkjentYtelseFor(any()) } just runs
    every { tilkjentYtelseRepository.save(any()) } answers {
        val tilkjentYtelse = firstArg<TilkjentYtelse>()

        val tilkjentYtelseMedUnikIdPåAndeler = tilkjentYtelse.copy(andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.map { it.copy(id = Random.nextLong()) }.toMutableSet())
        dataFraCucumber.tilkjenteYtelser[tilkjentYtelse.behandling.id] = tilkjentYtelseMedUnikIdPåAndeler
        tilkjentYtelse
    }
    every { tilkjentYtelseRepository.saveAndFlush(any()) } answers {
        val tilkjentYtelse = firstArg<TilkjentYtelse>()

        val tilkjentYtelseMedUnikIdPåAndeler = tilkjentYtelse.copy(andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.map { it.copy(id = Random.nextLong()) }.toMutableSet())
        dataFraCucumber.tilkjenteYtelser[tilkjentYtelse.behandling.id] = tilkjentYtelseMedUnikIdPåAndeler
        tilkjentYtelseMedUnikIdPåAndeler
    }
    every { tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.tilkjenteYtelser[behandlingId]
    }
    every { tilkjentYtelseRepository.harFagsakTattIBrukNyKlassekodeForUtvidetBarnetrygd(any()) } answers {
        val fagsakId = firstArg<Long>()
        dataFraCucumber.tilkjenteYtelser
            .map { it.value }
            .filter { it.behandling.fagsak.id == fagsakId }
            .mapNotNull { it.utbetalingsoppdrag }
            .any { it.contains("\"klassifisering\":\"BAUTV-OP\"") }
    }

    every { tilkjentYtelseRepository.findByOppdatertUtvidetBarnetrygdIUtbetalingsoppdrag(any()) } answers {
        val fagsakId = firstArg<Long>()
        dataFraCucumber.tilkjenteYtelser.map { it.value }.filter { it.utbetalingsoppdrag != null && it.utbetalingsoppdrag!!.contains("\"klassifisering\":\"BAUTV-OP\"") }
    }
    return tilkjentYtelseRepository
}
