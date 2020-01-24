package eu.europa.ec.fisheries.uvms.movement.service.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ejb.EJB;
import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import eu.europa.ec.fisheries.schema.movement.v1.MovementActivityType;
import eu.europa.ec.fisheries.schema.movement.v1.MovementActivityTypeType;
import eu.europa.ec.fisheries.schema.movement.v1.MovementBaseType;
import eu.europa.ec.fisheries.schema.movement.v1.MovementSegment;
import eu.europa.ec.fisheries.schema.movement.v1.MovementSourceType;
import eu.europa.ec.fisheries.schema.movement.v1.MovementType;
import eu.europa.ec.fisheries.schema.movement.v1.MovementTypeType;
import eu.europa.ec.fisheries.uvms.movement.model.util.DateUtil;
import eu.europa.ec.fisheries.uvms.movement.service.MovementHelpers;
import eu.europa.ec.fisheries.uvms.movement.service.TransactionalTests;
import eu.europa.ec.fisheries.uvms.movement.service.bean.IncomingMovementBean;
import eu.europa.ec.fisheries.uvms.movement.service.bean.MovementService;
import eu.europa.ec.fisheries.uvms.movement.service.dao.MovementDao;
import eu.europa.ec.fisheries.uvms.movement.service.entity.Activity;
import eu.europa.ec.fisheries.uvms.movement.service.entity.Movement;
import eu.europa.ec.fisheries.uvms.movement.service.entity.MovementConnect;
import eu.europa.ec.fisheries.uvms.movement.service.entity.Track;

@RunWith(Arquillian.class)
public class MovementEntityToModelTest extends TransactionalTests {

	@EJB
    private MovementService movementService;

    @EJB
    private MovementDao movementDao;

    @EJB
    private IncomingMovementBean incomingMovementBean;
	
	@Test
    @OperateOnDeployment("movementservice")
	public void testMovementBaseType() {
		MovementHelpers movementHelpers = new MovementHelpers(movementService);
		UUID connectId = UUID.randomUUID();
		Instant dateStartMovement = DateUtil.nowUTC();
		Instant lesTime = dateStartMovement;
		double lon = 11.641982;
		double lat = 57.632304;
		Movement movement =  movementHelpers.createMovement(lon, lat, connectId, "ONE", dateStartMovement);
		
		MovementBaseType output = MovementEntityToModelMapper.mapToMovementBaseType(movement);
		
		assertEquals(0.0, output.getReportedSpeed(), 0D);
		assertEquals(0.0, output.getReportedCourse(), 0D);
		assertEquals(movement.getId().toString(), output.getGuid());
		assertEquals(lat, output.getPosition().getLatitude(), 0D);
		assertEquals(lon, output.getPosition().getLongitude(), 0D);
		assertEquals(connectId.toString(), output.getConnectId());
		assertEquals(lesTime.truncatedTo(ChronoUnit.MILLIS),
				output.getLesReportTime().toInstant().truncatedTo(ChronoUnit.MILLIS));
		try {
			output = MovementEntityToModelMapper.mapToMovementBaseType(null);
			fail("null input should result in a nullpointer");
		} catch (NullPointerException e) {
			assertTrue(true);
		}
	}
	
	@Test
    @OperateOnDeployment("movementservice")
	public void testMapToMovementTypeWithMinimalMovementInput() {
		UUID connectId = UUID.randomUUID();
		double lon = 11.641982;
		double lat = 57.632304;
		Movement movement = new Movement();
		GeometryFactory gf = new GeometryFactory();
		movement.setLocation(gf.createPoint(new Coordinate(lon, lat)));
		movement.setMovementSource(MovementSourceType.IRIDIUM);
		movement.setMovementType(MovementTypeType.POS);
		MovementConnect movementConnect = new MovementConnect();
		movementConnect.setId(connectId);
        movement.setMovementConnect(movementConnect);
        movement.setTimestamp(Instant.now());
        movement.setId(UUID.randomUUID());
		//movement.setStatus(status);
		MovementType movementType = MovementEntityToModelMapper.mapToMinimalMovementType(movement);
		assertEquals(movement.getId().toString(), movementType.getGuid());
        assertEquals(lat, movementType.getPosition().getLatitude(), 0D);
        assertEquals(lon, movementType.getPosition().getLongitude(), 0D);
        assertEquals(connectId.toString(), movementType.getConnectId());
	}
	
	@Test
    @OperateOnDeployment("movementservice")
	public void testMapToMovementTypeWithMovementInput() {
		MovementHelpers movementHelpers = new MovementHelpers(movementService);
		UUID connectId = UUID.randomUUID();
		Instant dateStartMovement = DateUtil.nowUTC();
		double lon = 11.641982;
		double lat = 57.632304;
		Movement movement =  movementHelpers.createMovement(lon, lat, connectId, "ONE", dateStartMovement);
		
		MovementType output = MovementEntityToModelMapper.mapToMovementType(movement);
		
		assertEquals(0.0, output.getReportedSpeed(), 0D);
		assertEquals(0.0, output.getReportedCourse(), 0D);
		assertEquals(movement.getId().toString(), output.getGuid());
		assertEquals(lat, output.getPosition().getLatitude(), 0D);
		assertEquals(lon, output.getPosition().getLongitude(), 0D);
		assertEquals(connectId.toString(), output.getConnectId());
		assertEquals("POINT ( 11.641982 57.632304 )" , output.getWkt());
		assertTrue(!output.isDuplicate());
		
		movement = null;
		output = MovementEntityToModelMapper.mapToMovementType(movement);
		assertNull(output);
	}
	
	@Test
    @OperateOnDeployment("movementservice")
	public void testMapToActivityType() {
		Activity input = new Activity();
		
		MovementActivityType output = MovementEntityToModelMapper.mapToActivityType(input);
		output.setMessageType(MovementActivityTypeType.COB);
		output.setMessageId("42....");
		assertEquals("MovementActivityType[messageType=COB,messageId=42....,callback=<null>]" ,output.toString());
		
		output = MovementEntityToModelMapper.mapToActivityType(null); //null is basically an empty return 
		assertEquals("MovementActivityType[messageType=<null>,messageId=<null>,callback=<null>]" ,output.toString());
	}

	
	@Test
    @OperateOnDeployment("movementservice")
	public void testMapToMovementTypeWithAListOfMovements() {
		//Most of the method is tested by testMapToMovementType
		MovementHelpers movementHelpers = new MovementHelpers(movementService);
		UUID connectId = UUID.randomUUID();
		Instant dateStartMovement = DateUtil.nowUTC();
		
		List<Movement> input = movementHelpers.createFishingTourVarberg(1, connectId);
		em.flush();
		List<MovementType> output = MovementEntityToModelMapper.mapToMovementType(input);
		
		assertEquals(input.size(),output.size());
		
		input = null;
		try {
			output = MovementEntityToModelMapper.mapToMovementType(input);
			fail("Null as input");
		} catch (Exception e) {
			assertTrue(true);
		}
	}
	
	@Test
    @OperateOnDeployment("movementservice")
	public void testMapToMovementSegment() {
		MovementHelpers movementHelpers = new MovementHelpers(movementService);
		UUID connectId = UUID.randomUUID();
		Instant dateStartMovement = DateUtil.nowUTC();
		List<Movement> movementList = movementHelpers.createFishingTourVarberg(1, connectId);
		em.flush();
		
		List<MovementSegment> output = MovementEntityToModelMapper.mapToMovementSegment(movementList);
		assertThat(output.size(), CoreMatchers.is(movementList.size() - 1));
		
		try {
			MovementEntityToModelMapper.mapToMovementSegment(null);
			fail("Null as input");
		} catch (Exception e) {
			assertTrue(true);
		}
	}
	
	@Test
    @OperateOnDeployment("movementservice")
	public void testOrderMovementsByConnectId() {
		MovementHelpers movementHelpers = new MovementHelpers(movementService);
		List<UUID> connectId = new ArrayList<>();
		List<Movement> input = new ArrayList<>();
		UUID ID;
		for(int i = 0 ; i < 20 ; i++) {
			ID = UUID.randomUUID();
			connectId.add(ID);
			input.add(movementHelpers.createMovement(Math.random()* 90, Math.random()* 90, ID, "ONE", Instant.now().plusMillis((long)(Math.random() * 5000))));
		}
		
		Map<UUID, List<Movement>> output = MovementEntityToModelMapper.orderMovementsByConnectId(input);
		
		assertEquals(connectId.size(),output.keySet().size());
		for(UUID s : connectId) {
			assertTrue(output.containsKey(s));
		}
		
		try {
			output = MovementEntityToModelMapper.orderMovementsByConnectId(null);
			fail("Null as input");
		} catch (NullPointerException e) {
			assertTrue(true);
		}
	}

	@Test
    @OperateOnDeployment("movementservice")
	public void testExtractTracks() {
		MovementHelpers movementHelpers = new MovementHelpers(movementService);
		UUID connectId = UUID.randomUUID();
		ArrayList<Movement> movementList = new ArrayList<>(movementHelpers.createFishingTourVarberg(1, connectId));

		List<Track> output = MovementEntityToModelMapper.extractTracks(movementList);
		
		assertEquals(1, output.size());
		assertEquals(movementList.get(0).getTrack().getDuration(), output.get(0).getDuration(),0D);
		assertEquals(movementList.get(0).getTrack().getId(), output.get(0).getId());
		
		try {
			output = MovementEntityToModelMapper.extractTracks(null);
			fail("Null as invalue");
		} catch (NullPointerException e) {
			assertTrue(true);
		}
	}
}
