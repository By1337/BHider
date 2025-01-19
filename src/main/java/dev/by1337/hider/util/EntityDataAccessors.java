package dev.by1337.hider.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;

public class EntityDataAccessors {
    public static class Entity {
        public static final int DATA_SHARED_FLAGS_ID = 0;
    }


}
// 		Entity: [
//			{
//				name: DATA_SHARED_FLAGS_ID
//				id: 0
//				type: BYTE
//			},
//			{
//				name: DATA_AIR_SUPPLY_ID
//				id: 1
//				type: INT
//			},
//			{
//				name: DATA_CUSTOM_NAME
//				id: 2
//				type: OPTIONAL_COMPONENT
//			},
//			{
//				name: DATA_CUSTOM_NAME_VISIBLE
//				id: 3
//				type: BOOLEAN
//			},
//			{
//				name: DATA_SILENT
//				id: 4
//				type: BOOLEAN
//			},
//			{
//				name: DATA_NO_GRAVITY
//				id: 5
//				type: BOOLEAN
//			},
//			{
//				name: DATA_POSE
//				id: 6
//				type: POSE
//			},
//		]

//		Player: [
//			{
//				name: DATA_PLAYER_ABSORPTION_ID
//				id: 14
//				type: FLOAT
//			},
//			{
//				name: DATA_SCORE_ID
//				id: 15
//				type: INT
//			},
//			{
//				name: DATA_PLAYER_MODE_CUSTOMISATION
//				id: 16
//				type: BYTE
//			},
//			{
//				name: DATA_PLAYER_MAIN_HAND
//				id: 17
//				type: BYTE
//			},
//			{
//				name: DATA_SHOULDER_LEFT
//				id: 18
//				type: COMPOUND_TAG
//			},
//			{
//				name: DATA_SHOULDER_RIGHT
//				id: 19
//				type: COMPOUND_TAG
//			},
//		]

// 		LivingEntity: [
//			{
//				name: DATA_LIVING_ENTITY_FLAGS
//				id: 7
//				type: BYTE
//			},
//			{
//				name: DATA_HEALTH_ID
//				id: 8
//				type: FLOAT
//			},
//			{
//				name: DATA_EFFECT_COLOR_ID
//				id: 9
//				type: INT
//			},
//			{
//				name: DATA_EFFECT_AMBIENCE_ID
//				id: 10
//				type: BOOLEAN
//			},
//			{
//				name: DATA_ARROW_COUNT_ID
//				id: 11
//				type: INT
//			},
//			{
//				name: DATA_STINGER_COUNT_ID
//				id: 12
//				type: INT
//			},
//			{
//				name: SLEEPING_POS_ID
//				id: 13
//				type: OPTIONAL_BLOCK_POS
//			},
//		]