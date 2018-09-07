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
package eu.europa.ec.fisheries.uvms.movement.entity.area;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import eu.europa.ec.fisheries.uvms.movement.constant.UvmsConstants;
import eu.europa.ec.fisheries.uvms.movement.model.MovementInstantDeserializer;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 **/
@Entity
@Table(name = "area")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = UvmsConstants.AREA_FIND_ALL, query = "SELECT a FROM Area a"),
    @NamedQuery(name = "Area.findByRemoteId", query = "SELECT a FROM Area a where a.remoteId =:remoteId"),
    @NamedQuery(name = "Area.findByCode", query = "SELECT a FROM Area a where a.areaCode=:code"),
    @NamedQuery(name = "Area.findByRemoteIdAndCode", query = "SELECT a FROM Area a where a.remoteId =:remoteId AND a.areaCode =:code")
})
@DynamicUpdate
@DynamicInsert
public class Area implements Serializable {
    @OneToMany(mappedBy = "areatranAreaId", fetch = FetchType.LAZY)
    private List<Areatransition> areatransitionList;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Basic(optional = false)
    @Column(name = "area_id")
    private Long areaId;

    @Basic(optional = false)
    @Column(name = "area_name")
    private String areaName;

    @Column(name = "area_code", unique = true)
    private String areaCode;

    @Basic(optional = false)
    @Size(min = 1, max = 60)
    @Column(name = "area_remoteid")
    private String remoteId;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = MovementInstantDeserializer.class)
    @NotNull
    @Column(name = "area_updattim")
    private Instant areaUpdattim;

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 60)
    @Column(name = "area_upuser")
    private String areaUpuser;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "movareaAreaId", fetch = FetchType.LAZY)
    private List<Movementarea> movementareaList;

    @JoinColumn(name = "area_areatype_id", referencedColumnName = "areatype_id")
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AreaType areaType;

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getRemoteId() {
        return remoteId;
    }

    public void setRemoteId(String remoteId) {
        this.remoteId = remoteId;
    }

    public Instant getAreaUpdattim() {
        return areaUpdattim;
    }

    public void setAreaUpdattim(Instant areaUpdattim) {
        this.areaUpdattim = areaUpdattim;
    }

    public String getAreaUpuser() {
        return areaUpuser;
    }

    public void setAreaUpuser(String areaUpuser) {
        this.areaUpuser = areaUpuser;
    }

    public List<Movementarea> getMovementareaList() {
        return movementareaList;
    }

    public void setMovementareaList(List<Movementarea> movementareaList) {
        this.movementareaList = movementareaList;
    }

    public AreaType getAreaType() {
        return areaType;
    }

    public void setAreaType(AreaType areaType) {
        this.areaType = areaType;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public Area() {
    }

    @XmlTransient
    public List<Areatransition> getAreatransitionList() {
        return areatransitionList;
    }

    public void setAreatransitionList(List<Areatransition> areatransitionList) {
        this.areatransitionList = areatransitionList;
    }

}