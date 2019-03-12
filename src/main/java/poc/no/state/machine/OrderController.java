package poc.no.state.machine;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;

@RestController
@RequestMapping("/orders")
@AllArgsConstructor
public class OrderController {

    private OrderRepository orderRepository;

    @PostMapping
    public ResponseEntity<Order> create() {
        final String uuid = UUID.randomUUID().toString();
        final Order order = new Order();
        order.setStateMachineId(uuid);
        order.setStatus(OrderStates.CREATED);
        return ResponseEntity.ok(orderRepository.save(order));
    }

    @PutMapping("/{orderId}/events/{event}")
    public ResponseEntity<?> applyEvent(
            @PathVariable("orderId") String orderId,
            @PathVariable("event") OrderEvents event) {
        final Order order = orderRepository.findById(orderId).orElseThrow(RuntimeException::new);

        if (eventAllowed(order, event)) {
            order.setStatus(getStatusByEvent(event));
            return ResponseEntity.ok(orderRepository.save(order));
        } else {
            return ResponseEntity.badRequest().body("It was not possible to apply the event " + event + " on order status " + order.getStatus());
        }

    }

    private OrderStates getStatusByEvent(final OrderEvents event) {
        OrderStates state = null;
        switch (event) {
            case CONFIRMED_PAYMENT:
                state = OrderStates.APPROVED;
                break;
            case INVOICE_ISSUED:
                state = OrderStates.INVOICED;
                break;
            case CANCEL:
                state = OrderStates.CANCELLED;
                break;
            case SHIP:
                state = OrderStates.SHIPPED;
                break;
            case DELIVER:
                state = OrderStates.DELIVERED;
                break;
        }

        return state;
    }

    private boolean eventAllowed(final Order order, final OrderEvents event) {
        final List<OrderEvents> eventsAllowed = new ArrayList<>();

        switch (order.getStatus()) {
            case CREATED:
                eventsAllowed.add(OrderEvents.CONFIRMED_PAYMENT);
                break;
            case APPROVED:
                eventsAllowed.addAll(asList(OrderEvents.CANCEL, OrderEvents.INVOICE_ISSUED));
                break;
            case INVOICED:
                eventsAllowed.add(OrderEvents.SHIP);
                break;
            case SHIPPED:
                eventsAllowed.add(OrderEvents.DELIVER);
                break;
            case CANCELLED: case DELIVERED:
                break;
        }

        return eventsAllowed.contains(event);
    }

}
