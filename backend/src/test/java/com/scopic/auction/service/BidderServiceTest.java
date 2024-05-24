package com.scopic.auction.service;

import com.scopic.auction.domain.*;
import com.scopic.auction.dto.MoneyDto;
import com.scopic.auction.dto.SettingsDto;
import com.scopic.auction.repository.ItemRepository;
import com.scopic.auction.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidderServiceTest {

    private UserService objectToTest;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        objectToTest = new UserService(userRepository, itemRepository);
    }

    @Test
    void getSettingsTest() {
        final String username = "username";

        final var spiedObjectToTest = Mockito.spy(objectToTest);

        Bidder bidder = mock(Bidder.class);
        doReturn(bidder).when(spiedObjectToTest).getById(username);

        SettingsDto expected = mock(SettingsDto.class);
        when(bidder.getSettings()).thenReturn(expected);

        final SettingsDto settingsDto = spiedObjectToTest.getSettings(username);

        assertSame(expected, settingsDto);
    }

    @Test
    void updateSettingsTest() throws InvalidNewMaxBidAmountException {
        final String username = "username";
        SettingsDto data = new SettingsDto();
        final double value = 10d;
        data.maxBidAmount = new MoneyDto();
        data.maxBidAmount.value = value;
        data.maxBidAmount.currency = "USD";
        Bidder bidder = mock(Bidder.class);

        final UserService spiedObjectToTest = Mockito.spy(objectToTest);
        doReturn(bidder).when(spiedObjectToTest).getById(username);

        spiedObjectToTest.updateSettings(username, data);

        final var orderedVerifications = inOrder(bidder, userRepository);
        orderedVerifications.verify(bidder).update(new Money(value, "USD"));
        orderedVerifications.verify(userRepository).saveAndFlush(bidder);
    }

    @Test
    void getByIdForTheFirstTimeTest() {
        final String username = "username";
        SettingsDto data = new SettingsDto();
        final double value = 10d;
        data.maxBidAmount = new MoneyDto();
        data.maxBidAmount.value = value;
        data.maxBidAmount.currency = "USD";
        when(userRepository.findById(username))
                .thenReturn(Optional.empty());
        when(userRepository.existsById(username)).thenReturn(false);

        final Bidder bidder = objectToTest.getById(username);

        assertNotNull(bidder);

        verify(userRepository).saveAndFlush(bidder);
    }

    @Test
    void makeManualBidSuccessfullyTest() {
        final String username = "user1";
        final long newBidValue = 100L;
        final UUID itemId = UUID.randomUUID();
        final String itemIdAsString = itemId.toString();

        Item item = mock(Item.class);
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        final var expected = "success";
        when(
                item.handleBidAttemptFrom(any(Bidder.class), any(Money.class))
        ).thenReturn(expected);

        final String result = objectToTest.makeManualBid(itemIdAsString, newBidValue, username);
        assertEquals(expected, result);
        verify(item).handleBidAttemptFrom(
                new Bidder(username),
                new Money(newBidValue, "USD")
        );
        verify(itemRepository).save(item);
    }

    @Test
    void makeManualBidWithSmallerAmountTest() {
        final String username = "user1";
        final long newBidValue = 100L;
        final UUID itemId = UUID.randomUUID();
        final String itemIdAsString = itemId.toString();

        Item item = mock(Item.class);
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        final var expected = "outbidded";
        when(
                item.handleBidAttemptFrom(any(Bidder.class), any(Money.class))
        ).thenReturn(expected);

        final String result = objectToTest.makeManualBid(itemIdAsString, newBidValue, username);
        assertEquals(expected, result);
        verify(item).handleBidAttemptFrom(
                new Bidder(username),
                new Money(newBidValue, "USD")
        );
        verify(itemRepository).save(item);
    }

    @Test
    void makeManualBidChallengingBiggerBidderTest() {
        final String username = "user1";
        final long newBidValue = 100L;
        final UUID itemId = UUID.randomUUID();
        final String itemIdAsString = itemId.toString();

        Item item = mock(Item.class);
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        final var expected = "outbidded";
        when(
                item.handleBidAttemptFrom(any(Bidder.class), any(Money.class))
        ).thenReturn(expected);

        final String result = objectToTest.makeManualBid(itemIdAsString, newBidValue, username);
        assertEquals(expected, result);
        verify(item).handleBidAttemptFrom(
                new Bidder(username),
                new Money(newBidValue, "USD")
        );
        verify(itemRepository).save(item);
    }

    @Test
    void activateAutoBidOnItemTest() {
        final UUID itemId = UUID.randomUUID();
        final String itemIdAsString = itemId.toString();
        final String username = "username";
        final Item item = mock(Item.class);

        when(itemRepository.findById(itemId))
                .thenReturn(Optional.of(item));

        final UserService spiedObjectToTest = Mockito.spy(objectToTest);
        Bidder bidder = mock(Bidder.class);
        doReturn(bidder).when(spiedObjectToTest).getById(username);

        spiedObjectToTest.activateAutoBidOnItem(username, itemIdAsString);

        final var orderedChecker = inOrder(bidder, itemRepository, userRepository);

        orderedChecker.verify(bidder).activateAutoBidOn(item);
        orderedChecker.verify(itemRepository).save(item);
        orderedChecker.verify(userRepository).save(bidder);
    }

    @Test
    void deactivateAutoBidOnItemTest() {
        final UUID itemId = UUID.randomUUID();
        final String itemIdAsString = itemId.toString();
        final String username = "username";
        final Item item = mock(Item.class);
        Bidder bidder = mock(Bidder.class);

        when(itemRepository.findById(itemId))
                .thenReturn(Optional.of(item));

        final UserService spiedObjectToTest = Mockito.spy(objectToTest);
        doReturn(bidder).when(spiedObjectToTest).getById(username);

        spiedObjectToTest.deactivateAutoBidOnItem(username, itemIdAsString);

        verify(bidder).deactivateAutoBidOn(item);
        verify(userRepository).saveAndFlush(bidder);
    }
}