package no.nav.familie.ba.sak.common

import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class EnvService(private val environment: Environment) {
    fun erProd(): Boolean {
        return environment.activeProfiles.any {
            it == "prod"
        }
    }

    fun erPreprod(): Boolean {
        return environment.activeProfiles.any {
            it == "preprod"
        }
    }

    fun erE2E(): Boolean {
        return environment.activeProfiles.any {
            it == "e2e"
        }
    }

    fun erDev(): Boolean {
        return environment.activeProfiles.any {
            it == "dev"
        }
    }

    fun skalIverksetteBehandling(): Boolean {
        return !erProd()
    }
}