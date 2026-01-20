package no.nav.familie.ba.sak.kjerne.vedtak.feilutbetaltValuta

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.ekstern.restDomene.FeilutbetaltValutaDto
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Month

class FeilutbetaltValutaServiceTest(
    @Autowired val feilutbetaltValutaService: FeilutbetaltValutaService,
    @Autowired val aktørIdRepository: AktørIdRepository,
    @Autowired val fagsakRepository: FagsakRepository,
    @Autowired val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) : AbstractSpringIntegrationTest() {
    @Test
    fun kanLagreEndreOgSlette() {
        val fagsak =
            lagFagsakUtenId(aktør = randomAktør().also { aktørIdRepository.save(it) }).let { fagsakRepository.save(it) }
        val behandling = lagBehandlingUtenId(fagsak = fagsak).let { behandlingHentOgPersisterService.lagreEllerOppdater(it, false) }
        val feilutbetaltValuta =
            FeilutbetaltValutaDto(
                id = 0,
                fom = LocalDate.of(2020, Month.JANUARY, 1),
                tom = LocalDate.of(2021, Month.MAY, 31),
                feilutbetaltBeløp = 1234,
            )

        val id = feilutbetaltValutaService.leggTilFeilutbetaltValutaPeriode(feilutbetaltValuta = feilutbetaltValuta, behandlingId = behandling.id)

        feilutbetaltValutaService
            .hentFeilutbetaltValutaPerioder(behandlingId = behandling.id)
            .also { Assertions.assertThat(it[0].id).isEqualTo(id) }
            .also { Assertions.assertThat(it[0].fom).isNotNull() }
            .also { Assertions.assertThat(it[0].tom).isNotNull() }

        feilutbetaltValutaService.oppdatertFeilutbetaltValutaPeriode(
            feilutbetaltValutaDto =
                FeilutbetaltValutaDto(
                    id = id,
                    fom = LocalDate.of(2020, Month.JANUARY, 1),
                    tom = LocalDate.of(2020, Month.MAY, 31),
                    feilutbetaltBeløp = 1,
                ),
            id = id,
        )

        feilutbetaltValutaService
            .hentFeilutbetaltValutaPerioder(behandlingId = behandling.id)
            .also { Assertions.assertThat(it.get(0).id).isEqualTo(id) }
            .also { Assertions.assertThat(it.get(0).tom).isEqualTo("2020-05-31") }

        val feilutbetaltValuta2 =
            FeilutbetaltValutaDto(
                id = 0,
                fom = LocalDate.of(2019, Month.DECEMBER, 1),
                tom = LocalDate.of(2019, Month.DECEMBER, 31),
                feilutbetaltBeløp = 100,
            )

        val id2 = feilutbetaltValutaService.leggTilFeilutbetaltValutaPeriode(feilutbetaltValuta = feilutbetaltValuta2, behandlingId = behandling.id)

        feilutbetaltValutaService
            .hentFeilutbetaltValutaPerioder(behandlingId = behandling.id)
            .also { Assertions.assertThat(it.size).isEqualTo(2) }
            .also { Assertions.assertThat(it.get(0).id).isEqualTo(id2) }

        feilutbetaltValutaService.fjernFeilutbetaltValutaPeriode(id = id, behandlingId = behandling.id)

        feilutbetaltValutaService
            .hentFeilutbetaltValutaPerioder(behandlingId = behandling.id)
            .also { Assertions.assertThat(it.size).isEqualTo(1) }
            .also { Assertions.assertThat(it[0].id).isEqualTo(id2) }
    }
}
