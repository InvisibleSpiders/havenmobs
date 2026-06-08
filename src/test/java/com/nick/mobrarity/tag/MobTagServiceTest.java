package com.nick.mobrarity.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

final class MobTagServiceTest {
    @Test
    void storesSerializedRarityDataInPersistentDataContainer() {
        LivingEntity entity = mock(LivingEntity.class);
        PersistentDataContainer container = mock(PersistentDataContainer.class);
        MobRarityData data = new MobRarityData("rare", "rare_sheep", 14);
        when(entity.getPersistentDataContainer()).thenReturn(container);

        new MobTagService(rarityDataKey()).tag(entity, data);

        ArgumentCaptor<NamespacedKey> key = ArgumentCaptor.forClass(NamespacedKey.class);
        verify(container).set(key.capture(), eq(PersistentDataType.STRING), eq("rare|rare_sheep|14"));
        assertThat(key.getValue().getKey()).isEqualTo("rarity_data");
    }

    @Test
    void readsSerializedRarityDataFromPersistentDataContainer() {
        LivingEntity entity = mock(LivingEntity.class);
        PersistentDataContainer container = mock(PersistentDataContainer.class);
        when(entity.getPersistentDataContainer()).thenReturn(container);
        when(container.get(rarityDataKey(), PersistentDataType.STRING))
                .thenReturn("rare|rare_sheep|14");

        assertThat(new MobTagService(rarityDataKey()).read(entity))
                .contains(new MobRarityData("rare", "rare_sheep", 14));
    }

    @Test
    void clearsRarityDataFromPersistentDataContainer() {
        LivingEntity entity = mock(LivingEntity.class);
        PersistentDataContainer container = mock(PersistentDataContainer.class);
        when(entity.getPersistentDataContainer()).thenReturn(container);

        new MobTagService(rarityDataKey()).clear(entity);

        verify(container).remove(rarityDataKey());
    }

    private static NamespacedKey rarityDataKey() {
        return new NamespacedKey("mobrarity", "rarity_data");
    }
}
