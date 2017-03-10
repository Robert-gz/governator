package com.netflix.governator.test;

import org.spockframework.runtime.extension.AbstractGlobalExtension;
import org.spockframework.runtime.model.SpecInfo;

import com.netflix.governator.guice.test.AnnotationBasedTestInjectorManager;
import com.netflix.governator.guice.test.InjectorCreationMode;
import com.netflix.governator.guice.test.mocks.MockHandler;
import com.netflix.governator.test.mock.spock.SpockMockHandler;

import spock.lang.Specification;

public class GovernatorExtension extends AbstractGlobalExtension {


    @Override
    public void visitSpec(SpecInfo spec) {
        AnnotationBasedTestInjectorManager annotationBasedTestInjectorManager = new AnnotationBasedTestInjectorManager(spec.getReflection(), SpockMockHandler.class);
        annotationBasedTestInjectorManager.prepareConfigForTestClass(spec.getReflection());

        //Before test class 
        spec.getSetupSpecInterceptors().add(invocation -> {
            invocation.proceed();
        });
        
        //Before test methods
        spec.getSetupInterceptors().add(invocation -> {
            MockHandler mockHandler = annotationBasedTestInjectorManager.getMockHandler();
            if(mockHandler instanceof SpockMockHandler && invocation.getInstance() instanceof Specification) {
                ((SpockMockHandler)mockHandler).setSpecification((Specification) invocation.getInstance());
                annotationBasedTestInjectorManager.cleanUpMocks();
            }
            if (InjectorCreationMode.BEFORE_TEST_CLASS == annotationBasedTestInjectorManager.getInjectorCreationMode() && annotationBasedTestInjectorManager.getInjector() == null) {
                annotationBasedTestInjectorManager.createInjector();
            }
            annotationBasedTestInjectorManager.prepareTestFixture(invocation.getInstance());
            annotationBasedTestInjectorManager.prepareTestFixture(invocation.getSharedInstance());
            annotationBasedTestInjectorManager.prepareConfigForTestClass(spec.getReflection(),
                    invocation.getFeature().getFeatureMethod().getReflection());
            
            if (InjectorCreationMode.BEFORE_EACH_TEST_METHOD == annotationBasedTestInjectorManager.getInjectorCreationMode()) {
                annotationBasedTestInjectorManager.createInjector();
            }
            invocation.proceed();
        });

        //After test methods
        spec.addCleanupInterceptor(invocation -> {
            
            annotationBasedTestInjectorManager.cleanUpMethodLevelConfig();
            annotationBasedTestInjectorManager.cleanUpMocks();
//            new MockUtil().
            if (InjectorCreationMode.BEFORE_EACH_TEST_METHOD == annotationBasedTestInjectorManager.getInjectorCreationMode()) {
                annotationBasedTestInjectorManager.cleanUpInjector();
            }
            invocation.proceed();
            
        });
        
        //After test class
        spec.addCleanupSpecInterceptor(invocation -> {
            annotationBasedTestInjectorManager.cleanUpInjector();
            invocation.proceed();
        });

        super.visitSpec(spec);
    }
}
