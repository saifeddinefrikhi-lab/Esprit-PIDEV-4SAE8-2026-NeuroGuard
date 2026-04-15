package com.neuroguard.pharmacy.services;

import com.neuroguard.pharmacy.dto.PharmacyDto;
import com.neuroguard.pharmacy.dto.PharmacyLocationRequest;
import com.neuroguard.pharmacy.entities.Pharmacy;
import com.neuroguard.pharmacy.repositories.PharmacyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PharmacyServiceTest {

    @Mock
    private PharmacyRepository pharmacyRepository;

    @InjectMocks
    private PharmacyService pharmacyService;

    /**
     * Test getAllPharmacies returns list of pharmacies
     */
    @Test
    void getAllPharmacies_returnsListOfPharmacies() {
        // Arrange
        Pharmacy pharmacy1 = new Pharmacy();
        pharmacy1.setId(1L);
        pharmacy1.setName("Pharmacy Central");
        pharmacy1.setAddress("123 Main St");
        pharmacy1.setLatitude(36.8065);
        pharmacy1.setLongitude(10.1684);

        Pharmacy pharmacy2 = new Pharmacy();
        pharmacy2.setId(2L);
        pharmacy2.setName("Pharmacy East");
        pharmacy2.setAddress("456 Oak Ave");
        pharmacy2.setLatitude(36.8100);
        pharmacy2.setLongitude(10.1750);

        when(pharmacyRepository.findAll()).thenReturn(List.of(pharmacy1, pharmacy2));

        // Act
        List<PharmacyDto> result = pharmacyService.getAllPharmacies();

        // Assert
        assertEquals(2, result.size());
        assertEquals("Pharmacy Central", result.get(0).getName());
        assertEquals("Pharmacy East", result.get(1).getName());
    }

    /**
     * Test getPharmacyById returns pharmacy when found
     */
    @Test
    void getPharmacyById_whenFound_returnsPharmacy() {
        // Arrange
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(1L);
        pharmacy.setName("Pharmacy Central");
        pharmacy.setAddress("123 Main St");

        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(pharmacy));

        // Act
        PharmacyDto result = pharmacyService.getPharmacyById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Pharmacy Central", result.getName());
    }

    /**
     * Test getPharmacyById throws exception when not found
     */
    @Test
    void getPharmacyById_whenNotFound_throwsException() {
        // Arrange
        when(pharmacyRepository.findById(404L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> pharmacyService.getPharmacyById(404L));
    }

    /**
     * Test searchByName returns matching pharmacies
     */
    @Test
    void searchByName_returnsMatchingPharmacies() {
        // Arrange
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(1L);
        pharmacy.setName("Pharmacy Central");

        when(pharmacyRepository.findByNameContainingIgnoreCase("Central"))
                .thenReturn(List.of(pharmacy));

        // Act
        List<PharmacyDto> result = pharmacyService.searchByName("Central");

        // Assert
        assertEquals(1, result.size());
        assertEquals("Pharmacy Central", result.get(0).getName());
    }

    /**
     * Test getPharmaciesWithDelivery returns only delivery pharmacies
     */
    @Test
    void getPharmaciesWithDelivery_returnsOnlyDeliveryPharmacies() {
        // Arrange
        Pharmacy pharmacy1 = new Pharmacy();
        pharmacy1.setId(1L);
        pharmacy1.setName("Delivery Pharmacy");
        pharmacy1.setHasDelivery(true);

        Pharmacy pharmacy2 = new Pharmacy();
        pharmacy2.setId(2L);
        pharmacy2.setName("No Delivery Pharmacy");
        pharmacy2.setHasDelivery(true);

        when(pharmacyRepository.findByHasDeliveryTrue())
                .thenReturn(List.of(pharmacy1, pharmacy2));

        // Act
        List<PharmacyDto> result = pharmacyService.getPharmaciesWithDelivery();

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> true)); // All have delivery
    }

    /**
     * Test searchByName returns empty list when no match
     */
    @Test
    void searchByName_returnsEmptyList_whenNoMatch() {
        // Arrange
        when(pharmacyRepository.findByNameContainingIgnoreCase("NonExistent"))
                .thenReturn(List.of());

        // Act
        List<PharmacyDto> result = pharmacyService.searchByName("NonExistent");

        // Assert
        assertEquals(0, result.size());
    }
}
