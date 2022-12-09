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
import org.springframework.http.HttpStatus
import java.time.Month
import java.time.YearMonth

class TrekkILøpendeUtbetalingControllerTest(
    @Autowired val controller: TrekkILøpendeUtbetalingController,
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
                fom = YearMonth.of(2020, Month.JANUARY),
                tom = YearMonth.of(2021, Month.MAY)
            ),
            feilutbetaltBeløp = 1234
        )

        val lagret = controller.leggTilTrekkILøpendeUtbetaling(trekk)
            .also { Assertions.assertThat(it.statusCode).isEqualTo(HttpStatus.OK) }
            .also { Assertions.assertThat(it.body?.data?.trekkILøpendeUtbetaling?.size).isGreaterThan(0) }

        val id = lagret.body!!.data!!.trekkILøpendeUtbetaling!![0].identifikator.id

        controller.hentTrekkILøpendeUtbetalinger(behandlingId = behandling.id)
            .also { Assertions.assertThat(it.statusCode).isEqualTo(HttpStatus.OK) }
            .also { Assertions.assertThat(it.body?.data?.get(0)?.identifikator?.id).isGreaterThan(0) }
            .also { Assertions.assertThat(it.body?.data?.get(0)?.periode?.tom).isNotNull() }

        controller.oppdaterTrekkILøpendeUtbetaling(
            RestTrekkILøpendeUtbetaling(
                identifikator = TrekkILøpendeBehandlingRestIdentifikator(
                    id = id,
                    behandlingId = behandling.id
                ),
                periode = RestPeriode(
                    fom = YearMonth.of(2020, Month.JANUARY),
                    tom = null
                ),
                feilutbetaltBeløp = 1
            )
            )
            .also { Assertions.assertThat(it.statusCode).isEqualTo(HttpStatus.OK) }
            .also { Assertions.assertThat(it.body?.data?.trekkILøpendeUtbetaling?.get(0)?.periode?.tom).isNull() }

        controller.hentTrekkILøpendeUtbetalinger(behandlingId = behandling.id)
            .also { Assertions.assertThat(it.statusCode).isEqualTo(HttpStatus.OK) }
            .also { Assertions.assertThat(it.body?.data?.get(0)?.periode?.tom).isNull() }

        controller.oppdaterTrekkILøpendeUtbetaling(
            RestTrekkILøpendeUtbetaling(
                identifikator = TrekkILøpendeBehandlingRestIdentifikator(
                    id = id,
                    behandlingId = behandling.id
                ),
                periode = RestPeriode(
                    fom = YearMonth.of(2020, Month.JANUARY),
                    tom = null
                ),
                feilutbetaltBeløp = 2
            )
        )
            .also { Assertions.assertThat(it.statusCode).isEqualTo(HttpStatus.OK) }
            .also { Assertions.assertThat(it.body?.data?.trekkILøpendeUtbetaling?.get(0)?.periode?.tom).isNull() }

        controller.fjernTrekkILøpendeUtbetaling(
            TrekkILøpendeBehandlingRestIdentifikator(
                id = id,
                behandlingId = behandling.id
            )
        )

        controller.hentTrekkILøpendeUtbetalinger(behandlingId = behandling.id)
            .also { Assertions.assertThat(it.statusCode).isEqualTo(HttpStatus.OK) }
            .also { Assertions.assertThat(it.body?.data).isEmpty() }
    }
}
