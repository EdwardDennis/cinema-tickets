import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class TicketServiceImplTest {
    private TicketPaymentService paymentService;
    private SeatReservationService reservationService;
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        paymentService = mock(TicketPaymentService.class);
        reservationService = mock(SeatReservationService.class);
        ticketService = new TicketServiceImpl(paymentService, reservationService);
    }

    @Test
    void testPurchaseTickets() {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1)
        );

        verify(paymentService).makePayment(1L, 95);
        verify(reservationService).reserveSeat(1L, 5);
    }

    @Test
    void testPurchaseTicketsWithoutAdult() {
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2)
        ));
    }

    @Test
    void testPurchaseMoreThanMaxTickets() {
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 26)
        ));
    }

    @Test
    void testInfantsDoNotPayOrReserveSeat() {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2)
        );

        verify(paymentService).makePayment(1L, 50);
        verify(reservationService).reserveSeat(1L, 2);
    }

    @Test
    void testInvalidAccountIdThrowsException() {
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(0L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1)
        ));
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(-1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1)
        ));
    }

    @Test
    void testLessThanOneTicketThrowsException() {
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 0),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 0)
        ));
    }

    @Test
    void testInfantTicketsExceedAdults() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1),
                        new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2)
                ));
    }

    @Test
    void testValidInfantAdultRatio() {
        assertAll(
                () -> ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                        new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2)),
                () -> verify(paymentService).makePayment(1L, 50),
                () -> verify(reservationService).reserveSeat(1L, 2)
        );
    }

    @Test
    void testMixedEdgeCaseWithAsserts() {
        assertAll(
                () -> ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 20),
                        new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 5)),
                () -> verify(paymentService).makePayment(1L, 575),
                () -> verify(reservationService).reserveSeat(1L, 25)
        );
    }

    @Test
    void testZeroAdultTicketsThrowsException() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0)
                ));
    }

    @Test
    void testNegativeChildTicketsThrowsException() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.CHILD, -1)
                ));
    }

    @Test
    void testZeroInfantTicketsThrowsException() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 0)
                ));
    }

    @Test
    void testZeroTotalTicketsThrowsException() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0),
                        new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 0)
                ));
    }
}
