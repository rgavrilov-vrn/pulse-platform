package ru.agroeco.pulse;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Проверка архитектурных границ проекта.
 *
 * <p>Состоит из двух слоёв:</p>
 *
 * <ol>
 *   <li><b>Spring Modulith {@code verify()}</b> — валидирует {@code @ApplicationModule}-границы
 *       для модулей, которые <b>не</b> помечены как {@code sharedModules} в
 *       {@link PulseApplication#sharedModules()} (т.е. для {@code videoserver},
 *       {@code incident}, {@code goodwan}).</li>
 *
 *   <li><b>Явные ArchUnit-правила</b> — проверяют реальные зависимости
 *       shared-модулей ({@code platform}, {@code auth}, {@code refs}, {@code audit},
 *       {@code outbox}), которые {@code verify()} <b>пропускает</b>. Без этого слоя
 *       нарушения вроде {@code platform → auth}, {@code audit → auth},
 *       {@code incident → outbox} проходят незамеченными.</li>
 * </ol>
 *
 * <p>Правила из второго слоя дублируют {@code allowedDependencies} из каждого
 * модуля — но проверяются на уровне байткода и не могут быть отключены через
 * {@code sharedModules}.</p>
 */
class ModularityTests {

    private static ApplicationModules modules;
    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        modules = ApplicationModules.of(PulseApplication.class);
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("ru.agroeco.pulse");
    }

    // =================================================================
    // 1. Spring Modulith verify()
    // =================================================================

    @Test
    void verifiesModuleBoundaries() {
        modules.verify();
    }

    // =================================================================
    // 2. ArchUnit: shared-модули не должны зависеть от чужих модулей.
    //
    //    Эти правила обходят sharedModules и проверяют байткод напрямую.
    //    Для каждого shared-модуля перечислены явные запреты.
    // =================================================================

    /**
     * {@code platform} — фундамент. Не должен знать ни про {@code auth}
     * (AppPrincipal, UserDetailsService), ни про бизнес-домены.
     *
     * <p>Регрессия, которую ловит правило: {@code JwtAuthenticationFilter}
     * внедряет {@code UserDetailsService} — Spring Security-интерфейс,
     * но если кто-то заменит на конкретный {@code AppUserDetailsService}
     * (как когда-то было), зависимость появится в байткоде и тест упадёт.</p>
     */
    @Test
    void platform_must_not_depend_on_auth() {
        noClasses()
                .that().resideInAPackage("ru.agroeco.pulse.platform..")
                .should().dependOnClassesThat().resideInAPackage("ru.agroeco.pulse.auth..")
                .because("platform — инфраструктурный фундамент; auth зависит от platform, "
                        + "но не наоборот. AppPrincipal должен жить в platform, а не в auth")
                .check(classes);
    }

    @Test
    void platform_must_not_depend_on_business_modules() {
        noClasses()
                .that().resideInAPackage("ru.agroeco.pulse.platform..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "ru.agroeco.pulse.incident..",
                        "ru.agroeco.pulse.videoserver..",
                        "ru.agroeco.pulse.refs..",
                        "ru.agroeco.pulse.goodwan..")
                .because("platform — инфраструктура; она не должна знать о бизнес-доменах")
                .check(classes);
    }

    /**
     * {@code audit} — generic-журнал. Актор приходит через {@code DomainEvent.actorId()},
     * не через {@code auth.AppPrincipal}.
     *
     * <p>Регрессия, которую ловит правило: {@code AuditEventListener} использует
     * {@code instanceof AppPrincipal} для извлечения actor id. После выноса
     * {@code AppPrincipal} в {@code platform.security} — зависимость исчезает.</p>
     */
    @Test
    void audit_must_not_depend_on_auth() {
        noClasses()
                .that().resideInAPackage("ru.agroeco.pulse.audit..")
                .should().dependOnClassesThat().resideInAPackage("ru.agroeco.pulse.auth..")
                .because("audit — generic; actor id приходит через DomainEvent.actorId(), "
                        + "а не через auth-специфичные типы")
                .check(classes);
    }

    @Test
    void audit_must_not_depend_on_business_modules() {
        noClasses()
                .that().resideInAPackage("ru.agroeco.pulse.audit..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "ru.agroeco.pulse.incident..",
                        "ru.agroeco.pulse.videoserver..",
                        "ru.agroeco.pulse.refs..",
                        "ru.agroeco.pulse.goodwan..")
                .because("audit потребляет только platform.DomainEvent — никаких бизнес-типов")
                .check(classes);
    }

    /**
     * {@code refs} — автономный справочник. Зависит только от {@code platform}.
     */
    @Test
    void refs_must_not_depend_on_any_other_domain() {
        noClasses()
                .that().resideInAPackage("ru.agroeco.pulse.refs..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "ru.agroeco.pulse.incident..",
                        "ru.agroeco.pulse.videoserver..",
                        "ru.agroeco.pulse.auth..",
                        "ru.agroeco.pulse.audit..",
                        "ru.agroeco.pulse.outbox..",
                        "ru.agroeco.pulse.goodwan..")
                .because("refs — автономные справочные данные; allowedDependencies = {platform}")
                .check(classes);
    }

    /**
     * {@code outbox} — инфраструктурный сервис доставки. Не должен знать
     * о конкретных доменах (какие события возить) — это конфиг, а не код.
     */
    @Test
    void outbox_must_not_depend_on_business_modules() {
        noClasses()
                .that().resideInAPackage("ru.agroeco.pulse.outbox..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "ru.agroeco.pulse.incident..",
                        "ru.agroeco.pulse.videoserver..",
                        "ru.agroeco.pulse.refs..",
                        "ru.agroeco.pulse.goodwan..",
                        "ru.agroeco.pulse.auth..")
                .because("outbox — generic; конкретные топики и типы событий — в конфиге, "
                        + "а не в коде")
                .check(classes);
    }

    /**
     * {@code incident} публикует доменные события через {@code platform.EventDispatcher}.
     * Маршрутизацией в Kafka занимается {@code OutboxListener} (в {@code platform}).
     *
     * <p>Регрессия, которую ловит правило: прямой {@code OutboxPort.publish()}
     * из {@code CreateIncidentFromVideoEventUseCase}. После рефакторинга
     * этот импорт должен исчезнуть.</p>
     */
    @Test
    void incident_must_not_depend_on_outbox() {
        noClasses()
                .that().resideInAPackage("ru.agroeco.pulse.incident..")
                .should().dependOnClassesThat().resideInAPackage("ru.agroeco.pulse.outbox..")
                .because("incident публикует DomainEvent через platform.EventDispatcher; "
                        + "OutboxListener (в platform) роутит его в Kafka")
                .check(classes);
    }

    /**
     * {@code videoserver} — источник событий для {@code incident}.
     * Не должен зависеть от потребителя (циклическая зависимость).
     */
    @Test
    void videoserver_must_not_depend_on_incident_or_auth() {
        noClasses()
                .that().resideInAPackage("ru.agroeco.pulse.videoserver..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "ru.agroeco.pulse.incident..",
                        "ru.agroeco.pulse.auth..")
                .because("videoserver автономен; incident потребляет его события, "
                        + "а не наоборот")
                .check(classes);
    }

    // =================================================================
    // 3. Документация (опционально — не проверка, а генератор артефактов)
    // =================================================================

    /**
     * Генерирует PlantUML-диаграммы модулей в {@code target/spring-modulith-docs/}.
     * Полезно в CI как артефакт, не влияет на прохождение теста (кроме ошибок
     * сканирования Modulith, которые всё равно уронили бы {@link #verifiesModuleBoundaries()}).
     */
    @Test
    void writesModulithDocumentation() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}