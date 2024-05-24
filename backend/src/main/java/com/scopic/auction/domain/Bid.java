package com.scopic.auction.domain;

import com.scopic.auction.dto.BidDto;
import com.scopic.auction.repository.jpa.MoneyConverter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "t_bid")
public class Bid {

    @EmbeddedId
    private BidId id;

    @Column(name = "c_time", updatable = false)
    private LocalDateTime time;
    @Basic
    @Column(name = "c_amount", updatable = false)
    @Convert(converter = MoneyConverter.class)
    private Money amount;

    public Bid() {
    }

    public Bid(Item item, Bidder bidder, LocalDateTime time, Money amount) {
        this.id = new BidId(item, bidder);
        this.time = time;
        this.amount = amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bid)) return false;
        Bid bid = (Bid) o;
        return id.equals(bid.id) && time.equals(bid.time) && amount.equals(bid.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, time, amount);
    }

    public static Money sum(Collection<Bid> bids) {
        return bids.stream()
                .map(bid -> bid.amount)
                .reduce(
                        Money.ZERO_USD,
                        (a, b) -> a.add(b)
                );
    }

    public static Optional<Bid> newestOf(Collection<Bid> bids) {
        return bids.stream()
                .sorted((a, b) -> b.time.compareTo(a.time))
                .limit(1)
                .findFirst();
    }

    public BidDto toDto() {
        final BidDto result = new BidDto();
        result.user = this.id.bidder.toDto();
        result.time = this.time;
        result.amount = this.amount.toDto();
        return result;
    }

    public boolean isBiggerThan(Money amount) {
        return this.amount.isBiggerThan(amount);
    }

    public boolean isAbout(Item item) {
        return this.id.item.equals(item);
    }

    public Bid incrementFor(Bidder bidder) {
        Money nextAmount = this.amount.nextAmount();
        return new Bid(id.item, bidder, LocalDateTime.now(), nextAmount);
    }

    public Money addWithAmount(Money amount) {
        return this.amount.add(amount);
    }

    public boolean isLead() {
        return id.item.isLeadBy(id.bidder);
    }
}
