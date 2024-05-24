package com.scopic.auction.domain;

import com.scopic.auction.dto.MoneyDto;
import com.scopic.auction.dto.SettingsDto;
import com.scopic.auction.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static com.scopic.auction.utils.Whitebox.getFieldValue;
import static com.scopic.auction.utils.Whitebox.setFieldValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidderTest {

    private Bidder objectToTest;

    @BeforeEach
    void setUp() {
        objectToTest = new Bidder();
    }

    @Test
    void userTest() {
        final String username = "username";
        objectToTest = new Bidder(username);

        assertEquals(username, getFieldValue(objectToTest, "username"));
    }

    @Test
    void toDtoTest() {
        final String username = "user1";
        setFieldValue(objectToTest, "username", username);

        final UserDto userDto = objectToTest.toDto();

        assertEquals(username, userDto.username);
    }

    @Test
    void userWithUsernameAndAmountTest() {
        Money amount = mock(Money.class);
        final String username = "username";
        objectToTest = new Bidder(username, amount);

        assertSame(amount, getFieldValue(objectToTest, "maxBidAmount"));
        assertSame(username, getFieldValue(objectToTest, "username"));
    }

    @Test
    void getSettingsTest() {
        final String username = "username";
        MoneyDto amountDto = mock(MoneyDto.class);

        final Money amount = mock(Money.class);
        when(amount.toDto()).thenReturn(amountDto);
        setFieldValue(objectToTest, "username", username);
        setFieldValue(objectToTest, "maxBidAmount", amount);

        final SettingsDto dto = objectToTest.getSettings();

        assertEquals(amountDto, dto.maxBidAmount);
    }

    @Test
    void activateAutoBidTest() {
        final Item item = mock(Item.class);
        Set<Item> autoBidItems = getFieldValue(objectToTest, "autoBidItems");
        int size = autoBidItems.size();

        objectToTest.activateAutoBidOn(item);

        autoBidItems = getFieldValue(objectToTest, "autoBidItems");
        verify(item).tryAutoBidFor(objectToTest);
        assertEquals(size + 1, autoBidItems.size());
        assertTrue(autoBidItems.contains(item));
    }

    @Test
    void activateAutoBidOnItemWhenBeingLeadingBidderTest() {
        Set<Item> autoBidItems = getFieldValue(objectToTest, "autoBidItems");
        int size = autoBidItems.size();

        final Item item = mock(Item.class);
        when(item.isLeadBy(objectToTest)).thenReturn(true);

        final var exception = assertThrows(
                UnsupportedOperationException.class,
                () -> objectToTest.activateAutoBidOn(item)
        );

        assertEquals("CannotActivateAutoBidWhenBeingLeadingBidder", exception.getMessage());

        autoBidItems = getFieldValue(objectToTest, "autoBidItems");
        assertEquals(size, autoBidItems.size());
        assertFalse(autoBidItems.contains(item));
    }


    @Test
    void tryAutoBidOnItemNotActivatedByNewBidderForAutoBidTest() {
        var item = mock(Item.class);

        var currentLeadingBidder = new Bidder("currentLeadingBidder");
        setFieldValue(currentLeadingBidder, "autoBidItems", Set.of(item));

        final var outbid = objectToTest.tryToOutbidOn(item, Optional.of(currentLeadingBidder));
        assertTrue(outbid.isEmpty());
    }

    @Test
    void deactivateAutoBidTest() {
        final var item1 = mock(Item.class);
        final var item2 = mock(Item.class);
        Set<Item> autoBidItems = getFieldValue(objectToTest, "autoBidItems");
        autoBidItems.add(item1);
        autoBidItems.add(item2);
        int size = autoBidItems.size();

        objectToTest.deactivateAutoBidOn(item2);

        autoBidItems = getFieldValue(objectToTest, "autoBidItems");
        assertEquals(size - 1, autoBidItems.size());
        assertTrue(autoBidItems.contains(item1));
    }

    @Test
    void deactivateAutoBidOnItemWhenLeadingTest() {
        final var item = mock(Item.class);

        when(item.isLeadBy(objectToTest)).thenReturn(true);

        final var exception = assertThrows(
                IllegalStateException.class,
                () -> objectToTest.deactivateAutoBidOn(item)
        );

        assertEquals("CannotDeactivateAutoBidOnItemWhenLeading", exception.getMessage());
    }

}