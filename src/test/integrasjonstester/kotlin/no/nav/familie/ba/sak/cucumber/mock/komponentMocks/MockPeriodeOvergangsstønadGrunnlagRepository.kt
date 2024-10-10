package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlagRepository
import no.nav.familie.kontrakter.felles.ef.Datakilde

fun mockPeriodeOvergangsstønadGrunnlagRepository(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): PeriodeOvergangsstønadGrunnlagRepository {
    val periodeOvergangsstønadGrunnlagRepository = mockk<PeriodeOvergangsstønadGrunnlagRepository>()
    every { periodeOvergangsstønadGrunnlagRepository.deleteByBehandlingId(any()) } just runs
    every { periodeOvergangsstønadGrunnlagRepository.saveAll(any<List<PeriodeOvergangsstønadGrunnlag>>()) } answers {
        val overgangstønadsperioder = firstArg<List<PeriodeOvergangsstønadGrunnlag>>()
        val behandlingId = overgangstønadsperioder.firstOrNull()?.behandlingId

        if (behandlingId != null) {
            dataFraCucumber.overgangsstønader[behandlingId] = overgangstønadsperioder.map { it.tilInternPeriodeOvergangsstønad() }
        }

        overgangstønadsperioder
    }
    every { periodeOvergangsstønadGrunnlagRepository.findByBehandlingId(any()) } answers {
        val behandlingId = firstArg<Long>()

        dataFraCucumber.overgangsstønader[behandlingId]?.map {
            PeriodeOvergangsstønadGrunnlag(
                behandlingId = behandlingId,
                aktør = dataFraCucumber.persongrunnlag[behandlingId]!!.søker.aktør,
                fom = it.fomDato,
                tom = it.tomDato,
                datakilde = Datakilde.EF,
            )
        } ?: emptyList()
    }
    return periodeOvergangsstønadGrunnlagRepository
}
