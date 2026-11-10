package com.platform.appliance;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplianceTypeRepositoryTest {

    @Mock
    private ApplianceTypeMapper applianceTypeMapper;

    @Test
    void mapperXmlDefinesBothApplianceTypeQueries() throws Exception {
        Configuration configuration = new Configuration();
        configuration.addMapper(ApplianceTypeMapper.class);
        try (InputStream input = Resources.getResourceAsStream("mapper/appliance/ApplianceTypeMapper.xml")) {
            new XMLMapperBuilder(input, configuration, "mapper/appliance/ApplianceTypeMapper.xml",
                    configuration.getSqlFragments()).parse();
        }

        assertTrue(configuration.hasStatement("com.platform.appliance.ApplianceTypeMapper.findActiveByCode"));
        assertTrue(configuration.hasStatement("com.platform.appliance.ApplianceTypeMapper.findAllActive"));
    }

    @Test
    void findActiveByCodeWrapsMapperResult() {
        ApplianceType type = new ApplianceType(UUID.randomUUID(), "WASHER", "Washer");
        when(applianceTypeMapper.findActiveByCode("WASHER")).thenReturn(List.of(type));

        assertEquals(Optional.of(type), new ApplianceTypeRepository(applianceTypeMapper).findActiveByCode("WASHER"));
    }

    @Test
    void findAllActiveReturnsMapperResults() {
        List<ApplianceTypeResponse> activeTypes = List.of(new ApplianceTypeResponse("WASHER", "Washer"));
        when(applianceTypeMapper.findAllActive()).thenReturn(activeTypes);

        assertEquals(activeTypes, new ApplianceTypeRepository(applianceTypeMapper).findAllActive());
    }
}
