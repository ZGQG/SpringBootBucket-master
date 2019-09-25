package com.xncoding.pos.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * RabbitConfig
 *最核心的类就是RabbitMQ的配置类，在里面定制模版类、声明交换机、队列、绑定交换机到队列。
 * @author XiongNeng
 * @version 1.0
 * @since 2018/3/1
 */
@Configuration
public class RabbitConfig {
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 定制化amqp模版      可根据需要定制多个
     * <p>
     * <p>
     * 此处为模版类定义 Jackson消息转换器
     * ConfirmCallback接口用于实现消息发送到RabbitMQ交换器后接收ack回调   即消息发送到exchange  ack
     * ReturnCallback接口用于实现消息发送到RabbitMQ 交换器，但无相应队列与交换器绑定时的回调  即消息发送不到任何一个队列中  ack
     *
     * @return the amqp template
     */
    // @Primary
    @Bean
    public AmqpTemplate amqpTemplate() {
        Logger log = LoggerFactory.getLogger(RabbitTemplate.class);
        // 使用jackson 消息转换器
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        rabbitTemplate.setEncoding("UTF-8");
        // 用于确认消息是否成功路由到相关队列，消息发送失败返回到队列中，yml需要配置 publisher-returns: true
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            String correlationId = message.getMessageProperties().getCorrelationId();
            log.debug("消息：{} 发送失败, 应答码：{} 原因：{} 交换机: {}  路由键: {}", correlationId, replyCode, replyText, exchange, routingKey);
            System.out.println("消息："+correlationId+" 发送失败, 应答码："+replyCode+" 原因："+replyText+" 交换机: "+exchange+"  路由键: "+routingKey);
        });
        // 用于确认消息是否成功发送到exchange，yml需要配置 publisher-confirms: true
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("消息发送到exchange成功,id: {}", correlationData.getId());
                System.out.println("消息发送到exchange成功,id: "+ correlationData.getId());
            } else {
                log.debug("消息发送到exchange失败,原因: {}", cause);
                System.out.println("消息发送到exchange失败,原因: " + cause);
            }
        });
        return rabbitTemplate;
    }

    /* ----------------------------------------------------------------------------Direct exchange test--------------------------------------------------------------------------- */

    /**
     * 声明Direct交换机 支持持久化.
     *
     * @return the exchange
     */
    @Bean("directExchange")
    public Exchange directExchange() {
        return ExchangeBuilder.directExchange("DIRECT_EXCHANGE").durable(true).build();//durable字段表示持久化
    }//durable字段表示持久化

    /**
     * 声明一个队列 支持持久化.
     *
     * @return the queue
     */
    @Bean("directQueue")
    public Queue directQueue() {
        return QueueBuilder.durable("DIRECT_QUEUE").build();
    }//durable字段表示持久化

    /**
     * 通过绑定键 将指定队列绑定到一个指定的交换机.
     *
     * @param queue    the queue
     * @param exchange the exchange
     * @return the binding
     */
    @Bean
    public Binding directBinding(@Qualifier("directQueue") Queue queue,
                                 @Qualifier("directExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("DIRECT_ROUTING_KEY").noargs();
    }

    /* ----------------------------------------------------------------------------Fanout exchange test--------------------------------------------------------------------------- */

    /**
     * 声明 fanout 交换机.
     *
     * @return the exchange
     */
    @Bean("fanoutExchange")
    public FanoutExchange fanoutExchange() {
        return (FanoutExchange) ExchangeBuilder.fanoutExchange("FANOUT_EXCHANGE").durable(true).build();//durable字段表示持久化
    }

    /**
     * Fanout queue A.
     *
     * @return the queue
     */
    @Bean("fanoutQueueA")
    public Queue fanoutQueueA() {
        return QueueBuilder.durable("FANOUT_QUEUE_A").build();
    }

    /**
     * Fanout queue B .
     *
     * @return the queue
     */
    @Bean("fanoutQueueB")
    public Queue fanoutQueueB() {
        return QueueBuilder.durable("FANOUT_QUEUE_B").build();
    }

    /**
     * 绑定队列A 到Fanout 交换机.
     *
     * @param queue          the queue
     * @param fanoutExchange the fanout exchange
     * @return the binding
     */
    @Bean
    public Binding bindingA(@Qualifier("fanoutQueueA") Queue queue,
                            @Qualifier("fanoutExchange") FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(queue).to(fanoutExchange);
    }

    /**
     * 绑定队列B 到Fanout 交换机.
     *
     * @param queue          the queue
     * @param fanoutExchange the fanout exchange
     * @return the binding
     */
    @Bean
    public Binding bindingB(@Qualifier("fanoutQueueB") Queue queue,
                            @Qualifier("fanoutExchange") FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(queue).to(fanoutExchange);
    }


    /* ----------------------------------------------------------------------------top exchange test--------------------------------------------------------------------------- */

    /**
     * 声明top交换机 支持持久化.
     *
     * @return the exchange
     */
    @Bean("topExchange")
    public TopicExchange topExchange() {
        return (TopicExchange)ExchangeBuilder.topicExchange("TOP_EXCHANGE").durable(true).build();//durable字段表示持久化
    }

    /**
     * 声明一个队列 支持持久化.
     *
     * @return the queue
     */
    @Bean("topQueue")
    public Queue topQueue() {
        return QueueBuilder.durable("TOP_QUEUE").build();
    }



    /**
     * 通过绑定键 将指定队列绑定到一个指定的交换机.
     *
     * @param queue    the queue
     * @param exchange the exchange
     * @return the binding
     */
    @Bean
    public Binding topBinding(@Qualifier("topQueue") Queue queue,
                                 @Qualifier("topExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("TOP_ROUTING_KEY.#").noargs();//#是允许匹配多个单词，*是匹配一个单词
    }


}
