package com.platform.appliance;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApplianceTypeMapper {

    List<ApplianceType> findActiveByCode(@Param("code") String code);

    List<ApplianceTypeResponse> findAllActive();
}
