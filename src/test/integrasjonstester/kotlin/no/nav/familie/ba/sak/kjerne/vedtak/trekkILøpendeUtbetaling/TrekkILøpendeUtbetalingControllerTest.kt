package no.nav.familie.ba.sak.kjerne.vedtak.trekkILøpendeUtbetaling

import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Month

class TrekkILøpendeUtbetalingControllerTest(
    @Autowired val service: TrekkILøpendeUtbetalingService,
    @Autowired val aktørIdRepository: AktørIdRepository,
    @Autowired val fagsakRepository: FagsakRepository,
    @Autowired val behandlingRepository: BehandlingRepository
) : AbstractSpringIntegrationTest() {

    @Test
    fun kanLagreEndreOgSlette() {
        val fagsak =
            defaultFagsak(aktør = randomAktør().also { aktørIdRepository.save(it) }).let { fagsakRepository.save(it) }
        val behandling = lagBehandling(fagsak = fagsak).let { behandlingRepository.save(it) }
        val trekk = RestTrekkILøpendeUtbetaling(
            id = 0,
            fom = LocalDate.of(2020, Month.JANUARY, 1),
            tom = LocalDate.of(2021, Month.MAY, 31),
            feilutbetaltBeløp = 1234
        )

        val id = service.leggTilTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling = trekk, behandlingId = behandling.id)

        service.hentTrekkILøpendeUtbetalinger(behandlingId = behandling.id)
            .also { Assertions.assertThat(it[0].id).isEqualTo(id) }
            .also { Assertions.assertThat(it[0].fom).isNotNull() }
            .also { Assertions.assertThat(it[0].tom).isNotNull() }

        service.oppdaterTrekkILøpendeUtbetaling(
            trekkILøpendeUtbetaling = RestTrekkILøpendeUtbetaling(
                id = id,
                fom = LocalDate.of(2020, Month.JANUARY, 1),
                tom = LocalDate.of(2020, Month.MAY, 31),
                feilutbetaltBeløp = 1
            ),
            id = id
        )

        service.hentTrekkILøpendeUtbetalinger(behandlingId = behandling.id)
            .also { Assertions.assertThat(it.get(0).id).isEqualTo(id) }
            .also { Assertions.assertThat(it.get(0).tom).isEqualTo("2020-05-31") }

        val trekk2 = RestTrekkILøpendeUtbetaling(
            id = 0,
            fom = LocalDate.of(2019, Month.DECEMBER, 1),
            tom = LocalDate.of(2019, Month.DECEMBER, 31),
            feilutbetaltBeløp = 100
        )

        val id2 = service.leggTilTrekkILøpendeUtbetaling(trekkILøpendeUtbetaling = trekk2, behandlingId = behandling.id)

        service.hentTrekkILøpendeUtbetalinger(behandlingId = behandling.id)
            .also { Assertions.assertThat(it.size).isEqualTo(2) }
            .also { Assertions.assertThat(it.get(0).id).isEqualTo(id2) }

        service.fjernTrekkILøpendeUtbetaling(id = id, behandlingId = behandling.id)

        service.hentTrekkILøpendeUtbetalinger(behandlingId = behandling.id)
            .also { Assertions.assertThat(it.size).isEqualTo(1) }
            .also { Assertions.assertThat(it[0].id).isEqualTo(id2) }
    }
}
