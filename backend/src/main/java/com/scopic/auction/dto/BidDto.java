package com.scopic.auction.dto;

import java.time.LocalDateTime;

public class BidDto {
    public UserDto user;
    public LocalDateTime time;
    public MoneyDto amount;

    public BidDto() {
    }
}
