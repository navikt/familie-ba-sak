package no.nav.familie.ba.sak.fake

import no.nav.familie.ba.sak.common.EnvService
import org.springframework.core.env.Environment

class FakeEnvService(
    environment: Environment,
) : EnvService(environment) {
    private var dev: Boolean = true
    private var preprod: Boolean = true
    private var prod: Boolean = true

    override fun erDev(): Boolean = dev

    override fun erPreprod(): Boolean = preprod

    override fun erProd(): Boolean = prod

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
