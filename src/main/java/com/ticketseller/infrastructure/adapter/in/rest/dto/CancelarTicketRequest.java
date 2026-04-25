package com.ticketseller.infrastructure.adapter.in.rest.dto;

import java.util.List;
import java.util.UUID;

public record CancelarTicketRequest(
    List<UUID> ticketIds
) {}
