package kr.io.team.loop

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices

private const val BASE = "kr.io.team.loop"

@Suppress("ktlint:standard:property-naming")
@AnalyzeClasses(packages = [BASE], importOptions = [ImportOption.DoNotIncludeTests::class])
class ArchitectureTest {
    // ── Layer dependency rules ───────────────────────────────────────

    @ArchTest
    val `Domain은 Application에 의존하지 않는다`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..application..")
            .allowEmptyShould(true)

    @ArchTest
    val `Domain은 Infrastructure에 의존하지 않는다`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .allowEmptyShould(true)

    @ArchTest
    val `Domain은 Presentation에 의존하지 않는다`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..presentation..")
            .allowEmptyShould(true)

    @ArchTest
    val `Application은 Infrastructure에 의존하지 않는다`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .allowEmptyShould(true)

    @ArchTest
    val `Application은 Presentation에 의존하지 않는다`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..presentation..")
            .allowEmptyShould(true)

    @ArchTest
    val `Infrastructure는 Application에 의존하지 않는다`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..infrastructure..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..application..")
            .allowEmptyShould(true)

    @ArchTest
    val `Infrastructure는 Presentation에 의존하지 않는다`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..infrastructure..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..presentation..")
            .allowEmptyShould(true)

    @ArchTest
    val `Presentation은 Infrastructure에 의존하지 않는다`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..presentation..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .allowEmptyShould(true)

    // ── Domain purity rules ──────────────────────────────────────────

    @ArchTest
    val `Domain은 Spring Framework에 의존하지 않는다`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.springframework..")
            .allowEmptyShould(true)

    @ArchTest
    val `Domain은 Exposed ORM에 의존하지 않는다`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.jetbrains.exposed..")
            .allowEmptyShould(true)

    // ── BC isolation rules ───────────────────────────────────────────

    @ArchTest
    val `BC 간 직접 참조를 하지 않는다 (common 제외)`: ArchRule =
        slices()
            .matching("$BASE.(*)..")
            .should()
            .notDependOnEachOther()
            .ignoreDependency(
                JavaClass.Predicates.resideInAPackage("$BASE.common.."),
                DescribedPredicate.alwaysTrue(),
            ).ignoreDependency(
                DescribedPredicate.alwaysTrue(),
                JavaClass.Predicates.resideInAPackage("$BASE.common.."),
            ).allowEmptyShould(true)
}
