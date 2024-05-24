package com.scopic.auction.domain;

import com.scopic.auction.dto.ItemDto;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "t_item")
public class Item extends BaseDomainObject {
    @Column(name = "c_name")
    private String name;
    @Column(name = "c_description")
    private String description;
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "c_currentbidder", referencedColumnName = "c_username")
    private Bidder leadingBidder;
    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "id.item",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    private final Set<Bid> bids = new HashSet<>();

    public Item() {
    }

    public Item(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public ItemDto toDto() {
        final ItemDto result = new ItemDto(id.toString(), name, description);
        result.bids = this.bids.stream()
                .map(Bid::toDto)
                .collect(Collectors.toList());
        return result;
    }

    public String handleBidAttemptFrom(Bidder newBidder, Money amount) {
        if (Optional.ofNullable(this.leadingBidder).map(candidate -> candidate.equals(newBidder)).orElse(false)) {
            return "already-leading";
        }
        if (this.bids.stream().anyMatch(bid -> bid.isBiggerThan(amount))) {
            return "outbidded";
        }
        Bid bid = newBidder.makeManualBid(
                this,
                amount
        );
        var previousLeadingBidder = this.leadingBidder;
        addNewBids(List.of(bid), newBidder);
        if (previousLeadingBidder != null) {
            addNewBids(
                    previousLeadingBidder.tryToOutbidOn(this, Optional.of(newBidder)),
                    previousLeadingBidder
            );
        }
        return newBidder.equals(this.leadingBidder) ? "success" : "outbidded";
    }

    private void addNewBids(Collection<Bid> newBids, Bidder newLeadBidder) {
        if (newBids.size() % 2 == 1) {
            this.leadingBidder = newLeadBidder;
        }
        this.bids.addAll(newBids);
    }

    public Bidder tryAutoBidFor(Bidder newBidder) {
        final var bids = newBidder.tryToOutbidOn(
                this,
                Optional.ofNullable(this.leadingBidder)
        );
        addNewBids(bids, newBidder);
        return this.leadingBidder;
    }

    public boolean isCurrentBidBiggerThan(Money amount) {
        return getNewestBid()
                .map(bid -> bid.isBiggerThan(amount))
                .orElse(Money.ZERO_USD.isBiggerThan(amount));
    }

    private Optional<Bid> getNewestBid() {
        return Bid.newestOf(this.bids);
    }

    public Money addWithCurrentBid(Money amount) {
        return getNewestBid().map(bid -> bid.addWithAmount(amount)).orElse(amount);
    }

    public boolean isLeadBy(Bidder bidder) {
        return Optional.ofNullable(this.leadingBidder)
                .map(candidate -> candidate.equals(bidder))
                .orElse(false);
    }
}
