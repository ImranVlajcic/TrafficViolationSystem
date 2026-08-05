package com.academy.trafficviolationsystem.core.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource does not exist in the database.
 * Produces HTTP 404 Not Found.
 *
 * BaseService.findEntityById() throws this automatically when a repository
 * findById() returns empty. Domain services can also throw it directly:
 *
 *   driverRepository.findByLicenseNumber(plate)
 *       .orElseThrow(() -> new NotFoundException("No driver found for plate " + plate));
 */
public class NotFoundException extends AppException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
