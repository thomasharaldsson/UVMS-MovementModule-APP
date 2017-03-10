package eu.europa.fisheries.uvms.component.service.arquillian;

import eu.europa.ec.fisheries.schema.movement.v1.*;
import eu.europa.ec.fisheries.uvms.movement.message.event.CreateMovementEvent;
import eu.europa.ec.fisheries.uvms.movement.message.event.carrier.EventMessage;
import eu.europa.ec.fisheries.uvms.movement.message.producer.bean.MessageProducerBean;
import eu.europa.ec.fisheries.uvms.movement.model.exception.ModelMarshallException;
import eu.europa.ec.fisheries.uvms.movement.model.mapper.MovementModuleRequestMapper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import javax.ejb.EJBException;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.TextMessage;

import java.util.Random;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by andreasw on 2017-03-07.
 */
@RunWith(Arquillian.class)
public class Event_createMovementIntTest extends TransactionalTests {

    Random rnd = new Random();

    @Inject
    @CreateMovementEvent
    Event<EventMessage> createMovementEvent;

    @Deployment
    public static Archive<?> createDeployment() {
        return BuildMovementServiceTestDeployment.createEventDeployment();
    }

    @Test
    public void triggerEvent() throws JMSException, ModelMarshallException {
        System.setProperty(MessageProducerBean.MESSAGE_PRODUCER_METHODS_FAIL, "false");

        Double longitude = rnd.nextDouble();
        Double latitude = rnd.nextDouble();


        MovementBaseType movementBaseType = MovementEventTestHelper.createMovementBaseType(longitude, latitude);
        String text = MovementModuleRequestMapper.mapToCreateMovementRequest(movementBaseType, "TEST");

        TextMessage textMessage = MovementEventTestHelper.createTextMessage(text);
        try {
            createMovementEvent.fire(new EventMessage(textMessage));
            em.flush();
        } catch (EJBException EX) {
            Assert.fail("Should not reach me!");
        }
    }

    @Test
    public void triggerEventWithBrokenJMS() throws JMSException, ModelMarshallException {
        System.setProperty(MessageProducerBean.MESSAGE_PRODUCER_METHODS_FAIL, "true");
        Double longitude = rnd.nextDouble();
        Double latitude = rnd.nextDouble();

        MovementBaseType movementBaseType = MovementEventTestHelper.createMovementBaseType();
        String text = MovementModuleRequestMapper.mapToCreateMovementRequest(movementBaseType, "TEST");

        TextMessage textMessage = MovementEventTestHelper.createTextMessage(text);
        try {
            createMovementEvent.fire(new EventMessage(textMessage));
            Assert.fail("Should not reach me!");
        } catch (EJBException ignore) {
        }
    }
}
