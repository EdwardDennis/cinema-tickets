package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.List;

import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.INFANT;

public class TicketServiceImpl implements TicketService {
    private static final int MAX_TICKETS = 25;
    private static final int CHILD_TICKET_PRICE = 15;
    private static final int ADULT_TICKET_PRICE = 25;

    private final TicketPaymentService paymentService;
    private final SeatReservationService reservationService;

    public TicketServiceImpl(TicketPaymentService paymentService, SeatReservationService reservationService) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        validateAccountId(accountId);
        validateTicketRequests(List.of(ticketTypeRequests));
        int totalPrice = calculateTotalPrice(List.of(ticketTypeRequests));
        int totalSeats = calculateTotalSeats(List.of(ticketTypeRequests));

        paymentService.makePayment(accountId, totalPrice);
        reservationService.reserveSeat(accountId, totalSeats);
    }

    private void validateAccountId(Long accountId) {
        if (accountId <= 0) {
            throw new InvalidPurchaseException("Account ID must be greater than zero.");
        }
    }

    private void validateTicketRequests(List<TicketTypeRequest> ticketTypeRequests) {
        ticketTypeRequests.forEach(request -> {
            if (request.getNoOfTickets() < 1) {
                throw new InvalidPurchaseException("Ticket requests must have ≥1 tickets");
            }
        });

        int totalTickets = ticketTypeRequests.stream()
                .mapToInt(TicketTypeRequest::getNoOfTickets)
                .sum();

        if (totalTickets > MAX_TICKETS) {
            throw new InvalidPurchaseException("Maximum 25 tickets per purchase");
        }

        int adultTickets = getTicketsByType(ticketTypeRequests, ADULT);
        if (adultTickets < 1) {
            throw new InvalidPurchaseException("Adult ticket required");
        }

        int infantTickets = getTicketsByType(ticketTypeRequests, INFANT);
        if (infantTickets > adultTickets) {
            throw new InvalidPurchaseException("Infant tickets exceed adults");
        }
    }

    private int getTicketsByType(List<TicketTypeRequest> requests, TicketTypeRequest.Type type) {
        return requests.stream()
                .filter(req -> req.getTicketType() == type)
                .mapToInt(TicketTypeRequest::getNoOfTickets)
                .sum();
    }

    private int calculateTotalPrice(List<TicketTypeRequest> ticketTypeRequests) {
        return ticketTypeRequests.stream()
                .mapToInt(request -> switch (request.getTicketType()) {
                    case ADULT -> request.getNoOfTickets() * ADULT_TICKET_PRICE;
                    case CHILD -> request.getNoOfTickets() * CHILD_TICKET_PRICE;
                    case INFANT -> 0;
                })
                .sum();
    }

    private int calculateTotalSeats(List<TicketTypeRequest> ticketTypeRequests) {
        return ticketTypeRequests.stream()
                .filter(request -> request.getTicketType() != INFANT)
                .mapToInt(TicketTypeRequest::getNoOfTickets)
                .sum();
    }
}
