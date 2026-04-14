package com.sitepulse.engine.detection.application.service;

import com.sitepulse.engine.detection.infrastructure.persistence.DetectionClassEntity;
import com.sitepulse.engine.detection.infrastructure.persistence.DetectionClassRepository;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DetectionClassCatalogTest {

    @Test
    void byGroupOrdersGroupsAndClassesDeterministically() {
        DetectionClassCatalog catalog = new DetectionClassCatalog(repositoryWithClasses(
                new DetectionClassEntity(4, "worker", "people"),
                new DetectionClassEntity(2, "truck", "truck"),
                new DetectionClassEntity(5, "excavator", "earthmoving"),
                new DetectionClassEntity(1, "operator", "people")
        ));

        Map<String, List<DetectionClassEntity>> grouped = catalog.byGroup();

        assertEquals(List.of("people", "truck", "earthmoving"), List.copyOf(grouped.keySet()));
        assertEquals(List.of("operator", "worker"), grouped.get("people").stream().map(DetectionClassEntity::getClassName).toList());
        assertEquals(List.of("truck"), grouped.get("truck").stream().map(DetectionClassEntity::getClassName).toList());
        assertEquals(List.of("excavator"), grouped.get("earthmoving").stream().map(DetectionClassEntity::getClassName).toList());
    }

    private DetectionClassRepository repositoryWithClasses(DetectionClassEntity... classes) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("findAll".equals(method.getName())) {
                    return List.of(classes);
                }
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "toString" -> "DetectionClassRepositoryProxy";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> null;
                    };
                }
                throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
            }
        };
        return (DetectionClassRepository) Proxy.newProxyInstance(
                DetectionClassRepository.class.getClassLoader(),
                new Class<?>[] {DetectionClassRepository.class},
                handler
        );
    }
}
