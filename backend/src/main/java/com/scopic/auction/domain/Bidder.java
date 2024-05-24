package com.scopic.auction.domain;

import com.scopic.auction.dto.SettingsDto;
import com.scopic.auction.dto.UserDto;
import com.scopic.auction.repository.jpa.MoneyConverter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "t_bidder")
public class Bidder {
    @Id
    @Column(name = "c_username")
    private String username;
    @Basic
    @Column(name = "c_maxbidamount")
    @Convert(converter = MoneyConverter.class)
    private Money maxBidAmount;
    @ManyToMany
    @JoinTable(
            name = "t_autobiditems",
            joinColumns = @JoinColumn(name = "c_user"),
            inverseJoinColumns = @JoinColumn(name = "c_item")
    )
    private final Set<Item> autoBidItems = new HashSet<>();
    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "id.bidder",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    private final Set<Bid> myBids = new HashSet<>();

    public Bidder() {
    }

    public Bidder(String username) {
        this(username, Money.ZERO_USD);
    }

    public Bidder(String username, Money maxBidAmount) {
        this.username = username;
        this.maxBidAmount = maxBidAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bidder bidder = (Bidder) o;
        return username.equals(bidder.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    public UserDto toDto() {
        return new UserDto(username);
    }

    public SettingsDto getSettings() {
        final SettingsDto result = new SettingsDto();
        result.maxBidAmount = this.maxBidAmount.toDto();
        return result;
    }

    public void update(Money maxBidAmount) throws InvalidNewMaxBidAmountException {
        if (getAutoBidTotalEngagement().isBiggerThan(maxBidAmount)) {
            throw new InvalidNewMaxBidAmountException(
                    "newMaxBidAmountSmallerThanCurrentEngagement"
            );
        }
        this.maxBidAmount = maxBidAmount;
        bidObservedItems();
    }

    private void bidObservedItems() {
        autoBidItems.stream()
                .filter(item -> !item.isLeadBy(this))
                .forEach(item -> item.tryAutoBidFor(this));
    }

    public Optional<Bidder> activateAutoBidOn(Item item) {
        if (this.autoBidItems.contains(item)) {
            return Optional.empty();
        }
        if (item.isLeadBy(this)) {
            throw new UnsupportedOperationException("CannotActivateAutoBidWhenBeingLeadingBidder");
        }
        this.autoBidItems.add(item);
        return Optional.of(item.tryAutoBidFor(this));
    }

    private Money computeAvailableCashFor(Item item) {
        final var availableCashIfNotLeadingOnItem = this.maxBidAmount.subtract(
                getAutoBidTotalEngagement()
        );
        if (item.isLeadBy(this)) {
            return item.addWithCurrentBid(availableCashIfNotLeadingOnItem);
        }
        return availableCashIfNotLeadingOnItem;
    }

    private Money getAutoBidTotalEngagement() {
        final Set<Bid> leadingBids = getMyLeadingBids();
        return Bid.sum(leadingBids);
    }

    private Set<Bid> getMyLeadingBids() {
        return this.myBids.stream()
                .filter(Bid::isLead)
                .collect(Collectors.toSet());
    }

    public boolean isAutoBidActiveFor(Item item) {
        return this.autoBidItems.contains(item);
    }

    public Bid makeManualBid(Item item, Money amount) {
        if (this.autoBidItems.contains(item)) {
            throw new UnsupportedOperationException("CannotBidManuallyOnItemWithAutoBidActivated");
        }
        return registerNewBid(item, amount);
    }

    private Bid registerNewBid(Item item, Money amount) {
        return registerBid(new Bid(item, this, LocalDateTime.now(), amount));
    }

    private Bid registerBid(Bid bid) {
        this.myBids.add(bid);
        return bid;
    }

    public Collection<Bid> tryToOutbidOn(Item item, Optional<Bidder> currentLeadingBidder) {
        if (!this.autoBidItems.contains(item)) {
            return Collections.emptyList();
        }
        final var newBidderAvailableCash = computeAvailableCashFor(item);
        if (item.isCurrentBidBiggerThan(newBidderAvailableCash)) {
            return Collections.emptyList();
        }
        List<Bid> result = new ArrayList<>(2);
        if (!currentLeadingBidder.map(candidate -> candidate.isAutoBiddingOn(item)).orElse(false)) {
            final var newBid = currentLeadingBidder.flatMap(
                    candidate -> candidate.getMyLeadingBids()
                            .stream()
                            .filter(bid -> bid.isAbout(item))
                            .findFirst()
            )
                    .map(bid -> bid.incrementFor(this))
                    .orElse(new Bid(item, this, LocalDateTime.now(), new Money(1, "USD")));
            result.add(registerBid(newBid));
        } else {
            final var currentLeadingBidderInstance = currentLeadingBidder.get();
            final var currentLeadingBidderAvailableCash = currentLeadingBidderInstance.computeAvailableCashFor(item);
            Bidder winner;
            Bidder looser;
            Money amount;
            if (newBidderAvailableCash.isBiggerThan(currentLeadingBidderAvailableCash)) {
                winner = this;
                looser = currentLeadingBidderInstance;
                amount = currentLeadingBidderAvailableCash;
            } else {
                winner = currentLeadingBidderInstance;
                looser = this;
                amount = newBidderAvailableCash;
                result.add(registerNewBid(item, newBidderAvailableCash));
            }
            result.add(winner.registerNewBid(item, amount.nextAmount()));
        }
        return result;
    }

    private boolean isAutoBiddingOn(Item item) {
        return this.autoBidItems.contains(item);
    }

    public void deactivateAutoBidOn(Item item) {
        if (item.isLeadBy(this)) {
            throw new IllegalStateException("CannotDeactivateAutoBidOnItemWhenLeading");
        }
        autoBidItems.remove(item);
    }
}
