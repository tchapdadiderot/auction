package com.scopic.auction.domain;


import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
class BidId implements Serializable {

    @ManyToOne
    @JoinColumn(name = "c_item", referencedColumnName = "c_id")
    public Item item;
    @ManyToOne
    @JoinColumn(name = "c_bidder", referencedColumnName = "c_username")
    public Bidder bidder;

    public BidId() {
    }

    public BidId(Item item, Bidder bidder) {
        this.item = item;
        this.bidder = bidder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BidId)) return false;
        BidId bidId = (BidId) o;
        return item.equals(bidId.item) && bidder.equals(bidId.bidder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, bidder);
    }
}
