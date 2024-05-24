package com.scopic.auction.api;

import com.scopic.auction.dto.ItemDto;
import com.scopic.auction.dto.ItemFetchDto;
import com.scopic.auction.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class InventoryResourcesTest extends BaseResourcesTest {

    private InventoryResources objectToTest;
    @Mock
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        objectToTest = new InventoryResources(inventoryService);
    }

    @Test
    void getItemsTest() {
        final int pageIndex = 1;
        final int totalCount = 100;
        List<ItemDto> items = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            items.add(Mockito.mock(ItemDto.class));
        }
        Mockito.when(inventoryService.getItems(CURRENT_USER, pageIndex))
                .thenReturn(new ItemFetchDto(totalCount, items));

        final ItemFetchDto itemFetchDto = objectToTest.getItems(pageIndex);

        assertEquals(totalCount, itemFetchDto.totalCount);
        assertEquals(10, itemFetchDto.items.size());
        assertTrue(itemFetchDto.items.stream().allMatch(Objects::nonNull));
    }

    @Test
    void getItemByIdTest() {
        final String itemId = UUID.randomUUID().toString();

        ItemDto expectedItemDto = new ItemDto();
        expectedItemDto.id = itemId;

        Mockito.when(inventoryService.getItemById(CURRENT_USER, itemId)).thenReturn(expectedItemDto);

        final ItemDto itemDto = objectToTest.getItemById(itemId);

        assertEquals(itemId, itemDto.id);
    }

    @Test
    void addItemTest() {
        final var data = new ItemDto();
        final var id = UUID.randomUUID();
        Mockito.when(inventoryService.addItem(data)).thenReturn(id);

        String newItemId = objectToTest.addItem(data);

        assertEquals(id.toString(), newItemId);
    }
}