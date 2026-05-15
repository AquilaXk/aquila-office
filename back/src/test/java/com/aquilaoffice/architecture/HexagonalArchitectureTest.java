package com.aquilaoffice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(packages = "com.aquilaoffice", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule rest_controllers_should_live_in_inbound_web_adapter =
            classes()
                    .that()
                    .areAnnotatedWith(RestController.class)
                    .should()
                    .resideInAPackage("..adapter.in.web..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_should_not_depend_on_outer_layers =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..adapter..", "..global..", "org.springframework..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule application_should_not_depend_on_adapters =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..adapter..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule inbound_adapters_should_not_depend_on_outbound_adapters =
            noClasses()
                    .that()
                    .resideInAPackage("..adapter.in..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..adapter.out..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule outbound_adapters_should_not_depend_on_inbound_adapters =
            noClasses()
                    .that()
                    .resideInAPackage("..adapter.out..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..adapter.in..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_should_not_use_framework_annotations =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .beAnnotatedWith(Component.class)
                    .orShould()
                    .beAnnotatedWith(Controller.class)
                    .orShould()
                    .beAnnotatedWith(RestController.class)
                    .orShould()
                    .beAnnotatedWith(Service.class)
                    .orShould()
                    .beAnnotatedWith(Repository.class)
                    .allowEmptyShould(true);
}
