package com.sitepulse.engine.detection.infrastructure.persistence;

import com.sitepulse.engine.detection.domain.enums.DetectionClassGroup;
import com.sitepulse.engine.detection.domain.model.DetectionClassDefinition;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DetectionClassCatalogAdapterTest {

    @Test
    void byGroupOrdersGroupsAndClassesDeterministically() {
        DetectionClassCatalogAdapter catalog = new DetectionClassCatalogAdapter(repositoryWithClasses(
                new DetectionClassEntity(4, "worker", "people"),
                new DetectionClassEntity(2, "truck", "truck"),
                new DetectionClassEntity(5, "excavator", "earthmoving"),
                new DetectionClassEntity(1, "operator", "people")
        ));

        Map<String, List<DetectionClassDefinition>> grouped = catalog.byGroup();

        assertEquals(List.of(
                DetectionClassGroup.PEOPLE.toPersistenceValue(),
                DetectionClassGroup.TRUCK.toPersistenceValue(),
                DetectionClassGroup.EARTHMOVING.toPersistenceValue()
        ), List.copyOf(grouped.keySet()));
        assertEquals(List.of("operator", "worker"), grouped.get(DetectionClassGroup.PEOPLE.toPersistenceValue()).stream().map(DetectionClassDefinition::className).toList());
        assertEquals(List.of("truck"), grouped.get(DetectionClassGroup.TRUCK.toPersistenceValue()).stream().map(DetectionClassDefinition::className).toList());
        assertEquals(List.of("excavator"), grouped.get(DetectionClassGroup.EARTHMOVING.toPersistenceValue()).stream().map(DetectionClassDefinition::className).toList());
    }

    @Test
    void resolveBestEffortPreservesAliasMapping() {
        DetectionClassCatalogAdapter catalog = new DetectionClassCatalogAdapter(repositoryWithClasses(
                new DetectionClassEntity(1, "person", "people"),
                new DetectionClassEntity(2, "truck", "truck"),
                new DetectionClassEntity(3, "other_equipment", "other_equipment"),
                new DetectionClassEntity(4, "crane_mobile", "lifting")
        ));

        assertEquals("person", catalog.resolveBestEffort("worker").className());
        assertEquals("truck", catalog.resolveBestEffort("lorry").className());
        assertEquals("crane_mobile", catalog.resolveBestEffort("mobile_crane").className());
    }

    @Test
    void resolveBestEffortPreservesKeywordFallbackBehavior() {
        DetectionClassCatalogAdapter catalog = new DetectionClassCatalogAdapter(repositoryWithClasses(
                new DetectionClassEntity(1, "person", "people"),
                new DetectionClassEntity(2, "car", "light_vehicle"),
                new DetectionClassEntity(3, "other_vehicle", "other_vehicle"),
                new DetectionClassEntity(4, "other_equipment", "other_equipment")
        ));

        assertEquals("person", catalog.resolveBestEffort("human silhouette").className());
        assertEquals("other_vehicle", catalog.resolveBestEffort("site vehicle").className());
        assertEquals("other_equipment", catalog.resolveBestEffort("soil drilling machine").className());
        assertEquals("other_equipment", catalog.resolveBestEffort("totally_unknown_label").className());
    }

    @Test
    void byIdAndByNameRemainStableAcrossCalls() {
        DetectionClassCatalogAdapter catalog = new DetectionClassCatalogAdapter(repositoryWithClasses(
                new DetectionClassEntity(7, "telehandler", "lifting")
        ));

        DetectionClassDefinition byId = catalog.findById(7).orElseThrow();
        DetectionClassDefinition byName = catalog.findByName("telehandler").orElseThrow();

        assertSame(byId, byName);
        assertEquals(7, catalog.resolveIdBestEffort("telehandler"));
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
