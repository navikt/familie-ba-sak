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
            identifikator = TrekkILøpendeBehandlingRestIdentifikator(
                id = 0,
                behandlingId = behandling.id
            ),
            periode = RestPeriode(
                fom = LocalDate.of(2020, Month.JANUARY, 1),
                tom = LocalDate.of(2021, Month.MAY, 31)
            ),
            feilutbetaltBeløp = 1234
        )

        val id = service.leggTilTrekkILøpendeUtbetaling(trekk)

        service.hentTrekkILøpendeUtbetalinger(behandlingId = behandling.id)
            .also {Assertions.assertThat(it?.get(0)?.identifikator?.id).isEqualTo(id) }
            .also { Assertions.assertThat(it?.get(0)?.periode?.fom).isNotNull() }
            .also { Assertions.assertThat(it?.get(0)?.periode?.tom).isNotNull() }

        service.oppdaterTrekkILøpendeUtbetaling(
            RestTrekkILøpendeUtbetaling(
                identifikator = TrekkILøpendeBehandlingRestIdentifikator(
                    id = id,
                    behandlingId = behandling.id
                ),
                periode = RestPeriode(
                    fom = LocalDate.of(2020, Month.JANUARY, 1),
                    tom = LocalDate.of(2020, Month.MAY, 31),
                ),
                feilutbetaltBeløp = 1
            )
        )

        service.hentTrekkILøpendeUtbetalinger(behandlingId = behandling.id)
            .also { Assertions.assertThat(it?.get(0)?.identifikator?.id).isEqualTo(id) }
            .also { Assertions.assertThat(it?.get(0)?.periode?.tom).isEqualTo("2020-05-31") }

        val trekk2 = RestTrekkILøpendeUtbetaling(
            identifikator = TrekkILøpendeBehandlingRestIdentifikator(
                id = 0,
                behandlingId = behandling.id
            ),
            periode = RestPeriode(
                fom = LocalDate.of(2019, Month.DECEMBER, 1),
                tom = LocalDate.of(2019, Month.DECEMBER, 31)
            ),
            feilutbetaltBeløp = 100
        )

        val id2 = service.leggTilTrekkILøpendeUtbetaling(trekk2)

        service.hentTrekkILøpendeUtbetalinger(behandlingId = behandling.id)
            .also { Assertions.assertThat(it?.size).isEqualTo(2) }
            .also { Assertions.assertThat(it?.get(0)?.identifikator?.id).isEqualTo(id2) }

        service.fjernTrekkILøpendeUtbetaling(
            TrekkILøpendeBehandlingRestIdentifikator(
                id = id,
                behandlingId = behandling.id
            )
        )

        service.hentTrekkILøpendeUtbetalinger(behandlingId = behandling.id)
            .also { Assertions.assertThat(it?.size).isEqualTo(1) }
            .also { Assertions.assertThat(it?.get(0)?.identifikator?.id).isEqualTo(id2) }
    }
}
