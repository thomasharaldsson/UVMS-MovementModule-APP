package eu.europa.ec.fisheries.uvms.movement.client;

import eu.europa.ec.fisheries.uvms.commons.date.JsonBConfigurator;
import eu.europa.ec.fisheries.uvms.movement.client.model.MicroMovement;
import eu.europa.ec.fisheries.schema.movement.v1.MovementType;
import eu.europa.ec.fisheries.uvms.movement.client.model.MicroMovementExtended;
import eu.europa.ec.fisheries.uvms.movement.model.dto.MicroMovementsForConnectIdsBetweenDatesRequest;
import eu.europa.ec.fisheries.uvms.movement.model.dto.MovementsForConnectIdsBetweenDatesRequest;
import eu.europa.ec.fisheries.uvms.rest.security.InternalRestTokenHandler;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Stateless
public class MovementRestClient {

    private WebTarget webTarget;

    @Resource(name = "java:global/movement_endpoint")
    private String movementEndpoint;

    @Inject
    private InternalRestTokenHandler internalRestTokenHandler;

    @PostConstruct
    public void initClient() {
        String url = movementEndpoint + "/";

        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        clientBuilder.connectTimeout(10, TimeUnit.MINUTES);
        clientBuilder.readTimeout(10, TimeUnit.MINUTES);
        Client client = clientBuilder.build();

        client.register(JsonBConfigurator.class);
        webTarget = client.target(url);
    }

    public List<MicroMovementExtended> getMicroMovementsForConnectIdsBetweenDates(List<String> connectIds, Instant fromDate, Instant toDate) {
        MicroMovementsForConnectIdsBetweenDatesRequest request = new MicroMovementsForConnectIdsBetweenDatesRequest(connectIds, fromDate, toDate);

        Response response = webTarget
                .path("internal/microMovementsForConnectIdsBetweenDates")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, internalRestTokenHandler.createAndFetchToken("user"))
                .post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE));

        return response.readEntity(new GenericType<List<MicroMovementExtended>>() {});
    }

    public String getMicroMovementsForConnectIdsBetweenDates(List<String> connectIds, Instant fromDate, Instant toDate, List<String> sources) {
        MicroMovementsForConnectIdsBetweenDatesRequest request = new MicroMovementsForConnectIdsBetweenDatesRequest(connectIds, fromDate, toDate);
        request.setSources(sources);

        Response response = webTarget
                .path("internal/microMovementsForConnectIdsBetweenDates")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, internalRestTokenHandler.createAndFetchToken("user"))
                .post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE));

        return response.readEntity(String.class);
    }

    public MicroMovement getMicroMovementById(UUID id) {
            Response response = webTarget
                    .path("internal/getMicroMovement")
                    .path(id.toString())
                    .request(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, internalRestTokenHandler.createAndFetchToken("user"))
                    .get();

            return response.readEntity(MicroMovement.class);
    }
//    public List<MovementType> getMovementList(List<String> connectIds, Instant fromDate, Instant toDate){  
//    	  // MovementsForConnectIdsBetweenDatesRequest request = new MicroMovementsForConnectIdsBetweenDatesRequest(connectIds, fromDate, toDate);
//    	   MovementsForConnectIdsBetweenDatesRequest request = new MovementsForConnectIdsBetweenDatesRequest(connectIds, fromDate, toDate);
//           
//    	   Response response = webTarget
//                   .path("internal/movementsForConnectIdsBetweenDates")
//                   .request(MediaType.APPLICATION_JSON)
//                   .header(HttpHeaders.AUTHORIZATION, internalRestTokenHandler.createAndFetchToken("user"))
//                   .post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE));
//
//           return response.readEntity(new GenericType<List<MovementType>>() {});
//    }
    public String getMovementsForConnectIdsBetweenDates(List<String> connectIds, Instant fromDate, Instant toDate) { //, List<String> sources) {
    	 MovementsForConnectIdsBetweenDatesRequest request = new MovementsForConnectIdsBetweenDatesRequest(connectIds, fromDate, toDate);
        // request.setSources(sources);

        Response response = webTarget
                .path("internal/movementsForConnectIdsBetweenDates")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, internalRestTokenHandler.createAndFetchToken("user"))
                .post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE));

        return response.readEntity(String.class);
    }
//    public GetMovementListByQueryResponse getMovementList(Instant fromDate, Instant toDate){  
//    	//GetMovementListByQueryResponse movementListByQuery = new GetMovementListByQueryResponse();//  movementService.getList(query);
//    
//    	MovementQuery mQuery = new MovementQuery();
//    	RangeCriteria rc = new RangeCriteria();
//    	rc.setKey(RangeKeyType.DATE);
//    	rc.setFrom(fromDate.toString());
//    	rc.setTo(toDate.toString());
//    	
//    	ListCriteria lc = new ListCriteria();
//    	lc.setKey(SearchKey.DATE);
//    	lc.setValue(fromDate.toString());
//    	
//    	BigInteger bigInt = new BigInteger("10000000");
//    	ListPagination lp = new ListPagination();
//    	lp.setListSize(bigInt);
//    	lp.setPage(bigInt);
//    	
//    	mQuery.getMovementRangeSearchCriteria().add(rc);
//    	mQuery.getMovementSearchCriteria().add(lc);
//    	mQuery.setPagination(lp);
//    	
////    	private final static long serialVersionUID = 1L;
////        @XmlElement(required = true)
////        protected ListPagination pagination;
////        protected boolean excludeFirstAndLastSegment;
////        @XmlElement(required = true)
////        protected List<ListCriteria> movementSearchCriteria;
////        @XmlElement(required = true)
////        protected List<RangeCriteria> movementRangeSearchCriteria;
//        
//    	Response response = webTarget
//                .path("internal/list")
//                .request(MediaType.APPLICATION_JSON)
//                .header(HttpHeaders.AUTHORIZATION, internalRestTokenHandler.createAndFetchToken("user"))
//                .post(Entity.entity(mQuery, MediaType.APPLICATION_JSON_TYPE));
//
//        return response.readEntity(GetMovementListByQueryResponse.class);
//    }

}
