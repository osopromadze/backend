package science.icebreaker.device_availability;

import javax.validation.Valid;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ApiResponse;
import science.icebreaker.account.Account;
import science.icebreaker.device_availability.Exceptions.DeviceAvailabilityCreationException;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/device-availability")
public class DeviceAvailabilityController {

    private DeviceAvailabilityService service;

    public DeviceAvailabilityController(DeviceAvailabilityService service) {
        this.service = service;
    }

    @PostMapping("/")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Availability entry added"),
        @ApiResponse(code = 422, message = "Device with specified ID not found")
    })
    public void addDeviceAvailability(
        @RequestBody @Valid AddDeviceAvailabilityRequest addDeviceAvailabilityRequest,
        @ApiIgnore UsernamePasswordAuthenticationToken principal
    ) throws DeviceAvailabilityCreationException {
        service.addDeviceAvailability(
            addDeviceAvailabilityRequest.getDeviceId(),
            addDeviceAvailabilityRequest.getComment(),
            addDeviceAvailabilityRequest.getGermanPostalCode(),
            addDeviceAvailabilityRequest.getInstitution(),
            addDeviceAvailabilityRequest.getResearchGroup(),
            (Account)principal.getPrincipal()
        );
    }
}