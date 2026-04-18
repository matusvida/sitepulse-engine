package com.sitepulse.engine.detection.domain.model;

import com.sitepulse.engine.detection.domain.enums.DetectionClassGroup;
import java.util.List;
import java.util.Map;

public final class DetectionTaxonomy {

    public static final String PERSON = "person";
    public static final String MOTORCYCLE = "motorcycle";
    public static final String BICYCLE = "bicycle";
    public static final String PICKUP_TRUCK = "pickup_truck";
    public static final String TRUCK = "truck";
    public static final String DUMP_TRUCK = "dump_truck";
    public static final String CONCRETE_MIXER_TRUCK = "concrete_mixer_truck";
    public static final String EXCAVATOR = "excavator";
    public static final String BACKHOE_LOADER = "backhoe_loader";
    public static final String WHEEL_LOADER = "wheel_loader";
    public static final String SKID_STEER_LOADER = "skid_steer_loader";
    public static final String BULLDOZER = "bulldozer";
    public static final String GRADER = "grader";
    public static final String ROLLER = "roller";
    public static final String FORKLIFT = "forklift";
    public static final String TELEHANDLER = "telehandler";
    public static final String PAVER = "paver";
    public static final String CRANE_MOBILE = "crane_mobile";
    public static final String CRANE_TOWER = "crane_tower";
    public static final String CRANE_TRUCK = "crane_truck";
    public static final String HOIST = "hoist";
    public static final String CHERRY_PICKER = "cherry_picker";
    public static final String SCAFFOLDING = "scaffolding";
    public static final String GENERATOR = "generator";
    public static final String HELICOPTER = "helicopter";
    public static final String CAR = "car";
    public static final String VAN = "van";
    public static final String BUS = "bus";
    public static final String OTHER_VEHICLE = "other_vehicle";
    public static final String OTHER_EQUIPMENT = "other_equipment";

    public static final List<String> CLASS_GROUP_ORDER = List.of(
            DetectionClassGroup.PEOPLE.toPersistenceValue(),
            DetectionClassGroup.LIGHT_VEHICLE.toPersistenceValue(),
            DetectionClassGroup.TRUCK.toPersistenceValue(),
            DetectionClassGroup.TRANSPORT.toPersistenceValue(),
            DetectionClassGroup.EARTHMOVING.toPersistenceValue(),
            DetectionClassGroup.LIFTING.toPersistenceValue(),
            DetectionClassGroup.PAVING.toPersistenceValue(),
            DetectionClassGroup.STRUCTURE.toPersistenceValue(),
            DetectionClassGroup.POWER.toPersistenceValue(),
            DetectionClassGroup.AERIAL.toPersistenceValue(),
            DetectionClassGroup.OTHER_VEHICLE.toPersistenceValue(),
            DetectionClassGroup.OTHER_EQUIPMENT.toPersistenceValue(),
            DetectionClassGroup.UNKNOWN.toPersistenceValue()
    );

    public static final Map<String, String> ALIAS_TO_CANONICAL = Map.ofEntries(
            Map.entry("man", PERSON),
            Map.entry("woman", PERSON),
            Map.entry("worker", PERSON),
            Map.entry("operator", PERSON),
            Map.entry("supervisor", PERSON),
            Map.entry("human", PERSON),
            Map.entry("persons", PERSON),
            Map.entry("people", PERSON),
            Map.entry("motorbike", MOTORCYCLE),
            Map.entry("motorbike.", MOTORCYCLE),
            Map.entry("motorcycle", MOTORCYCLE),
            Map.entry("motor_cycle", MOTORCYCLE),
            Map.entry("bike", BICYCLE),
            Map.entry("bicycle", BICYCLE),
            Map.entry("pickup", PICKUP_TRUCK),
            Map.entry("pickuptruck", PICKUP_TRUCK),
            Map.entry("pickup_truck", PICKUP_TRUCK),
            Map.entry("ute", PICKUP_TRUCK),
            Map.entry("lorry", TRUCK),
            Map.entry("delivery_truck", TRUCK),
            Map.entry("dumptruck", DUMP_TRUCK),
            Map.entry("dump_truck", DUMP_TRUCK),
            Map.entry("mixer", CONCRETE_MIXER_TRUCK),
            Map.entry("cement_mixer", CONCRETE_MIXER_TRUCK),
            Map.entry("concrete_mixer", CONCRETE_MIXER_TRUCK),
            Map.entry("concrete_mixer_truck", CONCRETE_MIXER_TRUCK),
            Map.entry("excavator", EXCAVATOR),
            Map.entry("digger", EXCAVATOR),
            Map.entry("backhoe", BACKHOE_LOADER),
            Map.entry("loader", WHEEL_LOADER),
            Map.entry("wheel_loader", WHEEL_LOADER),
            Map.entry("skidsteer", SKID_STEER_LOADER),
            Map.entry("skid_steer", SKID_STEER_LOADER),
            Map.entry("bobcat", SKID_STEER_LOADER),
            Map.entry("bulldozer", BULLDOZER),
            Map.entry("dozer", BULLDOZER),
            Map.entry("grader", GRADER),
            Map.entry("roller", ROLLER),
            Map.entry("compactor", ROLLER),
            Map.entry("forklift", FORKLIFT),
            Map.entry("telehandler", TELEHANDLER),
            Map.entry("paver", PAVER),
            Map.entry("crane", CRANE_MOBILE),
            Map.entry("mobile_crane", CRANE_MOBILE),
            Map.entry("tower_crane", CRANE_TOWER),
            Map.entry("truck_crane", CRANE_TRUCK),
            Map.entry("crane_truck", CRANE_TRUCK),
            Map.entry("hoist", HOIST),
            Map.entry("cherrypicker", CHERRY_PICKER),
            Map.entry("cherry_picker", CHERRY_PICKER),
            Map.entry("boom_lift", CHERRY_PICKER),
            Map.entry("scaffold", SCAFFOLDING),
            Map.entry("scaffolding", SCAFFOLDING),
            Map.entry("generator", GENERATOR),
            Map.entry("helicopter", HELICOPTER),
            Map.entry("car", CAR),
            Map.entry("sedan", CAR),
            Map.entry("suv", CAR),
            Map.entry("hatchback", CAR),
            Map.entry("van", VAN),
            Map.entry("bus", BUS)
    );

    public static final List<String> PERSON_HINT_KEYWORDS = List.of("person", "worker", "human");
    public static final List<String> VEHICLE_HINT_KEYWORDS = List.of("truck", "car", "van", "bus", "vehicle");
    public static final List<String> EQUIPMENT_HINT_KEYWORDS = List.of(
            "crane", "excav", "loader", "forklift", "bulldozer", "grader", "roller", "paver", "hoist", "generator"
    );

    private DetectionTaxonomy() {
    }
}
