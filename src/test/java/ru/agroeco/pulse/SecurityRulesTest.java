package ru.agroeco.pulse;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;

class SecurityRulesTest {

    private static final JavaClasses classes = new ClassFileImporter()
            .importPackages("ru.agroeco.pulse");

    @Test
    void allMutationsShouldBeAuthorized() {
        ArchRule rule = methods()
                .that().areAnnotatedWith(PostMapping.class)
                .or().areAnnotatedWith(PutMapping.class)
                .or().areAnnotatedWith(DeleteMapping.class)
                .or().areAnnotatedWith(PatchMapping.class)
                .should().beAnnotatedWith(PreAuthorize.class)
                .because("every mutation endpoint must enforce authorization");

        rule.check(classes);
    }
}