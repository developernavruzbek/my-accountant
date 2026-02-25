package org.example.myaccountant

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@EnableJpaRepositories(repositoryBaseClass = BaseRepositoryImpl::class)
@SpringBootApplication
@EnableJpaAuditing
class MyAccountantApplication

fun main(args: Array<String>) {
    runApplication<MyAccountantApplication>(*args)
}
