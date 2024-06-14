package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.BegrunnelseTeksterStepDefinition
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository

fun mockAndelTilkjentYtelseRepository(dataFraCucumber: BegrunnelseTeksterStepDefinition): AndelTilkjentYtelseRepository {
    val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.tilkjenteYtelser[behandlingId]?.andelerTilkjentYtelse?.toList() ?: emptyList()
    }
    every { andelTilkjentYtelseRepository.hentSisteAndelPerIdentOgType(any()) } answers {
        val fagsakId = firstArg<Long>()
        val behandlingId =
            dataFraCucumber.behandlinger
                .filter { it.value.fagsak.id == fagsakId }
                .filter { it.value.status == BehandlingStatus.AVSLUTTET }
                .maxByOrNull { it.value.id }
                ?.key
        val andelerPåBehandling = dataFraCucumber.tilkjenteYtelser[behandlingId]?.andelerTilkjentYtelse ?: emptyList()
        andelerPåBehandling.tilSisteAndelPerAktørOgType()
    }
    return andelTilkjentYtelseRepository
}

fun Collection<AndelTilkjentYtelse>.tilSisteAndelPerAktørOgType(): List<AndelTilkjentYtelse> {
    val andelerGruppertPåAktørOgType = groupBy { Pair(it.aktør, it.type) }
    val sisteAndelPerAktørOgType = andelerGruppertPåAktørOgType.map { it.value.maxBy { it.stønadFom } }
    return sisteAndelPerAktørOgType
}
