import org.springframework.boot.builder.SpringApplicationBuilder

class DevLauncher

fun main(args: Array<String>) {
    SpringApplicationBuilder(DevLauncher::class.java).profiles(
        "dev",
        "mock-brev-klient",
        "mock-økonomi",
        "mock-infotrygd-feed",
        "mock-infotrygd-barnetrygd",
        "mock-pdl",
        "mock-ident-client",
        "mock-tilbakekreving-klient",
        "task-scheduling"
    ).run(*args)
}
