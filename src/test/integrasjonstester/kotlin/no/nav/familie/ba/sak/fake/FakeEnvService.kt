package no.nav.familie.ba.sak.fake

import no.nav.familie.ba.sak.common.EnvService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment

@TestConfiguration
@Primary
class FakeEnvService(
    environment: Environment,
) : EnvService(environment) {
    override fun erDev(): Boolean = dev

    override fun erPreprod(): Boolean = preprod

    override fun erProd(): Boolean = prod

    companion object {
        private var dev: Boolean = true
        private var preprod: Boolean = true
        private var prod: Boolean = true

        fun setErDev(erDev: Boolean) {
            dev = erDev
        }

        fun setErPreprod(erPreprod: Boolean) {
            preprod = erPreprod
        }

        fun setErProd(erProd: Boolean) {
            prod = erProd
        }
    }
}
