package kr.io.team.loop

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType

@SpringBootApplication
@ComponentScan(
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ["kr\\.io\\.team\\.loop\\.learning\\..*"],
        ),
    ],
)
class ServerApplication

fun main(args: Array<String>) {
    runApplication<ServerApplication>(*args)
}
