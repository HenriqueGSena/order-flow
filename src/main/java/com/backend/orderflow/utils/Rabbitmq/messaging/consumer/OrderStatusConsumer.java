package com.backend.orderflow.utils.Rabbitmq.messaging.consumer;

import com.backend.orderflow.service.OrdersService;
import com.backend.orderflow.utils.Rabbitmq.RabbitMQConfig;
import com.backend.orderflow.utils.Rabbitmq.messaging.message.OrderStatusMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusConsumer {

    private final OrdersService ordersService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_STATUS_QUEUE)
    public void consumeOrderStatus(OrderStatusMessage message) {
        log.info("Mensagem de atualização de status de pedido recebida: eventId={}, orderId={}, newStatus={}",
                message.eventId(), message.orderId(), message.newStatus());

        ordersService.processOrderStatus(message);

        log.info("Processamento da mensagem de status de pedido concluído com sucesso: eventId={}, orderId={}",
                message.eventId(), message.orderId());
    }
}
