package com.scopic.auction.service;

import com.scopic.auction.domain.Bidder;
import com.scopic.auction.domain.InvalidNewMaxBidAmountException;
import com.scopic.auction.domain.Item;
import com.scopic.auction.domain.Money;
import com.scopic.auction.dto.SettingsDto;
import com.scopic.auction.repository.ItemRepository;
import com.scopic.auction.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Autowired
    public UserService(UserRepository userRepository, ItemRepository itemRepository) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
    }

    Bidder getById(String id) {
        final Bidder bidder = userRepository.findById(id)
                .orElse(new Bidder(id));
        if (!userRepository.existsById(id)) {
            userRepository.saveAndFlush(bidder);
        }
        return bidder;
    }

    @Transactional
    public void updateSettings(String username, SettingsDto data) throws InvalidNewMaxBidAmountException {
        final Money maxBidAmount = new Money(
                data.maxBidAmount.value,
                data.maxBidAmount.currency
        );
        final Bidder bidder = getById(username);
        bidder.update(maxBidAmount);
        userRepository.saveAndFlush(bidder);
    }

    @Transactional
    public SettingsDto getSettings(String username) {
        return getById(username).getSettings();
    }

    @Transactional
    public void activateAutoBidOnItem(String username, String itemId) {
        final Bidder bidder = getById(username);
        final Item item = itemRepository.findById(UUID.fromString(itemId))
                .orElseThrow();
        Collection<Bidder> bidders = new HashSet<>(2);
        bidders.add(bidder);
        bidder.activateAutoBidOn(item).ifPresent(newLead -> bidders.add(newLead));
        userRepository.saveAll(bidders);
        itemRepository.save(item);
    }

    @Transactional
    public void deactivateAutoBidOnItem(String username, String itemId) {
        final Bidder bidder = getById(username);
        final Item item = itemRepository.findById(UUID.fromString(itemId))
                .orElseThrow();
        bidder.deactivateAutoBidOn(item);
        userRepository.saveAndFlush(bidder);
        itemRepository.save(item);
    }

    @Transactional
    public String makeManualBid(String itemId, Number bid, String bidderId) {
        final Item item = itemRepository.findById(UUID.fromString(itemId)).orElseThrow();
        Bidder bidder = getById(bidderId);
        final var result = item.handleBidAttemptFrom(bidder, new Money(bid, "USD"));
        userRepository.save(bidder);
        itemRepository.save(item);
        return result;
    }
}
