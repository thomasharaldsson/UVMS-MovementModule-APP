/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.europa.ec.fisheries.uvms.movement.service.entity;

import eu.europa.ec.fisheries.schema.movement.v1.MovementSourceType;
import eu.europa.ec.fisheries.schema.movement.v1.MovementTypeType;
import eu.europa.ec.fisheries.uvms.movement.model.constants.SatId;
import eu.europa.ec.fisheries.uvms.movement.service.mapper.SatelliteConverter;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.locationtech.jts.geom.Point;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movement", indexes = {
        @Index(columnList = "movementconnect_id", name = "movement_moveconn_fk_idx", unique = false),
        @Index(columnList = "track_id", name = "movement_trac_fk_idx", unique = false),
        @Index(columnList = "movementconnect_id, timestamp", name = "movement_count_idx", unique = false)
})
@NamedQueries({
    @NamedQuery(name = Movement.FIND_ALL_BY_TRACK, query = "SELECT m FROM Movement m WHERE m.track = :track ORDER BY m.timestamp DESC"),
    @NamedQuery(name = Movement.FIND_ALL_LOCATIONS_BY_TRACK, query = "SELECT m.location FROM Movement m WHERE m.track = :track ORDER BY m.timestamp DESC"),
    @NamedQuery(name = Movement.FIND_ALL_BY_MOVEMENTCONNECT, query = "SELECT m FROM Movement m WHERE m.movementConnect = :movementConnect ORDER BY m.timestamp ASC"),
    @NamedQuery(name = Movement.FIND_LATEST_BY_MOVEMENT_CONNECT, query = "SELECT m FROM Movement m WHERE m.movementConnect.id = :connectId ORDER BY m.timestamp DESC"),
    @NamedQuery(name = Movement.FIND_PREVIOUS, query = "SELECT m FROM Movement m  WHERE m.movementConnect.id = :id AND m.timestamp = (select max(mm.timestamp) from Movement mm where mm.movementConnect.id = :id and mm.source in :sources and mm.timestamp < :date) "),
    @NamedQuery(name = Movement.FIND_NEXT, query = "SELECT m FROM Movement m  WHERE m.movementConnect.id = :id AND m.timestamp = (select min(mm.timestamp) from Movement mm where mm.movementConnect.id = :id and mm.source in :sources and mm.timestamp > :date) "),
    @NamedQuery(name = Movement.FIND_FIRST, query = "SELECT m FROM Movement m  WHERE m.movementConnect.id = :id AND m.timestamp = (select min(mm.timestamp) from Movement mm  where mm.movementConnect.id = :id  AND mm.id <> :excludedMovement) "),
    @NamedQuery(name = Movement.FIND_EXISTING_DATE, query = "SELECT m FROM Movement m WHERE m.movementConnect.id = :id AND m.timestamp = :date "),
    @NamedQuery(name = Movement.NR_OF_MOVEMENTS_FOR_ASSET_IN_TIMESPAN, query = "SELECT COUNT (m) FROM Movement m WHERE m.movementConnect.id = :asset AND m.timestamp BETWEEN :fromDate AND :toDate "),

    @NamedQuery(name = Movement.FIND_ALL_FOR_ASSET_BETWEEN_DATES, query = "SELECT m FROM Movement m WHERE m.movementConnect.id = :id AND m.timestamp > :startDate AND m.timestamp < :endDate AND m.source in :sources ORDER BY m.timestamp DESC"),
    @NamedQuery(name = Movement.FIND_ALL_FOR_CONNECT_IDS_BETWEEN_DATES, query = "SELECT m FROM Movement m WHERE m.movementConnect.id in :connectIds AND m.timestamp >= :fromDate AND m.timestamp <= :toDate AND m.source in :sources ORDER BY m.timestamp DESC"),
    @NamedQuery(name = Movement.FIND_LATEST_SINCE, query = "SELECT new eu.europa.ec.fisheries.uvms.movement.service.dto.MovementProjection(m.id, m.location, m.speed, m.calculatedSpeed, m.heading, m.movementConnect.id, m.status, m.source, m.movementType, m.timestamp, m.lesReportTime, m.sourceSatelliteId, m.updated, m.updatedBy, m.aisPositionAccuracy) FROM Movement m JOIN MovementConnect mc ON m.id = mc.latestMovement.id WHERE mc.updated > :date AND m.source in :sources" ),


    @NamedQuery(name = Movement.FIND_LATEST_X_NUMBER_FOR_ASSET, query = "SELECT m FROM Movement m WHERE m.movementConnect.id = :id AND m.source in :sources ORDER BY m.timestamp DESC"),
    @NamedQuery(name = Movement.FIND_LATESTMOVEMENT_BY_MOVEMENT_CONNECT, query = "SELECT m FROM Movement m JOIN MovementConnect mc ON m.id = mc.latestMovement.id WHERE m.movementConnect.id = :connectId"),
    @NamedQuery(name = Movement.FIND_LATESTMOVEMENT_BY_MOVEMENT_CONNECT_LIST, query = "SELECT m FROM Movement m JOIN MovementConnect mc ON m.id = mc.latestMovement.id WHERE m.movementConnect.id in :connectId"), 
    @NamedQuery(name = Movement.FIND_LATEST, query = "SELECT mc.latestMovement FROM MovementConnect mc ORDER BY mc.updated DESC"),
    @NamedQuery(name = Movement.FIND_BY_PREVIOUS_MOVEMENT, query = "SELECT m FROM Movement m WHERE m.previousMovement = :previousMovement"),
    @NamedQuery(name = Movement.FIND_MOVEMENT_BY_ID_LIST, query = "SELECT m FROM Movement m WHERE m.id in :moveIds"),
})
@NamedNativeQueries({
        @NamedNativeQuery(name = Movement.UPDATE_TO_NEW_MOVEMENTCONNECT, query = "WITH subRequest as (" +
                                                                                    "SELECT id FROM movement.movement WHERE movementconnect_id = :oldMC LIMIT :limit FOR UPDATE) " +
                                                                                        "UPDATE movement.movement as m " +
                                                                                        "SET movementconnect_id = :newMC " +
                                                                                        "FROM subRequest " +
                                                                                        "WHERE m.id = subRequest.id"),
})
@DynamicUpdate
@DynamicInsert
public class Movement implements Serializable, Comparable<Movement> {

    public static final String FIND_ALL_BY_TRACK = "Movement.findAllByTrack";
    public static final String FIND_ALL_LOCATIONS_BY_TRACK = "Movement.findAllPointsByTrack";
    public static final String FIND_ALL_BY_MOVEMENTCONNECT = "Movement.findAllByMovementConnect";
    public static final String FIND_LATEST_BY_MOVEMENT_CONNECT = "Movement.findLatestByMovementConnect";
    public static final String FIND_PREVIOUS = "Movement.findPrevious";
    public static final String FIND_NEXT = "Movement.findNext";
    public static final String FIND_FIRST = "Movement.findFirst";
    public static final String FIND_EXISTING_DATE = "Movement.findExistingDate";
    public static final String NR_OF_MOVEMENTS_FOR_ASSET_IN_TIMESPAN = "Movement.nrOfMovementsForAssetInTimespan";
    public static final String FIND_LATEST_SINCE = "Movement.findLatestSince";
    public static final String FIND_LATESTMOVEMENT_BY_MOVEMENT_CONNECT = "Movement.findLatestMovementByMovementConnect";
    public static final String FIND_LATESTMOVEMENT_BY_MOVEMENT_CONNECT_LIST = "Movement.findLatestMovementByMovementConnectList";
    public static final String FIND_LATEST = "Movement.findLatest";
    public static final String FIND_BY_PREVIOUS_MOVEMENT = "Movement.findByPreviousMovement";
    public static final String FIND_LATEST_X_NUMBER_FOR_ASSET = "Movement.findLatestXNumberForAsset";
    public static final String FIND_MOVEMENT_BY_ID_LIST = "Movement.findMovementByMovementIdList";
    public static final String FIND_ALL_FOR_ASSET_BETWEEN_DATES = "Movement.findAllForAssetBetweenDates";
    public static final String FIND_ALL_FOR_CONNECT_IDS_BETWEEN_DATES = "Movement.findAllForConnectIdsBetweenDates";

    public static final String UPDATE_TO_NEW_MOVEMENTCONNECT = "Movement.updateToNewMovementConnect";
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", name = "id")
    private UUID id;

    @NotNull
    @Column(name = "location", columnDefinition = "Geometry")
    private Point location;

    @Column(name = "speed")
    private Float speed;

    @Column(name = "heading")
    private Float heading;
    
    @Size(max = 60)
    @Column(name = "status")
    private String status;

    @JsonbTransient
    @NotNull
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "movementconnect_id", referencedColumnName = "moveconn_id")
    @ManyToOne(cascade = CascadeType.MERGE)
    private MovementConnect movementConnect;

    @JsonbTransient
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "track_id", referencedColumnName = "trac_id")
    @ManyToOne(optional = true, cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private Track track;

    @JsonbTransient
    @JoinColumn(name = "prev_movement_id", referencedColumnName = "id")
    @OneToOne(fetch = FetchType.LAZY)
    private Movement previousMovement;

    @Column(name = "source_id")
    @Enumerated(EnumType.ORDINAL)
    private MovementSourceType source;

    @Column(name = "movetype_id")
    @Enumerated(EnumType.ORDINAL)
    private MovementTypeType movementType;

    @Column(name = "timestamp")
    private Instant timestamp;
    
    @Column(name = "lesreporttime")
    private Instant lesReportTime;

    @Column(name = "satellite_id")
    @Convert(converter = SatelliteConverter.class)
    private SatId sourceSatelliteId;

    @NotNull
    @Column(name = "update_time")
    private Instant updated;

    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "update_user")
    private String updatedBy;

    @Column(name = "ais_position_accuracy")     //Value can be 0 (>10m) and 1 (<10m). See https://gpsd.gitlab.io/gpsd/AIVDM.html for more info
    private Short aisPositionAccuracy;

    @Column(name = "calculatedspeed")
    private Double calculatedSpeed;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public MovementConnect getMovementConnect() {
        return movementConnect;
    }

    public void setMovementConnect(MovementConnect movementConnect) {
        this.movementConnect = movementConnect;
    }

    public Float getSpeed() {
        return speed;
    }

    public void setSpeed(Float speed) {
        this.speed = speed;
    }

    public Float getHeading() {
        return heading;
    }

    public void setHeading(Float heading) {
        this.heading = heading;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public MovementTypeType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementTypeType movementType) {
        this.movementType = movementType;
    }

    public MovementSourceType getSource() {
        return source;
    }

    public void setSource(MovementSourceType source) {
        this.source = source;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Movement getPreviousMovement() {
        return previousMovement;
    }

    public void setPreviousMovement(Movement previousMovement) {
        this.previousMovement = previousMovement;
    }

    public Instant getUpdated() {
        return updated;
    }

    public void setUpdated(Instant updated) {
        this.updated = updated;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }

    public Instant getLesReportTime() {
		return lesReportTime;
	}

	public void setLesReportTime(Instant lesReportTime) {
		this.lesReportTime = lesReportTime;
	}

    public SatId getSourceSatelliteId() {
        return sourceSatelliteId;
    }

    public void setSourceSatelliteId(SatId sourceSatelliteId) {
        this.sourceSatelliteId = sourceSatelliteId;
    }

    public Short getAisPositionAccuracy() {
        return aisPositionAccuracy;
    }

    public void setAisPositionAccuracy(Short aisPositionAccuracy) {
        this.aisPositionAccuracy = aisPositionAccuracy;
    }

    public Double getCalculatedSpeed() {
        return calculatedSpeed;
    }

    public void setCalculatedSpeed(Double calculatedSpeed) {
        this.calculatedSpeed = calculatedSpeed;
    }

    @Override
    public int compareTo(Movement o) {
        if (o == null) {
            return ObjectUtils.compare(this, null);
        } else {
            return ObjectUtils.compare(this.getTimestamp(), o.getTimestamp());
        }
    }

}
