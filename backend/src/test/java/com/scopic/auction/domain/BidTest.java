package com.scopic.auction.domain;

import com.scopic.auction.dto.BidDto;
import com.scopic.auction.dto.MoneyDto;
import com.scopic.auction.dto.UserDto;
import com.scopic.auction.utils.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static com.scopic.auction.utils.Whitebox.getFieldValue;
import static com.scopic.auction.utils.Whitebox.setFieldValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BidTest {

    private Bid objectToTest;

    @BeforeEach
    void setUp() {
        objectToTest = new Bid();
    }

    @Test
    void bidTest() {
        Item item = mock(Item.class);
        Bidder bidder = mock(Bidder.class);
        LocalDateTime time = LocalDateTime.now();
        Money amount = mock(Money.class);

        objectToTest = new Bid(item, bidder, time, amount);

        final BidId id = getFieldValue(objectToTest, "id");
        assertSame(item, id.item);
        assertSame(bidder, id.bidderId);
        assertSame(time, getFieldValue(objectToTest, "time"));
        assertSame(amount, getFieldValue(objectToTest, "amount"));
    }

    @Test
    void toDtoTest() {
        Bidder bidder = mock(Bidder.class);
        Item item = mock(Item.class);
        final LocalDateTime time = LocalDateTime.now();
        Money amount = mock(Money.class);
        setFieldValue(objectToTest, "id", new BidId(item, bidder));
        setFieldValue(objectToTest, "time", time);
        setFieldValue(objectToTest, "amount", amount);

        UserDto userDto = mock(UserDto.class);
        when(bidder.toDto()).thenReturn(userDto);
        MoneyDto amountDto = mock(MoneyDto.class);
        when(amount.toDto()).thenReturn(amountDto);

        final BidDto bidDto = objectToTest.toDto();

        assertEquals(userDto, bidDto.user);
        assertEquals(time, bidDto.time);
        assertEquals(amountDto, bidDto.amount);

        verify(item, Mockito.never()).toDto();
    }

    @Test
    void isBiggerThanTest() {
        Money amount1 = mock(Money.class);
        Money amount2 = mock(Money.class);
        Money amount3 = mock(Money.class);
        setFieldValue(objectToTest, "amount", amount1);

        when(amount1.isBiggerThan(amount2)).thenReturn(true);
        when(amount1.isBiggerThan(amount3)).thenReturn(false);

        assertTrue(objectToTest.isBiggerThan(amount2));
        assertFalse(objectToTest.isBiggerThan(amount3));
    }

    @Test
    void isAboutTest() {
        final var correctItem = mock(Item.class);
        final var wrongItem = mock(Item.class);

        setFieldValue(objectToTest, "id", new BidId(correctItem, mock(Bidder.class)));

        assertTrue(objectToTest.isAbout(correctItem));
        assertFalse(objectToTest.isAbout(wrongItem));
    }

    @Test
    void incrementForTest() {
        final var bidder = mock(Bidder.class);
        final var item = mock(Item.class);
        final var amount = mock(Money.class);
        final var nextAmount = mock(Money.class);

        when(amount.nextAmount()).thenReturn(nextAmount);

        setFieldValue(objectToTest, "id", new BidId(item, mock(Bidder.class)));
        setFieldValue(objectToTest, "amount", amount);

        final var newBid = objectToTest.incrementFor(bidder);

        final BidId id = getFieldValue(newBid, "id");
        assertEquals(item, id.item);
        assertEquals(bidder, id.bidderId);
        assertEquals(nextAmount, getFieldValue(newBid, "amount"));
        final LocalDateTime time = getFieldValue(newBid, "time");
        assertThat(time, CoreMatchers.notOlderThan(500));
    }

    @Test
    void addWithAmountTest() {
        final var amount = mock(Money.class);
        final var amountToAdd = mock(Money.class);
        final var expected = mock(Money.class);

        setFieldValue(objectToTest, "amount", amount);

        when(amount.add(amountToAdd)).thenReturn(expected);

        assertEquals(expected, objectToTest.addWithAmount(amountToAdd));
    }

    @Test
    void isLeadTrueTest() {
        final var bidder = mock(Bidder.class);
        final var item = mock(Item.class);
        when(item.isLeadBy(bidder)).thenReturn(true);

        setFieldValue(objectToTest, "id", new BidId(item, bidder));
        assertTrue(objectToTest.isLead());
    }

    @Test
    void isLeadFalseTest() {
        final var bidder = mock(Bidder.class);
        final var item = mock(Item.class);
        when(item.isLeadBy(bidder)).thenReturn(false);

        setFieldValue(objectToTest, "id", new BidId(item, bidder));

        assertFalse(objectToTest.isLead());
    }
}